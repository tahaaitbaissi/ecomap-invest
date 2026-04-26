package com.example.backend.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ProceedingJoinPoint pjp;

    @Mock
    private MethodSignature methodSignature;

    static class Dummy {
        @Audited(action = "TEST", maskParams = {"token"})
        public String doThing(String token, int n) {
            return "ok";
        }
    }

    @Test
    void around_persistsMaskedArgs_andRecordsSuccess() throws Throwable {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "a@a.com", "x", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        Method m = Dummy.class.getDeclaredMethod("doThing", String.class, int.class);
        Audited audited = m.getAnnotation(Audited.class);
        assertNotNull(audited);

        when(pjp.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringTypeName()).thenReturn(Dummy.class.getName());
        when(methodSignature.getName()).thenReturn("doThing");
        when(methodSignature.getParameterNames()).thenReturn(new String[] {"token", "n"});
        when(pjp.getArgs()).thenReturn(new Object[] {"secret-token", 5});
        when(pjp.proceed()).thenReturn("ok");

        AuditAspect aspect = new AuditAspect(auditLogRepository);
        Object out = aspect.around(pjp, audited);
        assertEquals("ok", out);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals("a@a.com", saved.getUserEmail());
        assertEquals("TEST", saved.getAction());
        assertEquals(true, saved.getSuccess());
        // argsSummary should be masked for token
        String args = saved.getArgsSummary();
        assertNotNull(args);
        // token param must not leak
        assertEquals(true, args.contains("\"***\""));
    }
}

