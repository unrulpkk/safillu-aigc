package cn.iocoder.yudao.module.aigc.auth;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.function.LongSupplier;
public class GoogleIdentityStore {
 public static class AuthError extends RuntimeException {public final int code;public AuthError(int code,String message){super(message);this.code=code;}}
 private final JdbcTemplate db;private final TransactionTemplate tx;
 public GoogleIdentityStore(JdbcTemplate db,PlatformTransactionManager manager){this.db=db;tx=new TransactionTemplate(manager);tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);}
 private void active(long id){var users=db.queryForList("SELECT status FROM system_users WHERE tenant_id=1 AND id=? AND deleted=0",id);if(users.isEmpty()||((Number)users.getFirst().get("status")).intValue()!=0)throw new AuthError(403,"账号不存在或已停用");}
 public long resolve(GoogleIdentityVerifier.Profile profile,Long owner,LongSupplier register){return tx.execute(status->{
  db.update("INSERT INTO system_social_user(tenant_id,type,openid,raw_token_info,nickname,raw_user_info,code,creator) VALUES(1,50,?,'{}',?,'{}','','google-oidc') ON DUPLICATE KEY UPDATE id=id",profile.subject(),profile.name());
  var identity=db.queryForMap("SELECT id FROM system_social_user WHERE tenant_id=1 AND aigc_google_subject=? FOR UPDATE",profile.subject());long social=((Number)identity.get("id")).longValue();
  var bindings=db.queryForList("SELECT user_id FROM system_social_user_bind WHERE tenant_id=1 AND user_type=2 AND aigc_google_identity=? FOR UPDATE",social);
  if(!bindings.isEmpty()){long user=((Number)bindings.getFirst().get("user_id")).longValue();if(owner!=null&&user!=owner)throw new AuthError(409,"这个 Google 账号已关联其他账号");active(user);return user;}
  long user;if(owner!=null){active(owner);var prior=db.queryForList("SELECT social_user_id FROM system_social_user_bind WHERE tenant_id=1 AND user_type=2 AND aigc_google_owner=? FOR UPDATE",owner);if(!prior.isEmpty())throw new AuthError(409,"当前账号已关联其他 Google 账号");user=owner;}else user=register.getAsLong();
  db.update("INSERT INTO system_social_user_bind(tenant_id,user_type,social_type,social_user_id,user_id,creator) VALUES(1,2,50,?,?,?)",social,user,String.valueOf(user));return user;
 });}
 public Map<String,Object> linked(long owner){var rows=db.queryForList("SELECT s.nickname FROM system_social_user_bind b JOIN system_social_user s ON s.id=b.social_user_id AND s.tenant_id=b.tenant_id WHERE b.tenant_id=1 AND b.user_type=2 AND b.user_id=? AND b.social_type=50 AND b.deleted=0 AND s.deleted=0",owner);return rows.isEmpty()?Map.of("linked",false):Map.of("linked",true,"name",Objects.toString(rows.getFirst().get("nickname"),"Google 用户"));}
}
