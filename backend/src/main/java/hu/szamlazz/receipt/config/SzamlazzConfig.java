package hu.szamlazz.receipt.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Számlázz.hu API konfigurációs beállítások.
 * Az agent kulcs és egyéb paraméterek environment változókból jönnek.
 */
@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "szamlazz")
public class SzamlazzConfig {

    /** Számlázz.hu Agent API kulcs (kötelező a nyugta létrehozáshoz) */
    private String agentKey;

    /** PDF letöltés engedélyezése (alapértelmezett: false) */
    private boolean pdfDownload = false;

    /** Számlázz.hu API alap URL */
    private String apiUrl = "https://www.szamlazz.hu/szamla/";

    /** HTTP kérés timeout milliszekundumban */
    private int timeoutMs = 30000;

    @PostConstruct
    public void validateConfig() {
        if (agentKey == null || agentKey.isBlank()) {
            log.warn("FIGYELMEZTETÉS: A SZAMLAZZ_AGENT_KEY nincs beállítva! " +
                     "Nyugta létrehozás nem fog működni. " +
                     "Állítsd be a SZAMLAZZ_AGENT_KEY environment változót.");
        } else {
            log.info("Számlázz.hu konfiguráció betöltve. API URL: {}, PDF letöltés: {}",
                     apiUrl, pdfDownload);
        }
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
