package hu.szamlazz.receipt.service;

import hu.szamlazz.receipt.dto.CreateReceiptRequest;
import hu.szamlazz.receipt.dto.ReceiptItemDto;
import hu.szamlazz.receipt.dto.ReceiptResponse;
import hu.szamlazz.receipt.entity.Receipt;
import hu.szamlazz.receipt.exception.ReceiptNotFoundException;
import hu.szamlazz.receipt.mapper.ReceiptMapper;
import hu.szamlazz.receipt.repository.ReceiptRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ReceiptService unit tesztek.
 * Az összeg-újraszámolás, listázás és hibakezelés tesztelése.
 */
@ExtendWith(MockitoExtension.class)
class ReceiptServiceTest {

    @Mock
    private ReceiptRepository repository;

    @Mock
    private SzamlazzApiService szamlazzApiService;

    @Mock
    private ReceiptMapper mapper;

    @InjectMocks
    private ReceiptService receiptService;

    @Test
    @DisplayName("Összeg-újraszámolás – netto, afa, brutto helyes számítása 27%-os ÁFA-val")
    void testRecalculateAmounts27Percent() {
        CreateReceiptRequest request = CreateReceiptRequest.builder()
                .elotag("NYGTA")
                .fizmod("készpénz")
                .penznem("Ft")
                .tetelek(List.of(
                        ReceiptItemDto.builder()
                                .megnevezes("Teszt")
                                .mennyiseg(new BigDecimal("2"))
                                .mennyisegiEgyseg("db")
                                .nettoEgysegar(new BigDecimal("10000"))
                                .afakulcs("27")
                                .netto(BigDecimal.ZERO)  // frontend rossz érték
                                .afa(BigDecimal.ZERO)
                                .brutto(BigDecimal.ZERO)
                                .build()
                ))
                .build();

        receiptService.recalculateAmounts(request);

        ReceiptItemDto item = request.getTetelek().get(0);
        assertThat(item.getNetto()).isEqualByComparingTo("20000.00");
        assertThat(item.getAfa()).isEqualByComparingTo("5400.00");
        assertThat(item.getBrutto()).isEqualByComparingTo("25400.00");
    }

    @Test
    @DisplayName("Összeg-újraszámolás – 5%-os ÁFA kulccsal")
    void testRecalculateAmounts5Percent() {
        CreateReceiptRequest request = CreateReceiptRequest.builder()
                .elotag("NYGTA")
                .fizmod("készpénz")
                .penznem("Ft")
                .tetelek(List.of(
                        ReceiptItemDto.builder()
                                .megnevezes("Könyv")
                                .mennyiseg(new BigDecimal("1"))
                                .mennyisegiEgyseg("db")
                                .nettoEgysegar(new BigDecimal("4000"))
                                .afakulcs("5")
                                .netto(BigDecimal.ZERO)
                                .afa(BigDecimal.ZERO)
                                .brutto(BigDecimal.ZERO)
                                .build()
                ))
                .build();

        receiptService.recalculateAmounts(request);

        ReceiptItemDto item = request.getTetelek().get(0);
        assertThat(item.getNetto()).isEqualByComparingTo("4000.00");
        assertThat(item.getAfa()).isEqualByComparingTo("200.00");
        assertThat(item.getBrutto()).isEqualByComparingTo("4200.00");
    }

    @Test
    @DisplayName("Összeg-újraszámolás – 0%-os ÁFA")
    void testRecalculateAmountsZeroVat() {
        CreateReceiptRequest request = CreateReceiptRequest.builder()
                .elotag("NYGTA")
                .fizmod("készpénz")
                .penznem("Ft")
                .tetelek(List.of(
                        ReceiptItemDto.builder()
                                .megnevezes("Mentes termék")
                                .mennyiseg(new BigDecimal("3"))
                                .mennyisegiEgyseg("db")
                                .nettoEgysegar(new BigDecimal("1000"))
                                .afakulcs("0")
                                .netto(BigDecimal.ZERO)
                                .afa(BigDecimal.ZERO)
                                .brutto(BigDecimal.ZERO)
                                .build()
                ))
                .build();

        receiptService.recalculateAmounts(request);

        ReceiptItemDto item = request.getTetelek().get(0);
        assertThat(item.getNetto()).isEqualByComparingTo("3000.00");
        assertThat(item.getAfa()).isEqualByComparingTo("0.00");
        assertThat(item.getBrutto()).isEqualByComparingTo("3000.00");
    }

    @Test
    @DisplayName("Nyugta listázás – üres lista")
    void testListReceiptsEmpty() {
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());
        when(mapper.toResponseList(anyList())).thenReturn(List.of());

        List<ReceiptResponse> result = receiptService.listReceipts();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Nem létező nyugta lekérdezése – ReceiptNotFoundException")
    void testGetNonExistentReceipt() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> receiptService.getReceipt(999L))
                .isInstanceOf(ReceiptNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("Sikeres nyugta létrehozás – mentés a DB-be")
    void testCreateReceiptSuccess() {
        CreateReceiptRequest request = CreateReceiptRequest.builder()
                .elotag("NYGTA")
                .fizmod("készpénz")
                .penznem("Ft")
                .tetelek(List.of(
                        ReceiptItemDto.builder()
                                .megnevezes("Teszt")
                                .mennyiseg(BigDecimal.ONE)
                                .mennyisegiEgyseg("db")
                                .nettoEgysegar(new BigDecimal("10000"))
                                .afakulcs("27")
                                .netto(new BigDecimal("10000"))
                                .afa(new BigDecimal("2700"))
                                .brutto(new BigDecimal("12700"))
                                .build()
                ))
                .build();

        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("szamlazzId", "100001");
        apiResponse.put("nyugtaszam", "NYGTA-2026-100");
        apiResponse.put("tipus", "NY");
        apiResponse.put("stornozott", false);
        apiResponse.put("teszt", true);
        apiResponse.put("kelt", LocalDate.of(2026, 3, 13));
        apiResponse.put("totalNetto", new BigDecimal("10000"));
        apiResponse.put("totalAfa", new BigDecimal("2700"));
        apiResponse.put("totalBrutto", new BigDecimal("12700"));
        apiResponse.put("tetelek", List.of());

        when(szamlazzApiService.createReceipt(any(), anyString())).thenReturn(apiResponse);
        when(repository.save(any(Receipt.class))).thenAnswer(invocation -> {
            Receipt r = invocation.getArgument(0);
            r.setId(1L);
            return r;
        });
        when(mapper.toResponse(any(Receipt.class))).thenReturn(ReceiptResponse.builder()
                .id(1L).nyugtaszam("NYGTA-2026-100").build());

        ReceiptResponse result = receiptService.createReceipt(request);

        assertThat(result.getNyugtaszam()).isEqualTo("NYGTA-2026-100");

        // Ellenőrzés: a repository.save() meghívódott
        ArgumentCaptor<Receipt> captor = ArgumentCaptor.forClass(Receipt.class);
        verify(repository).save(captor.capture());
        Receipt saved = captor.getValue();
        assertThat(saved.getNyugtaszam()).isEqualTo("NYGTA-2026-100");
        assertThat(saved.getTetelek()).hasSize(1);
    }
}
