package cn.iocoder.yudao.module.aigc.service;

import cn.iocoder.yudao.module.aigc.payment.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.*;
import static cn.iocoder.yudao.module.aigc.payment.CreditCheckoutStore.*;

@Service
public class StripePaymentService {
    private final StudioService studio;private final ObjectMapper json;private final StripePaymentConfig config;
    private final CreditCheckoutStore store;private final StripeCheckoutGateway gateway;
    public StripePaymentService(StudioService studio,JdbcTemplate db,PlatformTransactionManager manager,ObjectMapper json){this.studio=studio;this.json=json;config=StripePaymentConfig.load();store=new CreditCheckoutStore(db,manager);gateway=new StripeCheckoutGateway(config);}
    private Owner owner(){var s=studio.scope();return new Owner(s.tenant(),s.workspace(),s.user());}
    public Map<String,Object> catalog(){owner();return map("enabled",config.enabled(),"environment",config.environment(),"reason",config.disabledReason(),"packs",store.packs(config.environment()).stream().map(p->map("id",p.get("id").toString(),"name",p.get("name"),"currency",p.get("currency"),"amountMinor",p.get("amount_minor"),"creditPoints",p.get("credit_points"))).toList());}
    public Map<String,Object> create(Map<String,Object> body){if(!config.enabled())throw new PaymentError(503,config.disabledReason());var owner=owner();var purchase=store.prepare(owner,studio.id(body.get("packId")),Objects.toString(body.get("clientRequestId"),""),config.environment());
        if(Boolean.TRUE.equals(purchase.get("credited")))return map("order",store.dto(purchase),"checkoutUrl",null);
        if(!config.environment().equals(purchase.get("environment")))throw new PaymentError(409,"请在订单所属支付环境继续付款");
        if(Set.of("PAID","EXPIRED","FAILED","REVIEW").contains(purchase.get("status")))throw new PaymentError(409,"该订单已结束，请重新选择套餐");
        try{
            var session=purchase.get("checkout_session_id")==null?gateway.create(purchase):gateway.retrieve(purchase.get("checkout_session_id").toString());
            store.attach(((Number)purchase.get("id")).longValue(),session);
            return map("order",store.dto(store.owned(owner,((Number)purchase.get("id")).longValue())),"checkoutUrl",session.checkoutUrl());
        }catch(com.stripe.exception.StripeException e){store.noteFailure(((Number)purchase.get("id")).longValue(),"Stripe 收银台创建未确认；可重试同一订单，不会重复创建付款");throw new PaymentError(502,"Stripe 收银台暂时不可用，请稍后重试同一订单");}
    }
    public Map<String,Object> resume(long id){var purchase=store.owned(owner(),id);return create(map("packId",purchase.get("pack_id"),"clientRequestId",purchase.get("client_request_id")));}
    public Map<String,Object> orders(long before,int limit){return store.history(owner(),before,limit);}
    public Map<String,Object> detail(long id){return store.dto(store.owned(owner(),id));}
    public void webhook(String payload,String signature)throws Exception {
        var event=gateway.verify(payload,signature);var raw=json.readTree(payload).path("data").path("object");String type=event.getType();
        if(Set.of("checkout.session.completed","checkout.session.async_payment_succeeded","checkout.session.async_payment_failed","checkout.session.expired").contains(type)){
            if(!"zaomeng-aigc".equals(raw.path("metadata").path("application").asText()))return;
            String sessionId=raw.path("id").asText();var verified=gateway.retrieve(sessionId);
            store.accept(event.getId(),type,verified);
        }else if(Set.of("charge.refunded","charge.dispute.created").contains(type)){
            String intent=raw.path("payment_intent").asText();if(!intent.matches("pi_[A-Za-z0-9]+"))return;
            Long purchase=gateway.purchaseForIntent(intent);if(purchase!=null)store.risk(event.getId(),type,Boolean.TRUE.equals(event.getLivemode()),intent,raw.path("amount_refunded").asLong(0),purchase);
        }
    }
}
