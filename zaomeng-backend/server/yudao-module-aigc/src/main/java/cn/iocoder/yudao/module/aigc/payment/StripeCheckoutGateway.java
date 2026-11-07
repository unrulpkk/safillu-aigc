package cn.iocoder.yudao.module.aigc.payment;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionRetrieveParams;
import java.util.*;
import static cn.iocoder.yudao.module.aigc.payment.CreditCheckoutStore.*;

/** Hosted Checkout: the app never receives card numbers or client secret keys. */
public class StripeCheckoutGateway {
    private final StripePaymentConfig config;private final StripeClient client;
    public StripeCheckoutGateway(StripePaymentConfig config){this.config=config;client=config.enabled()?new StripeClient(config.secretKey()):null;}
    private void require(){if(client==null)throw new PaymentError(503,config.disabledReason());}
    public Facts create(Map<String,Object> purchase)throws StripeException {
        require();String id=purchase.get("id").toString();
        var metadata=Map.of("application","zaomeng-aigc","purchase_id",id,"environment",config.environment(),"credit_points",purchase.get("credit_points").toString());
        var price=SessionCreateParams.LineItem.PriceData.builder().setCurrency(purchase.get("currency").toString()).setUnitAmount(((Number)purchase.get("amount_minor")).longValue()).setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder().setName(purchase.get("pack_name").toString()).build()).build();
        var params=SessionCreateParams.builder().setMode(SessionCreateParams.Mode.PAYMENT).setClientReferenceId(purchase.get("purchase_no").toString()).putAllMetadata(metadata)
            .addLineItem(SessionCreateParams.LineItem.builder().setPriceData(price).setQuantity(1L).build())
            .setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder().putAllMetadata(metadata).build())
            .setSuccessUrl(config.safeReturnOrigin()+"/?stripe_return=success&order_id="+id+"#payments")
            .setCancelUrl(config.safeReturnOrigin()+"/?stripe_return=cancel&order_id="+id+"#payments")
            .putExtraParam("adaptive_pricing",Map.of("enabled",false))
            .putExtraParam("integration_identifier","zaomeng-credits-"+suffix(purchase.get("purchase_no").toString()))
            .setExpiresAt(epoch(purchase.get("expires_time"))).addExpand("line_items").build();
        var options=RequestOptions.builder().setIdempotencyKey("zaomeng-credit-"+purchase.get("purchase_no")).build();
        return facts(client.v1().checkout().sessions().create(params,options));
    }
    private long epoch(Object date){return date instanceof java.time.LocalDateTime t?t.toInstant(java.time.ZoneOffset.UTC).getEpochSecond():((java.util.Date)date).getTime()/1000;}
    private String suffix(String s){StringBuilder value=new StringBuilder();int hash=s.hashCode();for(int i=0;i<8;i++){value.append((char)('a'+Math.floorMod(hash,26)));hash=Integer.rotateRight(hash^0x5f3759df,3);}return value.toString();}
    public Facts retrieve(String session)throws StripeException {require();if(!session.matches("cs_(?:test_|live_)?[A-Za-z0-9]+"))throw new PaymentError(400,"Stripe 会话编号错误");return facts(client.v1().checkout().sessions().retrieve(session,SessionRetrieveParams.builder().addExpand("line_items").build()));}
    public Long purchaseForIntent(String id)throws StripeException {
        require();var intent=client.v1().paymentIntents().retrieve(id);var meta=intent.getMetadata();
        if(meta==null||!"zaomeng-aigc".equals(meta.get("application")))return null;
        if(!Boolean.valueOf(config.live()).equals(intent.getLivemode())||!config.environment().equals(meta.get("environment")))throw new PaymentError(409,"Stripe 付款环境不匹配");
        String purchase=meta.get("purchase_id");if(purchase==null||!purchase.matches("[1-9][0-9]{0,18}"))throw new PaymentError(409,"Stripe 付款订单编号无效");return Long.parseLong(purchase);
    }
    public Event verify(String payload,String signature)throws com.stripe.exception.SignatureVerificationException {
        require();if(payload.length()>262144)throw new PaymentError(413,"支付回调内容过大");
        Event event=Webhook.constructEvent(payload,signature,config.webhookSecret());
        if(!Boolean.valueOf(config.live()).equals(event.getLivemode()))throw new PaymentError(400,"支付回调环境不匹配");return event;
    }
    public static Facts facts(Session session){
        long quantity=0,lineAmount=0;if(session.getLineItems()!=null&&session.getLineItems().getData()!=null)for(var item:session.getLineItems().getData()){quantity+=item.getQuantity()==null?0:item.getQuantity();lineAmount+=item.getAmountTotal()==null?0:item.getAmountTotal();}
        var metadata=session.getMetadata()==null?Map.<String,String>of():session.getMetadata();
        if(!"payment".equals(session.getMode()))throw new PaymentError(409,"Stripe 会话不是一次性付款订单");
        return new Facts(session.getId(),session.getClientReferenceId(),metadata.get("application"),metadata.get("purchase_id"),Boolean.TRUE.equals(session.getLivemode()),session.getAmountTotal()==null?-1:session.getAmountTotal(),session.getCurrency(),session.getPaymentStatus(),session.getStatus(),session.getPaymentIntent(),quantity,lineAmount,metadata.get("credit_points"),session.getUrl());
    }
}
