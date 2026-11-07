package cn.iocoder.yudao.module.aigc.service;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import static cn.iocoder.yudao.module.aigc.service.StudioService.map;

/** Native Yudao RBAC over the very same creator tables; never a second account or billing store. */
@Service
public class StudioAdminService {
    private final JdbcTemplate db;
    private final StudioService studio;
    private final PermissionApi permissions;
    private final FileService files;
    public StudioAdminService(JdbcTemplate db,StudioService studio,PermissionApi permissions,FileService files){this.db=db;this.studio=studio;this.permissions=permissions;this.files=files;}
    private long tenant(){return TenantContextHolder.getRequiredTenantId();}
    private String permission(String resource){return switch(resource){case "tasks"->"task";case "assets"->"asset";case "workspaces"->"workspace";case "ledger"->"credit";case "models"->"model";case "orders","packs"->"order";case "conversations"->"conversation";case "subjects"->"subject";case "voices"->"voice";case "motions"->"motion";default->throw studio.error(400,"未知管理资源");};}
    private long require(String resource,String action){Long user=SecurityFrameworkUtils.getLoginUserId();if(user==null)throw studio.error(401,"请先登录");if(SecurityFrameworkUtils.getLoginUser().getUserType()!=2||!permissions.hasAnyPermissions(user,"aigc:"+permission(resource)+":"+action))throw studio.error(403,"当前操作没有权限");return user;}
    public Map<String,Object> overview(){Long user=SecurityFrameworkUtils.getLoginUserId();if(user==null)throw studio.error(401,"请先登录");var result=new LinkedHashMap<String,Object>();String[][] counts={{"users","system:user:query","system_users"},{"tasks","aigc:task:query","aigc_task"},{"assets","aigc:asset:query","aigc_asset"},{"workspaces","aigc:workspace:query","aigc_workspace"},{"orders","aigc:order:query","aigc_credit_purchase"}};for(var item:counts)if(permissions.hasAnyPermissions(user,item[1]))result.put(item[0],db.queryForObject("SELECT COUNT(*) FROM "+item[2]+" WHERE tenant_id=? AND deleted=0",Long.class,tenant()));if(permissions.hasAnyPermissions(user,"aigc:model:query"))result.put("models",db.queryForObject("SELECT COUNT(*) FROM aigc_model_capability WHERE deleted=0",Long.class));return result;}
    private record Spec(String from,String fields,String condition,String search,String status){}
    private Spec spec(String resource){
        String owner=" LEFT JOIN aigc_workspace w ON w.id=x.workspace_id AND w.tenant_id=x.tenant_id LEFT JOIN system_users u ON u.id=w.owner_user_id AND u.tenant_id=w.tenant_id ";
        String user="u.username,u.nickname,x.workspace_id AS workspaceId,";
        String condition="x.tenant_id=? AND x.deleted=0";
        return switch(resource){
            case "tasks"->new Spec("aigc_task x"+owner+" LEFT JOIN ai_model m ON m.id=x.ai_model_id",user+"x.id,x.task_no AS taskNo,x.operation,x.prompt,x.status,x.progress,x.retry_of_task_id AS retryOfTaskId,x.requested_count AS count,m.name AS modelName,x.error_code AS errorCode,x.error_message AS errorMessage,x.create_time AS created,x.finished_time AS finished",condition,"CONCAT_WS(' ',u.username,x.task_no,x.prompt)","x.status");
            case "assets"->new Spec("aigc_asset x"+owner,user+"x.id,x.title,x.kind,x.source,x.status,x.deleted AS trashed,x.width,x.height,x.duration_ms AS durationMs,x.description,x.create_time AS created", "x.tenant_id=? AND (x.deleted=0 OR x.trashed_time IS NOT NULL)","CONCAT_WS(' ',u.username,x.title,x.description)","CASE WHEN x.deleted=1 THEN 'TRASHED' ELSE x.status END");
            case "workspaces"->new Spec("aigc_workspace x LEFT JOIN system_users u ON u.id=x.owner_user_id AND u.tenant_id=x.tenant_id LEFT JOIN aigc_quota_account q ON q.workspace_id=x.id AND q.tenant_id=x.tenant_id AND q.deleted=0","x.id,x.owner_user_id AS userId,u.username,u.nickname,x.name,x.status,q.available_points AS available,q.reserved_points AS reserved,q.consumed_points AS consumed,x.create_time AS created",condition,"CONCAT_WS(' ',u.username,u.nickname,x.name)","x.status");
            case "ledger"->new Spec("aigc_quota_ledger x"+owner,user+"x.id,x.task_id AS taskId,x.entry_type AS entryType,x.business_key AS businessKey,x.available_delta AS availableDelta,x.reserved_delta AS reservedDelta,x.consumed_delta AS consumedDelta,x.available_after AS availableAfter,x.reserved_after AS reservedAfter,x.consumed_after AS consumedAfter,x.remark,x.creator,x.create_time AS created",condition,"CONCAT_WS(' ',u.username,x.business_key,x.remark)","x.entry_type");
            case "models"->new Spec("aigc_model_capability x JOIN ai_model m ON m.id=x.ai_model_id AND m.deleted=0 LEFT JOIN ai_api_key k ON k.id=m.key_id AND k.deleted=0","x.id,x.ai_model_id AS modelId,m.name,m.model,k.platform,x.operation,x.status,x.billing_unit AS billingUnit,x.points_per_unit AS pointsPerUnit,x.config_version AS configVersion,x.default_parameters AS defaults,x.limits_config AS limits,x.parameter_schema AS parameterSchema,x.update_time AS updated", "x.deleted=0", "CONCAT_WS(' ',m.name,m.model,x.operation)","x.status");
            case "orders"->new Spec("aigc_credit_purchase x"+owner+" JOIN pay_order p ON p.id=x.pay_order_id",user+"x.id,x.purchase_no AS purchaseNo,x.pay_order_id AS payOrderId,x.pack_name AS packName,x.environment,x.currency,x.amount_minor AS amountMinor,x.credit_points AS creditPoints,x.status,x.credited,x.billing_hold AS billingHold,x.refund_minor AS refundMinor,x.checkout_session_id AS checkoutSessionId,x.payment_intent_id AS paymentIntentId,p.status AS payOrderStatus,x.create_time AS created,x.paid_time AS paid",condition,"CONCAT_WS(' ',u.username,x.purchase_no,x.pack_name)","x.status");
            case "packs"->new Spec("aigc_credit_pack x","x.id,x.code,x.name,x.environment,x.currency,x.amount_minor AS amountMinor,x.credit_points AS creditPoints,x.enabled,x.sort_order AS sort,x.update_time AS updated","x.deleted=0","CONCAT_WS(' ',x.name,x.code)","x.environment");
            case "conversations"->new Spec("aigc_conversation x"+owner,user+"x.id,x.title,x.pinned,x.is_default AS isDefault,x.last_activity_time AS updated,x.create_time AS created",condition,"CONCAT_WS(' ',u.username,x.title)",null);
            case "subjects"->new Spec("aigc_subject x"+owner,user+"x.id,x.name,x.kind,x.status,x.cover_asset_id AS coverAssetId,x.current_version_no AS version,x.create_time AS created",condition,"CONCAT_WS(' ',u.username,x.name)","x.status");
            case "voices"->new Spec("aigc_voice x"+owner,user+"x.id,x.name,x.visibility,x.gender,x.age_group AS ageGroup,x.language_code AS language,x.status,x.create_time AS created","(x.tenant_id=? OR (x.tenant_id=0 AND x.visibility='PUBLIC')) AND x.deleted=0","CONCAT_WS(' ',u.username,x.name)","x.status");
            case "motions"->new Spec("aigc_motion_template x"+owner,user+"x.id,x.name,x.visibility,x.duration_ms AS durationMs,x.status,x.create_time AS created","(x.tenant_id=? OR (x.tenant_id=0 AND x.visibility='PUBLIC')) AND x.deleted=0","CONCAT_WS(' ',u.username,x.name)","x.status");
            default->throw studio.error(400,"未知管理资源");
        };
    }
    private List<Object> args(String resource){return new ArrayList<>(Set.of("models","packs").contains(resource)?List.of():List.of(tenant()));}
    private Map<String,Object> dto(Map<String,Object> row){
        var result=new LinkedHashMap<String,Object>(row);
        for(var key:new ArrayList<>(result.keySet())){
            Object value=result.get(key);
            if(value==null)continue;
            if(key.equals("id")||key.endsWith("Id")||key.equals("creator"))result.put(key,value.toString());
            else if(Set.of("created","updated","finished","paid").contains(key))result.put(key,studio.time(value));
            else if(Set.of("defaults","limits","parameterSchema").contains(key))result.put(key,studio.decode(value,Map.of()));
        }
        return result;
    }
    public Map<String,Object> page(String resource,int pageNo,int pageSize,String q,String status){
        require(resource,"query");if(pageNo<1||pageNo>100000||pageSize<1||pageSize>100||q.length()>200||status.length()>32)throw studio.error(400,"分页参数错误");
        var spec=spec(resource);var values=args(resource);String where=spec.condition;
        if(!q.isBlank()){where+=" AND "+spec.search+" LIKE ?";values.add("%"+q.trim()+"%");}
        if(!status.isBlank()&&spec.status!=null){where+=" AND "+spec.status+"=?";values.add(status);}
        long total=Objects.requireNonNull(db.queryForObject("SELECT COUNT(*) FROM "+spec.from+" WHERE "+where,Long.class,values.toArray()));
        values.add(pageSize);values.add((pageNo-1)*pageSize);
        var rows=db.queryForList("SELECT "+spec.fields+" FROM "+spec.from+" WHERE "+where+" ORDER BY x.id DESC LIMIT ? OFFSET ?",values.toArray());
        return map("list",rows.stream().map(this::dto).toList(),"total",total);
    }
    public Map<String,Object> detail(String resource,long id){
        require(resource,"query");var spec=spec(resource);var values=args(resource);values.add(id);
        var result=dto(studio.one("SELECT "+spec.fields+" FROM "+spec.from+" WHERE "+spec.condition+" AND x.id=?",values.toArray()));
        if(resource.equals("tasks")){
            var task=studio.one("SELECT parameter_snapshot,pricing_snapshot FROM aigc_task WHERE tenant_id=? AND id=? AND deleted=0",tenant(),id);
            result.put("parameters",studio.decode(task.get("parameter_snapshot"),Map.of()));result.put("pricing",studio.decode(task.get("pricing_snapshot"),Map.of()));
            result.put("events",db.queryForList("SELECT id,from_status AS fromStatus,to_status AS toStatus,message,create_time AS created FROM aigc_task_event WHERE tenant_id=? AND task_id=? AND deleted=0 ORDER BY id",tenant(),id).stream().map(this::dto).toList());
        }
        if(resource.equals("conversations"))result.put("tasks",db.queryForList("SELECT id,operation,prompt,status,progress,create_time AS created FROM aigc_task WHERE tenant_id=? AND conversation_id=? AND deleted=0 ORDER BY id DESC LIMIT 200",tenant(),id).stream().map(this::dto).toList());
        return result;
    }
    @Transactional public void adjustCredits(long workspace,Map<String,Object> body){
        long actor=require("ledger","manage");String reason=Objects.toString(body.get("reason"),"").trim(),key=Objects.toString(body.get("idempotencyKey"),"");
        BigDecimal delta;try{delta=new BigDecimal(Objects.toString(body.get("delta"),""));}catch(Exception e){throw studio.error(400,"积分调整参数错误");}
        if(reason.isBlank()||reason.length()>400||!key.matches("[A-Za-z0-9_-]{8,64}")||delta.signum()==0||delta.abs().compareTo(new BigDecimal("1000000"))>0||delta.scale()>4)throw studio.error(400,"积分调整参数错误");
        studio.one("SELECT id FROM aigc_workspace WHERE tenant_id=? AND id=? AND deleted=0",tenant(),workspace);
        var quota=studio.one("SELECT * FROM aigc_quota_account WHERE tenant_id=? AND workspace_id=? AND deleted=0 FOR UPDATE",tenant(),workspace);
        String business="admin-adjust:"+actor+":"+key;
        var previous=db.queryForList("SELECT available_delta,remark FROM aigc_quota_ledger WHERE tenant_id=? AND workspace_id=? AND business_key=?",tenant(),workspace,business);
        if(!previous.isEmpty()){if(new BigDecimal(previous.getFirst().get("available_delta").toString()).compareTo(delta)!=0||!reason.equals(previous.getFirst().get("remark")))throw studio.error(409,"请求编号已用于其他积分调整");return;}
        BigDecimal available=new BigDecimal(quota.get("available_points").toString()).add(delta);
        if(available.signum()<0)throw studio.error(400,"可用积分不足");
        db.update("UPDATE aigc_quota_account SET available_points=?,revision=revision+1,updater=? WHERE tenant_id=? AND workspace_id=?",available,String.valueOf(actor),tenant(),workspace);
        studio.insert("aigc_quota_ledger",map("tenant_id",tenant(),"workspace_id",workspace,"business_key",business,"entry_type","ADJUST","available_delta",delta,"reserved_delta",BigDecimal.ZERO,"consumed_delta",BigDecimal.ZERO,"available_after",available,"reserved_after",quota.get("reserved_points"),"consumed_after",quota.get("consumed_points"),"remark",reason,"creator",String.valueOf(actor)));
    }
    @Transactional public void updateModel(long id,Map<String,Object> body){
        long actor=require("models","manage");String name=Objects.toString(body.get("name"),"").trim(),status=Objects.toString(body.get("status"),"");
        BigDecimal points;int version;try{points=new BigDecimal(body.get("pointsPerUnit").toString());version=Integer.parseInt(body.get("configVersion").toString());}catch(Exception e){throw studio.error(400,"模型配置参数错误");}
        if(name.isBlank()||name.length()>100||!Set.of("ENABLED","DISABLED").contains(status)||points.signum()<0||points.compareTo(new BigDecimal("1000000"))>0||points.scale()>4)throw studio.error(400,"模型配置参数错误");
        var cap=studio.one("SELECT ai_model_id,config_version,operation,points_per_unit FROM aigc_model_capability WHERE id=? AND deleted=0 FOR UPDATE",id);
        if(((Number)cap.get("config_version")).intValue()!=version)throw studio.error(409,"模型配置已更新，请刷新后重试");
        if("VIDEO".equals(cap.get("operation"))&&new BigDecimal(cap.get("points_per_unit").toString()).compareTo(points)!=0)throw studio.error(400,"视频模型使用现有分辨率分档价格，此处费率只读");
        db.update("UPDATE ai_model SET name=? WHERE id=? AND deleted=0",name,cap.get("ai_model_id"));
        db.update("UPDATE aigc_model_capability SET status=?,points_per_unit=?,config_version=config_version+1,updater=? WHERE id=? AND deleted=0",status,points,String.valueOf(actor),id);
    }
    @Transactional public void workspaceStatus(long id,String status){long actor=require("workspaces","manage");if(!Set.of("ENABLED","DISABLED").contains(status))throw studio.error(400,"空间状态错误");if(db.update("UPDATE aigc_workspace SET status=?,updater=? WHERE tenant_id=? AND id=? AND deleted=0",status,String.valueOf(actor),tenant(),id)!=1)throw studio.error(404,"记录不存在或无权访问");}
    @Transactional public void assetStatus(long id,boolean trash){
        long actor=require("assets","manage");
        var asset=studio.one("SELECT id,workspace_id FROM aigc_asset WHERE tenant_id=? AND id=? AND (deleted=0 OR trashed_time IS NOT NULL) FOR UPDATE",tenant(),id);
        if(trash){studio.trashAssets(new StudioService.Scope(tenant(),studio.id(asset.get("workspace_id")),actor),List.of(String.valueOf(id)));return;}
        studio.one("SELECT f.id FROM aigc_asset_file f JOIN infra_file i ON i.id=f.infra_file_id AND i.deleted=0 WHERE f.tenant_id=? AND f.asset_id=? AND f.variant_code='ORIGINAL' AND f.deleted=0",tenant(),id);
        db.update("UPDATE aigc_asset SET deleted=0,trashed_time=NULL,updater=? WHERE tenant_id=? AND id=?",String.valueOf(actor),tenant(),id);
    }
    public StudioService.Content assetContent(long id)throws Exception{require("assets","query");var asset=studio.one("SELECT title FROM aigc_asset WHERE tenant_id=? AND id=? AND (deleted=0 OR trashed_time IS NOT NULL)",tenant(),id);var file=studio.one("SELECT i.config_id,i.path,i.type FROM aigc_asset_file f JOIN infra_file i ON i.id=f.infra_file_id AND i.deleted=0 WHERE f.tenant_id=? AND f.asset_id=? AND f.variant_code='ORIGINAL' AND f.deleted=0",tenant(),id);byte[] bytes=files.getFileContent(studio.id(file.get("config_id")),file.get("path").toString());if(bytes==null)throw studio.error(404,"文件暂不可用");return new StudioService.Content(bytes,Objects.toString(file.get("type"),"application/octet-stream"),asset.get("title").toString());}
    public StudioService.Content nativeFileContent(long id)throws Exception{Long user=SecurityFrameworkUtils.getLoginUserId();if(user==null||!permissions.hasAnyPermissions(user,"infra:file:query"))throw studio.error(403,"当前操作没有权限");var file=studio.one("SELECT config_id,path,type,name FROM infra_file WHERE id=? AND deleted=0",id);byte[] bytes=files.getFileContent(studio.id(file.get("config_id")),file.get("path").toString());if(bytes==null)throw studio.error(404,"文件暂不可用");return new StudioService.Content(bytes,Objects.toString(file.get("type"),"application/octet-stream"),Objects.toString(file.get("name"),"file"));}
    /** Resolve existing file metadata only; never fetch a caller-supplied external URL. */
    public StudioService.Content resolveNativeFile(String url)throws Exception{
        Long user=SecurityFrameworkUtils.getLoginUserId();
        if(user==null||SecurityFrameworkUtils.getLoginUser().getUserType()!=2)throw studio.error(401,"请先登录");
        if(url==null||url.length()>2048||url.isBlank())throw studio.error(400,"文件地址无效");
        boolean all=permissions.hasAnyPermissions(user,"infra:file:query");
        var file=all?studio.one("SELECT config_id,path,type,name FROM infra_file WHERE url=? AND deleted=0 ORDER BY id DESC LIMIT 1",url):studio.one("SELECT config_id,path,type,name FROM infra_file WHERE url=? AND creator=? AND deleted=0 ORDER BY id DESC LIMIT 1",url,String.valueOf(user));
        byte[] bytes=files.getFileContent(studio.id(file.get("config_id")),file.get("path").toString());
        if(bytes==null)throw studio.error(404,"文件暂不可用");
        return new StudioService.Content(bytes,Objects.toString(file.get("type"),"application/octet-stream"),Objects.toString(file.get("name"),"file"));
    }

}
