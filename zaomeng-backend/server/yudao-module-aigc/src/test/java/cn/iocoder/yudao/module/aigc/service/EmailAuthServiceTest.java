package cn.iocoder.yudao.module.aigc.service;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.aigc.auth.EmailCodeStore;
import cn.iocoder.yudao.module.aigc.auth.RegistrationMailSender;
import cn.iocoder.yudao.module.aigc.auth.GoogleIdentityStore.AuthError;
import cn.iocoder.yudao.module.aigc.controller.admin.EmailAuthController;
import cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthLoginReqVO;
import cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthRegisterReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.profile.UserProfileUpdateReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.mail.MailAccountDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.auth.AdminAuthService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailAuthServiceTest {
    final EmailCodeStore codes=mock(EmailCodeStore.class);
    final RegistrationMailSender mail=mock(RegistrationMailSender.class);
    final AdminUserService users=mock(AdminUserService.class);
    final AdminAuthService auth=mock(AdminAuthService.class);
    final JdbcTemplate db=mock(JdbcTemplate.class);
    final PlatformTransactionManager manager=mock(PlatformTransactionManager.class);
    final EmailAuthService service=new EmailAuthService(codes,mail,users,auth,db,manager);
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }
    void setupUser() {
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(users.registerUser(any())).thenAnswer(call -> {
            assertEquals(1L,TenantContextHolder.getTenantId()); assertFalse(TenantContextHolder.isIgnore());
            return new AdminUserDO().setId(42L);
        });
    }
    @Test void invalidCodeCannotCreateUserOrTokens() {
        doThrow(new AuthError(400,"invalid")).when(codes).consume("creator@example.com","123456");
        assertThrows(AuthError.class,()->service.register("Creator@Example.com","123456","Example123"));
        verifyNoInteractions(users,auth,manager);
    }
    @Test void smtpFailureRevokesCodeAndReturnsFailure() {
        var account=new MailAccountDO(); when(mail.account()).thenReturn(account);
        when(codes.issue("creator@example.com","ip")).thenReturn("123456");
        doThrow(new AuthError(503,"SMTP rejected")).when(mail).send(account,"creator@example.com","123456","es");
        assertThrows(AuthError.class,()->service.sendCode("Creator@Example.com","es","ip"));
        verify(codes).revoke("creator@example.com","123456"); verifyNoInteractions(users,auth);
    }
    @Test void registrationUsesNativeHashingProfileAndTokenServiceAndRestoresTenant() {
        setupUser(); TenantContextHolder.setTenantId(9L); TenantContextHolder.setIgnore(true);
        service.register("Creator@Example.com","123456","Example123");
        var req=ArgumentCaptor.forClass(AuthRegisterReqVO.class); verify(users).registerUser(req.capture());
        assertTrue(req.getValue().getUsername().matches("e[a-f0-9]{24}"));
        assertEquals("creator",req.getValue().getNickname()); assertEquals("Example123",req.getValue().getPassword());
        var profile=ArgumentCaptor.forClass(UserProfileUpdateReqVO.class); verify(users).updateUserProfile(eq(42L),profile.capture());
        assertEquals("creator@example.com",profile.getValue().getEmail());
        verify(db).update(startsWith("INSERT INTO aigc_email_identity"),eq("creator@example.com"),eq(42L));
        verify(manager).commit(any()); verify(auth).loginVerifiedSocialUser(42L);
        assertEquals(9L,TenantContextHolder.getTenantId()); assertTrue(TenantContextHolder.isIgnore());
    }
    @Test void uniquenessFailureRollsBackNativeUserAndNeverIssuesToken() {
        setupUser(); when(db.update(startsWith("INSERT INTO aigc_email_identity"),eq("creator@example.com"),eq(42L))).thenThrow(new DuplicateKeyException("duplicate"));
        assertThrows(AuthError.class,()->service.register("creator@example.com","123456","Example123"));
        verify(manager).rollback(any()); verify(manager,never()).commit(any()); verifyNoInteractions(auth);
    }
    @Test void existingEmailCannotConsumeCodeOrSendAnotherEmail() {
        when(db.queryForList(startsWith("SELECT user_id"),eq("creator@example.com"))).thenReturn(List.of(Map.of("user_id",42L)));
        assertThrows(AuthError.class,()->service.register("creator@example.com","123456","Example123"));
        assertThrows(AuthError.class,()->service.sendCode("creator@example.com","en","ip"));
        verifyNoInteractions(codes,mail,users,auth);
    }
    @Test void emailLoginResolvesVerifiedIdentityAndUsesNativePasswordCheck() {
        when(db.queryForList(startsWith("SELECT u.username"),eq("creator@example.com"))).thenReturn(List.of(Map.of("username","e0123456789abcdef01234567")));
        service.login("Creator@Example.com","Example123","ip");
        var request=ArgumentCaptor.forClass(AuthLoginReqVO.class); verify(auth).login(request.capture());
        assertEquals("e0123456789abcdef01234567",request.getValue().getUsername());
        assertEquals("Example123",request.getValue().getPassword()); verify(codes).limitLogin("creator@example.com","ip");
    }
    @Test void existingUsernameLoginRemainsCompatibleAndUnknownEmailFails() {
        service.login("olduser123","Example123","ip"); verify(auth).login(any());
        reset(auth); assertThrows(AuthError.class,()->service.login("unknown@example.com","Example123","ip"));
        verifyNoInteractions(auth);
    }
    @Test void httpValidationRejectsBadEmailCodeAndPasswordWithoutEchoingSecrets() {
        try(var factory=Validation.buildDefaultValidatorFactory()) {
            var validator=factory.getValidator();
            assertTrue(validator.validate(new EmailAuthController.RegisterRequest("creator@example.com","123456","Example123")).isEmpty());
            assertFalse(validator.validate(new EmailAuthController.RegisterRequest("not-email","12345","short")).isEmpty());
            assertFalse(new EmailAuthController.RegisterRequest("creator@example.com","123456","Example123").toString().contains("Example123"));
        }
    }
}
