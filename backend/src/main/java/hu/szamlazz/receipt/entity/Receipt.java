package hu.szamlazz.receipt.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Nyugta entitás – a helyi adatbázisban tárolt nyugta alapadatok.
 * A pénzösszegek BigDecimal típusúak a lebegőpontos hibák elkerülése érdekében.
 */
@Entity
@Table(name = "receipt", indexes = {
        @Index(name = "idx_receipt_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Számlázz.hu rendszer belső azonosító */
    private String szamlazzId;

    /** Nyugtaszám (pl. NYGTA-2026-001) */
    @Column(unique = true)
    private String nyugtaszam;

    /** Idempotencia azonosító – egyedi hívásazonosító */
    @Column(unique = true)
    private String hivasAzonosito;

    /** Nyugta előtag */
    @Column(nullable = false)
    private String elotag;

    /** Fizetési mód */
    @Column(nullable = false)
    private String fizmod;

    /** Pénznem (v1: csak Ft/HUF) */
    @Column(nullable = false)
    private String penznem;

    /** Keltezés dátuma */
    private LocalDate kelt;

    /** Nyugta típusa: NY (nyugta) vagy SN (sztornó) */
    @Column(nullable = false)
    @Builder.Default
    private String tipus = "NY";

    /** Sztornózott-e */
    @Builder.Default
    private boolean stornozott = false;

    /** Megjegyzés */
    private String megjegyzes;

    /** Teszt fiókból jött-e */
    @Builder.Default
    private boolean teszt = false;

    /** Teljes nettó összeg */
    @Column(precision = 19, scale = 2)
    private BigDecimal totalNetto;

    /** Teljes ÁFA összeg */
    @Column(precision = 19, scale = 2)
    private BigDecimal totalAfa;

    /** Teljes bruttó összeg */
    @Column(precision = 19, scale = 2)
    private BigDecimal totalBrutto;

    /** Létrehozás időbélyeg */
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Tételek listája */
    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<ReceiptItem> tetelek = new ArrayList<>();

    /**
     * Tétel hozzáadása a nyugtához – beállítja a kétirányú kapcsolatot.
     */
    public void addTetel(ReceiptItem item) {
        tetelek.add(item);
        item.setReceipt(this);
    }
}
