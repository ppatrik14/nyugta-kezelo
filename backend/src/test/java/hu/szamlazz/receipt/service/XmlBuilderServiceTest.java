package hu.szamlazz.receipt.service;

import hu.szamlazz.receipt.dto.CreateReceiptRequest;
import hu.szamlazz.receipt.dto.ReceiptItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XmlBuilderService unit tesztek.
 * Ellenőrzi az XML struktúrát, mezősorrendet, escape-elést és BigDecimal formázást.
 */
class XmlBuilderServiceTest {

    private XmlBuilderService xmlBuilderService;

    @BeforeEach
    void setUp() {
        xmlBuilderService = new XmlBuilderService();
    }

    @Test
    @DisplayName("Helyes XML struktúra generálása minimál mezőkkel")
    void testBuildMinimalXml() {
        CreateReceiptRequest request = CreateReceiptRequest.builder()
                .elotag("NYGTA")
                .fizmod("készpénz")
                .penznem("Ft")
                .tetelek(List.of(
                        ReceiptItemDto.builder()
                                .megnevezes("Teszt termék")
                                .mennyiseg(new BigDecimal("1.0"))
                                .mennyisegiEgyseg("db")
                                .nettoEgysegar(new BigDecimal("10000"))
                                .afakulcs("27")
                                .netto(new BigDecimal("10000.00"))
                                .afa(new BigDecimal("2700.00"))
                                .brutto(new BigDecimal("12700.00"))
                                .build()
                ))
                .build();

        String xml = xmlBuilderService.buildCreateReceiptXml(request, "test-key", false, "id-001");

        // Kötelező elemek meglétének ellenőrzése
        assertThat(xml).contains("<szamlaagentkulcs>test-key</szamlaagentkulcs>");
        assertThat(xml).contains("<pdfLetoltes>false</pdfLetoltes>");
        assertThat(xml).contains("<hivasAzonosito>id-001</hivasAzonosito>");
        assertThat(xml).contains("<elotag>NYGTA</elotag>");
        assertThat(xml).contains("<fizmod>készpénz</fizmod>");
        assertThat(xml).contains("<penznem>Ft</penznem>");
        assertThat(xml).contains("<megnevezes>Teszt termék</megnevezes>");
        assertThat(xml).contains("<mennyiseg>1</mennyiseg>");
        assertThat(xml).contains("<nettoEgysegar>10000</nettoEgysegar>");
        assertThat(xml).contains("<afakulcs>27</afakulcs>");
    }

    @Test
    @DisplayName("Mezők sorrendjének ellenőrzése – XSD kötött sorrend")
    void testFieldOrder() {
        CreateReceiptRequest request = createSimpleRequest();
        String xml = xmlBuilderService.buildCreateReceiptXml(request, "key", false, "id-002");

        // beallitasok a fejlec előtt kell legyen
        assertThat(xml.indexOf("<beallitasok>")).isLessThan(xml.indexOf("<fejlec>"));
        // fejlec a tetelek előtt
        assertThat(xml.indexOf("<fejlec>")).isLessThan(xml.indexOf("<tetelek>"));
        // fejlecen belül: hivasAzonosito → elotag → fizmod → penznem
        assertThat(xml.indexOf("<hivasAzonosito>")).isLessThan(xml.indexOf("<elotag>"));
        assertThat(xml.indexOf("<elotag>")).isLessThan(xml.indexOf("<fizmod>"));
        assertThat(xml.indexOf("<fizmod>")).isLessThan(xml.indexOf("<penznem>"));
        // tételen belül: megnevezes → mennyiseg → mennyisegiEgyseg → nettoEgysegar → afakulcs → netto → afa → brutto
        assertThat(xml.indexOf("<megnevezes>")).isLessThan(xml.indexOf("<mennyiseg>"));
        assertThat(xml.indexOf("<mennyisegiEgyseg>")).isLessThan(xml.indexOf("<nettoEgysegar>"));
        assertThat(xml.indexOf("<nettoEgysegar>")).isLessThan(xml.indexOf("<afakulcs>"));
    }

