package com.edu.unimag.rodatours.security;

import com.edu.unimag.rodatours.domain.entities.*;
import com.edu.unimag.rodatours.domain.entities.enums.*;
import com.edu.unimag.rodatours.media.VideoProvider;
import com.edu.unimag.rodatours.repositories.TestPostgresContainer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTest {
    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) { TestPostgresContainer.register(registry); }

    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired PasswordEncoder encoder;
    @MockitoBean VideoProvider provider;

    private final String password = "test-password-12345";
    private Tour ownTour;
    private Tour foreignTour;
    private Tourist tourist;
    private Reservation reservation;
    private Agency ownAgency;

    private <T> T persist(T entity) { em.persist(entity); return entity; }

    @BeforeEach
    void setup() {
        ownAgency = persist(Agency.builder().nombreComercial("Propia").nitORegistro("nit-own")
                .direccion("Rodadero").telefonoContacto("3001234567").build());
        var foreignAgency = persist(Agency.builder().nombreComercial("Ajena").nitORegistro("nit-foreign")
                .direccion("Santa Marta").telefonoContacto("3001234567").build());
        var type = persist(ExperienceType.builder().nombre("Naturaleza").build());
        ownTour = tour(ownAgency, type, "Minca");
        foreignTour = tour(foreignAgency, type, "Tayrona");
        staff("operator@example.com", AccountRole.AGENCY_OPERATOR, ownAgency.getId());
        staff("admin@example.com", AccountRole.ADMIN, null);
        tourist = persist(Tourist.builder().nombreCompleto("Ana").correoElectronico("ana@example.com")
                .contrasena(encoder.encode(password)).build());
        var schedule = persist(TourSchedule.builder().tour(ownTour).fechaTour(LocalDate.now().plusDays(7))
                .horaInicio(LocalTime.of(8, 0)).cuposTotales(10).cuposDisponibles(10).build());
        reservation = persist(Reservation.builder().turista(tourist).horario(schedule)
                .fechaInicio(schedule.getStartDateTime()).fechaFin(schedule.calculateEndDateTime())
                .cantidadPersonas(1).precioTotal(new BigDecimal("100000")).build());
        em.flush();
    }

    private Tour tour(Agency agency, ExperienceType type, String title) {
        return persist(Tour.builder().agencia(agency).tipoExperiencia(type).titulo(title).descripcion("Caminata")
                .destino(title).precio(new BigDecimal("100000")).duracionHoras(new BigDecimal("2"))
                .nivelDificultad(DifficultyLevel.BAJO).build());
    }

    private void staff(String email, AccountRole role, Long agencyId) {
        var account = new StaffAccount();
        account.setEmail(email);
        account.setPasswordHash(encoder.encode(password));
        account.setRole(role);
        account.setAgencyId(agencyId);
        persist(account);
    }

    @Test
    void authenticatesRealAccountsAndNeverExposesPasswordHash() throws Exception {
        mvc.perform(get("/api/auth/me").with(httpBasic("operator@example.com", password)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("AGENCY_OPERATOR"))
                .andExpect(jsonPath("$.agencyId").value(ownAgency.getId()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(content().string(not(containsString(password))));
        mvc.perform(get("/api/auth/me").with(httpBasic("ana@example.com", password)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.touristId").value(tourist.getId()));
        mvc.perform(get("/api/auth/me").with(httpBasic("ana@example.com", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicCatalogIsAccessibleButTouristDataIsPrivate() throws Exception {
        mvc.perform(get("/api/tours")).andExpect(status().isOk());
        mvc.perform(get("/api/tourists")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/tourists").with(httpBasic("ana@example.com", password)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/tourists/{id}", tourist.getId()).with(httpBasic("ana@example.com", password)))
                .andExpect(status().isOk());
    }

    @Test
    void operatorCannotUploadVideoToAnotherAgency() throws Exception {
        mvc.perform(post("/api/tours/{id}/video/upload-authorization", foreignTour.getId())
                        .with(httpBasic("operator@example.com", password)).with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(provider);
    }

    @Test
    void operatorCanUploadVerifyReadAndDeleteOwnVideo() throws Exception {
        when(provider.authorize(anyString(), any())).thenReturn(new VideoProvider.UploadAuthorization(
                "https://api.cloudinary.com/v1_1/test/video/upload", "key", Map.of(), "signature"));
        mvc.perform(post("/api/tours/{id}/video/upload-authorization", ownTour.getId())
                        .with(httpBasic("operator@example.com", password)).with(csrf()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.upload.signature").value("signature"));
        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(provider).authorize(captor.capture(), any());
        when(provider.inspect(captor.getValue())).thenReturn(new VideoProvider.VideoMetadata(captor.getValue(),
                "https://res.cloudinary.com/test/video/upload/tour.mp4", "mp4", 2000, 30, 1280, 720));
        mvc.perform(get("/api/tours/{id}/video", ownTour.getId())).andExpect(status().isNotFound());
        mvc.perform(post("/api/tours/{id}/video/complete", ownTour.getId())
                        .with(httpBasic("operator@example.com", password)).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.durationSeconds").value(30));
        mvc.perform(get("/api/tours/{id}/video", ownTour.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.url").exists());
        mvc.perform(delete("/api/tours/{id}/video", ownTour.getId())
                        .with(httpBasic("operator@example.com", password)).with(csrf()))
                .andExpect(status().isNoContent());
        verify(provider).delete(captor.getValue());
    }

    @Test
    void touristsCannotManageVideosOrConfirmReservations() throws Exception {
        mvc.perform(post("/api/tours/{id}/video/upload-authorization", ownTour.getId())
                        .with(httpBasic("ana@example.com", password)).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/reservations/{id}/confirm", reservation.getId())
                        .with(httpBasic("ana@example.com", password)).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/reservations/{id}", reservation.getId()).with(httpBasic("ana@example.com", password)))
                .andExpect(status().isOk());
    }

    @Test
    void csrfStillRequiredForAuthenticatedWrites() throws Exception {
        mvc.perform(post("/api/tours/{id}/video/upload-authorization", ownTour.getId())
                        .with(httpBasic("operator@example.com", password)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(provider);
    }

    @Test
    void disabledAgencyCannotAuthenticateOperator() throws Exception {
        ownAgency.setEstado(AgencyStatus.INACTIVE);
        em.flush();
        mvc.perform(get("/api/auth/me").with(httpBasic("operator@example.com", password)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicRegistrationCannotAssignAdministrativeRoles() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Nuevo turista","email":"new@example.com","password":"strong-password-123",
                         "role":"ADMIN","estado":"INACTIVE","agencyId":1}
                        """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("ACTIVE"));
        mvc.perform(get("/api/auth/me").with(httpBasic("new@example.com", "strong-password-123")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("TOURIST"));
    }

    @Test
    void onlyAdminCanCreateStaffAndStaffCannotEscalatePrivileges() throws Exception {
        String body = "{\"email\":\"new-staff@example.com\",\"password\":\"strong-password-123\",\"role\":\"COORDINATOR\"}";
        mvc.perform(post("/api/auth/accounts").with(httpBasic("operator@example.com", password)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/accounts").with(httpBasic("admin@example.com", password)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("COORDINATOR"));
    }

    @Test
    void globalCatalogFiltersAndComparisonWorkAcrossAgencies() throws Exception {
        ownTour.setApoyoComunidades(true);
        ownTour.setItinerario("Caminata y visita al río");
        em.flush();
        mvc.perform(get("/api/catalog/tours").param("supportsCommunities", "true").param("minPrice", "90000")
                        .param("maxHours", "3"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].tour.id").value(ownTour.getId()))
                .andExpect(jsonPath("$.content[0].details.itinerary").value("Caminata y visita al río"));
        mvc.perform(get("/api/catalog/tours/compare").param("ids", ownTour.getId() + "," + foreignTour.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/catalog/tours").param("minPrice", "200000").param("maxPrice", "100000"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/catalog/tours/compare").param("ids", ownTour.getId() + "," + ownTour.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void agencyLocationAndTourDetailsAreScopedToOwner() throws Exception {
        String location = "{\"latitude\":11.203,\"longitude\":-74.227}";
        mvc.perform(put("/api/agencies/{id}/location", ownAgency.getId()).with(httpBasic("operator@example.com", password))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(location))
                .andExpect(status().isOk()).andExpect(jsonPath("$.latitude").value(11.203));
        mvc.perform(get("/api/agencies/{id}/location", ownAgency.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.longitude").value(-74.227));
        mvc.perform(put("/api/agencies/{id}/location", ownAgency.getId()).with(httpBasic("operator@example.com", password))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"latitude\":100,\"longitude\":0}"))
                .andExpect(status().isBadRequest());
        String details = "{\"itinerary\":\"Visita al río\",\"conditions\":\"Llevar agua\",\"supportsCommunities\":true,\"environmentalPractices\":false}";
        mvc.perform(put("/api/tours/{id}/details", foreignTour.getId()).with(httpBasic("operator@example.com", password))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(details))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/tours/{id}/details", ownTour.getId()).with(httpBasic("operator@example.com", password))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(details))
                .andExpect(status().isOk()).andExpect(jsonPath("$.supportsCommunities").value(true));
    }

    @Test
    void availabilityShowsPendingHoldsNotRawStock() throws Exception {
        mvc.perform(get("/api/availability/tours/{id}", ownTour.getId())
                        .param("fecha", reservation.getHorario().getFechaTour().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].cuposDisponibles").value(9));
        mvc.perform(get("/api/availability/tours/{id}", ownTour.getId())
                        .param("fecha", reservation.getHorario().getFechaTour().toString()).param("cantidadPersonas", "10"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void notificationInboxIsPrivateToRecipient() throws Exception {
        var own = new com.edu.unimag.rodatours.notifications.Notification();
        own.setRecipientEmail("ana@example.com");
        own.setReservationId(reservation.getId());
        own.setSubject("Propia");
        own.setMessage("Mensaje propio");
        persist(own);
        var foreign = new com.edu.unimag.rodatours.notifications.Notification();
        foreign.setRecipientEmail("operator@example.com");
        foreign.setReservationId(reservation.getId());
        foreign.setSubject("Privada de otra cuenta");
        foreign.setMessage("No debe aparecer");
        persist(foreign);
        em.flush();
        mvc.perform(get("/api/notifications").with(httpBasic("ana@example.com", password)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(content().string(not(containsString("No debe aparecer"))));
        mvc.perform(put("/api/notifications/{id}/read", foreign.getId()).with(httpBasic("ana@example.com", password)).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/notifications/{id}/read", own.getId()).with(httpBasic("ana@example.com", password)).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.readAt").exists());
    }

    @Test
    void voucherIsPrivateAndOnlyOperatorCanRedeemConfirmedReservationOnce() throws Exception {
        reservation.setEstado(ReservationStatus.CONFIRMED);
        reservation.getHorario().setFechaTour(LocalDate.now());
        em.flush();
        mvc.perform(get("/api/reservations/{id}/voucher", reservation.getId()))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/reservations/{id}/voucher/qr", reservation.getId()).with(httpBasic("ana@example.com", password)))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG));
        String body = "{\"qrPayload\":\"rodatours:reservation:" + reservation.getCodigoQr() + "\"}";
        mvc.perform(post("/api/reservations/{id}/check-in", reservation.getId()).with(httpBasic("ana@example.com", password))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/reservations/{id}/check-in", reservation.getId()).with(httpBasic("operator@example.com", password))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.checkedInAt").exists());
        mvc.perform(post("/api/reservations/{id}/check-in", reservation.getId()).with(httpBasic("operator@example.com", password))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void reviewsUseAuthenticatedOwnerAndCompletedReservation() throws Exception {
        String body = "{\"reservaId\":" + reservation.getId() + ",\"calificacion\":5,\"comentario\":\"Excelente\"}";
        mvc.perform(post("/api/reviews").with(httpBasic("ana@example.com", password)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        reservation.setEstado(ReservationStatus.COMPLETED);
        em.flush();
        mvc.perform(post("/api/reviews").with(httpBasic("ana@example.com", password)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.calificacion").value(5));
        mvc.perform(post("/api/reviews").with(httpBasic("ana@example.com", password)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
    }
}
