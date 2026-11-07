package cn.iocoder.yudao.module.aigc.config;

import cn.iocoder.yudao.module.infra.controller.admin.file.vo.config.FileConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileConfigDO;
import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClient;
import cn.iocoder.yudao.module.infra.framework.file.core.client.s3.S3FileClientConfig;
import cn.iocoder.yudao.module.infra.service.file.FileConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ObjectStorageBootstrapTest {
    @TempDir Path directory;
    private final FileConfigService configs = mock(FileConfigService.class);
    private final FileClient client = mock(FileClient.class);

    private ObjectStorageBootstrap bootstrap(Map<String, String> env, boolean configured) throws Exception {
        Path secrets = directory.resolve("storage");
        Files.createDirectories(secrets.resolve("r2"));
        if (configured) {
            Files.writeString(secrets.resolve("r2/config.json"), "{\"endpoint\":\"https://0123456789abcdef0123456789abcdef.r2.cloudflarestorage.com\",\"bucket\":\"safillu-unit-test\",\"domain\":\"https://assets.example.test\"}");
            Files.writeString(secrets.resolve("r2/access_key"), "mock-access");
            Files.writeString(secrets.resolve("r2/access_secret"), "mock-secret");
        }
        when(configs.createFileConfig(any())).thenReturn(880004L);
        when(configs.getFileClient(anyLong())).thenReturn(client);
        when(client.getContent(anyString())).thenReturn(ObjectStorageBootstrap.probeBody());
        when(client.upload(any(), anyString(), anyString())).thenReturn("https://assets.example.test/probe.png");
        return new ObjectStorageBootstrap(configs, env, secrets, directory, url -> ObjectStorageBootstrap.probeBody());
    }

    @Test void automaticR2TakesPriorityAndActivatesOnlyAfterFullRoundtrip() throws Exception {
        bootstrap(Map.of("QINIU_STORAGE_BOOTSTRAP", "true"), true).run(null);
        ArgumentCaptor<FileConfigSaveReqVO> saved = ArgumentCaptor.forClass(FileConfigSaveReqVO.class);
        verify(configs).createFileConfig(saved.capture());
        assertEquals("file:/run/secrets/storage/r2/access_secret", saved.getValue().getConfig().get("accessSecret"));
        assertEquals(false, saved.getValue().getConfig().get("encryptContent"));
        assertEquals(true, saved.getValue().getConfig().get("enablePublicAccess"));
        var order = inOrder(client, configs);
        order.verify(client).upload(any(), anyString(), eq("image/png"));
        order.verify(client).getContent(anyString());
        order.verify(client).delete(anyString());
        order.verify(configs).updateFileConfigMaster(880004L);
        verify(configs, never()).getFileClient(880002L);
        assertTrue(Files.readString(directory.resolve("r2-storage-bootstrap.json")).contains("\"active\":true"));
    }

    @Test void uploadFailureRetainsPreviousMaster() throws Exception {
        var bootstrap = bootstrap(Map.of(), true);
        when(client.upload(any(), anyString(), anyString())).thenThrow(new IllegalStateException("mock upload failure"));
        bootstrap.run(null);
        verify(configs, never()).updateFileConfigMaster(anyLong());
        assertTrue(Files.readString(directory.resolve("r2-storage-bootstrap.json")).contains("\"active\":false"));
    }

    @Test void unreadablePublicUrlCleansProbeAndRetainsPreviousMaster() throws Exception {
        bootstrap(Map.of(), true);
        var bootstrap = new ObjectStorageBootstrap(configs, Map.of(), directory.resolve("storage"), directory,
                url -> { throw new IllegalStateException("Public domain is not enabled"); });
        bootstrap.run(null);
        verify(client).delete(anyString());
        verify(configs, never()).updateFileConfigMaster(anyLong());
    }

    @Test void mismatchedReadCleansProbeAndRetainsPreviousMaster() throws Exception {
        var bootstrap = bootstrap(Map.of(), true);
        when(client.getContent(anyString())).thenReturn(new byte[]{1, 2});
        bootstrap.run(null);
        verify(client).delete(anyString());
        verify(configs, never()).updateFileConfigMaster(anyLong());
    }

    @Test void missingR2ConfigurationAllowsQiniu() throws Exception {
        bootstrap(Map.of("QINIU_STORAGE_BOOTSTRAP", "true"), false).run(null);
        verify(configs).updateFileConfigMaster(880002L);
        verify(configs, never()).getFileClient(880004L);
    }

    @Test void missingCredentialsKeepsCurrentStorage() throws Exception {
        bootstrap(Map.of(), false).run(null);
        verify(configs, never()).updateFileConfigMaster(anyLong());
        verify(configs, never()).getFileClient(anyLong());
    }

    @Test void explicitDisableDoesNotActivateR2() throws Exception {
        bootstrap(Map.of("R2_STORAGE_BOOTSTRAP", "false"), true).run(null);
        verify(configs, never()).getFileClient(anyLong());
    }

    @Test void explicitProviderConflictIsRejected() throws Exception {
        var bootstrap = bootstrap(Map.of("R2_STORAGE_BOOTSTRAP", "true", "QINIU_STORAGE_BOOTSTRAP", "true"), true);
        assertThrows(IllegalStateException.class, () -> bootstrap.run(null));
        verify(configs, never()).updateFileConfigMaster(anyLong());
    }

    @Test void existingConfigurationIdIsNeverRepointedToAnotherBucket() throws Exception {
        var bootstrap = bootstrap(Map.of(), true);
        S3FileClientConfig old = new S3FileClientConfig();
        old.setEndpoint("https://0123456789abcdef0123456789abcdef.r2.cloudflarestorage.com");
        old.setBucket("historical-assets");
        when(configs.getFileConfig(880004L)).thenReturn(FileConfigDO.builder().id(880004L).storage(20).config(old).build());
        bootstrap.run(null);
        verify(configs, never()).createFileConfig(any());
        verify(configs, never()).updateFileConfig(any());
        verify(configs, never()).updateFileConfigMaster(anyLong());
    }
}
