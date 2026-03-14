package hu.szamlazz.receipt.controller;

import hu.szamlazz.receipt.dto.CreateReceiptRequest;
import hu.szamlazz.receipt.dto.ReceiptResponse;
import hu.szamlazz.receipt.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Nyugta REST controller – a frontend és külső kliensek számára elérhető végpontok.
 */
@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
@Tag(name = "Nyugták", description = "Nyugta kezelő végpontok")
public class ReceiptController {

    private final ReceiptService receiptService;

    /**
     * Nyugták listázása létrehozás dátuma szerint csökkenő sorrendben.
     */
    @GetMapping
    @Operation(summary = "Nyugták listázása", description = "Visszaadja az összes nyugtát a helyi adatbázisból.")
    public ResponseEntity<List<ReceiptResponse>> listReceipts() {
        return ResponseEntity.ok(receiptService.listReceipts());
    }

    /**
     * Nyugta részletek lekérdezése ID alapján.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Nyugta részletek", description = "Visszaadja egy nyugta összes tárolt adatát.")
    public ResponseEntity<ReceiptResponse> getReceipt(@PathVariable Long id) {
        return ResponseEntity.ok(receiptService.getReceipt(id));
    }

    /**
     * Nyugta PDF letöltése.
     */
    @GetMapping("/{id}/pdf")
    @Operation(summary = "Nyugta PDF letöltés", description = "Letölti a nyugta PDF dokumentumát a Számlázz.hu rendszeréből.")
    public ResponseEntity<Resource> getReceiptPdf(@PathVariable Long id) {
        byte[] pdfContent = receiptService.getReceiptPdf(id);
        ByteArrayResource resource = new ByteArrayResource(pdfContent);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=nyugta_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfContent.length)
                .body(resource);
    }

    /**
     * Új nyugta létrehozása.
     * A Számlázz.hu API-n keresztül hozza létre, majd elmenti a helyi DB-be.
     * Válasz: 201 Created + Location header + a létrehozott nyugta adatai.
     */
    @PostMapping
    @Operation(summary = "Új nyugta létrehozás",
               description = "Létrehoz egy nyugtát a Számlázz.hu-n és elmenti a helyi adatbázisba.")
    public ResponseEntity<ReceiptResponse> createReceipt(@Valid @RequestBody CreateReceiptRequest request) {
        ReceiptResponse created = receiptService.createReceipt(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }
}
