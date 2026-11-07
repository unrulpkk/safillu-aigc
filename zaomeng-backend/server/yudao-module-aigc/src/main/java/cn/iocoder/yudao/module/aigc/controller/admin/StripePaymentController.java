package cn.iocoder.yudao.module.aigc.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.aigc.payment.CreditCheckoutStore.PaymentError;
import cn.iocoder.yudao.module.aigc.service.StripePaymentService;
import jakarta.annotation.security.PermitAll;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/aigc/payments")
public class StripePaymentController {
    private final StripePaymentService service;
    public StripePaymentController(StripePaymentService service){this.service=service;}
    @ApiAccessLog(requestEnable=false,responseEnable=false) @GetMapping("/catalog") public CommonResult<?> catalog(){return success(service.catalog());}
    @ApiAccessLog(requestEnable=false,responseEnable=false) @PostMapping("/checkout") public CommonResult<?> checkout(@RequestBody Map<String,Object> body){return success(service.create(body));}
    @ApiAccessLog(requestEnable=false,responseEnable=false) @GetMapping("/orders") public CommonResult<?> orders(@RequestParam(defaultValue="9223372036854775807") long beforeId,@RequestParam(defaultValue="50") int limit){return success(service.orders(beforeId,limit));}
    @ApiAccessLog(requestEnable=false,responseEnable=false) @PostMapping("/orders/{id}/resume") public CommonResult<?> resume(@PathVariable long id){return success(service.resume(id));}
    @ApiAccessLog(requestEnable=false,responseEnable=false) @GetMapping("/orders/{id}") public CommonResult<?> detail(@PathVariable long id){return success(service.detail(id));}
    @ApiAccessLog(requestEnable=false,responseEnable=false) @PostMapping("/stripe/webhook") @PermitAll @TenantIgnore
    public ResponseEntity<?> webhook(@RequestBody String payload,@RequestHeader(value="Stripe-Signature",required=false) String signature){
        try{if(signature==null)return ResponseEntity.badRequest().body(Map.of("received",false));service.webhook(payload,signature);return ResponseEntity.ok(Map.of("received",true));}
        catch(com.stripe.exception.SignatureVerificationException e){return ResponseEntity.badRequest().body(Map.of("received",false));}
        catch(PaymentError e){return ResponseEntity.status(e.code==503?503:400).body(Map.of("received",false));}
        catch(Exception e){return ResponseEntity.status(500).body(Map.of("received",false));}
    }
    @ExceptionHandler(PaymentError.class) public CommonResult<?> paymentError(PaymentError e){return CommonResult.error(e.code,e.getMessage());}
}
