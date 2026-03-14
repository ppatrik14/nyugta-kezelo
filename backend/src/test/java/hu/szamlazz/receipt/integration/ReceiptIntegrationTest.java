package hu.szamlazz.receipt.integration;

import hu.szamlazz.receipt.entity.Receipt;
import hu.szamlazz.receipt.entity.ReceiptItem;
import hu.szamlazz.receipt.repository.ReceiptRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integrációs tesztek – teljes Spring kontextussal és H2 adatbázissal.
 * A dev profil seed adatokat tölt, ezért azon teszteljük a listázást és részleteket.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ReceiptIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReceiptRepository repository;

    @Test
    @DisplayName("Teszt adatok betöltődése – legalább 2 nyugta az adatbázisban")
    void testSeedDataLoaded() {
        List<Receipt> receipts = repository.findAll();
        assertThat(receipts).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("GET /api/receipts – seed adatok listázása")
    void testListSeedReceipts() throws Exception {
        mockMvc.perform(get("/api/receipts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("GET /api/receipts/1001 – seed nyugta részletek")
    void testGetSeedReceiptDetail() throws Exception {
        mockMvc.perform(get("/api/receipts/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nyugtaszam").value("NYGTA-2026-001"))
                .andExpect(jsonPath("$.tetelek.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Repository – CRUD műveletek BigDecimal értékekkel")
    void testRepositoryCrud() {
        Receipt receipt = Receipt.builder()
                .szamlazzId("999999")
                .nyugtaszam("TEST-2026-999")
                .hivasAzonosito("integration-test-001")
                .elotag("TEST")
                .fizmod("készpénz")
                .penznem("Ft")
                .kelt(LocalDate.now())
                .tipus("NY")
                .stornozott(false)
                .teszt(true)
                .totalNetto(new BigDecimal("10000.00"))
                .totalAfa(new BigDecimal("2700.00"))
                .totalBrutto(new BigDecimal("12700.00"))
                .createdAt(LocalDateTime.now())
                .build();

        ReceiptItem item = ReceiptItem.builder()
                .megnevezes("Integrációs teszt termék")
                .mennyiseg(new BigDecimal("1.0"))
                .mennyisegiEgyseg("db")
                .nettoEgysegar(new BigDecimal("10000.00"))
                .afakulcs("27")
                .netto(new BigDecimal("10000.00"))
                .afa(new BigDecimal("2700.00"))
                .brutto(new BigDecimal("12700.00"))
                .build();
        receipt.addTetel(item);

        Receipt saved = repository.save(receipt);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTotalNetto()).isEqualByComparingTo("10000.00");
        assertThat(saved.getTetelek()).hasSize(1);
        assertThat(saved.getTetelek().get(0).getMegnevezes()).isEqualTo("Integrációs teszt termék");
    }
}
