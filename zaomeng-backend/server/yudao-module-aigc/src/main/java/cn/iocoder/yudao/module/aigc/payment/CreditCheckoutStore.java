package cn.iocoder.yudao.module.aigc.payment;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/** Payment state and entitlement grant are one transaction. Test payments never buy live credits. */
public class CreditCheckoutStore {
    public record Owner(long tenant,long workspace,long user) {}
    public record Facts(String id,String reference,String application,String purchaseId,boolean live,long amount,String currency,String paymentStatus,String status,String paymentIntent,long quantity,long lineAmount,String points,String checkoutUrl) {}
    public static class PaymentError extends RuntimeException {public final int code;public PaymentError(int code,String message){super(message);this.code=code;}}
    private final JdbcTemplate db;private final TransactionTemplate tx;
    public CreditCheckoutStore(JdbcTemplate db,PlatformTransactionManager manager){this.db=db;tx=new TransactionTemplate(manager);tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);}
    public static Map<String,Object> map(Object... args){var result=new LinkedHashMap<String,Object>();for(int i=0;i<args.length;i+=2)result.put(args[i].toString(),args[i+1]);return result;}
    private long insert(String table,Map<String,Object> values){var keys=new GeneratedKeyHolder();String sql="INSERT INTO "+table+" ("+String.join(",",values.keySet())+") VALUES ("+String.join(",",Collections.nCopies(values.size(),"?"))+")";
        db.update(c->{var statement=c.prepareStatement(sql,new String[]{"id"});int i=1;for(var value:values.values())statement.setObject(i++,value);return statement;},keys);return Objects.requireNonNull(keys.getKey()).longValue();}
    private Map<String,Object> one(String sql,Object... args){var rows=db.queryForList(sql,args);if(rows.isEmpty())throw new PaymentError(404,"充值订单不存在或无权访问");return rows.getFirst();}
    public Map<String,Object> owned(Owner owner,long id){return one("SELECT * FROM aigc_credit_purchase WHERE tenant_id=? AND workspace_id=? AND user_id=? AND id=? AND deleted=0",owner.tenant,owner.workspace,owner.user,id);}
    public List<Map<String,Object>> packs(String environment){return db.queryForList("SELECT id,code,name,currency,amount_minor,credit_points FROM aigc_credit_pack WHERE environment=? AND enabled=1 AND deleted=0 ORDER BY sort_order,id",environment);}
    public Map<String,Object> prepare(Owner owner,long pack,String key,String environment){
        if(!key.matches("[A-Za-z0-9:_-]{8,64}"))throw new PaymentError(400,"无效的订单请求编号");
        return tx.execute(status->{
            one("SELECT id FROM aigc_quota_account WHERE tenant_id=? AND workspace_id=? FOR UPDATE",owner.tenant,owner.workspace);
            var existing=db.queryForList("SELECT * FROM aigc_credit_purchase WHERE tenant_id=? AND workspace_id=? AND client_request_id=?",owner.tenant,owner.workspace,key);
            if(!existing.isEmpty()){var prior=existing.getFirst();if(((Number)prior.get("pack_id")).longValue()!=pack||!environment.equals(prior.get("environment")))throw new PaymentError(409,"请求编号已用于其他充值套餐");return prior;}
            if(db.queryForObject("SELECT COUNT(*) FROM aigc_credit_purchase WHERE tenant_id=? AND workspace_id=? AND environment='LIVE' AND billing_hold=1",Long.class,owner.tenant,owner.workspace)>0)throw new PaymentError(402,"账号存在待核对的支付退款或争议，请联系管理员");
            if(db.queryForObject("SELECT COUNT(*) FROM aigc_credit_purchase WHERE tenant_id=? AND workspace_id=? AND status IN ('CREATING','OPEN','PROCESSING') AND (expires_time>CURRENT_TIMESTAMP OR status='PROCESSING')",Long.class,owner.tenant,owner.workspace)>=5)throw new PaymentError(429,"已有多个待支付订单，请先处理或等待订单过期");
            var product=one("SELECT * FROM aigc_credit_pack WHERE id=? AND environment=? AND enabled=1 AND deleted=0",pack,environment);
            String no="TP"+UUID.randomUUID().toString().replace("-","");Timestamp deadline=Timestamp.from(java.time.Instant.now().plusSeconds(3600));
            long order=insert("pay_order",map("app_id",880001,"channel_id",880001,"channel_code","stripe","user_id",owner.user,"user_type",2,"merchant_order_id",no,"subject",product.get("name"),"body","创作积分充值","price",product.get("amount_minor"),"status",0,"refund_price",0,"expire_time",deadline,"creator",String.valueOf(owner.user)));
            long extension=insert("pay_order_extension",map("no",no,"order_id",order,"channel_id",880001,"channel_code","stripe","status",0,"creator",String.valueOf(owner.user)));
            long id=insert("aigc_credit_purchase",map("tenant_id",owner.tenant,"workspace_id",owner.workspace,"user_id",owner.user,"purchase_no",no,"client_request_id",key,"pack_id",pack,"pack_name",product.get("name"),"environment",environment,"currency",product.get("currency"),"amount_minor",product.get("amount_minor"),"credit_points",product.get("credit_points"),"pay_order_id",order,"pay_extension_id",extension,"expires_time",deadline,"creator",String.valueOf(owner.user)));
            return owned(owner,id);
        });
    }
    public static void validate(Map<String,Object> purchase,Facts facts){
        boolean live="LIVE".equals(purchase.get("environment"));String id=purchase.get("id").toString();
        if(facts.id==null||!facts.id.matches("cs_(?:test_|live_)?[A-Za-z0-9]+")||facts.live!=live||!"zaomeng-aigc".equals(facts.application)||!id.equals(facts.purchaseId)||!purchase.get("purchase_no").equals(facts.reference))throw new PaymentError(409,"Stripe 订单归属或环境校验失败");
        if("paid".equals(facts.paymentStatus)&&(facts.paymentIntent==null||!facts.paymentIntent.matches("pi_[A-Za-z0-9]+")))throw new PaymentError(409,"Stripe 付款凭证缺失");
        if(facts.amount!=((Number)purchase.get("amount_minor")).longValue()||!purchase.get("currency").equals(facts.currency)||facts.quantity!=1||facts.lineAmount!=facts.amount)throw new PaymentError(409,"Stripe 订单金额、币种或商品校验失败");
        try{if(new BigDecimal(facts.points).compareTo(new BigDecimal(purchase.get("credit_points").toString()))!=0)throw new IllegalArgumentException();}catch(Exception e){throw new PaymentError(409,"Stripe 积分权益校验失败");}
        Object old=purchase.get("checkout_session_id");if(old!=null&&!old.toString().equals(facts.id))throw new PaymentError(409,"Stripe 支付会话与订单不匹配");
    }
    public void attach(long id,Facts facts){tx.executeWithoutResult(status->{var p=one("SELECT * FROM aigc_credit_purchase WHERE id=? FOR UPDATE",id);validate(p,facts);
        if(facts.checkoutUrl!=null){var uri=java.net.URI.create(facts.checkoutUrl);if(!"https".equals(uri.getScheme())||!"checkout.stripe.com".equals(uri.getHost())||uri.getUserInfo()!=null)throw new PaymentError(502,"Stripe 收银台地址校验失败");}
        db.update("UPDATE aigc_credit_purchase SET checkout_session_id=?,checkout_url=?,payment_intent_id=COALESCE(payment_intent_id,?),status=CASE WHEN status='CREATING' THEN 'OPEN' ELSE status END,last_error=NULL WHERE id=?",facts.id,facts.checkoutUrl,facts.paymentIntent,id);
        db.update("UPDATE pay_order_extension SET channel_extras=? WHERE id=?","{\"checkoutSessionId\":\""+facts.id+"\"}",p.get("pay_extension_id"));
    });}
    public void noteFailure(long id,String message){db.update("UPDATE aigc_credit_purchase SET last_error=? WHERE id=? AND status='CREATING'",message,id);}
    private boolean flag(Object value){return Boolean.TRUE.equals(value)||value instanceof Number n&&n.intValue()!=0;}
    private boolean eventSeen(String event,boolean live){return !db.queryForList("SELECT id FROM aigc_stripe_event WHERE event_id=? AND livemode=? AND handled=1",event,live).isEmpty();}
    private void recordEvent(String event,String type,boolean live,Long purchase,String reference,String summary){insert("aigc_stripe_event",map("event_id",event,"event_type",type,"livemode",live,"purchase_id",purchase,"provider_reference",reference,"handled",true,"summary",summary,"handled_time",Timestamp.from(java.time.Instant.now())));}
    public void accept(String event,String type,Facts facts){tx.executeWithoutResult(status->{
        var p=one("SELECT * FROM aigc_credit_purchase WHERE id=? FOR UPDATE",Long.parseLong(facts.purchaseId));validate(p,facts);
        if(eventSeen(event,facts.live))return;
        long id=((Number)p.get("id")).longValue();boolean paid="paid".equals(facts.paymentStatus);
        if(paid&&!flag(p.get("credited"))){
            long tenant=((Number)p.get("tenant_id")).longValue(),workspace=((Number)p.get("workspace_id")).longValue();
            var quota=one("SELECT * FROM aigc_quota_account WHERE tenant_id=? AND workspace_id=? FOR UPDATE",tenant,workspace);
            boolean hold=flag(p.get("billing_hold"))||db.queryForObject("SELECT COUNT(*) FROM aigc_stripe_event WHERE provider_reference=? AND event_type IN ('charge.refunded','charge.dispute.created')",Long.class,facts.paymentIntent)>0;
            if(hold){db.update("UPDATE aigc_credit_purchase SET status='REVIEW',billing_hold=1,payment_intent_id=?,checkout_session_id=?,last_error='支付有退款或争议，待人工核对' WHERE id=?",facts.paymentIntent,facts.id,id);recordEvent(event,type,facts.live,id,facts.paymentIntent,"支付待核对，未发放积分");return;}
            if(facts.live){
                BigDecimal delta=new BigDecimal(p.get("credit_points").toString()),available=new BigDecimal(quota.get("available_points").toString()).add(delta);
                String key="purchase:"+id+":grant";
                if(db.queryForList("SELECT id FROM aigc_quota_ledger WHERE tenant_id=? AND workspace_id=? AND business_key=?",tenant,workspace,key).isEmpty()){
                    db.update("UPDATE aigc_quota_account SET available_points=?,revision=revision+1 WHERE tenant_id=? AND workspace_id=?",available,tenant,workspace);
                    insert("aigc_quota_ledger",map("tenant_id",tenant,"workspace_id",workspace,"business_key",key,"entry_type","GRANT","available_delta",delta,"reserved_delta",BigDecimal.ZERO,"consumed_delta",BigDecimal.ZERO,"available_after",available,"reserved_after",quota.get("reserved_points"),"consumed_after",quota.get("consumed_points"),"remark","Stripe 充值到账 · "+p.get("purchase_no"),"creator",p.get("creator")));
                }
            }
            db.update("UPDATE aigc_credit_purchase SET status='PAID',credited=1,checkout_session_id=?,payment_intent_id=?,paid_time=CURRENT_TIMESTAMP(3),last_error=NULL WHERE id=?",facts.id,facts.paymentIntent,id);
            db.update("UPDATE pay_order SET status=10,success_time=CURRENT_TIMESTAMP(3),extension_id=?,no=?,channel_order_no=? WHERE id=?",p.get("pay_extension_id"),p.get("purchase_no"),facts.paymentIntent,p.get("pay_order_id"));
            db.update("UPDATE pay_order_extension SET status=10 WHERE id=?",p.get("pay_extension_id"));
        }else if(!paid&&!flag(p.get("credited"))&&!flag(p.get("billing_hold"))){
            String next=type.equals("checkout.session.expired")||"expired".equals(facts.status)?"EXPIRED":type.equals("checkout.session.async_payment_failed")?"FAILED":"PROCESSING";
            if(Set.of("EXPIRED","FAILED").contains(p.get("status")))next=p.get("status").toString();
            db.update("UPDATE aigc_credit_purchase SET status=?,checkout_session_id=?,payment_intent_id=? WHERE id=?",next,facts.id,facts.paymentIntent,id);
            if(next.equals("EXPIRED")||next.equals("FAILED")){db.update("UPDATE pay_order SET status=30 WHERE id=?",p.get("pay_order_id"));db.update("UPDATE pay_order_extension SET status=30 WHERE id=?",p.get("pay_extension_id"));}
        }
        recordEvent(event,type,facts.live,id,facts.paymentIntent,paid?"已验证支付；权益只发放一次":"未支付，不发放积分");
    });}
    public void risk(String event,String type,boolean live,String intent,long refunded){risk(event,type,live,intent,refunded,null);}
    public void risk(String event,String type,boolean live,String intent,long refunded,Long purchaseId){tx.executeWithoutResult(status->{
        // Serialise by the matching purchase; orphan events are retained for out-of-order fulfilment.
        var rows=purchaseId==null?db.queryForList("SELECT * FROM aigc_credit_purchase WHERE payment_intent_id=? AND environment=? FOR UPDATE",intent,live?"LIVE":"TEST"):db.queryForList("SELECT * FROM aigc_credit_purchase WHERE id=? AND environment=? FOR UPDATE",purchaseId,live?"LIVE":"TEST");
        if(purchaseId!=null&&rows.isEmpty())throw new PaymentError(409,"Stripe 退款对应的订单不存在");
        if(!rows.isEmpty()&&rows.getFirst().get("payment_intent_id")!=null&&!intent.equals(rows.getFirst().get("payment_intent_id")))throw new PaymentError(409,"Stripe 退款付款凭证不匹配");
        if(eventSeen(event,live))return;
        Long id=rows.isEmpty()?null:((Number)rows.getFirst().get("id")).longValue();
        if(id!=null){var p=rows.getFirst();db.update("UPDATE aigc_credit_purchase SET status='REVIEW',billing_hold=1,payment_intent_id=?,refund_minor=GREATEST(refund_minor,?),last_error='Stripe 退款或争议待人工核对' WHERE id=?",intent,refunded,id);if(refunded>0)db.update("UPDATE pay_order SET status=20,refund_price=GREATEST(refund_price,?) WHERE id=?",refunded,p.get("pay_order_id"));}
        recordEvent(event,type,live,id,intent,"退款或争议已记入核对；不自动扣成负数或重复发放权益");
    });}
    public Map<String,Object> history(Owner owner,long before,int limit){if(before<=0||limit<1||limit>100)throw new PaymentError(400,"订单分页参数错误");var rows=db.queryForList("SELECT * FROM aigc_credit_purchase WHERE tenant_id=? AND workspace_id=? AND user_id=? AND id<? AND deleted=0 ORDER BY id DESC LIMIT ?",owner.tenant,owner.workspace,owner.user,before,limit);
        return map("orders",rows.stream().map(this::dto).toList(),"nextId",rows.size()==limit?rows.getLast().get("id").toString():null,"testCreditedPoints",db.queryForObject("SELECT COALESCE(SUM(credit_points),0) FROM aigc_credit_purchase WHERE tenant_id=? AND workspace_id=? AND environment='TEST' AND credited=1",BigDecimal.class,owner.tenant,owner.workspace));}
    private long time(Object v){return v instanceof java.time.LocalDateTime t?t.toInstant(java.time.ZoneOffset.UTC).toEpochMilli():v instanceof java.util.Date d?d.getTime():0;}
    public Map<String,Object> dto(Map<String,Object> p){return map("id",p.get("id").toString(),"packId",p.get("pack_id").toString(),"orderNo",p.get("purchase_no"),"name",p.get("pack_name"),"environment",p.get("environment"),"currency",p.get("currency"),"amountMinor",p.get("amount_minor"),"creditPoints",p.get("credit_points"),"status",p.get("status"),"credited",flag(p.get("credited")),"created",time(p.get("create_time")),"paidAt",time(p.get("paid_time")),"expiresAt",time(p.get("expires_time")),"error",p.get("last_error"));}
}
