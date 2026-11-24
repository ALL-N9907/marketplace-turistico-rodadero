package com.edu.unimag.rodatours.payments;

import com.edu.unimag.rodatours.domain.entities.Reservation;
import com.edu.unimag.rodatours.domain.entities.enums.ReservationStatus;
import com.edu.unimag.rodatours.domain.repositories.ReservationRepository;
import com.edu.unimag.rodatours.services.exceptions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock PaymentRepository payments;@Mock ReservationRepository reservations;@Mock PaymentProvider provider;
    PaymentProperties properties;PaymentService service;Payment payment;
    static PaymentProperties configured(){var p=new PaymentProperties();p.setEnabled(true);p.setAccessToken("TEST-token");p.setWebhookSecret("test-secret-at-least-16");p.setCollectorId("123");p.setFrontendUrl("https://example.com");p.setNotificationUrl("https://example.com/api/payments/webhook");return p;}
    @BeforeEach void setup(){properties=configured();service=new PaymentService(payments,reservations,provider,properties);payment=new Payment();payment.setId(1L);payment.setReservationId(7L);payment.setReference("ref");payment.setAmount(new BigDecimal("180000.00"));}
    private PaymentProvider.RemotePayment remote(String status){return new PaymentProvider.RemotePayment("10","ref",new BigDecimal("180000"),"COP",status,false,"123",BigDecimal.ZERO,Instant.now());}
    private void lock(){when(payments.lockByReference("ref")).thenReturn(Optional.of(payment));}
    @Test void productiveTokensAndUnconfiguredProviderAreRejected(){properties.setAccessToken("APP_USR-production");assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.checkout(7L));verifyNoInteractions(provider,reservations);}
    @Test void unconfirmedReservationCannotStartCheckout(){when(reservations.findByIdForUpdate(7L)).thenReturn(Optional.of(Reservation.builder().estado(ReservationStatus.SCHEDULED).build()));assertThrows(ConflictException.class,()->service.checkout(7L));verifyNoInteractions(provider);}
    @Test void stableCheckoutIsReturnedWithoutNewProviderCall(){
        when(reservations.findByIdForUpdate(7L)).thenReturn(Optional.of(Reservation.builder().estado(ReservationStatus.CONFIRMED).fechaInicio(LocalDateTime.now().plusDays(1)).build()));
        when(payments.findByReservationId(7L)).thenReturn(Optional.of(payment));lock();payment.setCheckoutUrl("https://sandbox.mercadopago.com.co/checkout/v1/redirect?pref_id=abc");payment.setCheckoutExpiresAt(Instant.now().plusSeconds(600));
        assertNotNull(service.checkout(7L).checkoutUrl());verifyNoInteractions(provider);
    }
    @Test void uncertainCheckoutIsNotBlindlyRetried(){
        when(reservations.findByIdForUpdate(7L)).thenReturn(Optional.of(Reservation.builder().estado(ReservationStatus.CONFIRMED).fechaInicio(LocalDateTime.now().plusDays(1)).build()));
        when(payments.findByReservationId(7L)).thenReturn(Optional.of(payment));lock();payment.setStatus("CHECKOUT_UNKNOWN");
        assertThrows(ConflictException.class,()->service.checkout(7L));verifyNoInteractions(provider);
    }
    @Test void appliesVerifiedApprovalWithoutConfirmingReservation(){lock();service.apply(remote("approved"));assertEquals("APPROVED",payment.getStatus());assertEquals("10",payment.getProviderPaymentId());verifyNoInteractions(reservations);}
    @Test void rejectsLivePayments(){lock();var r=remote("approved");assertThrows(ValidationException.class,()->service.apply(new PaymentProvider.RemotePayment(r.id(),r.reference(),r.amount(),r.currency(),r.status(),true,r.collectorId(),r.refunded(),r.updatedAt())));verify(payments,never()).save(any());}
    @Test void rejectsWrongAmountsAndCurrency(){lock();assertThrows(ValidationException.class,()->service.apply(new PaymentProvider.RemotePayment("10","ref",BigDecimal.ONE,"COP","approved",false,"123",BigDecimal.ZERO,Instant.now())));assertThrows(ValidationException.class,()->service.apply(new PaymentProvider.RemotePayment("10","ref",payment.getAmount(),"USD","approved",false,"123",BigDecimal.ZERO,Instant.now())));verify(payments,never()).save(any());}
    @Test void duplicateApprovalIsFlaggedRatherThanOverwritingFirstPayment(){lock();payment.setStatus("APPROVED");payment.setProviderPaymentId("9");service.apply(remote("approved"));assertEquals("REVIEW_REQUIRED",payment.getStatus());assertTrue(payment.isDuplicateDetected());assertEquals("9",payment.getProviderPaymentId());}
    @Test void olderProviderSnapshotDoesNotRegressStatus(){lock();payment.setProviderPaymentId("10");payment.setProviderUpdatedAt(Instant.now().plusSeconds(30));payment.setStatus("APPROVED");service.apply(remote("pending"));assertEquals("APPROVED",payment.getStatus());verify(payments,never()).save(any());}
    @Test void fullAndPartialRefundsRemainSeparateFromReservation(){lock();service.apply(new PaymentProvider.RemotePayment("10","ref",payment.getAmount(),"COP","approved",false,"123",BigDecimal.TEN,Instant.now()));assertEquals("PARTIALLY_REFUNDED",payment.getStatus());service.apply(new PaymentProvider.RemotePayment("10","ref",payment.getAmount(),"COP","refunded",false,"123",payment.getAmount(),Instant.now().plusSeconds(1)));assertEquals("REFUNDED",payment.getStatus());verifyNoInteractions(reservations);}
    @Test void sandboxRedirectCannotBeAnArbitraryOrProductiveSite(){assertTrue(PaymentProperties.sandboxUrl("https://sandbox.mercadopago.com.co/checkout/v1/redirect?pref_id=1"));assertFalse(PaymentProperties.sandboxUrl("https://www.mercadopago.com.co/checkout"));assertFalse(PaymentProperties.sandboxUrl("https://sandbox.mercadopago.com.co.evil.example/"));assertFalse(PaymentProperties.sandboxUrl("http://sandbox.mercadopago.com.co/"));}
}
