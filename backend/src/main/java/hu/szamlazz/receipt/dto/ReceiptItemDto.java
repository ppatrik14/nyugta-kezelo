package hu.szamlazz.receipt.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Nyugta tétel DTO – az űrlapról érkező tételadatok.
 * A netto/afa/brutto értékeket a backend újraszámolja validáció céljából.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptItemDto {

    /** Tétel megnevezése */
    @NotBlank(message = "A megnevezés kötelező")
    private String megnevezes;

    /** Mennyiség – pozitív szám */
    @NotNull(message = "A mennyiség kötelező")
    @DecimalMin(value = "0.0001", message = "A mennyiségnek pozitívnak kell lennie")
    private BigDecimal mennyiseg;

    /** Mennyiségi egység (pl. db, kg) */
    @NotBlank(message = "A mennyiségi egység kötelező")
    private String mennyisegiEgyseg;

    /** Nettó egységár */
    @NotNull(message = "A nettó egységár kötelező")
    @DecimalMin(value = "0", message = "A nettó egységár nem lehet negatív")
    private BigDecimal nettoEgysegar;

    /** ÁFA kulcs – v1: csak numerikus értékek (0, 5, 18, 27) */
    @NotBlank(message = "Az ÁFA kulcs kötelező")
    @Pattern(regexp = "^(0|5|18|27)$", message = "Támogatott ÁFA kulcsok: 0, 5, 18, 27")
    private String afakulcs;

    /** Nettó érték – a frontend által számolt (backend újraszámolja) */
    @NotNull(message = "A nettó érték kötelező")
    @DecimalMin(value = "0", message = "A nettó érték nem lehet negatív")
    private BigDecimal netto;

    /** ÁFA érték – a frontend által számolt (backend újraszámolja) */
    @NotNull(message = "Az ÁFA érték kötelező")
    @DecimalMin(value = "0", message = "Az ÁFA érték nem lehet negatív")
    private BigDecimal afa;

    /** Bruttó érték – a frontend által számolt (backend újraszámolja) */
    @NotNull(message = "A bruttó érték kötelező")
    @DecimalMin(value = "0", message = "A bruttó érték nem lehet negatív")
    private BigDecimal brutto;
}
