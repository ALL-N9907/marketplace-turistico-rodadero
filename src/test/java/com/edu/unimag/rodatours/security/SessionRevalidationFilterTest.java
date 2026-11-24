package com.edu.unimag.rodatours.security;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionRevalidationFilterTest {
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    @Test
    @SuppressWarnings("unchecked")
    void passwordChangeInvalidatesExistingSession() throws Exception {
        ObjectProvider<UserDetailsService> provider=mock(ObjectProvider.class);UserDetailsService users=mock(UserDetailsService.class);
        when(provider.getObject()).thenReturn(users);
        var old=new ApiPrincipal("a@example.com","old",AccountRole.TOURIST,null,1L,null,true);
        when(users.loadUserByUsername(old.email())).thenReturn(new ApiPrincipal(old.email(),"new",old.role(),null,1L,null,true));
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(old,null,old.getAuthorities()));
        var request=new MockHttpServletRequest();var session=(MockHttpSession)request.getSession();
        new SessionRevalidationFilter(provider).doFilter(request,new MockHttpServletResponse(),new MockFilterChain());
        assertTrue(session.isInvalid());assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
    @Test
    void boundsAuthenticationAttempts() throws Exception {
        var filter=new SecurityRateLimitFilter();
        for(int i=0;i<31;i++){
            var req=new MockHttpServletRequest("POST","/api/auth/login");req.setServletPath("/api/auth/login");req.setRemoteAddr("127.0.0.1");
            var res=new MockHttpServletResponse();filter.doFilter(req,res,new MockFilterChain());
            assertEquals(i<30?200:429,res.getStatus());
        }
    }
}
