package cn.iocoder.yudao.module.aigc.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.module.aigc.service.StudioAdminService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/aigc/manage")
public class StudioAdminController {
    private final StudioAdminService admin;
    public StudioAdminController(StudioAdminService admin){this.admin=admin;}
    @GetMapping("/overview") public CommonResult<?> overview(){return success(admin.overview());}
    @GetMapping("/{resource}/page") public CommonResult<?> page(@PathVariable String resource,@RequestParam(defaultValue="1") int pageNo,@RequestParam(defaultValue="20") int pageSize,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String status){return success(admin.page(resource,pageNo,pageSize,q,status));}
    @GetMapping("/{resource}/{id}") @ApiAccessLog(responseEnable=false) public CommonResult<?> detail(@PathVariable String resource,@PathVariable long id){return success(admin.detail(resource,id));}
    @PostMapping("/workspaces/{id}/credits") public CommonResult<?> credits(@PathVariable long id,@RequestBody Map<String,Object> body){admin.adjustCredits(id,body);return success(true);}
    @PutMapping("/models/{id}") public CommonResult<?> model(@PathVariable long id,@RequestBody Map<String,Object> body){admin.updateModel(id,body);return success(true);}
    @PutMapping("/workspaces/{id}/status") public CommonResult<?> workspace(@PathVariable long id,@RequestBody Map<String,Object> body){admin.workspaceStatus(id,Objects.toString(body.get("status"),""));return success(true);}
    @PutMapping("/assets/{id}/trash") public CommonResult<?> asset(@PathVariable long id,@RequestBody Map<String,Object> body){admin.assetStatus(id,Boolean.TRUE.equals(body.get("trashed")));return success(true);}
    @GetMapping("/assets/{id}/content") @ApiAccessLog(responseEnable=false) public ResponseEntity<?> content(@PathVariable long id)throws Exception{var file=admin.assetContent(id);return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.mime())).header("Cache-Control","private, no-store").header("X-Content-Type-Options","nosniff").header("Content-Security-Policy","sandbox").header("Content-Disposition",ContentDisposition.inline().filename(file.name(),StandardCharsets.UTF_8).build().toString()).body(new ByteArrayResource(file.bytes()));}
    @GetMapping("/files/{id}/content") @ApiAccessLog(responseEnable=false) public ResponseEntity<?> file(@PathVariable long id)throws Exception{var file=admin.nativeFileContent(id);return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.mime())).header("Cache-Control","private, no-store").header("X-Content-Type-Options","nosniff").header("Content-Security-Policy","sandbox").header("Content-Disposition",ContentDisposition.attachment().filename(file.name(),StandardCharsets.UTF_8).build().toString()).body(new ByteArrayResource(file.bytes()));}
    @GetMapping("/files/resolve") @ApiAccessLog(requestEnable=false,responseEnable=false) public ResponseEntity<?> resolve(@RequestParam String url)throws Exception{var file=admin.resolveNativeFile(url);return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.mime())).header("Cache-Control","private, no-store").header("X-Content-Type-Options","nosniff").header("Content-Security-Policy","sandbox").header("Content-Disposition",ContentDisposition.attachment().filename(file.name(),StandardCharsets.UTF_8).build().toString()).body(new ByteArrayResource(file.bytes()));}

}