    @Test
    @DisplayName("Speciális karakterek escape-elése az XML-ben")
    void testXmlEscape() {
        CreateReceiptRequest request = CreateReceiptRequest.builder()
                .elotag("NYGTA")
                .fizmod("készpénz")
                .penznem("Ft")
                .megjegyzes("Teszt & <próba> \"idézet\" 'aposztróf'")
                .tetelek(List.of(
                        ReceiptItemDto.builder()
                                .megnevezes("Termék <speciális>")
                                .mennyiseg(BigDecimal.ONE)
                                .mennyisegiEgyseg("db")
                                .nettoEgysegar(new BigDecimal("100"))
                                .afakulcs("27")
                                .netto(new BigDecimal("100"))
                                .afa(new BigDecimal("27"))
                                .brutto(new BigDecimal("127"))
                                .build()
                ))
                .build();

        String xml = xmlBuilderService.buildCreateReceiptXml(request, "key", false, "id-003");

        assertThat(xml).contains("Teszt &amp; &lt;próba&gt; &quot;idézet&quot; &apos;aposztróf&apos;");
        assertThat(xml).contains("Termék &lt;speciális&gt;");
        assertThat(xml).doesNotContain("Teszt & <próba>");
    }

    @Test
    @DisplayName("Több tétel helyes generálása")
    void testMultipleItems() {
        CreateReceiptRequest request = CreateReceiptRequest.builder()
                .elotag("NYGTA")
                .fizmod("készpénz")
                .penznem("Ft")
                .tetelek(List.of(
                        ReceiptItemDto.builder()
                                .megnevezes("Termék A")
                                .mennyiseg(new BigDecimal("2"))
                                .mennyisegiEgyseg("db")
                                .nettoEgysegar(new BigDecimal("5000"))
                                .afakulcs("27")
                                .netto(new BigDecimal("10000"))
                                .afa(new BigDecimal("2700"))
                                .brutto(new BigDecimal("12700"))
                                .build(),
                        ReceiptItemDto.builder()
                                .megnevezes("Termék B")
                                .mennyiseg(new BigDecimal("3"))
                                .mennyisegiEgyseg("kg")
                                .nettoEgysegar(new BigDecimal("1000"))
                                .afakulcs("5")
                                .netto(new BigDecimal("3000"))
                                .afa(new BigDecimal("150"))
                                .brutto(new BigDecimal("3150"))
                                .build()
                ))
                .build();

        String xml = xmlBuilderService.buildCreateReceiptXml(request, "key", false, "id-004");

        // Mindkét tétel megjelenik
        assertThat(xml).contains("Termék A");
        assertThat(xml).contains("Termék B");
        // Két <tetel> blokk van
        assertThat(countOccurrences(xml, "<tetel>")).isEqualTo(2);
        assertThat(countOccurrences(xml, "</tetel>")).isEqualTo(2);
    }

    @Test
    @DisplayName("BigDecimal formázás – felesleges nullák eltávolítása")
    void testBigDecimalFormatting() {
        assertThat(xmlBuilderService.formatDecimal(new BigDecimal("10000.00"))).isEqualTo("10000");
        assertThat(xmlBuilderService.formatDecimal(new BigDecimal("2700.50"))).isEqualTo("2700.5");
        assertThat(xmlBuilderService.formatDecimal(new BigDecimal("0.0001"))).isEqualTo("0.0001");
        assertThat(xmlBuilderService.formatDecimal(null)).isEqualTo("0");
    }

    @Test
    @DisplayName("Megjegyzés nélküli kérés – a megjegyzes elem nem jelenik meg")
    void testNoComment() {
        CreateReceiptRequest request = createSimpleRequest();
        request.setMegjegyzes(null);
        String xml = xmlBuilderService.buildCreateReceiptXml(request, "key", false, "id-005");
        assertThat(xml).doesNotContain("<megjegyzes>");
    }

    @Test
    @DisplayName("PDF letöltés bekapcsolása – true érték az XML-ben")
    void testPdfDownloadTrue() {
        CreateReceiptRequest request = createSimpleRequest();
        String xml = xmlBuilderService.buildCreateReceiptXml(request, "key", true, "id-006");
        assertThat(xml).contains("<pdfLetoltes>true</pdfLetoltes>");
    }

    @Test
    @DisplayName("Nyugta lekérdezési XML generálása")
    void testBuildGetReceiptXml() {
        String xml = xmlBuilderService.buildGetReceiptXml("test-key", "NY-2026-001");

        assertThat(xml).contains("<szamlaagentkulcs>test-key</szamlaagentkulcs>");
        assertThat(xml).contains("<pdfLetoltes>true</pdfLetoltes>");
        assertThat(xml).contains("<nyugtaszam>NY-2026-001</nyugtaszam>");
        assertThat(xml).contains("xmlns=\"http://www.szamlazz.hu/xmlnyugtaget\"");
    }

    // Segédmetódusok

    private CreateReceiptRequest createSimpleRequest() {
        return CreateReceiptRequest.builder()
                .elotag("NYGTA")
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
    }

    private int countOccurrences(String str, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
