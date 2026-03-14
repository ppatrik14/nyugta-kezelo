package hu.szamlazz.receipt.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

/**
 * Nyugta létrehozási kérés DTO.
 * A frontend ebben a formátumban küldi az adatokat.
 * A backend a kapott értékeket újraszámolja és validálja.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReceiptRequest {

    /** Nyugta előtag – csak nagybetű és szám (pl. NYGTA) */
    @NotBlank(message = "Az előtag kötelező")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Az előtag csak nagybetűt és számot tartalmazhat")
    private String elotag;

    /** Fizetési mód (pl. készpénz, bankkártya, átutalás) */
    @NotBlank(message = "A fizetési mód kötelező")
    private String fizmod;

    /** Pénznem (v1: csak Ft vagy HUF) */
    @NotBlank(message = "A pénznem kötelező")
    @Pattern(regexp = "^(Ft|HUF)$", message = "Jelenleg csak Ft és HUF pénznem támogatott")
    private String penznem;

    /** Megjegyzés (opcionális) */
    private String megjegyzes;

    /** Tételek – legalább 1 kötelező */
    @NotEmpty(message = "Legalább egy tétel kötelező")
    @Valid
    private List<ReceiptItemDto> tetelek;
}
