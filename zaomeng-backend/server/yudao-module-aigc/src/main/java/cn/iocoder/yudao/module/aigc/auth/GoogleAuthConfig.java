package cn.iocoder.yudao.module.aigc.auth;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
public record GoogleAuthConfig(String clientId,String clientSecret,String redirectUri,String returnOrigin){
 public static GoogleAuthConfig load(){var env=System.getenv();return new GoogleAuthConfig(read(env.getOrDefault("GOOGLE_CLIENT_ID_FILE","/run/secrets/google/client_id")),read(env.getOrDefault("GOOGLE_CLIENT_SECRET_FILE","/run/secrets/google/client_secret")),env.getOrDefault("GOOGLE_REDIRECT_URI","http://localhost:8087/app-api/member/auth/google-login"),env.getOrDefault("GOOGLE_RETURN_ORIGIN","http://localhost:8088"));}
 private static String read(String file){try{return Files.readString(Path.of(file)).trim();}catch(Exception e){return "";}}
 public static boolean safeOrigin(URI u,boolean allowPrivate){if(u.getUserInfo()!=null||u.getHost()==null||u.getFragment()!=null||u.getRawQuery()!=null)return false;boolean local=Set.of("localhost","127.0.0.1","[::1]").contains(u.getHost());return "https".equals(u.getScheme())||"http".equals(u.getScheme())&&(local||allowPrivate&&u.getHost().matches("192[.]168[.][0-9]+[.][0-9]+"));}
 public String disabledReason(){if(!clientId.matches("[A-Za-z0-9_-]+[.]apps[.]googleusercontent[.]com")||clientSecret.isBlank())return "Google 登录配置尚未完成";try{URI callback=URI.create(redirectUri),origin=URI.create(returnOrigin);if(!safeOrigin(callback,false)||!Set.of("/app-api/member/auth/google-login","/admin-api/aigc/auth/google/callback").contains(callback.getPath()))return "Google 回调地址配置错误";boolean dev=Set.of("localhost","127.0.0.1").contains(callback.getHost());if(!safeOrigin(origin,dev)||origin.getPath()!=null&&!Set.of("","/").contains(origin.getPath()))return "Google 工作台返回地址配置错误";}catch(Exception e){return "Google 地址配置错误";}return "";}
 public boolean enabled(){return disabledReason().isEmpty();}
 public String origin(){return returnOrigin.replaceAll("/+$","");}
 @Override public String toString(){return "GoogleAuthConfig[credentials=redacted]";}
}
