package com.edu.unimag.rodatours.security;

import com.edu.unimag.rodatours.security.reset.*;
import com.edu.unimag.rodatours.domain.repositories.TouristRepository;
import com.edu.unimag.rodatours.services.exceptions.ValidationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {
    @Mock PasswordResetRepository resets;@Mock AccountDetailsService users;@Mock StaffAccountRepository staff;
    @Mock TouristRepository tourists;@Mock PasswordEncoder encoder;
    PasswordResetProperties properties;ResetTokenCodec codec;PasswordResetService service;
    @BeforeEach void setup(){
        properties=new PasswordResetProperties();properties.setEnabled(true);properties.setFrontendUrl("https://rodatours.example");
        properties.setEncryptionKey(Base64.getEncoder().encodeToString(new byte[32]));codec=new ResetTokenCodec(properties);
        service=new PasswordResetService(properties,codec,resets,users,staff,tourists,encoder);
        ReflectionTestUtils.setField(service,"mailEnabled",true);
    }
    private StaffAccount account(){
        var account=new StaffAccount();account.setId(1L);account.setEmail("a@example.com");account.setPasswordHash("old");account.setRole(AccountRole.ADMIN);
        when(staff.findByEmailIgnoreCase("a@example.com")).thenReturn(Optional.of(account));when(staff.lockById(1L)).thenReturn(Optional.of(account));return account;
    }
    @Test void encryptionRoundTripAndTamperRejection(){
        String token=codec.generate(),encrypted=codec.encrypt(token);assertEquals(43,token.length());assertFalse(encrypted.contains(token));
        assertEquals(token,codec.decrypt(encrypted));assertNotEquals(token,codec.hash(token));
        assertThrows(IllegalStateException.class,()->codec.decrypt(encrypted+"bad"));
    }
    @Test void unknownEmailReturnsNormallyWithoutCreatingToken(){
        when(users.loadUserByUsername("unknown@example.com")).thenThrow(new UsernameNotFoundException("missing"));
        service.request("unknown@example.com");verifyNoInteractions(resets,staff,tourists,encoder);
    }
    @Test void persistsOnlyHashAndEncryptedDeliveryToken(){
        account();when(users.loadUserByUsername("a@example.com")).thenReturn(new ApiPrincipal("a@example.com","old",AccountRole.ADMIN,null,null,null,true));
        service.request("a@example.com");var capture=ArgumentCaptor.forClass(PasswordReset.class);verify(resets).save(capture.capture());
        var reset=capture.getValue();String plain=codec.decrypt(reset.getEncryptedToken());
        assertEquals(codec.hash(plain),reset.getTokenHash());assertTrue(reset.getExpiresAt().isAfter(Instant.now()));assertNull(reset.getConsumedAt());
    }
    @Test void tokenIsSingleUseAndPasswordIsHashed(){
        var account=account();String token=codec.generate();var reset=new PasswordReset();reset.setEmail(account.getEmail());reset.setTokenHash(codec.hash(token));reset.setExpiresAt(Instant.now().plusSeconds(1800));
        when(resets.findByTokenHash(reset.getTokenHash())).thenReturn(Optional.of(reset));when(resets.lockByTokenHash(reset.getTokenHash())).thenReturn(Optional.of(reset));
        when(encoder.encode("new-password-123")).thenReturn("new-hash");
        service.confirm(token,"new-password-123");assertEquals("new-hash",account.getPasswordHash());assertNotNull(reset.getConsumedAt());
        assertThrows(ValidationException.class,()->service.confirm(token,"new-password-123"));verify(encoder,times(1)).encode(anyString());
    }
    @Test void disabledOrUnconfiguredNeverPretendsDelivery(){
        properties.setEnabled(false);assertThrows(ResponseStatusException.class,()->service.request("a@example.com"));verifyNoInteractions(resets,users);
    }
    @Test void expiredTokenDoesNotChangePassword(){
        account();String token=codec.generate();var reset=new PasswordReset();reset.setEmail("a@example.com");reset.setExpiresAt(Instant.now().minusSeconds(1));
        when(resets.findByTokenHash(codec.hash(token))).thenReturn(Optional.of(reset));when(resets.lockByTokenHash(codec.hash(token))).thenReturn(Optional.of(reset));
        assertThrows(ValidationException.class,()->service.confirm(token,"new-password-123"));verifyNoInteractions(encoder);
    }
}
