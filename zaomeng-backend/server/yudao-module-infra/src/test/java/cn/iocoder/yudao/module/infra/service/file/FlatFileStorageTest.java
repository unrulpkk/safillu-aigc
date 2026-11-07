package cn.iocoder.yudao.module.infra.service.file;

import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlatFileStorageTest {
    @Mock private FileConfigService configs;
    @Mock private FileMapper mapper;
    @Mock private FileClient client;
    @InjectMocks private FileServiceImpl files;

    private byte[] png() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", output);
        return output.toByteArray();
    }

    private void storage() throws Exception {
        when(configs.getMasterFileClient()).thenReturn(client);
        when(client.getId()).thenReturn(880004L);
        when(client.upload(any(), anyString(), anyString()))
                .thenAnswer(call -> "https://assets.example.test/" + call.getArgument(1));
    }

    @Test void storesDateAndUuidAtTheRootAndInfersMissingExtension() throws Exception {
        storage();
        String before = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
        String url = files.createFlatFile(png(), "original-without-extension", "video/mp4");
        String after = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
        var stored = ArgumentCaptor.forClass(FileDO.class);
        verify(mapper).insert(stored.capture());
        var file = stored.getValue();
        assertTrue(file.getPath().matches("\\d{8}_[0-9a-f-]{36}\\.png"));
        assertTrue(List.of(before, after).contains(file.getPath().substring(0, 8)));
        UUID.fromString(file.getPath().substring(9, 45));
        assertEquals(file.getPath(), file.getName());
        assertEquals("image/png", file.getType());
        assertEquals("https://assets.example.test/" + file.getPath(), url);
        assertEquals(880004L, file.getConfigId());
    }

    @Test void identicalNamesAndBytesStillGetDistinctObjects() throws Exception {
        storage();
        byte[] bytes = png();
        String first = files.createFlatFile(bytes, "same.PNG", "image/png");
        String second = files.createFlatFile(bytes, "same.PNG", "image/png");
        assertNotEquals(first, second);
        assertTrue(first.endsWith(".png"));
        assertTrue(second.endsWith(".png"));
        verify(mapper, times(2)).insert(any(FileDO.class));
    }

    @Test void flatUploadsStillRejectContentDisguisedAsAnImage() {
        byte[] html = "<!doctype html><html><body>not an image</body></html>".getBytes(StandardCharsets.UTF_8);
        assertThrows(RuntimeException.class, () -> files.createFlatFile(html, "disguised.png", "image/png"));
        verifyNoInteractions(configs, mapper, client);
    }

    @Test void ordinaryFrameworkUploadsKeepTheirDirectoryContract() throws Exception {
        storage();
        files.createFile(png(), "original.png", "framework/files", "image/png");
        var stored = ArgumentCaptor.forClass(FileDO.class);
        verify(mapper).insert(stored.capture());
        assertTrue(stored.getValue().getPath().startsWith("framework/files/"));
        assertEquals("original.png", stored.getValue().getName());
    }

    @Test void existingNestedAssetsAreReadWithTheirRecordedPath() throws Exception {
        String path = "aigc/1/2/generated/prior-job/20261107/original.mp4";
        byte[] expected = {1, 2, 3};
        when(configs.getFileClient(880004L)).thenReturn(client);
        when(client.getContent(path)).thenReturn(expected);
        assertArrayEquals(expected, files.getFileContent(880004L, path));
        verify(client).getContent(path);
        verifyNoInteractions(mapper);
    }
}
