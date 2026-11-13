package com.edu.unimag.rodatours.entities.enums;

public enum ReservationStatus {

    SCHEDULED,
    CONFIRMED,
    CANCELLED,
    COMPLETED,
    NO_SHOW;

    public boolean canTransitionTo(ReservationStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case SCHEDULED -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == CANCELLED || target == COMPLETED || target == NO_SHOW;
            case CANCELLED, COMPLETED, NO_SHOW -> false;
        };
    }

    public boolean isTerminal() {
        return this == CANCELLED || this == COMPLETED || this == NO_SHOW;
    }
}
