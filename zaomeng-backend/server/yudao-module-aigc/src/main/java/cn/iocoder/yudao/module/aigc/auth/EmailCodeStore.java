package cn.iocoder.yudao.module.aigc.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import static cn.iocoder.yudao.module.aigc.auth.GoogleIdentityStore.AuthError;

/** One-use registration codes. Redis contains only keyed digests, never the codes. */
@Component
public class EmailCodeStore {
    public static final int EXPIRES_SECONDS = 300, RESEND_SECONDS = 60;
    private final StringRedisTemplate redis;
    private final byte[] secret;
    private final SecureRandom random = new SecureRandom();
    private static final DefaultRedisScript<Long> ISSUE = new DefaultRedisScript<>("""
        if redis.call('exists',KEYS[2])==1 then return 0 end
        local a=redis.call('incr',KEYS[3]); if a==1 then redis.call('expire',KEYS[3],3600) end
        local b=redis.call('incr',KEYS[4]); if b==1 then redis.call('expire',KEYS[4],3600) end
        if a>5 or b>20 then return -1 end
        redis.call('set',KEYS[2],ARGV[1],'EX',60)
        redis.call('del',KEYS[1])
        redis.call('hset',KEYS[1],'digest',ARGV[1],'attempts',0)
        redis.call('expire',KEYS[1],300)
        return 1
        """, Long.class);
    private static final DefaultRedisScript<Long> VERIFY = new DefaultRedisScript<>("""
        local d=redis.call('hget',KEYS[1],'digest')
        if not d then return 0 end
        if d==ARGV[1] then redis.call('del',KEYS[1]); return 1 end
        if redis.call('hincrby',KEYS[1],'attempts',1)>=5 then redis.call('del',KEYS[1]) end
        return 0
        """, Long.class);
    private static final DefaultRedisScript<Long> REVOKE = new DefaultRedisScript<>("""
        if redis.call('hget',KEYS[1],'digest')==ARGV[1] then redis.call('del',KEYS[1]) end
        if redis.call('get',KEYS[2])==ARGV[1] then redis.call('del',KEYS[2]) end
        return 1
        """, Long.class);
    private static final DefaultRedisScript<Long> LOGIN_LIMIT = new DefaultRedisScript<>("""
        local a=redis.call('incr',KEYS[1]); if a==1 then redis.call('expire',KEYS[1],600) end
        local b=redis.call('incr',KEYS[2]); if b==1 then redis.call('expire',KEYS[2],600) end
        if a>20 or b>100 then return 0 end return 1
        """, Long.class);

    public EmailCodeStore(StringRedisTemplate redis, @Value("${mybatis-plus.encryptor.password}") String secret) {
        this.redis = redis;
        if (secret == null || secret.length() < 16) throw new IllegalStateException("Email verification requires DATA_ENCRYPTION_KEY");
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }
    String digest(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(("safillu:email:v1:" + value).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException("Email verification unavailable"); }
    }
    String key(String email) { return "safillu:email:register:" + digest(email); }
    public String issue(String email, String ip) {
        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        String key = key(email), hash = digest(email + ":" + code);
        Long result = redis.execute(ISSUE, List.of(key, key + ":cooldown", key + ":hour", "safillu:email:ip:" + digest(ip)), hash);
        if (!Long.valueOf(1).equals(result)) throw new AuthError(429, "发送过于频繁，请稍后重试");
        return code;
    }
    public void revoke(String email, String code) {
        String key = key(email);
        redis.execute(REVOKE, List.of(key, key + ":cooldown"), digest(email + ":" + code));
    }
    public void consume(String email, String code) {
        if (!Long.valueOf(1).equals(redis.execute(VERIFY, List.of(key(email)), digest(email + ":" + code))))
            throw new AuthError(400, "验证码错误或已过期，请重新获取");
    }
    public void limitLogin(String account, String ip) {
        if (!Long.valueOf(1).equals(redis.execute(LOGIN_LIMIT, List.of("safillu:email:login:" + digest(account), "safillu:email:login-ip:" + digest(ip)))))
            throw new AuthError(429, "登录尝试过于频繁，请稍后重试");
    }
}
