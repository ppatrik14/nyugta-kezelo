package hu.szamlazz.receipt.service;

import hu.szamlazz.receipt.config.SzamlazzConfig;
import hu.szamlazz.receipt.dto.CreateReceiptRequest;
import hu.szamlazz.receipt.dto.ReceiptItemDto;
import hu.szamlazz.receipt.exception.SzamlazzApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * SzamlazzApiService unit tesztek.
 * Az tényleges API hívás mock-olva van, csak a logikát teszteljük.
 */
@ExtendWith(MockitoExtension.class)
class SzamlazzApiServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private XmlBuilderService xmlBuilderService;

    @Mock
    private SzamlazzConfig config;

    @InjectMocks
    private SzamlazzApiService szamlazzApiService;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(config.getAgentKey()).thenReturn("test-agent-key");
        org.mockito.Mockito.lenient().when(config.getApiUrl()).thenReturn("https://www.szamlazz.hu/szamla/");
    }

    @Test
    @DisplayName("Sikeres nyugta létrehozás – XML válasz helyes feldolgozása")
    void testSuccessfulReceiptCreation() {
        String responseXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <xmlnyugtavalasz xmlns="http://www.szamlazz.hu/xmlnyugtavalasz">
                  <sikeres>true</sikeres>
                  <nyugta>
                    <alap>
                      <id>123456</id>
                      <nyugtaszam>NYGTA-2026-100</nyugtaszam>
                      <tipus>NY</tipus>
                      <stornozott>false</stornozott>
                      <kelt>2026-03-13</kelt>
                      <fizmod>készpénz</fizmod>
                      <penznem>Ft</penznem>
                      <teszt>true</teszt>
                    </alap>
                    <tetelek>
                      <tetel>
                        <megnevezes>Teszt termék</megnevezes>
                        <mennyiseg>2.0</mennyiseg>
                        <mennyisegiEgyseg>db</mennyisegiEgyseg>
                        <nettoEgysegar>10000</nettoEgysegar>
                        <netto>20000.0</netto>
                        <afakulcs>27</afakulcs>
                        <afa>5400.0</afa>
                        <brutto>25400.0</brutto>
                      </tetel>
                    </tetelek>
                    <osszegek>
                      <afakulcsossz>
                        <afakulcs>27</afakulcs>
                        <netto>20000</netto>
                        <afa>5400</afa>
                        <brutto>25400</brutto>
                      </afakulcsossz>
                      <totalossz>
                        <netto>20000</netto>
                        <afa>5400</afa>
                        <brutto>25400</brutto>
                      </totalossz>
                    </osszegek>
                  </nyugta>
                </xmlnyugtavalasz>
                """;

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(byte[].class)))
                .thenReturn(new ResponseEntity<>(responseXml.getBytes(), HttpStatus.OK));

        Map<String, Object> result = szamlazzApiService.createReceipt(createTestRequest(), "test-id");

        assertThat(result.get("nyugtaszam")).isEqualTo("NYGTA-2026-100");
        assertThat(result.get("szamlazzId")).isEqualTo("123456");
        assertThat(result.get("tipus")).isEqualTo("NY");
        assertThat(result.get("totalNetto")).isEqualTo(new BigDecimal("20000"));
        assertThat(result.get("totalBrutto")).isEqualTo(new BigDecimal("25400"));
    }

    @Test
    @DisplayName("Sikertelen válasz – hibakód 338 (duplikált hívásazonosító)")
    void testDuplicateCallId() {
        String errorXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <xmlnyugtavalasz xmlns="http://www.szamlazz.hu/xmlnyugtavalasz">
                  <sikeres>false</sikeres>
                  <hibakod>338</hibakod>
                  <hibauzenet>A hívásazonosító már létezik</hibauzenet>
                </xmlnyugtavalasz>
                """;

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(byte[].class)))
                .thenReturn(new ResponseEntity<>(errorXml.getBytes(), HttpStatus.OK));

        assertThatThrownBy(() -> szamlazzApiService.createReceipt(createTestRequest(), "dup-id"))
                .isInstanceOf(SzamlazzApiException.class)
                .satisfies(ex -> {
                    SzamlazzApiException apiEx = (SzamlazzApiException) ex;
                    assertThat(apiEx.getHibakod()).isEqualTo(338);
                    assertThat(apiEx.getMessage()).contains("hívásazonosító");
                });
    }

    @Test
    @DisplayName("Sikertelen válasz – hibakód 337 (előtag formátum hibás)")
    void testInvalidPrefix() {
        String errorXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <xmlnyugtavalasz xmlns="http://www.szamlazz.hu/xmlnyugtavalasz">
                  <sikeres>false</sikeres>
                  <hibakod>337</hibakod>
                  <hibauzenet>Az előtag nem megfelelő formátumú</hibauzenet>
                </xmlnyugtavalasz>
                """;

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(byte[].class)))
                .thenReturn(new ResponseEntity<>(errorXml.getBytes(), HttpStatus.OK));

        assertThatThrownBy(() -> szamlazzApiService.createReceipt(createTestRequest(), "id"))
                .isInstanceOf(SzamlazzApiException.class)
                .satisfies(ex -> {
                    SzamlazzApiException apiEx = (SzamlazzApiException) ex;
                    assertThat(apiEx.getHibakod()).isEqualTo(337);
                });
    }

    @Test
    @DisplayName("API nem elérhető – RestClientException kezelése")
    void testApiUnavailable() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(byte[].class)))
                .thenThrow(new RestClientException("Connection refused"));

        assertThatThrownBy(() -> szamlazzApiService.createReceipt(createTestRequest(), "id"))
                .isInstanceOf(SzamlazzApiException.class)
                .hasMessageContaining("nem érhető el");
    }

    @Test
    @DisplayName("Hiányzó agent kulcs – egyértelmű hibaüzenet")
    void testMissingAgentKey() {
        when(config.getAgentKey()).thenReturn("");

        assertThatThrownBy(() -> szamlazzApiService.createReceipt(createTestRequest(), "id"))
                .isInstanceOf(SzamlazzApiException.class)
                .hasMessageContaining("Agent kulcs nincs beállítva");
    }

    @Test
    @DisplayName("Üres válasz – exception")
    void testEmptyResponse() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(byte[].class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        assertThatThrownBy(() -> szamlazzApiService.createReceipt(createTestRequest(), "id"))
                .isInstanceOf(SzamlazzApiException.class)
                .hasMessageContaining("Üres válasz");
    }

    private CreateReceiptRequest createTestRequest() {
        return CreateReceiptRequest.builder()
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
    }
}
