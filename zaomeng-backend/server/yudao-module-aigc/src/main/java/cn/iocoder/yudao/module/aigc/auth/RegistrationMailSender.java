package cn.iocoder.yudao.module.aigc.auth;

import cn.iocoder.yudao.module.system.dal.dataobject.mail.MailAccountDO;
import cn.iocoder.yudao.module.system.service.mail.MailAccountService;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import org.dromara.hutool.extra.mail.MailAccount;
import org.dromara.hutool.extra.mail.MailUtil;
import org.springframework.stereotype.Component;
import java.util.List;
import static cn.iocoder.yudao.module.aigc.auth.GoogleIdentityStore.AuthError;

/** Reuses the administrator's Yudao mail account; OTP contents are never persisted in mail logs. */
@Component
public class RegistrationMailSender {
    private final MailAccountService accounts;
    private final ConfigApi config;
    public RegistrationMailSender(MailAccountService accounts, ConfigApi config) { this.accounts = accounts; this.config = config; }
    public MailAccountDO account() {
        String id = config.getConfigValueByKey("aigc.email.mail-account-id");
        MailAccountDO account;
        try {
            if (id != null && !id.isBlank()) account = accounts.getMailAccount(Long.valueOf(id));
            else {
                var available = accounts.getMailAccountList();
                account = available.size() == 1 ? available.getFirst() : null;
            }
        } catch (NumberFormatException e) { account = null; }
        if (account == null || account.getHost() == null || account.getMail() == null || account.getPassword() == null)
            throw new AuthError(503, "验证码邮件尚未配置，请联系管理员");
        return account;
    }
    public void send(MailAccountDO account, String to, String code, String locale) {
        String title, content;
        switch (locale) {
            case "en" -> { title = "Safillu verification code"; content = "Your Safillu registration code is " + code + ". It expires in 5 minutes. Do not share this code. If you did not request it, ignore this email."; }
            case "es" -> { title = "Código de verificación de Safillu"; content = "Tu código de registro en Safillu es " + code + ". Caduca en 5 minutos. No lo compartas. Si no lo solicitaste, ignora este correo."; }
            default -> { title = "Safillu 注册验证码"; content = "你的 Safillu 注册验证码为 " + code + "，5 分钟内有效。请勿向他人透露。若非本人操作，请忽略此邮件。"; }
        }
        var mail = new MailAccount().setFrom("Safillu <" + account.getMail() + ">").setAuth(true)
            .setUser(account.getUsername()).setPass(account.getPassword().toCharArray())
            .setHost(account.getHost()).setPort(account.getPort())
            .setSslEnable(account.getSslEnable()).setStarttlsEnable(account.getStarttlsEnable());
        // SMTP is synchronous: the UI reports success only after the server accepts the message.
        mail.setTimeout(30000).setConnectionTimeout(10000).setWriteTimeout(15000)
            .setCustomProperty("mail.smtp.quitwait", false);
        // Some SMTP providers require an explicit TLS protocol. Keep this optional
        // and operator-controlled, without allowing obsolete TLS versions.
        String protocols=config.getConfigValueByKey("aigc.email.ssl-protocols");
        if (protocols!=null && !protocols.isBlank()) {
            if (!protocols.matches("TLSv1\\.[23]( TLSv1\\.[23])?"))
                throw new AuthError(503, "验证码邮件尚未配置，请联系管理员");
            mail.setSslProtocols(protocols);
        }
        try { MailUtil.send(mail, List.of(to), List.of(), List.of(), title, content, false); }
        catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Registration mail rejected by SMTP account {} ({})", account.getId(), e.getClass().getSimpleName());
            throw new AuthError(503, "验证码发送失败，请稍后重试");
        }
    }
}
