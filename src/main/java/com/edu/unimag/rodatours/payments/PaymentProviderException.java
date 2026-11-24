package com.edu.unimag.rodatours.payments;

public class PaymentProviderException extends RuntimeException {
    public PaymentProviderException() { super("No se pudo verificar la operación con Mercado Pago sandbox. Intente de nuevo."); }
}
