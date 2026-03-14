package hu.szamlazz.receipt.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Nyugta tétel entitás – egy nyugtához tartozó egyedi sor.
 * Pénzösszegek BigDecimal típusúak.
 */
@Entity
@Table(name = "receipt_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** A szülő nyugta */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    @JsonIgnore
    private Receipt receipt;

    /** Tétel megnevezése */
    @Column(nullable = false)
    private String megnevezes;

    /** Mennyiség */
    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal mennyiseg;

    /** Mennyiségi egység (pl. db, kg) */
    @Column(nullable = false)
    private String mennyisegiEgyseg;

    /** Nettó egységár */
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal nettoEgysegar;

    /** ÁFA kulcs (v1: "0", "5", "18", "27") */
    @Column(nullable = false)
    private String afakulcs;

    /** Nettó érték (mennyiseg * nettoEgysegar) */
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal netto;

    /** ÁFA érték */
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal afa;

    /** Bruttó érték (netto + afa) */
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal brutto;
}
