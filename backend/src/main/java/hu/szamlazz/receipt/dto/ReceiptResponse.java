package hu.szamlazz.receipt.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Nyugta válasz DTO – a REST API által visszaadott részletes nyugta adatok.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptResponse {

    private Long id;
    private String szamlazzId;
    private String nyugtaszam;
    private String hivasAzonosito;
    private String elotag;
    private String fizmod;
    private String penznem;
    private LocalDate kelt;
    private String tipus;
    private boolean stornozott;
    private String megjegyzes;
    private boolean teszt;
    private BigDecimal totalNetto;
    private BigDecimal totalAfa;
    private BigDecimal totalBrutto;
    private LocalDateTime createdAt;
    private List<ReceiptItemDto> tetelek;
}
