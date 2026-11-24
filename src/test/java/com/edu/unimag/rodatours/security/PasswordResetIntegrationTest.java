package com.edu.unimag.rodatours.security;

import com.edu.unimag.rodatours.domain.entities.Tourist;
import com.edu.unimag.rodatours.repositories.TestPostgresContainer;
import com.edu.unimag.rodatours.security.reset.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.jpa.hibernate.ddl-auto=create-drop","rodatours.password-reset.enabled=true",
        "rodatours.password-reset.encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "rodatours.password-reset.frontend-url=https://example.com","rodatours.notifications.mail-enabled=true"})
@AutoConfigureMockMvc @Transactional
class PasswordResetIntegrationTest {
    @DynamicPropertySource static void postgres(DynamicPropertyRegistry r){TestPostgresContainer.register(r);}
    @Autowired MockMvc mvc;@Autowired EntityManager em;@Autowired PasswordEncoder encoder;
    @Autowired PasswordResetRepository resets;@Autowired ResetTokenCodec codec;
    @MockitoBean JavaMailSender mail;
    @Test void recoveryIsOneUseAndRevokesSessionOnNextRequest()throws Exception{
        String email="reset-integration@example.com",password="old-password-123";
        var tourist=Tourist.builder().nombreCompleto("Prueba").correoElectronico(email).contrasena(encoder.encode(password)).build();em.persist(tourist);em.flush();
        var login=mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\""+email+"\",\"password\":\""+password+"\"}")).andExpect(status().isOk()).andReturn();
        var session=(MockHttpSession)login.getRequest().getSession(false);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/password-reset/request").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\""+email+"\"}")).andExpect(status().isAccepted()).andExpect(jsonPath("$.token").doesNotExist());
        var reset=resets.findByEmail(email).orElseThrow();String token=codec.decrypt(reset.getEncryptedToken());
        String body="{\"token\":\""+token+"\",\"password\":\"new-password-123\"}";
        mvc.perform(post("/api/auth/password-reset/confirm").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNoContent());
        assertTrue(encoder.matches("new-password-123",tourist.getContrasena()));
        mvc.perform(post("/api/auth/password-reset/confirm").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
        assertTrue(session.isInvalid());
    }
}
