package cn.iocoder.yudao.module.aigc.config;

import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClient;
import cn.iocoder.yudao.module.infra.service.file.FileConfigService;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.config.FileConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.framework.file.core.client.s3.S3FileClientConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

/** Explicit, verified activation. Existing files keep their original storage config ID. */
@Component
public class ObjectStorageBootstrap implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(ObjectStorageBootstrap.class);
    private final FileConfigService configs;
    private final Map<String, String> environment;
    private final Path secrets;
    private final Path logs;
    private final PublicProbeReader publicReader;

    @FunctionalInterface
    interface PublicProbeReader { byte[] read(String url) throws Exception; }

    static byte[] probeBody() {
        return Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jx1sAAAAASUVORK5CYII=");
    }

    @Autowired
    public ObjectStorageBootstrap(FileConfigService configs) {
        this(configs, System.getenv(), Path.of("/run/secrets/storage"), Path.of("/app/logs"), ObjectStorageBootstrap::readPublicProbe);
    }

    ObjectStorageBootstrap(FileConfigService configs, Map<String, String> environment, Path secrets, Path logs, PublicProbeReader publicReader) {
        this.configs = configs;
        this.environment = Map.copyOf(environment);
        this.secrets = secrets;
        this.logs = logs;
        this.publicReader = publicReader;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        boolean qiniu = Boolean.parseBoolean(environment.getOrDefault("QINIU_STORAGE_BOOTSTRAP", "false"));
        String r2Mode = environment.getOrDefault("R2_STORAGE_BOOTSTRAP", "auto");
        boolean auto = "auto".equalsIgnoreCase(r2Mode);
        boolean r2 = Boolean.parseBoolean(r2Mode) || auto && hasR2Configuration();
        // With automatic selection, configured R2 takes priority over Qiniu.
        if (auto && r2) qiniu = false;
        if (!qiniu && !r2) return;
        if (qiniu && r2) throw new IllegalStateException("Enable only one storage bootstrap provider");
        long configId = r2 ? 880004L : 880002L;
        String provider = r2 ? "r2" : "qiniu";
        Path result = logs.resolve(provider + "-storage-bootstrap.json");
        String object = "aigc/system/probe/" + UUID.randomUUID() + ".png";
        FileClient file = null;
        boolean uploaded = false;
        try {
            if (r2) registerR2Configuration();
            file = configs.getFileClient(configId);
            byte[] bytes = probeBody();
            String publicUrl = file.upload(bytes, object, "image/png");
            uploaded = true;
            if (!Arrays.equals(bytes, file.getContent(object))) throw new IllegalStateException("Storage roundtrip mismatch");
            if (r2 && !Arrays.equals(bytes, publicReader.read(publicUrl))) throw new IllegalStateException("R2 public image URL verification failed");
            file.delete(object);
            uploaded = false;
            configs.updateFileConfigMaster(configId);
            Files.writeString(result, "{\"active\":true,\"roundtripVerified\":true,\"configId\":" + configId + "}");
            LOG.info("{} storage activated after upload/read/delete verification", provider);
        } catch (Exception error) {
            // Provider messages/URLs can contain keys or signatures. Store only the type.
            Files.writeString(result, "{\"active\":false,\"roundtripVerified\":false,\"errorType\":\""
                    + error.getClass().getSimpleName() + "\"}");
            LOG.warn("{} storage activation failed ({}); existing master retained", provider, error.getClass().getSimpleName());
        } finally {
            if (uploaded && file != null) try { file.delete(object); } catch (Exception ignored) { }
        }
    }

    private boolean hasR2Configuration() {
        return Files.isRegularFile(secrets.resolve("r2/config.json"))
                && Files.isRegularFile(secrets.resolve("r2/access_key"))
                && Files.isRegularFile(secrets.resolve("r2/access_secret"));
    }

    private static byte[] readPublicProbe(String url) throws Exception {
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
        var response = http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).GET().build(),
                HttpResponse.BodyHandlers.ofInputStream());
        try (var input = response.body()) {
            if (response.statusCode() != 200) throw new IllegalStateException("R2 public image URL is not readable");
            return input.readNBytes(1024 * 1024);
        }
    }

    private void registerR2Configuration() throws Exception {
        Path path = secrets.resolve("r2/config.json");
        // Explicit activation also supports a configuration installed by the admin/CLI.
        if (!Files.isRegularFile(path)) return;
        if (Files.size(path) > 16384) throw new IllegalArgumentException("R2 configuration is too large");
        Map<String, Object> input = JsonUtils.parseObject(Files.readString(path),
                new TypeReference<Map<String, Object>>() {});
        String endpoint = Objects.toString(input.get("endpoint"), "");
        String bucket = Objects.toString(input.get("bucket"), "");
        String domain = Objects.toString(input.get("domain"), "").replaceAll("/+$", "");
        if (!endpoint.matches("https://[a-f0-9]{32}(?:\\.(?:eu|fedramp))?\\.r2\\.cloudflarestorage\\.com")
                || !bucket.matches("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]")) {
            throw new IllegalArgumentException("R2 endpoint or bucket is invalid");
        }
        URI publicDomain = URI.create(domain);
        if (!"https".equals(publicDomain.getScheme()) || publicDomain.getHost() == null
                || publicDomain.getUserInfo() != null || publicDomain.getRawQuery() != null
                || publicDomain.getFragment() != null || !publicDomain.getPath().isEmpty()
                || publicDomain.getHost().endsWith(".r2.cloudflarestorage.com")) {
            throw new IllegalArgumentException("R2 public HTTPS domain is required");
        }
        String accessKey = "file:/run/secrets/storage/r2/access_key";
        String accessSecret = "file:/run/secrets/storage/r2/access_secret";
        var existing = configs.getFileConfig(880004L);
        if (existing != null) {
            // Never repoint a config ID: historical files refer to its original bucket.
            if (!(existing.getConfig() instanceof S3FileClientConfig s3)
                    || !endpoint.equals(s3.getEndpoint()) || !bucket.equals(s3.getBucket())
                    || !accessKey.equals(s3.getAccessKey()) || !accessSecret.equals(s3.getAccessSecret())
                    || !domain.equals(s3.getDomain())
                    || Boolean.TRUE.equals(s3.getEncryptContent())
                    || !Boolean.TRUE.equals(s3.getEnablePublicAccess())) {
                throw new IllegalStateException("R2 config ID is already used by a different storage configuration");
            }
            return;
        }
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("endpoint", endpoint);
        settings.put("bucket", bucket);
        settings.put("region", "auto");
        settings.put("domain", domain);
        settings.put("accessKey", accessKey);
        settings.put("accessSecret", accessSecret);
        settings.put("enablePathStyleAccess", true);
        settings.put("enablePublicAccess", true);
        settings.put("encryptContent", false);
        Long created = configs.createFileConfig(new FileConfigSaveReqVO().setId(880004L)
                .setName("Cloudflare R2").setStorage(20).setConfig(settings)
                .setRemark("Configured using private storage files"));
        if (!Long.valueOf(880004L).equals(created)) throw new IllegalStateException("Unexpected R2 configuration ID");
    }
}
