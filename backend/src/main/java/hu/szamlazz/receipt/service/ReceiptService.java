package hu.szamlazz.receipt.service;

import hu.szamlazz.receipt.dto.CreateReceiptRequest;
import hu.szamlazz.receipt.dto.ReceiptItemDto;
import hu.szamlazz.receipt.dto.ReceiptResponse;
import hu.szamlazz.receipt.entity.Receipt;
import hu.szamlazz.receipt.entity.ReceiptItem;
import hu.szamlazz.receipt.exception.ReceiptNotFoundException;
import hu.szamlazz.receipt.mapper.ReceiptMapper;
import hu.szamlazz.receipt.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Nyugta üzleti logika service.
 * Felelős a nyugta létrehozásáért (API hívás + DB mentés),
 * a listázásért és a részletek lekérdezéséért.
 * A backend az összegek végső számítási forrása.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final ReceiptRepository repository;
    private final SzamlazzApiService szamlazzApiService;
    private final ReceiptMapper mapper;

    /**
     * Összes nyugta listázása, létrehozás dátuma szerint csökkenő sorrendben.
     */
    @Transactional(readOnly = true)
    public List<ReceiptResponse> listReceipts() {
        return mapper.toResponseList(repository.findAllByOrderByCreatedAtDesc());
    }

    /**
     * Nyugta részletek lekérdezése ID alapján.
     *
     * @throws ReceiptNotFoundException ha a nyugta nem létezik
     */
    @Transactional(readOnly = true)
    public ReceiptResponse getReceipt(Long id) {
        Receipt receipt = repository.findById(id)
                .orElseThrow(() -> new ReceiptNotFoundException(id));
        return mapper.toResponse(receipt);
    }

    /**
     * Új nyugta létrehozása:
     * 1. Összegek újraszámolása és validálása (backend = igazság forrása)
     * 2. Számlázz.hu API hívás
     * 3. Sikeres válasz esetén mentés a helyi DB-be
     * 4. Hibás válasz esetén NEM mentünk (kivétel repül)
     */
    @Transactional
    public ReceiptResponse createReceipt(CreateReceiptRequest request) {
        // Trimmezés a biztonság kedvéért
        if (request.getElotag() != null) {
            request.setElotag(request.getElotag().trim().toUpperCase());
        }
        if (request.getPenznem() != null) {
            request.setPenznem(request.getPenznem().trim().toUpperCase());
        }

        // 1. Backend összeg-újraszámolás
        recalculateAmounts(request);

        // 2. Hívásazonosító generálás (idempotencia)
        String hivasAzonosito = UUID.randomUUID().toString();
        log.info("Nyugta létrehozás indítása. Előtag: {}, hívásazonosító: {}",
                request.getElotag(), hivasAzonosito);

        // 3. Számlázz.hu API hívás – hiba esetén kivétel, nem mentünk DB-be
        Map<String, Object> apiResponse = szamlazzApiService.createReceipt(request, hivasAzonosito);

        // 4. Sikeres válasz → entitás összeállítása
        Receipt receipt = buildReceiptFromResponse(request, hivasAzonosito, apiResponse);

        // 5. Mentés
        Receipt saved = repository.save(receipt);
        log.info("Nyugta sikeresen létrehozva. ID: {}, nyugtaszám: {}",
                saved.getId(), saved.getNyugtaszam());

        return mapper.toResponse(saved);
    }

    /**
     * A tételek netto/afa/brutto értékeinek újraszámolása.
     * A frontend értékeit felülírjuk a backend által számoltakkal.
     * Ez biztosítja az összegek konzisztenciáját.
     */
    void recalculateAmounts(CreateReceiptRequest request) {
        for (ReceiptItemDto item : request.getTetelek()) {
            BigDecimal mennyiseg = item.getMennyiseg();
            BigDecimal nettoEgysegar = item.getNettoEgysegar();

            // netto = mennyiség × nettó egységár
            BigDecimal netto = mennyiseg.multiply(nettoEgysegar).setScale(2, RoundingMode.HALF_UP);
            item.setNetto(netto);

            // ÁFA számítás numerikus kulcs alapján
            BigDecimal afakulcs = new BigDecimal(item.getAfakulcs());
            BigDecimal afaMultiplier = afakulcs.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal afa = netto.multiply(afaMultiplier).setScale(2, RoundingMode.HALF_UP);
            item.setAfa(afa);

            // brutto = netto + ÁFA
            item.setBrutto(netto.add(afa));
        }
    }

    /**
     * Receipt entitás összeállítása az API válasz és a kérés adataiból.
     */
    /**
     * Nyugta PDF letöltése ID alapján.
     *
     * @param id a nyugta helyi azonosítója
     * @return a PDF fájl tartalma
     * @throws ReceiptNotFoundException ha a nyugta nem létezik
     */
    @Transactional(readOnly = true)
    public byte[] getReceiptPdf(Long id) {
        Receipt receipt = repository.findById(id)
                .orElseThrow(() -> new ReceiptNotFoundException(id));

        log.info("Nyugta PDF letöltés indítása. ID: {}, nyugtaszám: {}", id, receipt.getNyugtaszam());
        return szamlazzApiService.downloadReceiptPdf(receipt.getNyugtaszam());
    }

    private Receipt buildReceiptFromResponse(CreateReceiptRequest request,
                                              String hivasAzonosito,
                                              Map<String, Object> apiResponse) {
        Receipt receipt = Receipt.builder()
                .szamlazzId(getString(apiResponse, "szamlazzId"))
                .nyugtaszam(getString(apiResponse, "nyugtaszam"))
                .hivasAzonosito(hivasAzonosito)
                .elotag(request.getElotag())
                .fizmod(request.getFizmod())
                .penznem(request.getPenznem())
                .kelt(apiResponse.containsKey("kelt") ? (LocalDate) apiResponse.get("kelt") : LocalDate.now())
                .tipus(getString(apiResponse, "tipus") != null ? getString(apiResponse, "tipus") : "NY")
                .stornozott(apiResponse.containsKey("stornozott") && (Boolean) apiResponse.get("stornozott"))
                .megjegyzes(request.getMegjegyzes())
                .teszt(apiResponse.containsKey("teszt") && (Boolean) apiResponse.get("teszt"))
                .createdAt(LocalDateTime.now())
                .build();

        // Összesítő értékek az API válaszból (ha van) vagy újraszámolás
        BigDecimal totalNetto = getBigDecimal(apiResponse, "totalNetto");
        BigDecimal totalAfa = getBigDecimal(apiResponse, "totalAfa");
        BigDecimal totalBrutto = getBigDecimal(apiResponse, "totalBrutto");

        // Ha az API nem adott összesítőt, számoljuk mi
        if (totalNetto == null) {
            totalNetto = BigDecimal.ZERO;
            totalAfa = BigDecimal.ZERO;
            totalBrutto = BigDecimal.ZERO;
            for (ReceiptItemDto item : request.getTetelek()) {
                totalNetto = totalNetto.add(item.getNetto());
                totalAfa = totalAfa.add(item.getAfa());
                totalBrutto = totalBrutto.add(item.getBrutto());
            }
        }
        receipt.setTotalNetto(totalNetto);
        receipt.setTotalAfa(totalAfa);
        receipt.setTotalBrutto(totalBrutto);

        // Tételek hozzáadása
        for (ReceiptItemDto itemDto : request.getTetelek()) {
            ReceiptItem item = ReceiptItem.builder()
                    .megnevezes(itemDto.getMegnevezes())
                    .mennyiseg(itemDto.getMennyiseg())
                    .mennyisegiEgyseg(itemDto.getMennyisegiEgyseg())
                    .nettoEgysegar(itemDto.getNettoEgysegar())
                    .afakulcs(itemDto.getAfakulcs())
                    .netto(itemDto.getNetto())
                    .afa(itemDto.getAfa())
                    .brutto(itemDto.getBrutto())
                    .build();
            receipt.addTetel(item);
        }

        return receipt;
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private BigDecimal getBigDecimal(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof BigDecimal) return (BigDecimal) val;
        return null;
    }
}
