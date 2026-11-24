package com.edu.unimag.rodatours.security;

import com.edu.unimag.rodatours.api.controllers.SessionController;
import com.edu.unimag.rodatours.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SessionController.class)
@Import(SecurityConfig.class)
class SessionControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean org.springframework.security.core.userdetails.UserDetailsService users;
    @Test
    void createsServerSessionWithoutReturningPasswordAndLogoutInvalidatesIt() throws Exception {
        var actor = new ApiPrincipal("ana@example.com","private-hash",AccountRole.TOURIST,null,1L,null,true);
        when(authenticationManager.authenticate(any())).thenReturn(UsernamePasswordAuthenticationToken.authenticated(actor,null,actor.getAuthorities()));
        var oldSession=new MockHttpSession();String oldId=oldSession.getId();
        var result=mvc.perform(post("/api/auth/login").session(oldSession).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@example.com\",\"password\":\"test-password\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.touristId").value(1))
                .andExpect(jsonPath("$.passwordHash").doesNotExist()).andReturn();
        var session=(MockHttpSession)result.getRequest().getSession(false);
        assertNotNull(session);assertNotEquals(oldId,session.getId());
        assertNotNull(session.getAttribute("SPRING_SECURITY_CONTEXT"));
        when(users.loadUserByUsername("ana@example.com")).thenReturn(actor);
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        assertTrue(session.isInvalid());
    }
    @Test
    void loginRequiresCsrf() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ana@example.com\",\"password\":\"test-password\"}")).andExpect(status().isForbidden());
        verifyNoInteractions(authenticationManager);
    }
    @Test
    void invalidCredentialsAreNotAnInternalServerError() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Credenciales incorrectas."));
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ana@example.com\",\"password\":\"wrong\"}")).andExpect(status().isUnauthorized());
    }
}
