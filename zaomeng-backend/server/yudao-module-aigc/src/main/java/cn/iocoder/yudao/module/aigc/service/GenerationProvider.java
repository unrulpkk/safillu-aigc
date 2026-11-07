package cn.iocoder.yudao.module.aigc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

/** Credential-bearing requests only ever go to the configured Safillu origin. */
@Component
public class GenerationProvider {
    private final ObjectMapper json;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NEVER).build();
    public GenerationProvider(ObjectMapper json){this.json=json;}
    public static class ProviderException extends RuntimeException {
        public final String code;public final boolean uncertain;
        ProviderException(String code,String message,boolean uncertain){super(message);this.code=code;this.uncertain=uncertain;}
    }
    public record Media(byte[] bytes,String mime) {}
    public JsonNode call(String base,String secret,String method,String path,Object body,String requestKey) throws Exception {
        URI uri=URI.create(base+path);
        if(!"https".equals(uri.getScheme())||!"api.safillu.com".equals(uri.getHost())||uri.getPort()>0)throw new IllegalStateException("模型服务地址未获配置");
        if(!"file:/run/secrets/provider_key".equals(secret))throw new IllegalStateException("模型密钥引用错误");
        String key=Files.readString(Path.of("/run/secrets/provider_key")).trim();
        var builder=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(180)).header("Authorization","Bearer "+key).header("Content-Type","application/json").header("Idempotency-Key",requestKey);
        builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        HttpResponse<InputStream> response;
        try{response=http.send(builder.build(),HttpResponse.BodyHandlers.ofInputStream());}
        catch(Exception e){throw new ProviderException("PROVIDER_UNCERTAIN","模型服务连接中断，结果尚未确认；本任务不会自动重复提交",true);}
        byte[] raw;try(var stream=response.body()){raw=stream.readNBytes(16*1024*1024+1);}
        if(raw.length>16*1024*1024)throw new ProviderException("PROVIDER_RESPONSE_TOO_LARGE","模型返回的数据过大",false);
        JsonNode result;
        try{result=json.readTree(raw);}catch(Exception e){throw new ProviderException("PROVIDER_BAD_RESPONSE","模型服务返回了无法识别的响应",false);}
        if(response.statusCode()>=400||result.hasNonNull("error")&&!result.path("error").asText().isBlank()){
            String providerCode=result.path("error").path("code").asText("");
            if(providerCode.equals("model_not_found"))throw new ProviderException("MODEL_CHANNEL_UNAVAILABLE","当前模型在服务商分组中没有可用渠道，请更换模型或联系管理员",false);
            if(response.statusCode()==401||response.statusCode()==403)throw new ProviderException("PROVIDER_AUTH","模型服务鉴权失败，请联系管理员检查密钥与分组",false);
            if(response.statusCode()==429)throw new ProviderException("PROVIDER_RATE_LIMIT","模型服务繁忙或额度不足，请稍后重新生成",false);
            throw new ProviderException("PROVIDER_HTTP_"+response.statusCode(),"模型服务拒绝请求（HTTP "+response.statusCode()+"），积分会退回",false);
        }
        return result;
    }
    public String jobId(JsonNode result){
        for(String name:List.of("task_id","taskId","id"))if(result.path(name).isValueNode()&&!result.path(name).asText().isBlank())return result.path(name).asText();
        if(result.path("data").isTextual()&&result.path("data").asText().startsWith("task_"))return result.path("data").asText();if(result.path("data").isObject())return jobId(result.path("data"));return null;
    }
    public String status(JsonNode result){
        String s=result.path("status").asText(result.path("state").asText(""));
        if(s.isEmpty()&&result.path("data").isObject())return status(result.path("data"));return s.toUpperCase(Locale.ROOT);
    }
    public Media artifact(String base,String secret,String path) throws Exception {
        if(!base.equals("https://api.safillu.com")||!secret.equals("file:/run/secrets/provider_key")||!path.matches("/v1/tasks/task_[A-Za-z0-9]+/artifacts/(video|audio|image|image_[1-4])/content"))throw new ProviderException("BAD_ARTIFACT","生成结果编号不合法",false);
        String key=Files.readString(Path.of("/run/secrets/provider_key")).trim();
        var response=http.send(HttpRequest.newBuilder(URI.create(base+path)).timeout(Duration.ofSeconds(180)).header("Authorization","Bearer "+key).GET().build(),HttpResponse.BodyHandlers.ofInputStream());
        try(var stream=response.body()){
            if(response.statusCode()!=200)throw new ProviderException("OUTPUT_DOWNLOAD_FAILED","生成完成，文件归档暂时失败",false);
            byte[] bytes=stream.readNBytes(50*1024*1024+1);if(bytes.length==0||bytes.length>50*1024*1024)throw new ProviderException("OUTPUT_TOO_LARGE","生成文件为空或超过 50MB",false);
            return new Media(bytes,response.headers().firstValue("content-type").orElse("application/octet-stream").split(";")[0]);
        }
    }
}
