package cn.iocoder.yudao.module.aigc.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.aigc.service.StudioService;
import cn.iocoder.yudao.module.aigc.service.GenerationService;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.aigc.config.MediaAuthorizationFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/** Authenticated personal resources. Ownership is always resolved in the service. */
@RestController
@RequestMapping("/aigc")
public class StudioController {
    private final StudioService studio;
    private final GenerationService generation;
    public StudioController(StudioService studio,GenerationService generation){this.studio=studio;this.generation=generation;}
    @GetMapping("/bootstrap") public CommonResult<?> bootstrap(){generation.trial();return success(studio.bootstrap());}
    @PostMapping("/media-session") @ApiAccessLog(requestEnable=false,responseEnable=false)
    public ResponseEntity<CommonResult<Boolean>> mediaSession(HttpServletRequest request){
        if(SecurityFrameworkUtils.getLoginUserId()==null)throw new cn.iocoder.yudao.framework.common.exception.ServiceException(401,"请先登录");
        String header=request.getHeader("Authorization");
        if(header==null||!header.startsWith("Bearer "))throw new cn.iocoder.yudao.framework.common.exception.ServiceException(401,"请先登录");
        ResponseCookie cookie=ResponseCookie.from(MediaAuthorizationFilter.COOKIE,header.substring(7))
            .httpOnly(true).secure(request.isSecure()).sameSite("Strict").path("/admin-api/aigc/assets/").build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,cookie.toString()).body(success(true));
    }
    @PostMapping("/conversations") public CommonResult<?> conversation(@RequestBody Map<String,Object> body){return success(studio.createConversation(Objects.toString(body.get("title"),"未命名创作")));}
    @PatchMapping("/conversations/{id}") public CommonResult<?> update(@PathVariable long id,@RequestBody Map<String,Object> body){studio.updateConversation(id,body);return success(true);}
    @DeleteMapping("/conversations/{id}") public CommonResult<?> delete(@PathVariable long id){studio.deleteConversation(id);return success(true);}
    @PutMapping("/conversations/{id}/draft") @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<?> draft(@PathVariable long id,@RequestBody Map<String,Object> body){return success(StudioService.map("revision",studio.saveDraft(id,body)));}
    @PostMapping("/assets/upload") @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<?> upload(@RequestParam("file") MultipartFile file)throws Exception{return success(studio.upload(file));}
    @GetMapping("/assets/{id}/content") @ApiAccessLog(responseEnable=false)
    public ResponseEntity<Resource> content(@PathVariable long id)throws Exception{
        var file=studio.content(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.mime()))
            .header("X-Content-Type-Options","nosniff").header("Cache-Control","private, no-store")
            .header("Content-Security-Policy","sandbox")
            .header("Content-Disposition",ContentDisposition.inline().filename(file.name(),StandardCharsets.UTF_8).build().toString()).body(new ByteArrayResource(file.bytes()));
    }
    @GetMapping("/assets") public CommonResult<?> assets(@RequestParam(defaultValue="9223372036854775807") long beforeId,@RequestParam(defaultValue="500") int limit){return success(studio.assetPage(beforeId,limit));}
    @GetMapping("/assets/trash") public CommonResult<?> trash(){return success(studio.trash());}
    @PostMapping("/assets/restore") public CommonResult<?> restore(@RequestBody List<String> ids){studio.restoreAssets(ids);return success(true);}
    @PostMapping("/assets/delete") public CommonResult<?> deleteAssets(@RequestBody List<String> ids){studio.deleteAssets(ids);return success(true);}
    @PutMapping("/assets/{id}/edit-draft") @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<?> editDraft(@PathVariable long id,@RequestBody Map<String,Object> body){return success(StudioService.map("revision",studio.saveAssetEdit(id,body)));}
    @PutMapping("/favorites/{kind}/{id}") public CommonResult<?> favorite(@PathVariable String kind,@PathVariable long id,@RequestBody Map<String,Object> body){studio.favorite(kind,id,Boolean.TRUE.equals(body.get("liked")));return success(true);}
    @PostMapping("/subjects") public CommonResult<?> subject(@RequestBody Map<String,Object> body){return success(studio.createSubject(body));}
    @DeleteMapping("/subjects/{id}") public CommonResult<?> deleteSubject(@PathVariable long id){studio.deleteSubject(id);return success(true);}
    @PostMapping("/tasks/quote") @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<?> quote(@RequestBody Map<String,Object> body){return success(generation.quote(body));}
    @GetMapping("/tasks") public CommonResult<?> tasks(@RequestParam(defaultValue="9223372036854775807") long beforeId,@RequestParam(defaultValue="50") int limit,@RequestParam(defaultValue="ALL") String status){return success(generation.list(beforeId,limit,status));}
    @GetMapping("/tasks/{id}") public CommonResult<?> taskDetail(@PathVariable long id){return success(generation.detail(id));}
    @PostMapping("/tasks/{id}/retry") @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<?> retry(@PathVariable long id,@RequestBody Map<String,Object> body){return success(generation.retry(id,body));}
    @GetMapping("/quota/ledger") public CommonResult<?> ledger(@RequestParam(defaultValue="9223372036854775807") long beforeId,@RequestParam(defaultValue="50") int limit){return success(generation.ledgerPage(beforeId,limit));}
    @PostMapping("/tasks") @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<?> task(@RequestBody Map<String,Object> body){return success(generation.create(body));}
    @PostMapping("/tasks/{id}/cancel") public CommonResult<?> cancel(@PathVariable long id){generation.cancel(id);return success(true);}
}
