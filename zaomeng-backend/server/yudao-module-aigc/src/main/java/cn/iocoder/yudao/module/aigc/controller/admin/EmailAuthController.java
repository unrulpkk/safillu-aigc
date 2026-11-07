package cn.iocoder.yudao.module.aigc.controller.admin;

import cn.iocoder.yudao.module.aigc.service.EmailAuthService;
import cn.iocoder.yudao.module.aigc.auth.GoogleIdentityStore.AuthError;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/aigc/auth/email")
@TenantIgnore
public class EmailAuthController {
    private final EmailAuthService auth;
    public EmailAuthController(EmailAuthService auth) { this.auth=auth; }
    public record CodeRequest(@NotBlank @Email @Size(max=50) String email, @Pattern(regexp="zh-CN|en|es") String locale) {
        @Override public String toString() { return "CodeRequest[redacted]"; }
    }
    public record RegisterRequest(@NotBlank @Email @Size(max=50) String email, @NotBlank @Pattern(regexp="[0-9]{6}") String code,
                                  @NotBlank @Size(min=8,max=16) String password) {
        @Override public String toString() { return "RegisterRequest[redacted]"; }
    }
    public record LoginRequest(@NotBlank @Size(max=50) String account, @NotBlank @Size(min=4,max=16) String password) {
        @Override public String toString() { return "LoginRequest[redacted]"; }
    }
    private String ip(HttpServletRequest request) {
        // Frontends overwrite X-Real-IP; never trust a client-supplied X-Forwarded-For chain.
        String ip=request.getHeader("X-Real-IP");
        return ip!=null && ip.matches("[0-9a-fA-F:.]{3,45}") ? ip : request.getRemoteAddr();
    }
    @PostMapping("/code") @PermitAll @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<?> code(@Valid @RequestBody CodeRequest body, HttpServletRequest request) {
        return success(auth.sendCode(body.email(),body.locale()==null?"zh-CN":body.locale(),ip(request)));
    }
    @PostMapping("/register") @PermitAll @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<?> register(@Valid @RequestBody RegisterRequest body) {
        return success(auth.register(body.email(),body.code(),body.password()));
    }
    @PostMapping("/login") @PermitAll @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<?> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request) {
        return success(auth.login(body.account(),body.password(),ip(request)));
    }
    @ExceptionHandler(AuthError.class)
    public CommonResult<?> error(AuthError error) { return CommonResult.error(error.code,error.getMessage()); }
}
