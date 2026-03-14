package hu.szamlazz.receipt.exception;

import hu.szamlazz.receipt.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Globális kivételkezelő – egységes hibaválasz formátum minden végpontra.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Validációs hibák kezelése (@Valid annotációból származó hibák).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Validációs hiba: {}", message);

        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .status(400)
                        .error("VALIDATION_ERROR")
                        .message(message)
                        .build()
        );
    }

    /**
     * Nem található nyugta.
     */
    @ExceptionHandler(ReceiptNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ReceiptNotFoundException ex) {
        log.warn("Nyugta nem található: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse.builder()
                        .status(404)
                        .error("NOT_FOUND")
                        .message(ex.getMessage())
                        .build()
        );
    }

    /**
     * Számlázz.hu API hiba – a hibakód alapján megfelelő HTTP státuszt adunk vissza.
     */
    @ExceptionHandler(SzamlazzApiException.class)
    public ResponseEntity<ErrorResponse> handleSzamlazzApi(SzamlazzApiException ex) {
        log.error("Számlázz.hu API hiba (kód: {}): {}", ex.getHibakod(), ex.getMessage());

        HttpStatus status = mapSzamlazzErrorToHttpStatus(ex.getHibakod());

        return ResponseEntity.status(status).body(
                ErrorResponse.builder()
                        .status(status.value())
                        .error("SZAMLAZZ_API_ERROR")
                        .message(ex.getMessage())
                        .build()
        );
    }

    /**
     * Általános, váratlan hibák kezelése.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Váratlan hiba történt", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse.builder()
                        .status(500)
                        .error("INTERNAL_ERROR")
                        .message("Belső szerverhiba történt. Kérjük, próbáld újra később.")
                        .build()
        );
    }

    /**
     * Számlázz.hu hibakódok leképezése HTTP státuszokra.
     * <ul>
     *   <li>336 – előtag foglalt → 400</li>
     *   <li>337 – előtag formátum → 400</li>
     *   <li>338 – duplikált hívásazonosító → 409</li>
     *   <li>340 – kifizetés összeg eltérés → 400</li>
     *   <li>egyéb / null → 502 (Bad Gateway)</li>
     * </ul>
     */
    private HttpStatus mapSzamlazzErrorToHttpStatus(Integer hibakod) {
        if (hibakod == null) {
            return HttpStatus.BAD_GATEWAY;
        }
        return switch (hibakod) {
            case 336, 337, 340 -> HttpStatus.BAD_REQUEST;
            case 338 -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_GATEWAY;
        };
    }
}
