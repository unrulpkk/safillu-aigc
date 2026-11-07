package cn.iocoder.yudao.module.aigc.controller.admin;
import cn.iocoder.yudao.module.aigc.service.ExploreService;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import jakarta.annotation.security.PermitAll;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.springframework.core.io.*;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController @RequestMapping("/aigc/explore")
public class ExploreController {
 private final ExploreService service;public ExploreController(ExploreService service){this.service=service;}
 @GetMapping @PermitAll public CommonResult<?> list(@RequestParam(defaultValue="all") String mode,@RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="24") int limit){return success(service.catalog(mode,offset,limit));}
 @GetMapping("/{id:[0-9]+}") @PermitAll public CommonResult<?> detail(@PathVariable long id){return success(service.detail(id));}
 @GetMapping("/permissions") @PermitAll public CommonResult<?> permissions(){return success(service.permissions());}
 @GetMapping("/manage") public CommonResult<?> manage(@RequestParam(defaultValue="false") boolean trash){return success(service.management(trash));}
 @GetMapping("/manage/capabilities") public CommonResult<?> capabilities(){return success(service.capabilities());}
 @PostMapping("/manage") @ApiAccessLog(requestEnable=false,responseEnable=false) public CommonResult<?> create(@RequestBody Map<String,Object> body){return success(service.save(null,body));}
 @PutMapping("/manage/{id}") @ApiAccessLog(requestEnable=false,responseEnable=false) public CommonResult<?> update(@PathVariable long id,@RequestBody Map<String,Object> body){return success(service.save(id,body));}
 @PatchMapping("/manage/{id}/status") public CommonResult<?> status(@PathVariable long id,@RequestBody Map<String,Object> body){service.status(id,body);return success(true);}
 @DeleteMapping("/manage/{id}") public CommonResult<?> remove(@PathVariable long id,@RequestParam int revision){service.remove(id,revision);return success(true);}
 @PostMapping("/manage/{id}/restore") public CommonResult<?> restore(@PathVariable long id,@RequestParam int revision){service.restore(id,revision);return success(true);}
 @PostMapping("/manage/media") @ApiAccessLog(requestEnable=false,responseEnable=false) public CommonResult<?> upload(@RequestParam("file") MultipartFile file)throws Exception{return success(service.upload(file));}
 @GetMapping("/{id:[0-9]+}/media/{slot}") @PermitAll @ApiAccessLog(responseEnable=false) public ResponseEntity<Resource> content(@PathVariable long id,@PathVariable String slot)throws Exception{return media(id,slot,false);}
 @GetMapping("/manage/{id}/media/{slot}") @ApiAccessLog(responseEnable=false) public ResponseEntity<Resource> preview(@PathVariable long id,@PathVariable String slot)throws Exception{return media(id,slot,true);}
 private ResponseEntity<Resource> media(long id,String slot,boolean admin)throws Exception{var f=service.content(id,slot,admin);return ResponseEntity.ok().contentType(MediaType.parseMediaType(f.mime())).header("Cache-Control","no-store").header("X-Content-Type-Options","nosniff").header("Content-Security-Policy","sandbox").header("Content-Disposition",ContentDisposition.inline().filename(f.name(),StandardCharsets.UTF_8).build().toString()).body(new ByteArrayResource(f.bytes()));}
}
