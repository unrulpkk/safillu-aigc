package cn.iocoder.yudao.module.aigc.service;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.aigc.auth.EmailCodeStore;
import cn.iocoder.yudao.module.aigc.auth.RegistrationMailSender;
import cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthLoginReqVO;
import cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthLoginRespVO;
import cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthRegisterReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.profile.UserProfileUpdateReqVO;
import cn.iocoder.yudao.module.system.service.auth.AdminAuthService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import static cn.iocoder.yudao.module.aigc.auth.GoogleIdentityStore.AuthError;

@Service
public class EmailAuthService {
    private final EmailCodeStore codes;
    private final RegistrationMailSender mail;
    private final AdminUserService users;
    private final AdminAuthService auth;
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    public EmailAuthService(EmailCodeStore codes, RegistrationMailSender mail, AdminUserService users,
                           AdminAuthService auth, JdbcTemplate db, PlatformTransactionManager manager) {
        this.codes=codes; this.mail=mail; this.users=users; this.auth=auth; this.db=db;
        tx=new TransactionTemplate(manager);
    }
    static String email(String input) { return input.strip().toLowerCase(Locale.ROOT); }
    private <T> T tenant(Supplier<T> action) {
        Long old=TenantContextHolder.getTenantId(); boolean ignored=TenantContextHolder.isIgnore();
        try { TenantContextHolder.setTenantId(1L); TenantContextHolder.setIgnore(false); return action.get(); }
        finally { TenantContextHolder.setTenantId(old); TenantContextHolder.setIgnore(ignored); }
    }
    private boolean exists(String email) {
        return !db.queryForList("SELECT user_id FROM aigc_email_identity WHERE tenant_id=1 AND email=?", email).isEmpty()
            || !db.queryForList("SELECT id FROM system_users WHERE tenant_id=1 AND deleted=0 AND LOWER(email)=? LIMIT 1", email).isEmpty();
    }
    public Map<String,Integer> sendCode(String address, String locale, String ip) {
        return tenant(() -> {
            String email=email(address);
            if (exists(email)) throw new AuthError(409, "邮箱已注册，请直接登录");
            var account=mail.account();
            String code=codes.issue(email,ip);
            try { mail.send(account,email,code,locale); }
            catch (RuntimeException e) { codes.revoke(email,code); throw e; }
            return Map.of("expiresIn",EmailCodeStore.EXPIRES_SECONDS,"retryAfter",EmailCodeStore.RESEND_SECONDS);
        });
    }
    public AuthLoginRespVO register(String address, String code, String password) {
        return tenant(() -> {
            String email=email(address);
            if (exists(email)) throw new AuthError(409,"邮箱已注册，请直接登录");
            codes.consume(email,code);
            Long id;
            try {
                id=tx.execute(status -> {
                    if (exists(email)) throw new AuthError(409,"邮箱已注册，请直接登录");
                    var registration=new AuthRegisterReqVO();
                    registration.setUsername("e"+UUID.randomUUID().toString().replace("-", "").substring(0,24));
                    registration.setNickname(email.substring(0,email.indexOf('@')).substring(0,Math.min(30,email.indexOf('@'))));
                    registration.setPassword(password);
                    var user=users.registerUser(registration);
                    var profile=new UserProfileUpdateReqVO(); profile.setEmail(email);
                    users.updateUserProfile(user.getId(),profile);
                    db.update("INSERT INTO aigc_email_identity(tenant_id,email,user_id) VALUES(1,?,?)",email,user.getId());
                    return user.getId();
                });
            } catch (DuplicateKeyException e) { throw new AuthError(409,"邮箱已注册，请直接登录"); }
            return auth.loginVerifiedSocialUser(id);
        });
    }
    public AuthLoginRespVO login(String account, String password, String ip) {
        return tenant(() -> {
            String value=account.strip(); codes.limitLogin(value.toLowerCase(Locale.ROOT),ip);
            String username=value;
            if (value.contains("@")) {
                var rows=db.queryForList("SELECT u.username FROM aigc_email_identity e JOIN system_users u ON u.id=e.user_id AND u.tenant_id=e.tenant_id WHERE e.tenant_id=1 AND e.email=? AND u.deleted=0",email(value));
                if (rows.size()!=1) throw new AuthError(400,"邮箱或密码不正确");
                username=rows.getFirst().get("username").toString();
            } else if (!value.matches("[A-Za-z0-9]{4,30}")) throw new AuthError(400,"邮箱或密码不正确");
            var request=new AuthLoginReqVO(); request.setUsername(username); request.setPassword(password);
            return auth.login(request);
        });
    }
}
