package cn.iocoder.yudao.module.aigc.auth;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.util.*;
public class GoogleIdentityVerifier {
 public record Profile(String subject,String email,String name){}
 private final String audience;private final JwtDecoder decoder;
 public GoogleIdentityVerifier(String audience){this(audience,googleDecoder());}
 public GoogleIdentityVerifier(String audience,JwtDecoder decoder){this.audience=audience;this.decoder=decoder;}
 private static JwtDecoder googleDecoder(){var factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(5000);factory.setReadTimeout(8000);String proxy=System.getenv("GOOGLE_PROXY_HOST");if(proxy!=null&&!proxy.isBlank())factory.setProxy(new java.net.Proxy(java.net.Proxy.Type.HTTP,new java.net.InetSocketAddress(proxy,3129)));return NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").jwsAlgorithm(SignatureAlgorithm.RS256).restOperations(new RestTemplate(factory)).build();}
 public Profile verify(String token,String nonce){if(token==null||token.length()>20000)throw new IllegalArgumentException("Google 身份凭证无效");var jwt=decoder.decode(token);String issuer=jwt.getClaimAsString("iss"),subject=jwt.getSubject(),email=jwt.getClaimAsString("email");if(!Set.of("https://accounts.google.com","accounts.google.com").contains(issuer)||!jwt.getAudience().contains(audience)||!Objects.equals(nonce,jwt.getClaimAsString("nonce"))||nonce==null||subject==null||subject.isBlank()||subject.length()>255||!Boolean.TRUE.equals(jwt.getClaim("email_verified"))||email==null||email.length()>320||!email.contains("@"))throw new IllegalArgumentException("Google 身份信息校验失败");String azp=jwt.getClaimAsString("azp");if(azp!=null&&!audience.equals(azp))throw new IllegalArgumentException("Google 身份受众不匹配");String name=Objects.toString(jwt.getClaimAsString("name"),"Google 用户").strip();if(name.isBlank())name="Google 用户";return new Profile(subject,email,name.substring(0,Math.min(name.length(),30)));}
}
