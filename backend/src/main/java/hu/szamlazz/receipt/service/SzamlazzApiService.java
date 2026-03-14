package hu.szamlazz.receipt.service;

import hu.szamlazz.receipt.config.SzamlazzConfig;
import hu.szamlazz.receipt.dto.CreateReceiptRequest;
import hu.szamlazz.receipt.exception.SzamlazzApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Számlázz.hu API kommunikációs service.
 * Felelős az XML kérés elküldéséért (multipart/form-data) és a válasz feldolgozásáért.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SzamlazzApiService {

    private final SzamlazzConfig config;
    private final RestTemplate restTemplate;
    private final XmlBuilderService xmlBuilderService;

    /**
     * Nyugta létrehozása a Számlázz.hu API-n keresztül.
     *
     * @param request a nyugta adatai
     * @param hivasAzonosito egyedi hívásazonosító
     * @return a feldolgozott válasz adatok (nyugtaszám, szamlazzId, kelt, stb.)
     * @throws SzamlazzApiException ha az API hívás sikertelen
     */
    public Map<String, Object> createReceipt(CreateReceiptRequest request, String hivasAzonosito) {
        // Agent kulcs ellenőrzése
        if (config.getAgentKey() == null || config.getAgentKey().isBlank()) {
            throw new SzamlazzApiException("A Számlázz.hu Agent kulcs nincs beállítva. " +
                    "Állítsd be a SZAMLAZZ_AGENT_KEY environment változót.");
        }

        // XML összeállítása
        String xml = xmlBuilderService.buildCreateReceiptXml(
                request, config.getAgentKey(), config.isPdfDownload(), hivasAzonosito);
        log.debug("Számlázz.hu kérés XML:\n{}", xml);

        // Multipart/form-data kérés összeállítása
        byte[] xmlBytes = xml.getBytes(StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(xmlBytes) {
            @Override
            public String getFilename() {
                return "nyugta.xml";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("action-szamla_agent_nyugta_create", resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // API hívás
        ResponseEntity<byte[]> response;
        try {
            response = restTemplate.exchange(
                    config.getApiUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    byte[].class
            );
        } catch (RestClientException ex) {
            log.error("Számlázz.hu API hívás sikertelen", ex);
            throw new SzamlazzApiException("A Számlázz.hu szerver nem érhető el. Kérjük, próbáld újra később.", ex);
        }

        // Válasz feldolgozása
        if (response.getBody() == null) {
            throw new SzamlazzApiException("Üres válasz érkezett a Számlázz.hu-tól.");
        }

        return parseResponse(response.getBody());
    }

    /**
     * Nyugta PDF letöltése a Számlázz.hu API-n keresztül.
     *
     * @param nyugtaszam a letöltendő nyugta száma
     * @return a PDF fájl tartalma byte tömbként
     * @throws SzamlazzApiException ha az API hívás sikertelen vagy nem PDF érkezik
     */
    public byte[] downloadReceiptPdf(String nyugtaszam) {
        if (config.getAgentKey() == null || config.getAgentKey().isBlank()) {
            throw new SzamlazzApiException("A Számlázz.hu Agent kulcs nincs beállítva.");
        }

        String xml = xmlBuilderService.buildGetReceiptXml(config.getAgentKey(), nyugtaszam);
        log.debug("Számlázz.hu PDF letöltés kérés XML:\n{}", xml);

        byte[] xmlBytes = xml.getBytes(StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(xmlBytes) {
            @Override
            public String getFilename() {
                return "nyugtaget.xml";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        // Javítás: a dokumentáció szerint 'action-szamla_agent_nyugta_get' kell PDF letöltéshez
        body.add("action-szamla_agent_nyugta_get", resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        
        ResponseEntity<byte[]> response;
        try {
            response = restTemplate.exchange(
                    config.getApiUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    byte[].class
            );
        } catch (RestClientException ex) {
            log.error("Számlázz.hu API hívás (PDF letöltés) sikertelen", ex);
            throw new SzamlazzApiException("A Számlázz.hu szerver nem érhető el.", ex);
        }


        log.info("Számlázz.hu válasz érkezett (PDF letöltés). Status: {}, Content-Length: {}, Content-Type: {}",
                response.getStatusCode(),
                response.getBody() != null ? response.getBody().length : 0,
                response.getHeaders().getContentType());


        if (response.getBody() == null || response.getBody().length == 0) {
            log.error("Üres válasz érkezett a Számlázz.hu-tól (nyugtaszám: {})", nyugtaszam);
            throw new SzamlazzApiException("Üres válasz érkezett a Számlázz.hu-tól.");
        }

        // Ha a válasz XML, akkor valószínűleg hiba történt (a PDF bináris)
        String contentType = response.getHeaders().getContentType() != null ?
                response.getHeaders().getContentType().toString() : "";

        // XML hiba vagy kódolt PDF ellenőrzés
        String bodyStrStart = new String(response.getBody(), 0, Math.min(response.getBody().length, 100), StandardCharsets.UTF_8).trim();
        
        if (bodyStrStart.startsWith("<?xml") || contentType.contains("xml")) {
            log.debug("XML válasz érkezett a Számlázz.hu-tól. Feldolgozás...");
            return parsePdfFromXml(response.getBody());
        }

        return response.getBody();
    }

    /**
     * PDF kinyerése az XML válaszból (ha Base64 kódolva érkezik).
     */
    private byte[] parsePdfFromXml(byte[] xmlBytes) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlBytes));
            doc.getDocumentElement().normalize();

            // Sikeresség ellenőrzése
            String sikeres = getElementText(doc, "sikeres");
            if (!"true".equalsIgnoreCase(sikeres)) {
                String hibakodStr = getElementText(doc, "hibakod");
                String hibauzenet = getElementText(doc, "hibauzenet");
                Integer hibakod = null;
                if (hibakodStr != null && !hibakodStr.isBlank()) {
                    try { hibakod = Integer.parseInt(hibakodStr); } catch (NumberFormatException ignored) {}
                }
                throw new SzamlazzApiException(hibakod, translateError(hibakod, hibauzenet));
            }

            // PDF kinyerése a <nyugtaPdf> tag-ből (Base64 kódolt)
            String base64Pdf = getElementText(doc, "nyugtaPdf");
            if (base64Pdf != null && !base64Pdf.isBlank()) {
                log.debug("Base64 PDF található az XML-ben. Dekódolás...");
                return java.util.Base64.getDecoder().decode(base64Pdf.trim());
            }

            
            log.error("Nem található PDF az XML válaszban.");
            throw new SzamlazzApiException("A Számlázz.hu válasza nem tartalmaz PDF adatot.");
        } catch (SzamlazzApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Hiba a PDF XML-ből való kinyerésekor", e);
            throw new SzamlazzApiException("A PDF kinyerése sikertelen.", e);
        }
    }

    /**
     * A Számlázz.hu XML válasz feldolgozása.
     * Sikertelen válasz esetén SzamlazzApiException-t dob a hibakóddal és üzenettel.
     */
    Map<String, Object> parseResponse(byte[] responseBytes) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // XXE védelem
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(responseBytes));
            doc.getDocumentElement().normalize();

            Map<String, Object> result = new HashMap<>();

            // Sikeresség ellenőrzése
            String sikeres = getElementText(doc, "sikeres");
            if (!"true".equalsIgnoreCase(sikeres)) {
                String hibakodStr = getElementText(doc, "hibakod");
                String hibauzenet = getElementText(doc, "hibauzenet");
                Integer hibakod = null;
                if (hibakodStr != null && !hibakodStr.isBlank()) {
                    try { hibakod = Integer.parseInt(hibakodStr); } catch (NumberFormatException ignored) {}
                }
                String message = translateError(hibakod, hibauzenet);
                throw new SzamlazzApiException(hibakod, message);
            }

            // Alap adatok kinyerése a <nyugta><alap> blokkból
            NodeList alapList = doc.getElementsByTagName("alap");
            if (alapList.getLength() > 0) {
                Element alap = (Element) alapList.item(0);
                result.put("szamlazzId", getChildText(alap, "id"));
                result.put("nyugtaszam", getChildText(alap, "nyugtaszam"));
                result.put("tipus", getChildText(alap, "tipus"));
                result.put("stornozott", "true".equalsIgnoreCase(getChildText(alap, "stornozott")));
                result.put("teszt", "true".equalsIgnoreCase(getChildText(alap, "teszt")));

                String keltStr = getChildText(alap, "kelt");
                if (keltStr != null && !keltStr.isBlank()) {
                    result.put("kelt", LocalDate.parse(keltStr));
                }
            }

            // Összegek kinyerése a <osszegek><totalossz> blokkból
            NodeList totalList = doc.getElementsByTagName("totalossz");
            if (totalList.getLength() > 0) {
                Element total = (Element) totalList.item(0);
                result.put("totalNetto", parseBigDecimal(getChildText(total, "netto")));
                result.put("totalAfa", parseBigDecimal(getChildText(total, "afa")));
                result.put("totalBrutto", parseBigDecimal(getChildText(total, "brutto")));
            }

            // Tételek kinyerése – a válaszban a tételnevek eltérhetnek a kérés neveitől
            NodeList tetelList = doc.getElementsByTagName("tetel");
            List<Map<String, Object>> tetelek = new ArrayList<>();
            for (int i = 0; i < tetelList.getLength(); i++) {
                Element tetel = (Element) tetelList.item(i);
                Map<String, Object> item = new HashMap<>();
                item.put("megnevezes", getChildText(tetel, "megnevezes"));
                item.put("mennyiseg", parseBigDecimal(getChildText(tetel, "mennyiseg")));
                item.put("mennyisegiEgyseg", getChildText(tetel, "mennyisegiEgyseg"));
                item.put("nettoEgysegar", parseBigDecimal(getChildText(tetel, "nettoEgysegar")));
                item.put("afakulcs", getChildText(tetel, "afakulcs"));
                // A válasz mező nevek eltérhetnek (netto vs nettoErtek)
                BigDecimal netto = parseBigDecimal(getChildText(tetel, "netto"));
                if (netto == null) netto = parseBigDecimal(getChildText(tetel, "nettoErtek"));
                item.put("netto", netto);
                BigDecimal afa = parseBigDecimal(getChildText(tetel, "afa"));
                if (afa == null) afa = parseBigDecimal(getChildText(tetel, "afaErtek"));
                item.put("afa", afa);
                BigDecimal brutto = parseBigDecimal(getChildText(tetel, "brutto"));
                if (brutto == null) brutto = parseBigDecimal(getChildText(tetel, "bruttoErtek"));
                item.put("brutto", brutto);
                tetelek.add(item);
            }
            result.put("tetelek", tetelek);

            return result;
        } catch (SzamlazzApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Számlázz.hu válasz feldolgozási hiba", e);
            throw new SzamlazzApiException("A Számlázz.hu válasz feldolgozása sikertelen.", e);
        }
    }

    /**
     * Számlázz.hu hibakódok lefordítása magyar nyelvű hibaüzenetekre.
     */
    private String translateError(Integer hibakod, String originalMessage) {
        if (hibakod == null) {
            return originalMessage != null ? originalMessage : "Ismeretlen hiba a Számlázz.hu-tól.";
        }
        return switch (hibakod) {
            case 336 -> "Ezt az előtagot a számlázás már használja, így nem lehet nyugta előtag.";
            case 337 -> "Az előtag nem megfelelő formátumú: csak nagybetű és szám lehet.";
            case 338 -> "A megadott hívásazonosító már létezik – ez a nyugta már létrejött.";
            case 339 -> "Nincs ilyen nyugtaszám a rendszerben.";
            case 340 -> "A kifizetési összeg eltér a nyugta bruttó összegétől.";
            default -> originalMessage != null ? originalMessage : "Számlázz.hu hiba (kód: " + hibakod + ")";
        };
    }

    private String getElementText(Document doc, String tagName) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return null;
    }

    private String getChildText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return null;
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
