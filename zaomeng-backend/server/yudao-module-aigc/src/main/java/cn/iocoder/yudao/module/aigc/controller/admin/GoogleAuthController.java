package cn.iocoder.yudao.module.aigc.controller.admin;
import cn.iocoder.yudao.module.aigc.service.GoogleAuthService;
import cn.iocoder.yudao.module.aigc.auth.GoogleIdentityStore.AuthError;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import jakarta.annotation.security.PermitAll;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.net.URI;
import java.util.Map;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
@RestController @RequestMapping("/aigc/auth/google") @TenantIgnore
public class GoogleAuthController {
 private final GoogleAuthService auth;public GoogleAuthController(GoogleAuthService auth){this.auth=auth;}
 @GetMapping("/config") @PermitAll @ApiAccessLog(requestEnable=false,responseEnable=false) public CommonResult<?> config(){return success(auth.configuration());}
 @PostMapping("/start") @PermitAll @ApiAccessLog(requestEnable=false,responseEnable=false) public CommonResult<?> start(@RequestBody Map<String,Object> body,@RequestHeader(value="Origin",required=false)String origin){return success(auth.start(body,origin));}
 @GetMapping("/callback") @PermitAll @ApiAccessLog(requestEnable=false,responseEnable=false) public ResponseEntity<?> callback(@RequestParam(required=false)String state,@RequestParam(required=false)String code,@RequestParam(required=false)String error){return ResponseEntity.status(303).location(URI.create(auth.callback(state,code,error))).cacheControl(CacheControl.noStore()).header("Referrer-Policy","no-referrer").build();}
 @PostMapping("/exchange") @PermitAll @ApiAccessLog(requestEnable=false,responseEnable=false) public CommonResult<?> exchange(@RequestBody Map<String,Object> body,@RequestHeader(value="Origin",required=false)String origin){return success(auth.exchange(body,origin));}
 @GetMapping("/linked") @ApiAccessLog(requestEnable=false,responseEnable=false) public CommonResult<?> linked(){return success(auth.linked());}
 @ExceptionHandler(AuthError.class)public CommonResult<?> error(AuthError e){return CommonResult.error(e.code,e.getMessage());}
}
