package hu.szamlazz.receipt.service;

import hu.szamlazz.receipt.dto.CreateReceiptRequest;
import hu.szamlazz.receipt.dto.ReceiptItemDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;


/**
 * XML építő service – a Számlázz.hu API-nak megfelelő XML-t állít elő.
 * A mezők sorrendje kötött az XSD séma szerint!
 * Az XML escape-elés biztosított a speciális karakterek kezelésére.
 */
@Service
public class XmlBuilderService {

    /**
     * Nyugta létrehozási XML összeállítása a kérés adataiból.
     *
     * @param request  a nyugta adatai
     * @param agentKey Számlázz.hu agent kulcs
     * @param pdfDownload PDF letöltés engedélyezése
     * @param hivasAzonosito egyedi hívásazonosító (idempotencia)
     * @return teljes XML string
     */
    public String buildCreateReceiptXml(CreateReceiptRequest request,
                                         String agentKey,
                                         boolean pdfDownload,
                                         String hivasAzonosito) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<xmlnyugtacreate xmlns=\"http://www.szamlazz.hu/xmlnyugtacreate\" ")
           .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
           .append("xsi:schemaLocation=\"http://www.szamlazz.hu/xmlnyugtacreate ")
           .append("https://www.szamlazz.hu/szamla/docs/xsds/nyugtacreate/xmlnyugtacreate.xsd\">\n");

        // Beállítások blokk – sorrend: felhasznalo, jelszo, szamlaagentkulcs, pdfLetoltes
        xml.append("  <beallitasok>\n");
        xml.append("    <szamlaagentkulcs>").append(escapeXml(agentKey)).append("</szamlaagentkulcs>\n");
        xml.append("    <pdfLetoltes>").append(pdfDownload).append("</pdfLetoltes>\n");
        xml.append("  </beallitasok>\n");

        // Fejléc blokk – sorrend: hivasAzonosito, elotag, fizmod, penznem, ...
        xml.append("  <fejlec>\n");
        xml.append("    <hivasAzonosito>").append(escapeXml(hivasAzonosito)).append("</hivasAzonosito>\n");
        xml.append("    <elotag>").append(escapeXml(request.getElotag())).append("</elotag>\n");
        xml.append("    <fizmod>").append(escapeXml(request.getFizmod())).append("</fizmod>\n");
        xml.append("    <penznem>").append(escapeXml(request.getPenznem())).append("</penznem>\n");
        if (request.getMegjegyzes() != null && !request.getMegjegyzes().isBlank()) {
            xml.append("    <megjegyzes>").append(escapeXml(request.getMegjegyzes())).append("</megjegyzes>\n");
        }
        xml.append("  </fejlec>\n");

        // Tételek blokk – sorrend: megnevezes, mennyiseg, mennyisegiEgyseg, nettoEgysegar, afakulcs, netto, afa, brutto
        xml.append("  <tetelek>\n");
        for (ReceiptItemDto item : request.getTetelek()) {
            xml.append("    <tetel>\n");
            xml.append("      <megnevezes>").append(escapeXml(item.getMegnevezes())).append("</megnevezes>\n");
            xml.append("      <mennyiseg>").append(formatDecimal(item.getMennyiseg())).append("</mennyiseg>\n");
            xml.append("      <mennyisegiEgyseg>").append(escapeXml(item.getMennyisegiEgyseg())).append("</mennyisegiEgyseg>\n");
            xml.append("      <nettoEgysegar>").append(formatDecimal(item.getNettoEgysegar())).append("</nettoEgysegar>\n");
            xml.append("      <afakulcs>").append(escapeXml(item.getAfakulcs())).append("</afakulcs>\n");
            xml.append("      <netto>").append(formatDecimal(item.getNetto())).append("</netto>\n");
            xml.append("      <afa>").append(formatDecimal(item.getAfa())).append("</afa>\n");
            xml.append("      <brutto>").append(formatDecimal(item.getBrutto())).append("</brutto>\n");
            xml.append("    </tetel>\n");
        }
        xml.append("  </tetelek>\n");

        xml.append("</xmlnyugtacreate>");
        return xml.toString();
    }

    /**
     * Nyugta lekérdezési XML összeállítása.
     * Szükséges a PDF letöltéshez.
     *
     * @param agentKey Számlázz.hu agent kulcs
     * @param nyugtaszam A letöltendő nyugta száma
     * @return teljes XML string
     */
    public String buildGetReceiptXml(String agentKey, String nyugtaszam) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<xmlnyugtaget xmlns=\"http://www.szamlazz.hu/xmlnyugtaget\" ")
           .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
           .append("xsi:schemaLocation=\"http://www.szamlazz.hu/xmlnyugtaget ")
           .append("https://www.szamlazz.hu/szamla/docs/xsds/nyugtaget/xmlnyugtaget.xsd\">\n");

        xml.append("  <beallitasok>\n");
        xml.append("    <szamlaagentkulcs>").append(escapeXml(agentKey)).append("</szamlaagentkulcs>\n");
        xml.append("    <pdfLetoltes>true</pdfLetoltes>\n");
        xml.append("  </beallitasok>\n");

        xml.append("  <fejlec>\n");
        xml.append("    <nyugtaszam>").append(escapeXml(nyugtaszam)).append("</nyugtaszam>\n");
        xml.append("  </fejlec>\n");

        xml.append("</xmlnyugtaget>");
        return xml.toString();
    }

    /**
     * XML speciális karakterek escape-elése.
     * Kezeli: &, <, >, ", '
     */
    String escapeXml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * BigDecimal formázása XML-be: pont tizedes elválasztóval, felesleges nullák nélkül.
     */
    String formatDecimal(BigDecimal value) {
        if (value == null) return "0";
        return value.stripTrailingZeros().toPlainString();
    }
}
