package cn.iocoder.yudao.module.aigc.service;

import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileConfigDO;
import cn.iocoder.yudao.module.infra.framework.file.core.client.s3.S3FileClientConfig;
import cn.iocoder.yudao.module.infra.service.file.FileConfigService;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.util.Map;
import java.util.Base64;

/** Public plaintext references are prepared before any generation request is sent. */
@Service
public class GenerationReferenceStorage {
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/png", "png", "image/jpeg", "jpg", "image/webp", "webp");
    private final FileConfigService configs;
    private final FileService files;

    public GenerationReferenceStorage(FileConfigService configs, FileService files) {
        this.configs = configs;
        this.files = files;
    }

    public String imageUrl(long configId, String path, String mime, long tenant, long workspace, long task) {
        String extension = EXTENSIONS.get(mime);
        if (extension == null) throw failure("REFERENCE_FORMAT", "模型参考图仅支持 PNG、JPEG 和 WebP");
        return publicUrl(configId, path, mime, extension, 8 * 1024 * 1024, tenant, workspace, task);
    }

    /** The legacy Qwen edit route only accepts data URLs; new H3 routes use public URLs. */
    public String legacyImageDataUrl(long configId, String path, String mime) {
        if (!EXTENSIONS.containsKey(mime)) throw failure("REFERENCE_FORMAT", "模型参考图仅支持 PNG、JPEG 和 WebP");
        try {
            byte[] bytes=files.getFileContent(configId,path);
            if(bytes==null||bytes.length==0||bytes.length>8*1024*1024)throw failure("REFERENCE_UNAVAILABLE","参考素材不可用或超过大小限制");
            return "data:"+mime+";base64,"+Base64.getEncoder().encodeToString(bytes);
        }catch(GenerationProvider.ProviderException error){throw error;}
        catch(Exception error){throw failure("REFERENCE_UNAVAILABLE","参考素材不可用或超过大小限制");}
    }

    public String mediaUrl(long configId, String path, String mime, long tenant, long workspace, long task) {
        String extension = switch (mime) {
            case "audio/mpeg" -> "mp3";
            case "audio/wav", "audio/x-wav", "audio/wave", "audio/vnd.wave" -> "wav";
            case "audio/flac", "audio/x-flac" -> "flac";
            case "video/mp4" -> "mp4";
            case "video/quicktime" -> "mov";
            default -> throw failure("REFERENCE_FORMAT", "参考音视频仅支持 MP3、WAV、FLAC、MP4 和 MOV");
        };
        return publicUrl(configId, path, mime, extension, 50 * 1024 * 1024, tenant, workspace, task);
    }

    private String publicUrl(long configId, String path, String mime, String extension, int maxBytes,
                             long tenant, long workspace, long task) {
        try {
            FileConfigDO source = configs.getFileConfig(configId);
            if (isPublicPlaintext(source)) {
                return requirePublicUrl(configs.getFileClient(configId).presignGetUrl(path, null));
            }
            // Historical encrypted/local assets keep their original configuration and bytes.
            // Prepare a separate public copy on the currently selected R2/S3 storage.
            var target = configs.getMasterFileClient();
            if (target == null || !isPublicPlaintext(configs.getFileConfig(target.getId()))) {
                throw failure("REFERENCE_STORAGE_NOT_PUBLIC", "参考图片需要配置公开的 R2 或 S3 存储地址");
            }
            byte[] bytes = files.getFileContent(configId, path);
            if (bytes == null || bytes.length == 0 || bytes.length > maxBytes) {
                throw failure("REFERENCE_UNAVAILABLE", "参考素材不可用或超过大小限制");
            }
            String result = files.createFlatFile(bytes, "reference." + extension, mime);
            return requirePublicUrl(result);
        } catch (GenerationProvider.ProviderException error) {
            throw error;
        } catch (Exception error) {
            throw failure("REFERENCE_UPLOAD_FAILED", "参考图片上传失败，生成请求未发送，积分会退回");
        }
    }

    private boolean isPublicPlaintext(FileConfigDO config) {
        if (config == null || !(config.getConfig() instanceof S3FileClientConfig s3)
                || Boolean.TRUE.equals(s3.getEncryptContent())
                || !Boolean.TRUE.equals(s3.getEnablePublicAccess())) return false;
        try { requirePublicUrl(s3.getDomain()); return true; }
        catch (GenerationProvider.ProviderException ignored) { return false; }
    }

    private String requirePublicUrl(String value) {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            if (!"https".equals(uri.getScheme()) || host == null || uri.getUserInfo() != null
                    || uri.getRawQuery() != null || uri.getFragment() != null
                    || host.endsWith(".r2.cloudflarestorage.com") || host.equals("localhost")
                    || host.startsWith("127.") || host.startsWith("192.168.") || host.startsWith("10.")
                    || host.equals("[::1]") || host.matches("172\\.(1[6-9]|2[0-9]|3[01])\\..*")) {
                throw new IllegalArgumentException();
            }
            return value;
        } catch (Exception error) {
            throw failure("REFERENCE_URL_INVALID", "请配置无需鉴权即可读取的公开 HTTPS 图片地址");
        }
    }

    private GenerationProvider.ProviderException failure(String code, String message) {
        return new GenerationProvider.ProviderException(code, message, false);
    }
}
