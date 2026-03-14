package hu.szamlazz.receipt.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.szamlazz.receipt.dto.CreateReceiptRequest;
import hu.szamlazz.receipt.dto.ReceiptItemDto;
import hu.szamlazz.receipt.dto.ReceiptResponse;
import hu.szamlazz.receipt.exception.ReceiptNotFoundException;
import hu.szamlazz.receipt.service.ReceiptService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ReceiptController MockMvc tesztek.
 * Végpont szintű tesztelés: HTTP státuszok, válasz formátumok, validáció.
 */
@WebMvcTest(ReceiptController.class)
class ReceiptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReceiptService receiptService;

    @Test
    @DisplayName("GET /api/receipts – nyugták listázása 200 OK")
    void testListReceipts() throws Exception {
        ReceiptResponse receipt = ReceiptResponse.builder()
                .id(1L)
                .nyugtaszam("NYGTA-2026-001")
                .kelt(LocalDate.of(2026, 3, 1))
                .totalNetto(new BigDecimal("10000"))
                .totalBrutto(new BigDecimal("12700"))
                .build();

        when(receiptService.listReceipts()).thenReturn(List.of(receipt));

        mockMvc.perform(get("/api/receipts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nyugtaszam").value("NYGTA-2026-001"))
                .andExpect(jsonPath("$[0].totalNetto").value(10000));
    }

    @Test
    @DisplayName("GET /api/receipts/{id} – létező nyugta 200 OK")
    void testGetExistingReceipt() throws Exception {
        ReceiptResponse receipt = ReceiptResponse.builder()
                .id(1L)
                .nyugtaszam("NYGTA-2026-001")
                .elotag("NYGTA")
                .fizmod("készpénz")
                .penznem("Ft")
                .kelt(LocalDate.of(2026, 3, 1))
                .totalNetto(new BigDecimal("10000"))
                .totalAfa(new BigDecimal("2700"))
                .totalBrutto(new BigDecimal("12700"))
                .tetelek(List.of())
                .build();

        when(receiptService.getReceipt(1L)).thenReturn(receipt);

        mockMvc.perform(get("/api/receipts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nyugtaszam").value("NYGTA-2026-001"))
                .andExpect(jsonPath("$.elotag").value("NYGTA"));
    }

    @Test
    @DisplayName("GET /api/receipts/{id} – nem létező nyugta 404")
    void testGetNonExistentReceipt() throws Exception {
        when(receiptService.getReceipt(999L)).thenThrow(new ReceiptNotFoundException(999L));

        mockMvc.perform(get("/api/receipts/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/receipts – sikeres létrehozás 201 Created + Location header")
    void testCreateReceiptSuccess() throws Exception {
        CreateReceiptRequest request = CreateReceiptRequest.builder()
                .elotag("NYGTA")
                .fizmod("készpénz")
                .penznem("Ft")
                .tetelek(List.of(
                        ReceiptItemDto.builder()
                                .megnevezes("Teszt termék")
                                .mennyiseg(new BigDecimal("1"))
                                .mennyisegiEgyseg("db")
                                .nettoEgysegar(new BigDecimal("10000"))
                                .afakulcs("27")
                                .netto(new BigDecimal("10000"))
                                .afa(new BigDecimal("2700"))
                                .brutto(new BigDecimal("12700"))
                                .build()
                ))
                .build();

        ReceiptResponse response = ReceiptResponse.builder()
                .id(1L)
                .nyugtaszam("NYGTA-2026-100")
                .elotag("NYGTA")
                .fizmod("készpénz")
                .penznem("Ft")
                .createdAt(LocalDateTime.now())
                .build();

        when(receiptService.createReceipt(any())).thenReturn(response);

        mockMvc.perform(post("/api/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.nyugtaszam").value("NYGTA-2026-100"));
    }

    @Test
    @DisplayName("POST /api/receipts – üres előtag 400 validációs hiba")
    void testCreateReceiptEmptyPrefix() throws Exception {
        CreateReceiptRequest request = CreateReceiptRequest.builder()
                .elotag("")
                .fizmod("készpénz")
                .penznem("Ft")
                .tetelek(List.of(
                        ReceiptItemDto.builder()
                                .megnevezes("Teszt")
                                .mennyiseg(BigDecimal.ONE)
                                .mennyisegiEgyseg("db")
                                .nettoEgysegar(new BigDecimal("1000"))
                                .afakulcs("27")
                                .netto(new BigDecimal("1000"))
                                .afa(new BigDecimal("270"))
                                .brutto(new BigDecimal("1270"))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/receipts – tételek nélkül 400 validációs hiba")
    void testCreateReceiptNoItems() throws Exception {
        CreateReceiptRequest request = CreateReceiptRequest.builder()
                .elotag("NYGTA")
                .fizmod("készpénz")
                .penznem("Ft")
                .tetelek(List.of())
                .build();

        mockMvc.perform(post("/api/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/receipts – érvénytelen pénznem (EUR) 400 validációs hiba")
    void testCreateReceiptInvalidCurrency() throws Exception {
        CreateReceiptRequest request = CreateReceiptRequest.builder()
                .elotag("NYGTA")
                .fizmod("készpénz")
                .penznem("EUR")
                .tetelek(List.of(
                        ReceiptItemDto.builder()
                                .megnevezes("Teszt")
                                .mennyiseg(BigDecimal.ONE)
                                .mennyisegiEgyseg("db")
                                .nettoEgysegar(new BigDecimal("1000"))
                                .afakulcs("27")
                                .netto(new BigDecimal("1000"))
                                .afa(new BigDecimal("270"))
                                .brutto(new BigDecimal("1270"))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/receipts – érvénytelen ÁFA kulcs (AAM) 400 validációs hiba")
    void testCreateReceiptInvalidVatRate() throws Exception {
        CreateReceiptRequest request = CreateReceiptRequest.builder()
                .elotag("NYGTA")
                .fizmod("készpénz")
                .penznem("Ft")
                .tetelek(List.of(
                        ReceiptItemDto.builder()
                                .megnevezes("Teszt")
                                .mennyiseg(BigDecimal.ONE)
                                .mennyisegiEgyseg("db")
                                .nettoEgysegar(new BigDecimal("1000"))
                                .afakulcs("AAM")
                                .netto(new BigDecimal("1000"))
                                .afa(BigDecimal.ZERO)
                                .brutto(new BigDecimal("1000"))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}
