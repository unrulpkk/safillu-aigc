package cn.iocoder.yudao.module.aigc.service;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.framework.file.core.utils.FileTypeUtils;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.*;

/** JDBC queries deliberately include tenant AND workspace: no implicit SQL interceptor is assumed. */
@Service
public class StudioService {
    private final JdbcTemplate db;
    private final ObjectMapper json;
    private final FileService files;
    private final AdminUserService users;
    private static final Set<String> OPERATIONS = Set.of("IMAGE","IMAGE_EDIT","VIDEO","MUSIC","AUDIO","AVATAR","MOTION");
    public StudioService(JdbcTemplate db, ObjectMapper json, FileService files, AdminUserService users) {
        this.db=db; this.json=json; this.files=files; this.users=users;
    }
    public record Scope(long tenant, long workspace, long user) {}
    public static Map<String,Object> map(Object... pairs) {
        Map<String,Object> result=new LinkedHashMap<>();
        for(int i=0;i<pairs.length;i+=2) result.put(pairs[i].toString(),pairs[i+1]);
        return result;
    }
    ServiceException error(int code,String text) { return new ServiceException(code,text); }
    private String text(Object value,int max) {
        String s=Objects.toString(value,"").trim();
        if(s.isEmpty()||s.length()>max) throw error(400,"文本不能为空且不能超过 "+max+" 字");
        return s;
    }
    long id(Object value) {
        try { long id=Long.parseLong(value.toString()); if(id>0)return id; } catch(Exception ignored) {}
        throw error(400,"无效的记录编号");
    }
    String encode(Object value) {
        try { return json.writeValueAsString(value); } catch(Exception e) { throw error(400,"参数格式错误"); }
    }
    Object decode(Object value,Object fallback) {
        if(value==null)return fallback;
        try { return json.readValue(value.toString(),Object.class); } catch(Exception e) { return fallback; }
    }
    long time(Object value) { if(value instanceof java.util.Date d)return d.getTime();if(value instanceof java.time.LocalDateTime d)return d.toInstant(java.time.ZoneOffset.UTC).toEpochMilli();if(value instanceof java.time.OffsetDateTime d)return d.toInstant().toEpochMilli();return 0; }
    Map<String,Object> one(String sql,Object... args) {
        var rows=db.queryForList(sql,args); if(rows.isEmpty())throw error(404,"记录不存在或无权访问"); return rows.getFirst();
    }
    long insert(String table,Map<String,Object> values) {
        String sql="INSERT INTO "+table+" ("+String.join(",",values.keySet())+") VALUES ("+String.join(",",Collections.nCopies(values.size(),"?"))+")";
        var keys=new GeneratedKeyHolder();
        db.update(connection->{var ps=connection.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);int i=1;for(Object v:values.values())ps.setObject(i++,v);return ps;},keys);
        return Objects.requireNonNull(keys.getKey()).longValue();
    }
    long scopedInsert(String table,Scope s,Map<String,Object> values) {
        values.putAll(map("tenant_id",s.tenant,"workspace_id",s.workspace,"creator",String.valueOf(s.user),"updater",String.valueOf(s.user)));
        return insert(table,values);
    }
    Map<String,Object> owned(String table,Scope s,long id) {
        return one("SELECT * FROM "+table+" WHERE tenant_id=? AND workspace_id=? AND id=? AND deleted=0",s.tenant,s.workspace,id);
    }
    Scope scope() {
        Long user=SecurityFrameworkUtils.getLoginUserId();
        if(user==null||SecurityFrameworkUtils.getLoginUser().getUserType()!=2)throw error(401,"请先登录");
        long tenant=TenantContextHolder.getRequiredTenantId();
        var rows=db.queryForList("SELECT id,status FROM aigc_workspace WHERE tenant_id=? AND owner_user_type=2 AND active_owner_user_id=?",tenant,user);
        if(rows.isEmpty()) {
            try { insert("aigc_workspace",map("tenant_id",tenant,"owner_user_type",2,"owner_user_id",user,"name","个人创作空间","creator",user.toString())); }
            catch(DuplicateKeyException ignored) { /* Concurrent first requests share the unique owner key. */ }
            rows=db.queryForList("SELECT id,status FROM aigc_workspace WHERE tenant_id=? AND owner_user_type=2 AND active_owner_user_id=?",tenant,user);
        }
        var row=rows.getFirst();
        if(!"ENABLED".equals(row.get("status")))throw error(403,"当前空间已停用");
        Scope s=new Scope(tenant,id(row.get("id")),user);
        if(db.queryForList("SELECT id FROM aigc_quota_account WHERE tenant_id=? AND workspace_id=?",tenant,s.workspace).isEmpty())db.update("INSERT IGNORE INTO aigc_quota_account(tenant_id,workspace_id) VALUES(?,?)",tenant,s.workspace);
        if(db.queryForList("SELECT id FROM aigc_conversation WHERE tenant_id=? AND workspace_id=? AND active_default=1",tenant,s.workspace).isEmpty())db.update("INSERT IGNORE INTO aigc_conversation(tenant_id,workspace_id,title,is_default,pinned,creator) VALUES(?,?,'默认创作',1,1,?)",tenant,s.workspace,user.toString());
        return s;
    }
    @Transactional
    public Map<String,Object> bootstrap() {
        Scope s=scope();var user=users.getUser(s.user);
        var conversations=db.queryForList("SELECT * FROM aigc_conversation WHERE tenant_id=? AND workspace_id=? AND deleted=0 ORDER BY pinned DESC,last_activity_time DESC,id DESC",s.tenant,s.workspace);
        var drafts=db.queryForList("SELECT * FROM aigc_draft WHERE tenant_id=? AND workspace_id=? AND deleted=0",s.tenant,s.workspace);
        List<Map<String,Object>> sessions=new ArrayList<>();
        for(var c:conversations) {
            var d=drafts.stream().filter(x->Objects.equals(x.get("conversation_id"),c.get("id"))).toList();
            Map<String,Object> current=new LinkedHashMap<>();Map<String,Object> byMode=new LinkedHashMap<>();
            d.stream().sorted(Comparator.comparing(x->x.get("update_time").toString())).forEach(x->{
                @SuppressWarnings("unchecked") var parameters=(Map<String,Object>)decode(x.get("parameters"),new LinkedHashMap<>());
                parameters.put("_revision",x.get("revision"));byMode.put(x.get("operation").toString().toLowerCase(),parameters);
                current.clear();current.putAll(parameters);
            });
            current.put("modeDrafts",byMode);
            sessions.add(map("id",c.get("id").toString(),"title",c.get("title"),"pinned",Boolean.TRUE.equals(c.get("pinned")),"isDefault",Boolean.TRUE.equals(c.get("is_default")),"created",time(c.get("last_activity_time")),"draft",current));
        }
        List<Map<String,Object>> assets=assets(s);
        var subjects=db.queryForList("SELECT s.*,v.id AS version_id FROM aigc_subject s LEFT JOIN aigc_subject_version v ON v.tenant_id=s.tenant_id AND v.workspace_id=s.workspace_id AND v.subject_id=s.id AND v.version_no=s.current_version_no WHERE s.tenant_id=? AND s.workspace_id=? AND s.deleted=0 ORDER BY s.id DESC",s.tenant,s.workspace);
        List<Map<String,Object>> subjectList=subjects.stream().map(x->map("id",x.get("id").toString(),"name",x.get("name"),"assetId",Objects.toString(x.get("cover_asset_id"),""),"versionId",Objects.toString(x.get("version_id"),""))).toList();
        var quota=one("SELECT available_points,reserved_points,consumed_points FROM aigc_quota_account WHERE tenant_id=? AND workspace_id=?",s.tenant,s.workspace);
        var capabilities=db.queryForList("SELECT c.id,m.name,m.model,c.operation,c.config_version,c.parameter_schema,c.default_parameters,c.limits_config,c.billing_unit,c.points_per_unit,c.status FROM aigc_model_capability c JOIN ai_model m ON m.id=c.ai_model_id AND m.deleted=0 AND m.status=0 JOIN ai_api_key k ON k.id=m.key_id AND k.deleted=0 AND k.status=0 WHERE c.deleted=0 AND c.status='ENABLED' ORDER BY m.sort,c.id");
        var tasks=db.queryForList("SELECT id,conversation_id,operation,prompt,status,progress,error_code,error_message,create_time FROM aigc_task WHERE tenant_id=? AND workspace_id=? AND deleted=0 ORDER BY id DESC LIMIT 100",s.tenant,s.workspace);
        for(var task:tasks) {task.put("id",task.get("id").toString());if(task.get("conversation_id")!=null)task.put("conversation_id",task.get("conversation_id").toString());}
        return map("user",map("id",String.valueOf(s.user),"username",user.getUsername(),"nickname",user.getNickname()),
            "workspace",map("id",String.valueOf(s.workspace),"name","个人创作空间"),"sessions",sessions,"records",assets,"assetNextId",assets.size()==500?assets.getLast().get("id"):null,"subjects",subjectList,
            "credits",quota.get("available_points"),"quota",quota,"capabilities",capabilities,"tasks",tasks,
            "voices",catalog(s,"voice"),"motionTemplates",catalog(s,"motion_template"),"voiceLikes",favorites(s,"voice_id"));
    }
    private List<String> favorites(Scope s,String column) {
        return db.queryForList("SELECT "+column+" FROM aigc_favorite WHERE tenant_id=? AND workspace_id=? AND deleted=0 AND "+column+" IS NOT NULL",s.tenant,s.workspace).stream().map(x->x.get(column).toString()).toList();
    }
    private List<Map<String,Object>> catalog(Scope s,String kind) {
        var rows=db.queryForList("SELECT * FROM aigc_"+kind+" WHERE deleted=0 AND status='ENABLED' AND ((visibility='PUBLIC' AND tenant_id=0) OR (visibility='PRIVATE' AND tenant_id=? AND workspace_id=?)) ORDER BY id",s.tenant,s.workspace);
        return rows.stream().map(x->{var v=map("id",x.get("id").toString(),"name",x.get("name"),"visibility",x.get("visibility"));
            if(kind.equals("voice"))v.putAll(map("gender",x.get("gender"),"age",x.get("age_group"),"language",x.get("language_code"),"traits",decode(x.get("traits"),List.of())));
            else v.putAll(map("duration",x.get("duration_ms"),"tags",decode(x.get("tags"),List.of())));
            return v;
        }).toList();
    }
    private List<Map<String,Object>> assets(Scope s) { return assets(s,Long.MAX_VALUE,500); }
    public Map<String,Object> assetPage(long beforeId,int limit) {
        if(beforeId<=0||limit<1||limit>500)throw error(400,"分页参数错误");
        var rows=assets(scope(),beforeId,limit);return map("records",rows,"nextId",rows.size()==limit?rows.getLast().get("id"):null);
    }
    private List<Map<String,Object>> assets(Scope s,long beforeId,int limit) {
        Map<String,Map<String,Object>> edits=new LinkedHashMap<>();
        for(var e:db.queryForList("SELECT * FROM aigc_asset_edit_draft WHERE tenant_id=? AND workspace_id=? AND deleted=0 ORDER BY update_time,id",s.tenant,s.workspace)) {
            @SuppressWarnings("unchecked") var p=(Map<String,Object>)decode(e.get("parameters"),Map.of());
            var entry=map("tool",e.get("tool"),"value",p.get("value"),"instruction",p.get("instruction"),"maskAssetId",e.get("mask_asset_id")==null?null:e.get("mask_asset_id").toString(),"revision",e.get("revision"));
            edits.computeIfAbsent(e.get("asset_id").toString(),k->new LinkedHashMap<>()).put(e.get("tool").toString(),entry);
        }
        var rows=db.queryForList("SELECT a.*,f.mime_type,f.byte_size,f.infra_file_id,t.conversation_id,t.prompt AS task_prompt,t.operation,t.parameter_snapshot,t.id AS task_id,EXISTS(SELECT 1 FROM aigc_favorite fav WHERE fav.tenant_id=a.tenant_id AND fav.workspace_id=a.workspace_id AND fav.asset_id=a.id AND fav.deleted=0) AS liked FROM aigc_asset a LEFT JOIN aigc_asset_file f ON f.tenant_id=a.tenant_id AND f.workspace_id=a.workspace_id AND f.asset_id=a.id AND f.variant_code='ORIGINAL' AND f.deleted=0 LEFT JOIN aigc_task_result r ON r.tenant_id=a.tenant_id AND r.workspace_id=a.workspace_id AND r.asset_id=a.id AND r.status='READY' LEFT JOIN aigc_task t ON t.tenant_id=r.tenant_id AND t.workspace_id=r.workspace_id AND t.id=r.task_id WHERE a.tenant_id=? AND a.workspace_id=? AND a.deleted=0 AND a.id<? ORDER BY a.id DESC LIMIT ?",s.tenant,s.workspace,beforeId,limit);
        return rows.stream().map(a->{
            var parameters=decode(a.get("parameter_snapshot"),Map.of());
            @SuppressWarnings("unchecked") var p=parameters instanceof Map?(Map<String,Object>)parameters:Map.<String,Object>of();
            String kind=a.get("kind").toString().toLowerCase();
            var assetEdits=edits.getOrDefault(a.get("id").toString(),Map.of());
            var latest=assetEdits.values().stream().reduce((first,last)->last).orElse(null);
            return map("taskId",a.get("task_id")==null?null:a.get("task_id").toString(),"id",a.get("id").toString(),"assetId",a.get("id").toString(),"editDrafts",assetEdits,"editDraft",latest,"session",a.get("conversation_id")==null?null:a.get("conversation_id").toString(),
                "title",a.get("title"),"prompt",a.get("task_prompt")==null?a.get("title"):a.get("task_prompt"),"mode",kind.equals("image")&&"UPLOAD".equals(a.get("source"))?"upload":kind,
                "operation",a.get("operation"),"upload","UPLOAD".equals(a.get("source")),"cloud",true,"source",a.get("source"),"status",a.get("status"),"mime",a.get("mime_type"),"byteSize",a.get("byte_size"),
                "contentPath","/admin-api/aigc/assets/"+a.get("id")+"/content","src","","model",a.get("task_id")==null?"上传素材":p.getOrDefault("model","AI 创作"),
                "ratio",p.getOrDefault("ratio",ratio(a.get("width"),a.get("height"))),"resolution",p.getOrDefault("resolution","原始"),"created",time(a.get("create_time")),
                "liked",((Number)a.get("liked")).intValue()==1,"width",a.get("width"),"height",a.get("height"));
        }).toList();
    }
    private String ratio(Object width,Object height) {
        if(width==null||height==null)return "原始";int w=((Number)width).intValue(),h=((Number)height).intValue(),a=w,b=h;
        while(b!=0){int t=a%b;a=b;b=t;}return w/a+":"+h/a;
    }
    @Transactional
    public String createConversation(String title) {return String.valueOf(scopedInsert("aigc_conversation",scope(),map("title",text(title,200))));}
    @Transactional
    public void updateConversation(long conversation,Map<String,Object> body) {
        Scope s=scope();owned("aigc_conversation",s,conversation);
        if(body.containsKey("title"))db.update("UPDATE aigc_conversation SET title=?,updater=? WHERE tenant_id=? AND workspace_id=? AND id=? AND deleted=0",text(body.get("title"),200),String.valueOf(s.user),s.tenant,s.workspace,conversation);
        if(body.containsKey("pinned"))db.update("UPDATE aigc_conversation SET pinned=?,updater=? WHERE tenant_id=? AND workspace_id=? AND id=? AND deleted=0",Boolean.TRUE.equals(body.get("pinned")),String.valueOf(s.user),s.tenant,s.workspace,conversation);
    }
    @Transactional
    public void deleteConversation(long conversation) {
        Scope s=scope();var c=owned("aigc_conversation",s,conversation);
        if(Boolean.TRUE.equals(c.get("is_default")))throw error(400,"默认创作不能删除");
        db.update("UPDATE aigc_conversation SET deleted=1 WHERE tenant_id=? AND workspace_id=? AND id=?",s.tenant,s.workspace,conversation);
        db.update("UPDATE aigc_draft SET deleted=1 WHERE tenant_id=? AND workspace_id=? AND conversation_id=? AND deleted=0",s.tenant,s.workspace,conversation);
    }
    @Transactional
    public long saveDraft(long conversation,Map<String,Object> body) {
        Scope s=scope();owned("aigc_conversation",s,conversation);
        Object value=body.get("draft");
        if(!(value instanceof Map<?,?> raw))throw error(400,"缺少草稿参数");
        Map<String,Object> d=new LinkedHashMap<>();raw.forEach((k,v)->d.put(k.toString(),v));
        String operation=Objects.toString(d.get("mode"),"image").toUpperCase(Locale.ROOT);if(!OPERATIONS.contains(operation))throw error(400,"不支持的创作模式");
        d.remove("modeDrafts");d.remove("_revision");
        if(d.get("editDraft") instanceof Map<?,?> edit) {
            if(edit.get("assetId")!=null)owned("aigc_asset",s,id(edit.get("assetId")));
            if(edit.get("maskAssetId")!=null)owned("aigc_asset",s,id(edit.get("maskAssetId")));
        }
        if(encode(d).length()>100000)throw error(400,"草稿参数过大，请先上传参考素材");
        for(String key:List.of("refs","attachments")) {
            List<Object> inputs=new ArrayList<>();
            if(d.get(key) instanceof List<?> refs)for(Object ref:refs){if(!(ref instanceof Map<?,?> r)||r.get("assetId")==null)throw error(400,"请先把参考素材上传到当前账号");
                long asset=id(r.get("assetId"));var input=owned("aigc_asset",s,asset);
                if(!"READY".equals(input.get("status")))throw error(400,"参考素材尚不可用");
                String kind=input.get("kind").toString(),slot=Objects.toString(r.get("slot"),"reference");
                if(key.equals("refs")&&!kind.equals("IMAGE"))throw error(400,"参考图片类型错误");
                if(key.equals("attachments")&&(!Set.of("AUDIO","VIDEO").contains(kind)))throw error(400,"附加素材类型错误");
                if(Set.of("IMAGE","IMAGE_EDIT","MUSIC").contains(operation)&&!kind.equals("IMAGE"))throw error(400,"当前模式仅支持图片参考");
                if(operation.equals("AUDIO")&&!kind.equals("AUDIO"))throw error(400,"音频模式仅支持音频参考");
                if(operation.equals("AVATAR")&&!(kind.equals("IMAGE")&&slot.equals("role")||kind.equals("AUDIO")&&slot.equals("speech")))throw error(400,"数字人参考槽位错误");
                if(operation.equals("MOTION")&&!(kind.equals("IMAGE")&&slot.equals("role")||kind.equals("VIDEO")&&slot.equals("motion")))throw error(400,"动作模仿参考槽位错误");
                if(r.get("subjectVersionId")!=null)owned("aigc_subject_version",s,id(r.get("subjectVersionId")));
                inputs.add(map("assetId",String.valueOf(asset),"name",Objects.toString(r.get("name"),"素材"),"type",r.get("type"),"slot",r.get("slot"),"subjectVersionId",r.get("subjectVersionId")));}
            d.put(key,inputs);
        }
        int refs=((List<?>)d.get("refs")).size(),attachments=((List<?>)d.get("attachments")).size();
        int max=operation.equals("VIDEO")?12:Set.of("AVATAR","MOTION").contains(operation)?2:6;
        if(refs+attachments>max||Set.of("AVATAR","MOTION").contains(operation)&&(refs>1||attachments>1))throw error(400,"参考素材数量超过限制");
        if(d.get("voiceId")!=null&&!d.get("voiceId").toString().isBlank())one("SELECT id FROM aigc_voice WHERE id=? AND deleted=0 AND status='ENABLED' AND ((visibility='PUBLIC' AND tenant_id=0) OR (visibility='PRIVATE' AND tenant_id=? AND workspace_id=?))",id(d.get("voiceId")),s.tenant,s.workspace);
        var existing=db.queryForList("SELECT id,revision FROM aigc_draft WHERE tenant_id=? AND workspace_id=? AND conversation_id=? AND active_operation=? FOR UPDATE",s.tenant,s.workspace,conversation,operation);
        long expected=((Number)body.getOrDefault("revision",0)).longValue();
        String prompt=Objects.toString(d.get("prompt"),"");if(prompt.length()>20000)throw error(400,"描述不能超过 20000 字");
        if(existing.isEmpty()) {
            if(expected!=0)throw error(409,"草稿版本已变化，请重新加载");
            scopedInsert("aigc_draft",s,map("conversation_id",conversation,"operation",operation,"prompt",prompt,"parameters",encode(d),"input_snapshot",encode(d.get("refs")),"revision",1));
            return 1;
        }
        long current=((Number)existing.getFirst().get("revision")).longValue();
        if(current!=expected)throw error(409,"另一标签页已更新草稿，请重新加载后继续");
        db.update("UPDATE aigc_draft SET prompt=?,parameters=?,input_snapshot=?,revision=revision+1,updater=? WHERE tenant_id=? AND workspace_id=? AND id=? AND revision=?",prompt,encode(d),encode(d.get("refs")),String.valueOf(s.user),s.tenant,s.workspace,existing.getFirst().get("id"),current);
        return current+1;
    }
    @Transactional(rollbackFor=Exception.class)
    public String upload(MultipartFile file) throws Exception {
        Scope s=scope();if(file.isEmpty()||file.getSize()>50L*1024*1024)throw error(400,"单个文件需大于 0 且不超过 50MB");
        byte[] data=file.getBytes();String name=Objects.toString(file.getOriginalFilename(),"asset");
        name=name.replace('\\','/');name=name.substring(name.lastIndexOf('/')+1);if(name.length()>220)throw error(400,"文件名过长");
        String mime=FileTypeUtils.getMineType(data,name);String kind;
        if(mime.startsWith("image/")&&!mime.equals("image/svg+xml"))kind="IMAGE";
        else if(mime.startsWith("audio/"))kind="AUDIO";else if(mime.startsWith("video/"))kind="VIDEO";
        else if(Set.of("application/pdf","text/plain","text/markdown").contains(mime))kind="DOCUMENT";else throw error(400,"仅支持常用图片、音视频、PDF 和文本文件");
        Integer width=null,height=null;
        if(kind.equals("IMAGE")){
            // Read dimensions before decoding pixel data to avoid decompression bombs.
            try(var imageInput=ImageIO.createImageInputStream(new ByteArrayInputStream(data))){var readers=ImageIO.getImageReaders(imageInput);
                if(readers.hasNext()){var reader=readers.next();try{reader.setInput(imageInput);width=reader.getWidth(0);height=reader.getHeight(0);if((long)width*height>100_000_000)throw error(400,"图片像素超过限制");}finally{reader.dispose();}}}
        }
        String directory="aigc/"+s.tenant+"/"+s.workspace+"/"+UUID.randomUUID();
        String url=files.createFile(data,name,directory,mime);
        var stored=one("SELECT id FROM infra_file WHERE url=? AND deleted=0 ORDER BY id DESC LIMIT 1",url);
        long asset=scopedInsert("aigc_asset",s,map("kind",kind,"source","UPLOAD","status","READY","title",name,"width",width,"height",height));
        scopedInsert("aigc_asset_file",s,map("asset_id",asset,"variant_code","ORIGINAL","infra_file_id",stored.get("id"),"mime_type",mime,"byte_size",data.length,"width",width,"height",height));
        return String.valueOf(asset);
    }
    public record Content(byte[] bytes,String mime,String name) {}
    public Content content(long asset) throws Exception {
        Scope s=scope();var a=owned("aigc_asset",s,asset);
        var f=one("SELECT i.config_id,i.path,i.name,i.type FROM aigc_asset_file f JOIN infra_file i ON i.id=f.infra_file_id AND i.deleted=0 WHERE f.tenant_id=? AND f.workspace_id=? AND f.asset_id=? AND f.variant_code='ORIGINAL' AND f.deleted=0",s.tenant,s.workspace,asset);
        byte[] bytes=files.getFileContent(id(f.get("config_id")),f.get("path").toString());if(bytes==null)throw error(404,"文件暂不可用");
        return new Content(bytes,Objects.toString(f.get("type"),"application/octet-stream"),Objects.toString(a.get("title"),"asset"));
    }
    @Transactional
    public void favorite(String kind,long target,boolean liked) {
        Scope s=scope();String column=switch(kind){case "asset"->"asset_id";case "voice"->"voice_id";case "motion"->"motion_template_id";default->throw error(400,"收藏类型错误");};
        if(kind.equals("asset"))owned("aigc_asset",s,target);
        else {String table=kind.equals("voice")?"voice":"motion_template";one("SELECT id FROM aigc_"+table+" WHERE id=? AND deleted=0 AND status='ENABLED' AND ((visibility='PUBLIC' AND tenant_id=0) OR (visibility='PRIVATE' AND tenant_id=? AND workspace_id=?))",target,s.tenant,s.workspace);}
        if(liked)db.update("INSERT IGNORE INTO aigc_favorite(tenant_id,workspace_id,"+column+",creator) VALUES(?,?,?,?)",s.tenant,s.workspace,target,String.valueOf(s.user));
        else db.update("UPDATE aigc_favorite SET deleted=1 WHERE tenant_id=? AND workspace_id=? AND "+column+"=? AND deleted=0",s.tenant,s.workspace,target);
    }
    public List<Map<String,Object>> trash() {
        Scope s=scope();return db.queryForList("SELECT id,title,kind,trashed_time FROM aigc_asset WHERE tenant_id=? AND workspace_id=? AND deleted=1 AND trashed_time IS NOT NULL ORDER BY trashed_time DESC,id DESC",s.tenant,s.workspace).stream().map(a->map("id",a.get("id").toString(),"title",a.get("title"),"kind",a.get("kind"),"trashedTime",time(a.get("trashed_time")))).toList();
    }
    @Transactional
    public void restoreAssets(List<String> ids) {
        if(ids.isEmpty()||ids.size()>200)throw error(400,"请选择 1 至 200 个资产");Scope s=scope();
        for(String value:new LinkedHashSet<>(ids)){
            long asset=id(value);
            one("SELECT a.id FROM aigc_asset a JOIN aigc_asset_file f ON f.asset_id=a.id AND f.tenant_id=a.tenant_id AND f.workspace_id=a.workspace_id AND f.variant_code='ORIGINAL' AND f.deleted=0 JOIN infra_file i ON i.id=f.infra_file_id AND i.deleted=0 WHERE a.tenant_id=? AND a.workspace_id=? AND a.id=? AND a.deleted=1 AND a.trashed_time IS NOT NULL FOR UPDATE",s.tenant,s.workspace,asset);
            db.update("UPDATE aigc_asset SET deleted=0,trashed_time=NULL,updater=? WHERE tenant_id=? AND workspace_id=? AND id=?",String.valueOf(s.user),s.tenant,s.workspace,asset);
        }
    }
    @Transactional
    public void deleteAssets(List<String> ids) {
        trashAssets(scope(),ids);
    }
    // Both native administrators and owners must preserve subject references and
    // remove unusable composer inputs in the same transaction as the soft delete.
    void trashAssets(Scope s,List<String> ids) {
        if(ids.isEmpty()||ids.size()>200)throw error(400,"请选择 1 至 200 个资产");
        for(String value:new LinkedHashSet<>(ids)){long asset=id(value);
            one("SELECT id FROM aigc_asset WHERE tenant_id=? AND workspace_id=? AND id=? AND deleted=0 FOR UPDATE",s.tenant,s.workspace,asset);
            var subjectRefs=db.queryForList("SELECT id FROM aigc_subject WHERE tenant_id=? AND workspace_id=? AND cover_asset_id=? AND deleted=0 LIMIT 1",s.tenant,s.workspace,asset);
            if(!subjectRefs.isEmpty())throw error(400,"素材正在用作主体，请先删除相应主体再删除素材");
            db.update("UPDATE aigc_asset SET deleted=1,trashed_time=CURRENT_TIMESTAMP(3),updater=? WHERE tenant_id=? AND workspace_id=? AND id=?",String.valueOf(s.user),s.tenant,s.workspace,asset);
            db.update("UPDATE aigc_asset_edit_draft SET mask_asset_id=NULL,revision=revision+1 WHERE tenant_id=? AND workspace_id=? AND mask_asset_id=? AND deleted=0",s.tenant,s.workspace,asset);
        }
        // Keep saved composer drafts usable after the user deletes a referenced asset.
        Set<String> removed=new HashSet<>(ids);
        for(var row:db.queryForList("SELECT id,parameters FROM aigc_draft WHERE tenant_id=? AND workspace_id=? AND deleted=0 FOR UPDATE",s.tenant,s.workspace)) {
            @SuppressWarnings("unchecked") var parameters=(Map<String,Object>)decode(row.get("parameters"),new LinkedHashMap<>());
            boolean changed=false;
            for(String key:List.of("refs","attachments"))if(parameters.get(key) instanceof List<?> list){
                var kept=list.stream().filter(x->!(x instanceof Map<?,?> r)||!removed.contains(Objects.toString(r.get("assetId"),""))).toList();
                if(kept.size()!=list.size()){parameters.put(key,kept);changed=true;}
            }
            if(changed)db.update("UPDATE aigc_draft SET parameters=?,input_snapshot=?,revision=revision+1 WHERE tenant_id=? AND workspace_id=? AND id=?",encode(parameters),encode(parameters.getOrDefault("refs",List.of())),s.tenant,s.workspace,row.get("id"));
        }
    }
    @Transactional
    public long saveAssetEdit(long asset,Map<String,Object> body) {
        Scope s=scope();var a=owned("aigc_asset",s,asset);if(!"IMAGE".equals(a.get("kind")))throw error(400,"该工具仅支持图片资产");
        String tool=text(body.get("tool"),32);
        if(!Set.of("智能超清","智能改图","局部重绘","抠图","扩图","消除笔").contains(tool))throw error(400,"不支持的编辑工具");
        String instruction=Objects.toString(body.get("instruction"),"");if(instruction.length()>5000)throw error(400,"编辑说明不能超过 5000 字");
        String value=text(body.get("value"),32);
        if(tool.equals("智能超清")){if(!Set.of("2K","4K","8K").contains(value))throw error(400,"不支持的目标分辨率");}
        else {try{int strength=Integer.parseInt(value);if(strength<0||strength>100)throw error(400,"处理强度需在 0 至 100 之间");}catch(NumberFormatException e){throw error(400,"处理强度必须为整数");}}
        Long mask=body.get("maskAssetId")==null?null:id(body.get("maskAssetId"));
        if(mask!=null&&!"IMAGE".equals(owned("aigc_asset",s,mask).get("kind")))throw error(400,"蒙版必须为图片");
        var rows=db.queryForList("SELECT id,revision FROM aigc_asset_edit_draft WHERE tenant_id=? AND workspace_id=? AND asset_id=? AND active_tool=? FOR UPDATE",s.tenant,s.workspace,asset,tool);
        long revision=((Number)body.getOrDefault("revision",0)).longValue();String parameters=encode(map("value",value,"instruction",instruction));
        if(rows.isEmpty()){
            if(revision!=0)throw error(409,"编辑草稿已变化，请重新打开作品");
            scopedInsert("aigc_asset_edit_draft",s,map("asset_id",asset,"tool",tool,"parameters",parameters,"mask_asset_id",mask));return 1;
        }
        long previous=((Number)rows.getFirst().get("revision")).longValue();if(previous!=revision)throw error(409,"另一标签页已更新编辑草稿，请重新打开作品");
        db.update("UPDATE aigc_asset_edit_draft SET parameters=?,mask_asset_id=?,revision=revision+1,updater=? WHERE tenant_id=? AND workspace_id=? AND id=? AND revision=?",parameters,mask,String.valueOf(s.user),s.tenant,s.workspace,rows.getFirst().get("id"),previous);
        return previous+1;
    }
    @Transactional
    public String createSubject(Map<String,Object> body) {
        Scope s=scope();String name=text(body.get("name"),100);long asset=id(body.get("assetId"));var a=one("SELECT * FROM aigc_asset WHERE tenant_id=? AND workspace_id=? AND id=? AND deleted=0 FOR UPDATE",s.tenant,s.workspace,asset);
        if(!"IMAGE".equals(a.get("kind"))||!"READY".equals(a.get("status")))throw error(400,"请选择已上传的图片作为主体");
        long subject=scopedInsert("aigc_subject",s,map("name",name,"kind","CHARACTER","cover_asset_id",asset,"current_version_no",1,"status","READY"));
        long version=scopedInsert("aigc_subject_version",s,map("subject_id",subject,"version_no",1,"description",name,"configuration","{}"));
        scopedInsert("aigc_subject_reference",s,map("subject_version_id",version,"asset_id",asset));return String.valueOf(subject);
    }
    @Transactional
    public void deleteSubject(long subject) {Scope s=scope();owned("aigc_subject",s,subject);db.update("UPDATE aigc_subject SET deleted=1 WHERE tenant_id=? AND workspace_id=? AND id=?",s.tenant,s.workspace,subject);}
}
