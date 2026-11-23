package com.edu.unimag.rodatours.notifications;

import com.edu.unimag.rodatours.security.AccountRole;
import com.edu.unimag.rodatours.security.StaffAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashSet;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class ReservationNotificationListener {
    private final NotificationRepository notifications;
    private final StaffAccountRepository accounts;

    /** La bandeja y el correo pendiente se guardan en la misma transacción que la reserva. */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onReservationChanged(ReservationChanged event) {
        var recipients = new LinkedHashSet<String>();
        recipients.add(event.touristEmail().toLowerCase(Locale.ROOT));
        accounts.findByAgencyIdAndRoleAndEnabledTrue(event.agencyId(), AccountRole.AGENCY_OPERATOR)
                .forEach(account -> recipients.add(account.getEmail().toLowerCase(Locale.ROOT)));
        for (String recipient : recipients) {
            Notification notification = new Notification();
            notification.setRecipientEmail(recipient);
            notification.setReservationId(event.reservationId());
            notification.setSubject("Actualización de reserva #" + event.reservationId());
            notification.setMessage("La reserva #" + event.reservationId() + " está en estado " + event.status()
                    + ". Consulte su cuenta para ver los detalles.");
            notifications.save(notification);
        }
    }
}
