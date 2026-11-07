package cn.iocoder.yudao.module.aigc.payment;

import java.net.URI;
import java.nio.file.*;
import java.util.Map;

/** Deployment-owned configuration; never serialised to browser responses. */
public record StripePaymentConfig(String environment,String secretKey,String webhookSecret,String returnOrigin,String publicWebhookUrl,boolean liveEnabled) {
    public static StripePaymentConfig load(){return load(System.getenv());}
    public static StripePaymentConfig load(Map<String,String> env){return new StripePaymentConfig(env.getOrDefault("STRIPE_MODE","TEST").toUpperCase(),secret(env.getOrDefault("STRIPE_SECRET_KEY_FILE","/run/secrets/stripe/secret_key")),secret(env.getOrDefault("STRIPE_WEBHOOK_SECRET_FILE","/run/secrets/stripe/webhook_secret")),env.getOrDefault("STRIPE_RETURN_ORIGIN","http://localhost:8088"),env.getOrDefault("STRIPE_PUBLIC_WEBHOOK_URL",""),Boolean.parseBoolean(env.getOrDefault("STRIPE_LIVE_ENABLED","false")));}
    private static String secret(String path){try{return Files.readString(Path.of(path)).trim();}catch(Exception ignored){return "";}}
    public boolean live(){return environment.equals("LIVE");}
    public String disabledReason(){
        if(!environment.equals("TEST")&&!live())return "Stripe 环境配置错误";
        if(secretKey.isEmpty()||webhookSecret.isEmpty())return "Stripe 收款账户与回调签名密钥尚未配置";
        if(!secretKey.matches("[sr]k_"+(live()?"live":"test")+"_[A-Za-z0-9]+")||!webhookSecret.startsWith("whsec_"))return "Stripe 密钥与当前环境不匹配";
        try{URI origin=URI.create(returnOrigin);if(origin.getHost()==null||origin.getUserInfo()!=null||origin.getRawQuery()!=null||origin.getFragment()!=null||origin.getPath()!=null&&!origin.getPath().isEmpty()&&!origin.getPath().equals("/"))return "支付返回地址配置错误";
            if(live()&&!origin.getScheme().equals("https"))return "正式收款的返回地址必须使用 HTTPS";
            if(!live()&&!java.util.Set.of("http","https").contains(origin.getScheme()))return "支付返回地址配置错误";
            if(live()){URI hook=URI.create(publicWebhookUrl);if(!liveEnabled||!"https".equals(hook.getScheme())||hook.getHost()==null||!hook.getPath().equals("/admin-api/aigc/payments/stripe/webhook"))return "正式收款尚未启用或缺少公网 HTTPS 回调配置";}
        }catch(Exception e){return "Stripe 地址配置错误";}
        return "";
    }
    public boolean enabled(){return disabledReason().isEmpty();}
    public String safeReturnOrigin(){return returnOrigin.replaceAll("/+$","");}
    @Override public String toString(){return "StripePaymentConfig[environment="+environment+", credentials=redacted]";}
}
