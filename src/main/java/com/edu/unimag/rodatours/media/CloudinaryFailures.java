package com.edu.unimag.rodatours.media;

import org.slf4j.LoggerFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/** Nunca registra cuerpos, credenciales, firmas, URLs ni mensajes del proveedor. */
final class CloudinaryFailures {
    private CloudinaryFailures() {}

    static void configured(CloudinaryProperties properties, String preset) {
        if (!properties.getCloudName().matches("[A-Za-z0-9_-]+")
                || !properties.getApiKey().matches("[0-9]+")
                || properties.getApiSecret().isBlank() || properties.getApiSecret().matches(".*[<>\\s].*")
                || preset.isBlank() || preset.matches(".*[<>\\s].*")) {
            LoggerFactory.getLogger(CloudinaryFailures.class).warn("Cloudinary categoria=CONFIGURACION");
            throw new MediaProviderException("Cloudinary no está configurado correctamente. Revise cloud, API key, API secret y preset firmado en el backend.");
        }
    }

    static MediaProviderException failure(String operation, RuntimeException error) {
        int status = error instanceof RestClientResponseException response ? response.getStatusCode().value() : 0;
        String category = status == 401 || status == 403 ? "AUTENTICACION"
                : status == 404 ? "NO_ENCONTRADO"
                : status == 429 ? "LIMITE"
                : error instanceof ResourceAccessException ? "RED" : "PROVEEDOR";
        LoggerFactory.getLogger(CloudinaryFailures.class).warn(
                "Cloudinary operacion={} categoria={} http={}", operation, category, status);
        String detail = switch (category) {
            case "AUTENTICACION" -> "Cloudinary rechazó las credenciales o la firma. Revise la configuración del backend.";
            case "NO_ENCONTRADO" -> "El archivo aún no existe en Cloudinary. Cárguelo antes de verificarlo.";
            case "LIMITE" -> "Cloudinary alcanzó su límite de solicitudes. Reintente más tarde.";
            case "RED" -> "No se pudo conectar con Cloudinary. Reintente más tarde.";
            default -> "Cloudinary no pudo completar la operación. Revise el diagnóstico del backend.";
        };
        return new MediaProviderException(detail);
    }
}
