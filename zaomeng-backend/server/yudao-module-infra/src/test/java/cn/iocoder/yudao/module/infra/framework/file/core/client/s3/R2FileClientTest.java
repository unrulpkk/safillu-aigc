package cn.iocoder.yudao.module.infra.framework.file.core.client.s3;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/** Local S3 protocol simulation, never a request to a real Cloudflare account. */
class R2FileClientTest {
    private S3FileClientConfig config(String endpoint) {
        S3FileClientConfig config = new S3FileClientConfig();
        config.setEndpoint(endpoint);
        config.setBucket("safillu-test");
        config.setAccessKey("mock-access-key");
        config.setAccessSecret("mock-secret-key");
        config.setEnablePathStyleAccess(true);
        config.setEnablePublicAccess(false);
        return config;
    }

    @Test
    void privateR2SignsApiEndpointWithAutoRegionEvenWithCdnDomain() {
        String endpoint = "https://0123456789abcdef0123456789abcdef.r2.cloudflarestorage.com";
        S3FileClientConfig config = config(endpoint);
        config.setDomain("https://cdn.example.invalid");
        S3FileClient client = new S3FileClient(880003L, config);
        client.init();
        URI url = URI.create(client.presignGetUrl("folder/图像 1.png", 60));
        assertEquals(URI.create(endpoint).getHost(), url.getHost());
        assertEquals("/safillu-test/folder/图像 1.png", url.getPath());
        assertTrue(URLDecoder.decode(url.getRawQuery(), StandardCharsets.UTF_8).contains("/auto/s3/aws4_request"));
        assertTrue(url.getRawQuery().contains("X-Amz-Expires=60"));
        assertEquals(URI.create(endpoint).getHost(), URI.create(client.presignPutUrl("upload.png")).getHost());
    }

    @Test
    void encryptedUploadReadDeleteRoundtripAgainstLocalS3Mock() throws Exception {
        Map<String, byte[]> objects = new ConcurrentHashMap<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", request -> {
            String auth = request.getRequestHeaders().getFirst("Authorization");
            if (auth == null || !auth.contains("/auto/s3/aws4_request")) {
                request.sendResponseHeaders(403, -1); request.close(); return;
            }
            String key = request.getRequestURI().getPath();
            switch (request.getRequestMethod()) {
                case "PUT" -> {
                    objects.put(key, request.getRequestBody().readAllBytes());
                    request.getResponseHeaders().add("ETag", "\"mock-etag\"");
                    request.sendResponseHeaders(200, -1);
                }
                case "GET" -> {
                    byte[] body = objects.get(key);
                    request.sendResponseHeaders(body == null ? 404 : 200, body == null ? -1 : body.length);
                    if (body != null) request.getResponseBody().write(body);
                }
                case "DELETE" -> { objects.remove(key); request.sendResponseHeaders(204, -1); }
                default -> request.sendResponseHeaders(405, -1);
            }
            request.close();
        });
        server.start();
        try {
            S3FileClientConfig config = config("http://127.0.0.1:" + server.getAddress().getPort());
            config.setRegion("auto");
            config.setEncryptContent(true);
            config.setEncryptionKeyReference(Base64.getEncoder().encodeToString(new byte[32]));
            S3FileClient client = new S3FileClient(880003L, config);
            client.init();
            String key = "folder/图像 1.png";
            byte[] original = "Safillu mock R2 payload".getBytes(StandardCharsets.UTF_8);
            client.upload(original, key, "image/png");
            byte[] stored = objects.get("/safillu-test/" + key);
            assertNotNull(stored);
            assertFalse(java.util.Arrays.equals(original, stored));
            assertArrayEquals(original, client.getContent(key));
            assertThrows(IllegalStateException.class, () -> client.presignPutUrl(key));
            stored[stored.length - 1] ^= 1;
            assertThrows(IllegalStateException.class, () -> client.getContent(key));
            client.delete(key);
            assertTrue(objects.isEmpty());
        } finally { server.stop(0); }
    }
}
