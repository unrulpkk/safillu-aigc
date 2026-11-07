package cn.iocoder.yudao.module.aigc.service;

import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileConfigDO;
import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClient;
import cn.iocoder.yudao.module.infra.framework.file.core.client.s3.S3FileClientConfig;
import cn.iocoder.yudao.module.infra.service.file.FileConfigService;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GenerationReferenceStorageTest {
    private final FileConfigService configs = mock(FileConfigService.class);
    private final FileService files = mock(FileService.class);
    private final FileClient source = mock(FileClient.class);
    private final FileClient target = mock(FileClient.class);
    private final GenerationReferenceStorage storage = new GenerationReferenceStorage(configs, files);
    private final String publicUrl = "https://assets.example.test/owner/reference.png";

    private FileConfigDO config(long id, boolean encrypted, boolean publiclyReadable) {
        var s3 = new S3FileClientConfig();
        s3.setDomain("https://assets.example.test");
        s3.setEncryptContent(encrypted);
        s3.setEnablePublicAccess(publiclyReadable);
        return FileConfigDO.builder().id(id).storage(20).config(s3).build();
    }

    @Test void anAlreadyUploadedPublicAssetIsReferencedWithoutReuploading() throws Exception {
        when(configs.getFileConfig(880004L)).thenReturn(config(880004L, false, true));
        when(configs.getFileClient(880004L)).thenReturn(source);
        when(source.presignGetUrl("owner/reference.png", null)).thenReturn(publicUrl);
        assertEquals(publicUrl, storage.imageUrl(880004L, "owner/reference.png", "image/png", 1, 2, 3));
        verifyNoInteractions(files);
        verify(configs, never()).getMasterFileClient();
    }

    @Test void legacyEncryptedAssetsAreReadThenUploadedToPublicStorage() throws Exception {
        when(configs.getFileConfig(880002L)).thenReturn(config(880002L, true, false));
        when(configs.getMasterFileClient()).thenReturn(target);
        when(target.getId()).thenReturn(880004L);
        when(configs.getFileConfig(880004L)).thenReturn(config(880004L, false, true));
        byte[] bytes = {1, 2, 3};
        when(files.getFileContent(880002L, "old/image.png")).thenReturn(bytes);
        when(files.createFile(eq(bytes), anyString(), eq("aigc/1/2/model-inputs/3"), eq("image/png"))).thenReturn(publicUrl);
        assertEquals(publicUrl, storage.imageUrl(880002L, "old/image.png", "image/png", 1, 2, 3));
        var order = inOrder(files);
        order.verify(files).getFileContent(880002L, "old/image.png");
        order.verify(files).createFile(eq(bytes), anyString(), anyString(), eq("image/png"));
        verify(configs, never()).updateFileConfig(any());
    }

    @Test void missingPublicStorageFailsBeforeUploadingAnything() {
        when(configs.getMasterFileClient()).thenReturn(target);
        when(target.getId()).thenReturn(1L);
        var error = assertThrows(GenerationProvider.ProviderException.class,
                () -> storage.imageUrl(1L, "local.png", "image/png", 1, 2, 3));
        assertEquals("REFERENCE_STORAGE_NOT_PUBLIC", error.code);
        verifyNoInteractions(files);
    }

    @Test void uploadFailureIsReportedBeforeGenerationCanBeSubmitted() throws Exception {
        when(configs.getMasterFileClient()).thenReturn(target);
        when(target.getId()).thenReturn(880004L);
        when(configs.getFileConfig(880004L)).thenReturn(config(880004L, false, true));
        when(files.getFileContent(1L, "local.png")).thenReturn(new byte[]{1});
        when(files.createFile(any(), anyString(), anyString(), anyString())).thenThrow(new IllegalStateException("mock upload failed"));
        var error = assertThrows(GenerationProvider.ProviderException.class,
                () -> storage.imageUrl(1L, "local.png", "image/png", 1, 2, 3));
        assertEquals("REFERENCE_UPLOAD_FAILED", error.code);
    }

    @Test void authenticatedOrInlineUrlsAreNeverSentAsPublicImageUrls() {
        when(configs.getFileConfig(880004L)).thenReturn(config(880004L, false, true));
        when(configs.getFileClient(880004L)).thenReturn(source);
        for (String invalid : new String[]{"data:image/png;base64,AAAA", "http://localhost/image.png", "https://assets.example.test/image.png?token=secret"}) {
            when(source.presignGetUrl("image.png", null)).thenReturn(invalid);
            var error = assertThrows(GenerationProvider.ProviderException.class,
                    () -> storage.imageUrl(880004L, "image.png", "image/png", 1, 2, 3));
            assertEquals("REFERENCE_URL_INVALID", error.code);
        }
    }

    @Test void publicAudioAndVideoArePassedByUrlWithoutCopyingBytes() throws Exception {
        when(configs.getFileConfig(880004L)).thenReturn(config(880004L,false,true));
        when(configs.getFileClient(880004L)).thenReturn(source);
        for(var fixture:java.util.Map.of("audio/wav","voice.wav","video/mp4","clip.mp4").entrySet()){
            String url="https://assets.example.test/"+fixture.getValue();
            when(source.presignGetUrl(fixture.getValue(),null)).thenReturn(url);
            assertEquals(url,storage.mediaUrl(880004L,fixture.getValue(),fixture.getKey(),1,2,3));
        }
        assertThrows(GenerationProvider.ProviderException.class,()->storage.mediaUrl(880004L,"text.txt","text/plain",1,2,3));
        verifyNoInteractions(files);
    }

    @Test void legacyWireFormatUsesTheOriginalFileServiceDecryption() throws Exception {
        when(files.getFileContent(880002L,"old/image.png")).thenReturn(new byte[]{1,2,3});
        assertEquals("data:image/png;base64,AQID",storage.legacyImageDataUrl(880002L,"old/image.png","image/png"));
        verify(files).getFileContent(880002L,"old/image.png");
        verifyNoInteractions(configs);
    }
}
