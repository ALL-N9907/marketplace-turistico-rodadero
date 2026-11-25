package com.edu.unimag.rodatours.security;

import com.edu.unimag.rodatours.domain.repositories.*;
import com.edu.unimag.rodatours.security.reset.*;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.*;
import java.util.Base64;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordResetTransactionTest {
    @Test void missingEmailCommitsNormallyThroughBothTransactionalProxies(){
        var staff=mock(StaffAccountRepository.class);var tourists=mock(TouristRepository.class);
        when(staff.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());
        when(tourists.findByCorreoElectronicoIgnoreCase("missing@example.com")).thenReturn(Optional.empty());
        var resets=mock(PasswordResetRepository.class);var tx=new TestTransactions();
        var service=service(tx,staff,tourists,resets);
        assertDoesNotThrow(()->service.request(" missing@example.com "));
        assertEquals(1,tx.commits);assertEquals(0,tx.rollbacks);verifyNoInteractions(resets);
    }

    @Test void databaseFailureStillRollsBackAndIsNotTreatedAsUnknownEmail(){
        var staff=mock(StaffAccountRepository.class);var tourists=mock(TouristRepository.class);
        when(staff.findByEmailIgnoreCase("missing@example.com")).thenThrow(new DataAccessResourceFailureException("database unavailable"));
        var resets=mock(PasswordResetRepository.class);var tx=new TestTransactions();
        var service=service(tx,staff,tourists,resets);
        assertThrows(DataAccessResourceFailureException.class,()->service.request("missing@example.com"));
        assertEquals(0,tx.commits);assertEquals(1,tx.rollbacks);verifyNoInteractions(resets,tourists);
    }

    private PasswordResetService service(TestTransactions tx,StaffAccountRepository staff,TouristRepository tourists,PasswordResetRepository resets){
        var users=proxy(new AccountDetailsService(staff,tourists,mock(AgencyRepository.class),mock(GuideRepository.class)),tx);
        var properties=new PasswordResetProperties();properties.setEnabled(true);properties.setFrontendUrl("https://example.com");
        properties.setEncryptionKey(Base64.getEncoder().encodeToString(new byte[32]));
        var service=new PasswordResetService(properties,new ResetTokenCodec(properties),resets,users,staff,tourists,mock(PasswordEncoder.class));
        ReflectionTestUtils.setField(service,"mailEnabled",true);
        return proxy(service,tx);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(T target,TestTransactions tx){
        var interceptor=new TransactionInterceptor();interceptor.setTransactionManager(tx);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        var proxy=new ProxyFactory(target);proxy.setProxyTargetClass(true);proxy.addAdvice(interceptor);
        return (T)proxy.getProxy();
    }

    // Recurso mínimo: Spring administra participación, rollback-only y commit reales
    // a través de ambos proxies. No requiere PostgreSQL ni sustituye su integración.
    private static class TestTransactions extends AbstractPlatformTransactionManager {
        private final ThreadLocal<State> current=new ThreadLocal<>();
        int commits,rollbacks;
        @Override protected Object doGetTransaction(){var state=current.get();return state==null?new State():state;}
        @Override protected boolean isExistingTransaction(Object transaction){return ((State)transaction).active;}
        @Override protected void doBegin(Object transaction,TransactionDefinition definition){var state=(State)transaction;state.active=true;current.set(state);}
        @Override protected void doSetRollbackOnly(DefaultTransactionStatus status){((State)status.getTransaction()).rollbackOnly=true;}
        @Override protected void doCommit(DefaultTransactionStatus status){commits++;}
        @Override protected void doRollback(DefaultTransactionStatus status){rollbacks++;}
        @Override protected void doCleanupAfterCompletion(Object transaction){current.remove();}
        private static class State implements SmartTransactionObject {
            boolean active,rollbackOnly;
            @Override public boolean isRollbackOnly(){return rollbackOnly;}
            @Override public void flush(){}
        }
    }
}
