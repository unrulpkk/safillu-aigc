package cn.iocoder.yudao.module.aigc.service;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.framework.file.core.utils.FileTypeUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import static cn.iocoder.yudao.module.aigc.service.StudioService.map;

/** Durable jobs and platform points. Supplier money is never presented as platform points. */
@Service
public class GenerationService {
    private final StudioService studio;private final JdbcTemplate db;private final TransactionTemplate tx;
    private final GenerationProvider provider;private final FileService files;
    private final GenerationReferenceStorage references;
    private static final Set<String> TERMINAL=Set.of("SUCCEEDED","PARTIAL_SUCCEEDED","FAILED","CANCELED","TIMED_OUT");
    public GenerationService(StudioService studio,JdbcTemplate db,PlatformTransactionManager manager,GenerationProvider provider,FileService files,GenerationReferenceStorage references){this.studio=studio;this.db=db;this.tx=new TransactionTemplate(manager);this.tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);this.provider=provider;this.files=files;this.references=references;}
    private BigDecimal decimal(Object o){return new BigDecimal(o.toString());}
    private int number(Object o,int fallback){try{return Integer.parseInt(o.toString());}catch(Exception e){return fallback;}}
    private String string(Object value){return Objects.toString(value,"");}
    @SuppressWarnings("unchecked") private Map<String,Object> object(Object value){if(value instanceof Map<?,?> m)return new LinkedHashMap<>((Map<String,Object>)m);return (Map<String,Object>)studio.decode(value,new LinkedHashMap<>());}
    private StudioService.Scope taskScope(Map<String,Object> t){return new StudioService.Scope(((Number)t.get("tenant_id")).longValue(),((Number)t.get("workspace_id")).longValue(),Long.parseLong(string(t.get("creator"))));}
    public void trial(){tx.executeWithoutResult(status->{var s=studio.scope();
        var quota=studio.one("SELECT * FROM aigc_quota_account WHERE tenant_id=? AND workspace_id=? FOR UPDATE",s.tenant(),s.workspace());
        if(db.queryForList("SELECT id FROM aigc_quota_ledger WHERE tenant_id=? AND workspace_id=? AND business_key='trial:v1' FOR UPDATE",s.tenant(),s.workspace()).isEmpty()){
            ledger(s,null,"trial:v1","GRANT",new BigDecimal("100"),BigDecimal.ZERO,BigDecimal.ZERO,"首版试用积分（平台积分，非现金充值）");
        }
    });}
    private void ledger(StudioService.Scope s,Long task,String key,String kind,BigDecimal available,BigDecimal reserved,BigDecimal consumed,String remark){
        if(!db.queryForList("SELECT id FROM aigc_quota_ledger WHERE tenant_id=? AND workspace_id=? AND business_key=? FOR UPDATE",s.tenant(),s.workspace(),key).isEmpty())return;
        var q=studio.one("SELECT * FROM aigc_quota_account WHERE tenant_id=? AND workspace_id=? FOR UPDATE",s.tenant(),s.workspace());
        BigDecimal a=decimal(q.get("available_points")).add(available),r=decimal(q.get("reserved_points")).add(reserved),c=decimal(q.get("consumed_points")).add(consumed);
        if(a.signum()<0||r.signum()<0||c.signum()<0)throw studio.error(400,"可用积分不足，请调整生成数量或联系管理员");
        db.update("UPDATE aigc_quota_account SET available_points=?,reserved_points=?,consumed_points=?,revision=revision+1 WHERE tenant_id=? AND workspace_id=?",a,r,c,s.tenant(),s.workspace());
        studio.scopedInsert("aigc_quota_ledger",s,map("task_id",task,"business_key",key,"entry_type",kind,"available_delta",available,"reserved_delta",reserved,"consumed_delta",consumed,"available_after",a,"reserved_after",r,"consumed_after",c,"remark",remark));
    }
    public Map<String,Object> quote(Map<String,Object> body){return tx.execute(status->quote(studio.scope(),body));}
    private Map<String,Object> quote(StudioService.Scope s,Map<String,Object> body){
        var d=object(body.get("draft"));String operation=string(body.getOrDefault("operation",d.getOrDefault("mode","image"))).toUpperCase(Locale.ROOT);
        if(!Set.of("IMAGE","VIDEO","AUDIO","MUSIC").contains(operation))throw studio.error(503,"此创作模式暂未接入可执行模型");
        long capability=studio.id(body.getOrDefault("capabilityId",d.get("capabilityId")));
        var c=studio.one("SELECT c.*,m.name AS model_name,m.model AS model_code,m.key_id FROM aigc_model_capability c JOIN ai_model m ON m.id=c.ai_model_id AND m.status=0 AND m.deleted=0 JOIN ai_api_key k ON k.id=m.key_id AND k.status=0 AND k.deleted=0 WHERE c.id=? AND c.operation=? AND c.status='ENABLED' AND c.deleted=0",capability,operation);
        var limits=object(c.get("limits_config"));var defaults=object(c.get("default_parameters"));
        int count=number(d.get("count"),1),max=number(limits.get("maxCount"),1);
        if(count<1||count>max)throw studio.error(400,"此模型生成数量需为 1 至 "+max);
        String prompt=string(d.get("prompt")).trim();int maxPrompt=number(limits.get("maxPrompt"),12000);
        if(prompt.isEmpty()||prompt.length()>maxPrompt)throw studio.error(400,"创作描述需为 1 至 "+maxPrompt+" 字");
        List<?> refs=d.get("refs") instanceof List<?> list?list:List.of(),attachments=d.get("attachments") instanceof List<?> list?list:List.of();
        boolean bestH3="best-h3".equals(defaults.get("adapter"));
        if(!attachments.isEmpty()&&!bestH3)throw studio.error(400,"此模型当前适配不接受音视频参考，请先移除附加素材");
        List<Map<String,Object>> normalizedAttachments=normalizeAttachments(s,attachments,limits);
        if(refs.size()<number(limits.get("minRefs"),0)||refs.size()>number(limits.get("maxRefs"),0))throw studio.error(400,"此模型参考图片数量需为 "+number(limits.get("minRefs"),0)+" 至 "+number(limits.get("maxRefs"),0));
        List<Map<String,Object>> normalized=new ArrayList<>();
        for(Object ref:refs){var input=object(ref);long id=studio.id(input.get("assetId"));var a=studio.owned("aigc_asset",s,id);if(!"IMAGE".equals(a.get("kind"))||!"READY".equals(a.get("status")))throw studio.error(400,"请选择当前账号的就绪图片");
            var f=studio.one("SELECT mime_type,byte_size FROM aigc_asset_file WHERE tenant_id=? AND workspace_id=? AND asset_id=? AND variant_code='ORIGINAL' AND deleted=0",s.tenant(),s.workspace(),id);
            if(((Number)f.get("byte_size")).longValue()>8*1024*1024)throw studio.error(400,"模型参考图片不能超过 8MB");
            normalized.add(map("assetId",String.valueOf(id),"name",input.get("name"),"slot",input.getOrDefault("slot","reference"),"type",f.get("mime_type")));
        }
        String ratio=string(d.getOrDefault("ratio",defaults.getOrDefault("ratio","1:1")));if(ratio.equals("智能"))ratio=string(defaults.getOrDefault("ratio","1:1"));
        String resolution=string(d.getOrDefault("resolution",defaults.getOrDefault("resolution","1K")));
        if(limits.get("ratios") instanceof List<?> list&&!list.contains(ratio))throw studio.error(400,"此模型不支持所选画面比例");
        if(limits.get("resolutions") instanceof List<?> list&&!list.contains(resolution))throw studio.error(400,"此模型不支持所选分辨率");
        int seconds=number(d.get(operation.equals("VIDEO")?"videoSeconds":"musicSeconds"),operation.equals("VIDEO")?5:30);
        if(bestH3&&d.containsKey("videoSeconds")){
            try{seconds=decimal(d.get("videoSeconds")).intValueExact();}
            catch(Exception error){throw studio.error(400,"视频时长需为整数秒");}
        }
        if(operation.equals("VIDEO")&&(seconds<number(limits.get("minSeconds"),5)||seconds>number(limits.get("maxSeconds"),10)))throw studio.error(400,"此模型视频时长不受支持");
        if(operation.equals("MUSIC")&&(seconds<5||seconds>number(limits.get("maxSeconds"),120)))throw studio.error(400,"音乐时长需在 5 至 120 秒之间");
        BigDecimal rate=decimal(c.get("points_per_unit"));
        if(operation.equals("VIDEO"))rate=new BigDecimal(switch(resolution){case "480P"->"1";case "1080P"->"5";case "2K"->"7";default->"3";});
        int units=operation.equals("VIDEO")?seconds:operation.equals("IMAGE")?count:1;
        String voiceDesign=string(d.getOrDefault("voiceDesign","自然清晰的普通话声音"));
        double speed;try{speed=Double.parseDouble(d.getOrDefault("speed",0.9).toString());}catch(Exception e){throw studio.error(400,"语速参数错误");}
        if(voiceDesign.isBlank()||voiceDesign.length()>4000||!Double.isFinite(speed)||speed<0.5||speed>2)throw studio.error(400,"声音描述需为 1–4000 字，语速需在 0.5–2 之间");
        if(string(d.get("negativePrompt")).length()>12000)throw studio.error(400,"负面提示词不能超过 12000 字");
        var parameters=map("mode",operation.toLowerCase(Locale.ROOT),"model",c.get("model_name"),"modelCode",c.get("model_code"),"capabilityId",String.valueOf(capability),"adapter",defaults.get("adapter"),"configVersion",c.get("config_version"),"count",count,"prompt",prompt,"ratio",ratio,"resolution",resolution,"videoSeconds",seconds,"musicSeconds",seconds,"voiceDesign",string(d.getOrDefault("voiceDesign","自然清晰的普通话声音")),"speed",d.getOrDefault("speed",0.9),"negativePrompt",string(d.getOrDefault("negativePrompt","")),"refs",normalized,"attachments",normalizedAttachments);
        if(bestH3&&d.containsKey("seed")&&d.get("seed")!=null){
            try{BigDecimal seed=decimal(d.get("seed"));long value=seed.longValueExact();if(value<0||value>9007199254740991L)throw new ArithmeticException();parameters.put("seed",value);}
            catch(Exception error){throw studio.error(400,"随机种子需为非负安全整数");}
        }
        return map("capabilityId",String.valueOf(capability),"modelId",c.get("ai_model_id"),"parameters",parameters,"operation",operation,"unit",c.get("billing_unit"),"rate",rate,"units",units,"points",rate.multiply(BigDecimal.valueOf(units)),"currency","POINTS");
    }
    private List<Map<String,Object>> normalizeAttachments(StudioService.Scope s,List<?> attachments,Map<String,Object> limits){
        int audio=0,video=0;var normalized=new ArrayList<Map<String,Object>>();
        for(Object ref:attachments){
            var input=object(ref);long id=studio.id(input.get("assetId"));var asset=studio.owned("aigc_asset",s,id);
            if(!"READY".equals(asset.get("status")))throw studio.error(400,"请选择当前账号的就绪素材");
            String kind=string(asset.get("kind"));String role;
            if(Set.of("AUDIO","MUSIC").contains(kind)){audio++;role="reference_audio";}
            else if(kind.equals("VIDEO")){video++;role="reference_video";}
            else throw studio.error(400,"附加参考仅支持音频和视频");
            if(audio>number(limits.get("maxAudioRefs"),0)||video>number(limits.get("maxVideoRefs"),0))throw studio.error(400,"音视频参考数量超过模型限制");
            var file=studio.one("SELECT mime_type,byte_size FROM aigc_asset_file WHERE tenant_id=? AND workspace_id=? AND asset_id=? AND variant_code='ORIGINAL' AND deleted=0",s.tenant(),s.workspace(),id);
            if(((Number)file.get("byte_size")).longValue()>50*1024*1024)throw studio.error(400,"模型参考音视频不能超过 50MB");
            String mime=string(file.get("mime_type"));
            if(role.equals("reference_audio")&&!Set.of("audio/mpeg","audio/wav","audio/x-wav","audio/wave","audio/vnd.wave","audio/flac","audio/x-flac").contains(mime)
                    ||role.equals("reference_video")&&!Set.of("video/mp4","video/quicktime").contains(mime))throw studio.error(400,"参考音视频仅支持 MP3、WAV、FLAC、MP4 和 MOV");
            normalized.add(map("assetId",String.valueOf(id),"name",input.get("name"),"slot",input.getOrDefault("slot","reference"),"type",mime,"role",role));
        }
        return normalized;
    }
    public Map<String,Object> create(Map<String,Object> body){return tx.execute(status->{
        var s=studio.scope();long conversation=studio.id(body.get("conversationId"));studio.owned("aigc_conversation",s,conversation);
        String key=string(body.get("clientRequestId"));if(!key.matches("[A-Za-z0-9:_-]{8,64}"))throw studio.error(400,"缺少有效的请求幂等编号");
        // Serialize all submissions for this workspace before checking its unique request key.
        studio.one("SELECT id FROM aigc_quota_account WHERE tenant_id=? AND workspace_id=? FOR UPDATE",s.tenant(),s.workspace());
        if(db.queryForObject("SELECT COUNT(*) FROM aigc_credit_purchase WHERE tenant_id=? AND workspace_id=? AND environment='LIVE' AND billing_hold=1",Long.class,s.tenant(),s.workspace())>0)throw studio.error(402,"账号支付退款或争议待核对，请联系管理员");
        var existing=db.queryForList("SELECT id FROM aigc_task WHERE tenant_id=? AND workspace_id=? AND client_request_id=?",s.tenant(),s.workspace(),key);
        if(!existing.isEmpty())return map("id",existing.getFirst().get("id").toString(),"reused",true);
        if(db.queryForObject("SELECT COUNT(*) FROM aigc_task WHERE tenant_id=? AND workspace_id=? AND status NOT IN ('SUCCEEDED','PARTIAL_SUCCEEDED','FAILED','CANCELED','TIMED_OUT')",Long.class,s.tenant(),s.workspace())>=3)throw studio.error(429,"当前账号最多同时处理 3 个任务");
        var q=quote(s,body);var p=object(q.get("parameters"));Long retry=body.get("retryOfTaskId")==null?null:studio.id(body.get("retryOfTaskId"));
        if(retry!=null)studio.owned("aigc_task",s,retry);
        var pricing=map("unit",q.get("unit"),"rate",q.get("rate"),"units",q.get("units"),"reservedPoints",q.get("points"),"currency","POINTS","policy","仅归档成功的结果结算，其余释放");
        long id=studio.scopedInsert("aigc_task",s,map("conversation_id",conversation,"task_no","ZM"+UUID.randomUUID().toString().replace("-",""),"client_request_id",key,"operation",q.get("operation"),"capability_id",Long.parseLong(q.get("capabilityId").toString()),"ai_model_id",q.get("modelId"),"prompt",p.get("prompt"),"parameter_snapshot",studio.encode(p),"pricing_snapshot",studio.encode(pricing),"requested_count",p.get("count"),"status","QUEUED","retry_of_task_id",retry,"next_run_time",new Timestamp(System.currentTimeMillis())));
        int inputOrder=0;
        if(p.get("refs") instanceof List<?> refs)for(Object value:refs){var ref=object(value);String alias=string(ref.get("name"));if(alias.length()>100)alias=alias.substring(0,100);
            studio.scopedInsert("aigc_task_input",s,map("task_id",id,"sort_order",inputOrder++,"role","REFERENCE","alias",alias,"asset_id",studio.id(ref.get("assetId")),"configuration_snapshot",studio.encode(map("slot",ref.get("slot"),"type",ref.get("type")))));
        }
        BigDecimal points=decimal(q.get("points"));ledger(s,id,"task:"+id+":reserve","RESERVE",points.negate(),points,BigDecimal.ZERO,"生成提交预占积分");
        event(s,id,null,"QUEUED","已进入队列，预占 "+points.toPlainString()+" 积分");
        db.update("UPDATE aigc_conversation SET last_activity_time=CURRENT_TIMESTAMP(3) WHERE tenant_id=? AND workspace_id=? AND id=?",s.tenant(),s.workspace(),conversation);
        return map("id",String.valueOf(id),"reused",false,"reservedPoints",points);
    });}
    public Map<String,Object> retry(long task,Map<String,Object> body){var s=studio.scope();var old=studio.owned("aigc_task",s,task);if(!TERMINAL.contains(old.get("status")))throw studio.error(400,"请等待当前任务结束后再次生成");return create(map("conversationId",body.getOrDefault("conversationId",old.get("conversation_id")),"clientRequestId",body.get("clientRequestId"),"operation",old.get("operation"),"draft",object(old.get("parameter_snapshot")),"retryOfTaskId",String.valueOf(task)));}
    public Map<String,Object> list(long before,int limit,String filter){var s=studio.scope();if(before<=0||limit<1||limit>100)throw studio.error(400,"分页参数错误");
        if(!filter.equals("ALL")&&!filter.equals("ACTIVE")&&!TERMINAL.contains(filter))throw studio.error(400,"任务筛选条件错误");
        String clause=filter.equals("ALL")?"":filter.equals("ACTIVE")?" AND status NOT IN ('SUCCEEDED','PARTIAL_SUCCEEDED','FAILED','CANCELED','TIMED_OUT')":" AND status='"+filter+"'";
        var rows=db.queryForList("SELECT * FROM aigc_task WHERE tenant_id=? AND workspace_id=? AND deleted=0 AND id<?"+clause+" ORDER BY id DESC LIMIT ?",s.tenant(),s.workspace(),before,limit);
        return map("tasks",rows.stream().map(this::dto).toList(),"nextId",rows.size()==limit?rows.getLast().get("id").toString():null,"quota",quota(s));
    }
    private Map<String,Object> quota(StudioService.Scope s){return studio.one("SELECT available_points,reserved_points,consumed_points FROM aigc_quota_account WHERE tenant_id=? AND workspace_id=?",s.tenant(),s.workspace());}
    private Map<String,Object> dto(Map<String,Object> t){var p=object(t.get("parameter_snapshot"));return map("id",t.get("id").toString(),"taskNo",t.get("task_no"),"conversationId",Objects.toString(t.get("conversation_id"),""),"operation",t.get("operation"),"prompt",t.get("prompt"),"model",p.get("model"),"status",t.get("status"),"progress",t.get("progress"),"pricing",object(t.get("pricing_snapshot")),"errorCode",t.get("error_code"),"errorMessage",t.get("error_message"),"retryOfTaskId",t.get("retry_of_task_id")==null?null:t.get("retry_of_task_id").toString(),"created",studio.time(t.get("create_time")),"updated",studio.time(t.get("update_time")));}
    public Map<String,Object> detail(long task){var s=studio.scope();var t=studio.owned("aigc_task",s,task);var result=dto(t);result.put("parameters",object(t.get("parameter_snapshot")));
        result.put("events",db.queryForList("SELECT id,from_status,to_status,message,create_time FROM aigc_task_event WHERE tenant_id=? AND workspace_id=? AND task_id=? ORDER BY id",s.tenant(),s.workspace(),task).stream().map(e->map("id",e.get("id").toString(),"from",e.get("from_status"),"to",e.get("to_status"),"message",e.get("message"),"created",studio.time(e.get("create_time")))).toList());
        result.put("results",db.queryForList("SELECT r.result_index,r.status,r.asset_id,a.deleted AS trashed FROM aigc_task_result r LEFT JOIN aigc_asset a ON a.id=r.asset_id AND a.tenant_id=r.tenant_id AND a.workspace_id=r.workspace_id WHERE r.tenant_id=? AND r.workspace_id=? AND r.task_id=? ORDER BY r.result_index",s.tenant(),s.workspace(),task).stream().map(r->map("index",r.get("result_index"),"status",r.get("status"),"assetId",r.get("asset_id")==null?null:r.get("asset_id").toString(),"trashed",r.get("trashed"))).toList());return result;}
    public Map<String,Object> ledgerPage(long before,int limit){var s=studio.scope();if(before<=0||limit<1||limit>100)throw studio.error(400,"分页参数错误");var rows=db.queryForList("SELECT id,task_id,entry_type,available_delta,reserved_delta,consumed_delta,available_after,reserved_after,consumed_after,remark,create_time FROM aigc_quota_ledger WHERE tenant_id=? AND workspace_id=? AND id<? ORDER BY id DESC LIMIT ?",s.tenant(),s.workspace(),before,limit);
        for(var row:rows){row.put("id",row.get("id").toString());if(row.get("task_id")!=null)row.put("task_id",row.get("task_id").toString());row.put("created",studio.time(row.remove("create_time")));}return map("entries",rows,"nextId",rows.size()==limit?rows.getLast().get("id"):null,"quota",quota(s));}
    public void cancel(long task){tx.executeWithoutResult(status->{var s=studio.scope();var t=studio.one("SELECT * FROM aigc_task WHERE tenant_id=? AND workspace_id=? AND id=? AND deleted=0 FOR UPDATE",s.tenant(),s.workspace(),task);if(TERMINAL.contains(t.get("status")))return;if(!Set.of("QUEUED","PENDING").contains(t.get("status")))throw studio.error(400,"任务已提交模型服务，当前渠道不支持中途取消");finish(t,"CANCELED",BigDecimal.ZERO,"USER_CANCELED","用户取消，预占积分已退回");});}
    private void event(StudioService.Scope s,long id,String from,String to,String message){studio.scopedInsert("aigc_task_event",s,map("task_id",id,"event_type","STATUS_CHANGED","from_status",from,"to_status",to,"message",message));}
    private void transition(Map<String,Object> t,String to,int progress,String message){var s=taskScope(t);long id=((Number)t.get("id")).longValue();db.update("UPDATE aigc_task SET status=?,progress=?,revision=revision+1 WHERE tenant_id=? AND workspace_id=? AND id=?",to,progress,s.tenant(),s.workspace(),id);event(s,id,string(t.get("status")),to,message);t.put("status",to);}
    private void finish(Map<String,Object> t,String state,BigDecimal charge,String code,String message){
        if(TERMINAL.contains(t.get("status")))return;var s=taskScope(t);long id=((Number)t.get("id")).longValue();var pricing=object(t.get("pricing_snapshot"));BigDecimal reserved=decimal(pricing.get("reservedPoints"));
        if(charge.signum()>0)ledger(s,id,"task:"+id+":settle","SETTLE",BigDecimal.ZERO,charge.negate(),charge,"生成结果归档成功结算");
        BigDecimal refund=reserved.subtract(charge);if(refund.signum()>0)ledger(s,id,"task:"+id+":release","RELEASE",refund,refund.negate(),BigDecimal.ZERO,"未交付部分积分退回");
        transition(t,state,state.equals("SUCCEEDED")||state.equals("PARTIAL_SUCCEEDED")?100:number(t.get("progress"),0),message);
        db.update("UPDATE aigc_task SET finished_time=CURRENT_TIMESTAMP(3),lease_owner=NULL,lease_until=NULL,error_code=?,error_message=? WHERE tenant_id=? AND workspace_id=? AND id=?",code,message,s.tenant(),s.workspace(),id);
        db.update("UPDATE aigc_task_attempt SET status=?,error_code=?,finished_time=CURRENT_TIMESTAMP(3) WHERE tenant_id=? AND workspace_id=? AND task_id=? AND status IN ('SUBMITTING','RUNNING','UNKNOWN')",state.equals("SUCCEEDED")||state.equals("PARTIAL_SUCCEEDED")?"SUCCEEDED":state.equals("CANCELED")?"CANCELED":"FAILED",code,s.tenant(),s.workspace(),id);
    }
    public List<Long> due(){return db.queryForList("SELECT id FROM aigc_task WHERE deleted=0 AND status IN ('QUEUED','SUBMITTING','RUNNING','MATERIALIZING') AND (next_run_time IS NULL OR next_run_time<=CURRENT_TIMESTAMP(3)) AND (lease_until IS NULL OR lease_until<CURRENT_TIMESTAMP(3)) ORDER BY id LIMIT 2",Long.class);}
    public void execute(long id){
        Map<String,Object> task=tx.execute(status->{var rows=db.queryForList("SELECT * FROM aigc_task WHERE id=? AND deleted=0 AND status IN ('QUEUED','SUBMITTING','RUNNING','MATERIALIZING') AND (lease_until IS NULL OR lease_until<CURRENT_TIMESTAMP(3)) FOR UPDATE",id);if(rows.isEmpty())return null;var t=rows.getFirst();db.update("UPDATE aigc_task SET lease_owner='java-worker',lease_until=DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL 5 MINUTE) WHERE id=?",id);return t;});
        if(task==null)return;var s=taskScope(task);TenantContextHolder.setTenantId(s.tenant());
        try{run(task);}catch(GenerationProvider.ProviderException e){tx.executeWithoutResult(status->{var current=studio.one("SELECT * FROM aigc_task WHERE id=? FOR UPDATE",id);finish(current,"FAILED",BigDecimal.ZERO,e.code,e.getMessage());});}
        catch(Exception e){tx.executeWithoutResult(status->{var current=studio.one("SELECT * FROM aigc_task WHERE id=? FOR UPDATE",id);finish(current,"FAILED",BigDecimal.ZERO,"GENERATION_ERROR","生成或结果归档失败，预占积分已退回；请查看任务后重新生成");});}
        finally{TenantContextHolder.clear();}
    }
    private void run(Map<String,Object> task) throws Exception {
        long id=((Number)task.get("id")).longValue();var s=taskScope(task);var p=object(task.get("parameter_snapshot"));String adapter=string(p.get("adapter"));
        var registry=studio.one("SELECT k.id,k.url,k.api_key FROM ai_model m JOIN ai_api_key k ON k.id=m.key_id WHERE m.id=? AND k.status=0 AND k.deleted=0",task.get("ai_model_id"));
        var attempts=db.queryForList("SELECT * FROM aigc_task_attempt WHERE tenant_id=? AND workspace_id=? AND task_id=? ORDER BY attempt_no DESC LIMIT 1",s.tenant(),s.workspace(),id);
        JsonNode response;long attempt;
        if(task.get("status").equals("MATERIALIZING")&&!attempts.isEmpty()){
            attempt=((Number)attempts.getFirst().get("id")).longValue();response=new com.fasterxml.jackson.databind.ObjectMapper().readTree(string(attempts.getFirst().get("response_summary")));
        }else if(task.get("status").equals("QUEUED")){
            attempt=Objects.requireNonNull(tx.execute(status->{transition(task,"SUBMITTING",10,"正在提交模型服务");db.update("UPDATE aigc_task SET started_time=CURRENT_TIMESTAMP(3) WHERE id=?",id);return studio.scopedInsert("aigc_task_attempt",s,map("task_id",id,"attempt_no",1,"api_key_id",registry.get("id"),"provider_request_key","zaomeng-"+task.get("task_no"),"status","SUBMITTING"));}));
            var inputs=inputUrls(s,p,id);String path;Map<String,Object> body;
            if(adapter.equals("image")){
                path=p.get("modelCode").equals("Z-Image-Base")?"/image/v1/generations":"/qwen-image/v1/generations";body=map("model",p.get("modelCode"),"prompt",p.get("prompt"),"n",p.get("count"),"ratio",p.get("ratio"),"resolution",p.get("resolution"),"negative_prompt",p.getOrDefault("negativePrompt",""));
            }else if(adapter.equals("edit")){
                path="/image-edit/v1/edits";body=map("model",p.get("modelCode"),"prompt",p.get("prompt"),"images",legacyImageInputs(s,p),"ratio",p.get("ratio"),"resolution",p.get("resolution"));
            }else if(isVideoAdapter(adapter)){
                path=videoPrefix(adapter)+"/v1/video/generateVideo";
                List<Map<String,Object>> refs=new ArrayList<>(inputs.stream().map(input->map("type","image_url","role","reference_image","image_url",map("url",input))).toList());
                var metadata=map("resolution",p.get("resolution"),"references",refs);
                if(adapter.equals("best-h3")){
                    refs.addAll(attachmentUrls(s,p,id));
                    if(p.containsKey("seed"))metadata.put("seed",p.get("seed"));
                }else{metadata.put("ratio",p.get("ratio"));metadata.put("generate_audio",true);}
                body=map("model",p.get("modelCode"),"prompt",p.get("prompt"),"duration",p.get("videoSeconds"),"metadata",metadata);
                if(!adapter.equals("best-h3"))body.put("resolution",p.get("resolution"));
            }else{
                if(adapter.equals("speech")){path="/audio/v1/voice-design";body=map("model",p.get("modelCode"),"text",p.get("prompt"),"voice_prompt",p.get("voiceDesign"),"speed",p.getOrDefault("speed",0.9));}
                else{path="/audio/v1/bgm";body=map("model",p.get("modelCode"),"prompt",p.get("prompt"),"duration",p.get("musicSeconds"));}
            }
            response=provider.call(string(registry.get("url")),string(registry.get("api_key")),"POST",path,body,"zaomeng-"+task.get("task_no"));
        }else{
            if(attempts.isEmpty())throw new GenerationProvider.ProviderException("TASK_RECOVERY_ERROR","任务执行记录缺失，预占积分退回",false);
            var a=attempts.getFirst();attempt=((Number)a.get("id")).longValue();String job=string(a.get("provider_task_id"));
            if(job.isBlank())throw new GenerationProvider.ProviderException("PROVIDER_UNCERTAIN","上次提交状态未确认，为避免重复扣费不会自动重提，平台预占积分已退回",true);
            if(studio.time(task.get("started_time"))<System.currentTimeMillis()-30*60*1000)throw new GenerationProvider.ProviderException("PROVIDER_TIMEOUT","模型任务超过 30 分钟未完成，平台预占积分已退回",false);
            boolean video=isVideoAdapter(adapter);
            String prefix=adapter.equals("image")?(p.get("modelCode").equals("Z-Image-Base")?"/image/v1/tasks/":"/qwen-image/v1/tasks/"):adapter.equals("edit")?"/image-edit/v1/tasks/":"/audio/v1/tasks/";
            String path=video?videoPrefix(adapter)+"/v1/video/getVideoStatus":prefix+URLEncoderSafe(job);
            response=provider.call(string(registry.get("url")),string(registry.get("api_key")),video?"POST":"GET",path,video?map("taskICode",job):null,"zaomeng-"+task.get("task_no"));
        }
        String state=provider.status(response);if(Set.of("FAILED","FAILURE","ERROR","CANCELED","CANCELLED").contains(state))throw new GenerationProvider.ProviderException("PROVIDER_TASK_FAILED","模型服务返回任务失败，预占积分已退回",false);
        String responseJob=provider.jobId(response);
        String job=responseJob!=null&&responseJob.matches("task_[A-Za-z0-9]+")?responseJob:attempts.isEmpty()?"":string(attempts.getFirst().get("provider_task_id"));
        if(!job.matches("task_[A-Za-z0-9]+"))throw new GenerationProvider.ProviderException("INVALID_JOB_ID","模型返回的任务编号无法识别，请联系管理员核对",false);
        db.update("UPDATE aigc_task_attempt SET provider_task_id=? WHERE id=?",job,attempt);
        List<String> outputs=new ArrayList<>();
        if(Set.of("SUCCESS","SUCCEEDED").contains(state)){
            if(isVideoAdapter(adapter))outputs.add("/v1/tasks/"+job+"/artifacts/video/content");
            else if(adapter.equals("speech")||adapter.equals("music"))outputs.add("/v1/tasks/"+job+"/artifacts/audio/content");
            else for(JsonNode output:response.path("data")){
                java.net.URI uri=java.net.URI.create(output.path("url").asText());String path=uri.getPath();
                if((uri.getHost()!=null&&!uri.getHost().equals("api.safillu.com"))||uri.getRawQuery()!=null||!path.matches("/v1/tasks/"+job+"/artifacts/image(?:_[1-4])?/content"))throw new GenerationProvider.ProviderException("INVALID_OUTPUT","模型返回的成品编号错误",false);
                outputs.add(path);
            }
            if(outputs.isEmpty())throw new GenerationProvider.ProviderException("NO_OUTPUT","模型未交付成品，积分退回",false);
        }
        if(outputs.isEmpty()){

            tx.executeWithoutResult(status->{var current=studio.one("SELECT * FROM aigc_task WHERE id=? FOR UPDATE",id);if(!current.get("status").equals("RUNNING"))transition(current,"RUNNING",35,"模型正在生成，等待服务商返回结果");db.update("UPDATE aigc_task_attempt SET provider_task_id=?,status='RUNNING' WHERE id=?",job,attempt);db.update("UPDATE aigc_task SET lease_until=NULL,lease_owner=NULL,next_run_time=DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL 8 SECOND) WHERE id=?",id);});return;
        }
        tx.executeWithoutResult(status->{var current=studio.one("SELECT * FROM aigc_task WHERE id=? FOR UPDATE",id);if(!current.get("status").equals("MATERIALIZING"))transition(current,"MATERIALIZING",85,"生成完成，正在将原始文件归档到个人资产");db.update("UPDATE aigc_task_attempt SET response_summary=? WHERE id=?",studio.encode(response),attempt);});
        List<GenerationProvider.Media> media=new ArrayList<>();int requested=((Number)task.get("requested_count")).intValue();
        for(String output:outputs.subList(0,Math.min(requested,outputs.size())))media.add(provider.artifact(string(registry.get("url")),string(registry.get("api_key")),output));
        archive(task,attempt,media);
    }
    private String URLEncoderSafe(String s){return java.net.URLEncoder.encode(s,java.nio.charset.StandardCharsets.UTF_8);}
    private boolean isVideoAdapter(String adapter){return Set.of("video","selflift","best-h3").contains(adapter);}
    private String videoPrefix(String adapter){return switch(adapter){case "selflift"->"/h3-selflift";case "best-h3"->"/best-minimax-h3";default->"/tfrouter-frames";};}
    @SuppressWarnings("unchecked") private List<String> legacyImageInputs(StudioService.Scope s,Map<String,Object> p){
        var result=new ArrayList<String>();
        for(var ref:(List<Map<String,Object>>)p.getOrDefault("refs",List.of())){
            var file=studio.one("SELECT i.config_id,i.path,f.mime_type FROM aigc_asset_file f JOIN infra_file i ON i.id=f.infra_file_id AND i.deleted=0 WHERE f.tenant_id=? AND f.workspace_id=? AND f.asset_id=? AND f.variant_code='ORIGINAL' AND f.deleted=0",s.tenant(),s.workspace(),studio.id(ref.get("assetId")));
            result.add(references.legacyImageDataUrl(((Number)file.get("config_id")).longValue(),string(file.get("path")),string(file.get("mime_type"))));
        }
        return result;
    }
    @SuppressWarnings("unchecked") private List<Map<String,Object>> attachmentUrls(StudioService.Scope s,Map<String,Object> p,long task){
        var result=new ArrayList<Map<String,Object>>();
        for(var ref:(List<Map<String,Object>>)p.getOrDefault("attachments",List.of())){
            var file=studio.one("SELECT i.config_id,i.path,f.mime_type FROM aigc_asset_file f JOIN infra_file i ON i.id=f.infra_file_id AND i.deleted=0 WHERE f.tenant_id=? AND f.workspace_id=? AND f.asset_id=? AND f.variant_code='ORIGINAL' AND f.deleted=0",s.tenant(),s.workspace(),studio.id(ref.get("assetId")));
            String mime=string(file.get("mime_type")),type=mime.startsWith("audio/")?"audio_url":"video_url",role=mime.startsWith("audio/")?"reference_audio":"reference_video";
            String url=references.mediaUrl(((Number)file.get("config_id")).longValue(),string(file.get("path")),mime,s.tenant(),s.workspace(),task);
            result.add(map("type",type,"role",role,type,map("url",url)));
        }
        return result;
    }
    @SuppressWarnings("unchecked") private List<String> inputUrls(StudioService.Scope s,Map<String,Object> p,long task) throws Exception {
        var result=new ArrayList<String>();for(var ref:(List<Map<String,Object>>)p.getOrDefault("refs",List.of())){
            var f=studio.one("SELECT i.config_id,i.path,f.mime_type FROM aigc_asset_file f JOIN infra_file i ON i.id=f.infra_file_id AND i.deleted=0 WHERE f.tenant_id=? AND f.workspace_id=? AND f.asset_id=? AND f.variant_code='ORIGINAL' AND f.deleted=0",s.tenant(),s.workspace(),studio.id(ref.get("assetId")));
            result.add(references.imageUrl(((Number)f.get("config_id")).longValue(),string(f.get("path")),string(f.get("mime_type")),s.tenant(),s.workspace(),task));
        }return result;
    }
    private String imageSize(String ratio,String resolution){String[] parts=ratio.split(":");double r=Double.parseDouble(parts[0])/Double.parseDouble(parts[1]);int pixels=resolution.equals("2K")?2048:1024;int w=(int)(pixels*Math.sqrt(r)/64)*64,h=(int)(pixels/Math.sqrt(r)/64)*64;return w+"x"+h;}
    private void archive(Map<String,Object> task,long attempt,List<GenerationProvider.Media> media) throws Exception {
        var s=taskScope(task);long id=((Number)task.get("id")).longValue();String operation=string(task.get("operation"));var stored=new ArrayList<Map<String,Object>>();
        for(int index=0;index<media.size();index++){
            var item=media.get(index);String mime=FileTypeUtils.getMineType(item.bytes(),operation.equals("IMAGE")?"result.png":operation.equals("VIDEO")?"result.mp4":"result.wav");
            if(operation.equals("IMAGE")&&!mime.startsWith("image/")||operation.equals("VIDEO")&&!mime.startsWith("video/")||Set.of("AUDIO","MUSIC").contains(operation)&&!mime.startsWith("audio/"))throw new GenerationProvider.ProviderException("INVALID_MEDIA","模型返回的文件与创作类型不符，积分已退回",false);
            String extension=FileTypeUtils.getExtension(mime);
            if(extension==null||!extension.matches("\\.[a-zA-Z0-9]{1,12}"))throw new GenerationProvider.ProviderException("INVALID_MEDIA","模型返回的文件格式不受支持，积分已退回",false);
            String url=files.createFlatFile(item.bytes(),"result"+extension,mime);
            var storedFile=studio.one("SELECT id,name FROM infra_file WHERE url=? AND deleted=0 ORDER BY id DESC LIMIT 1",url);
            long file=((Number)storedFile.get("id")).longValue();String name=storedFile.get("name").toString();
            Integer width=null,height=null;if(operation.equals("IMAGE")){var image=ImageIO.read(new ByteArrayInputStream(item.bytes()));if(image!=null){width=image.getWidth();height=image.getHeight();}}
            stored.add(map("file",file,"mime",mime,"name",name,"size",item.bytes().length,"width",width,"height",height));
        }
        tx.executeWithoutResult(status->{var current=studio.one("SELECT * FROM aigc_task WHERE id=? FOR UPDATE",id);if(TERMINAL.contains(current.get("status")))return;
            int index=0;for(var f:stored){long asset=studio.scopedInsert("aigc_asset",s,map("kind",operation,"source","GENERATED","status","READY","title",f.get("name"),"description",task.get("prompt"),"width",f.get("width"),"height",f.get("height")));
                studio.scopedInsert("aigc_asset_file",s,map("asset_id",asset,"variant_code","ORIGINAL","infra_file_id",f.get("file"),"mime_type",f.get("mime"),"byte_size",f.get("size"),"width",f.get("width"),"height",f.get("height")));
                studio.scopedInsert("aigc_task_result",s,map("task_id",id,"attempt_id",attempt,"result_index",++index,"asset_id",asset,"status","READY"));}
            var pricing=object(current.get("pricing_snapshot"));BigDecimal reserved=decimal(pricing.get("reservedPoints")),charge=operation.equals("IMAGE")?decimal(pricing.get("rate")).multiply(BigDecimal.valueOf(stored.size())):reserved;
            studio.scopedInsert("aigc_usage_record",s,map("task_id",id,"attempt_id",attempt,"provider_usage_key","archive:"+id,"unit_type",operation.equals("IMAGE")?"IMAGE":"REQUEST","units",operation.equals("IMAGE")?stored.size():1,"usage_summary",studio.encode(map("archivedOutputs",stored.size(),"requestedUnit",pricing.get("unit"),"requestedUnits",pricing.get("units"),"supplierMoneyKnown",false,"supplierReportedUsageKnown",false))));
            finish(current,stored.size()<((Number)current.get("requested_count")).intValue()?"PARTIAL_SUCCEEDED":"SUCCEEDED",charge,null,"结果已保存至个人资产，结算 "+charge.toPlainString()+" 积分");
        });
    }
}
