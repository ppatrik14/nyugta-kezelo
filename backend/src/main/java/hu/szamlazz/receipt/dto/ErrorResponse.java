package hu.szamlazz.receipt.dto;

import lombok.*;

/**
 * Általános hiba válasz DTO – a GlobalExceptionHandler által visszaadott formátum.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    /** HTTP státusz kód */
    private int status;

    /** Hiba típusa (pl. VALIDATION_ERROR, NOT_FOUND, API_ERROR) */
    private String error;

    /** Ember által olvasható hibaüzenet */
    private String message;
}
