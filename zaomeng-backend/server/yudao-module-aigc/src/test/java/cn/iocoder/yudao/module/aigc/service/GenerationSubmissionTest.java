package cn.iocoder.yudao.module.aigc.service;

import cn.iocoder.yudao.module.infra.service.file.FileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GenerationSubmissionTest {
    private final StudioService studio = mock(StudioService.class);
    private final JdbcTemplate db = mock(JdbcTemplate.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final GenerationProvider provider = mock(GenerationProvider.class);
    private final GenerationReferenceStorage references = mock(GenerationReferenceStorage.class);

    private GenerationService setup() throws Exception {
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(studio.id(any())).thenAnswer(call -> Long.parseLong(call.getArgument(0).toString()));
        when(studio.scopedInsert(anyString(), any(), anyMap())).thenReturn(99L);
        when(studio.one(startsWith("SELECT k.id"), any())).thenReturn(Map.of("id", 1L, "url", "https://api.safillu.com", "api_key", "file:/run/secrets/provider_key"));
        when(studio.one(startsWith("SELECT i.config_id"), any(), any(), any())).thenReturn(Map.of("config_id", 880004L, "path", "asset.png", "mime_type", "image/png"));
        when(references.imageUrl(880004L, "asset.png", "image/png", 1L, 2L, 3L)).thenReturn("https://assets.example.test/reference.png");
        when(references.legacyImageDataUrl(880004L,"asset.png","image/png")).thenReturn("data:image/png;base64,AQID");
        var response = new ObjectMapper().readTree("{\"task_id\":\"task_unit123\",\"status\":\"QUEUED\"}");
        when(provider.call(anyString(), anyString(), anyString(), anyString(), any(), anyString())).thenReturn(response);
        when(provider.status(response)).thenReturn("QUEUED");
        when(provider.jobId(response)).thenReturn("task_unit123");
        return new GenerationService(studio, db, transactions, provider, mock(FileService.class), references);
    }

    private Map<String, Object> task() {
        var task = new HashMap<String, Object>();
        task.putAll(Map.of("id", 3L, "tenant_id", 1L, "workspace_id", 2L, "creator", "4", "status", "QUEUED", "ai_model_id", 870013L, "task_no", "unit", "operation", "IMAGE", "requested_count", 1));
        task.put("parameter_snapshot", Map.of("adapter", "edit", "modelCode", "Qwen-Image-2.1-Edit", "prompt", "change background", "ratio", "1:1", "resolution", "1K", "refs", List.of(Map.of("assetId", "7"))));
        return task;
    }

    @Test void qwenKeepsItsLegacyWireFormatAfterPublicStorageIsReady() throws Exception {
        var service = setup();
        var task = task();
        when(studio.one(startsWith("SELECT * FROM aigc_task WHERE id=?"), any())).thenReturn(task);
        ReflectionTestUtils.invokeMethod(service, "run", task);
        var order = inOrder(references, provider);
        order.verify(references).imageUrl(880004L, "asset.png", "image/png", 1L, 2L, 3L);
        order.verify(references).legacyImageDataUrl(880004L,"asset.png","image/png");
        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
        order.verify(provider).call(anyString(), anyString(), eq("POST"), eq("/image-edit/v1/edits"), body.capture(), anyString());
        assertEquals(List.of("data:image/png;base64,AQID"), ((Map<?, ?>)body.getValue()).get("images"));
    }

    @Test void aReferenceUploadFailurePreventsAnyModelRequest() throws Exception {
        var service = setup();
        when(references.imageUrl(anyLong(), anyString(), anyString(), anyLong(), anyLong(), anyLong()))
                .thenThrow(new GenerationProvider.ProviderException("REFERENCE_UPLOAD_FAILED", "Upload failed", false));
        assertThrows(GenerationProvider.ProviderException.class, () -> ReflectionTestUtils.invokeMethod(service, "run", task()));
        verify(provider, never()).call(anyString(), anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test void bestH3SendsOnlyPublicMediaReferencesAndPreservesSeedZero() throws Exception {
        var service=setup();var task=task();task.put("operation","VIDEO");task.put("ai_model_id",870018L);
        var parameters=new LinkedHashMap<String,Object>();
        parameters.putAll(Map.of("adapter","best-h3","modelCode","best-minimax-h3","prompt","Ocean waves","resolution","480P","videoSeconds",4,"seed",0L,"refs",List.of(Map.of("assetId","7")),"attachments",List.of(Map.of("assetId","8"),Map.of("assetId","9"))));
        task.put("parameter_snapshot",parameters);
        when(studio.one(startsWith("SELECT i.config_id"),any(),any(),eq(8L))).thenReturn(Map.of("config_id",880004L,"path","voice.wav","mime_type","audio/wav"));
        when(studio.one(startsWith("SELECT i.config_id"),any(),any(),eq(9L))).thenReturn(Map.of("config_id",880004L,"path","clip.mp4","mime_type","video/mp4"));
        when(references.mediaUrl(880004L,"voice.wav","audio/wav",1,2,3)).thenReturn("https://assets.example.test/voice.wav");
        when(references.mediaUrl(880004L,"clip.mp4","video/mp4",1,2,3)).thenReturn("https://assets.example.test/clip.mp4");
        when(studio.one(startsWith("SELECT * FROM aigc_task WHERE id=?"),any())).thenReturn(task);
        ReflectionTestUtils.invokeMethod(service,"run",task);
        var order=inOrder(references,provider);
        order.verify(references).imageUrl(880004L,"asset.png","image/png",1,2,3);
        order.verify(references).mediaUrl(880004L,"voice.wav","audio/wav",1,2,3);
        order.verify(references).mediaUrl(880004L,"clip.mp4","video/mp4",1,2,3);
        ArgumentCaptor<Object> body=ArgumentCaptor.forClass(Object.class);
        order.verify(provider).call(anyString(),anyString(),eq("POST"),eq("/best-minimax-h3/v1/video/generateVideo"),body.capture(),anyString());
        Map<?,?> request=(Map<?,?>)body.getValue(),metadata=(Map<?,?>)request.get("metadata");
        assertEquals(Set.of("model","prompt","duration","metadata"),request.keySet());
        assertEquals("best-minimax-h3",request.get("model"));assertEquals(4,request.get("duration"));assertEquals(0L,metadata.get("seed"));
        assertEquals(List.of(
                Map.of("type","image_url","role","reference_image","image_url",Map.of("url","https://assets.example.test/reference.png")),
                Map.of("type","audio_url","role","reference_audio","audio_url",Map.of("url","https://assets.example.test/voice.wav")),
                Map.of("type","video_url","role","reference_video","video_url",Map.of("url","https://assets.example.test/clip.mp4"))),metadata.get("references"));
        assertFalse(metadata.containsKey("chunks"));
        assertEquals(0L,parameters.get("seed"));
    }

    @Test void bestH3PollsItsOwnNativeEndpointWithThePersistedTaskId() throws Exception {
        var service=setup();var task=task();task.put("status","RUNNING");task.put("started_time",new java.sql.Timestamp(System.currentTimeMillis()));
        when(studio.time(any())).thenReturn(System.currentTimeMillis());
        task.put("parameter_snapshot",Map.of("adapter","best-h3","modelCode","best-minimax-h3"));
        when(db.queryForList(startsWith("SELECT * FROM aigc_task_attempt"),eq(1L),eq(2L),eq(3L))).thenReturn(List.of(Map.of("id",99L,"provider_task_id","task_existing")));
        when(studio.one(startsWith("SELECT * FROM aigc_task WHERE id=?"),any())).thenReturn(task);
        ReflectionTestUtils.invokeMethod(service,"run",task);
        verify(provider).call(anyString(),anyString(),eq("POST"),eq("/best-minimax-h3/v1/video/getVideoStatus"),eq(Map.of("taskICode","task_existing")),anyString());
        verifyNoInteractions(references);
    }

    @Test void bestH3QuoteRejectsFractionalDurationsAndPreservesAZeroSeed() throws Exception {
        var service=setup();when(studio.scope()).thenReturn(new StudioService.Scope(1,2,4));
        var capability=new HashMap<String,Object>();
        capability.putAll(Map.of("model_name","Best MiniMax H3","model_code","best-minimax-h3","ai_model_id",870018L,"billing_unit","SECOND","points_per_unit",1,"default_parameters",Map.of("adapter","best-h3","resolution","480P","ratio","16:9"),"limits_config",Map.of("maxCount",1,"maxRefs",9,"maxAudioRefs",3,"maxVideoRefs",3,"minSeconds",4,"maxSeconds",15,"ratios",List.of("16:9"),"resolutions",List.of("480P","768P","1080P","2K"))));
        when(studio.one(startsWith("SELECT c.*"),any(),any())).thenReturn(capability);
        for(Object duration:List.of(3,16,4.5,"NaN")){
            var draft=Map.of("prompt","Ocean waves","videoSeconds",duration);
            assertThrows(RuntimeException.class,()->service.quote(Map.of("operation","VIDEO","capabilityId","870018","draft",draft)));
        }
        for(int duration:List.of(4,15)){
            var quoted=service.quote(Map.of("operation","VIDEO","capabilityId","870018","draft",Map.of("prompt","Ocean waves","videoSeconds",duration,"seed",0)));
            assertEquals(new java.math.BigDecimal(duration),quoted.get("points"));
            assertEquals(0L,((Map<?,?>)quoted.get("parameters")).get("seed"));
        }
    }
}
