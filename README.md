# Nyugta Kezelő Alkalmazás

Webalkalmazás nyugták létrehozására, listázására és részleteik megtekintésére a [Számlázz.hu](https://www.szamlazz.hu/) API integrációval.

## Technológiai stack

- **Frontend:** Angular 19 + Angular Material
- **Backend:** Spring Boot 3.4 + H2 (in-memory) + Java 21
- **Indítás:** Docker Compose

## Gyors indítás

### 1. Konfiguráció

Másold le a `.env.example` fájlt `.env` néven és állítsd be az Agent kulcsot:

```bash
cp .env.example .env
```

A demo fiók kulcsa (`97039xbwy2gws4iv7yn4xk8cniuird56tyamat6gy3`) alapból be van állítva.

### 2. Indítás

```bash
docker compose up --build
```

### 3. Elérhető URL-ek

| Szolgáltatás | URL |
|---|---|
| **Frontend** | [http://localhost:4200](http://localhost:4200) |
| **Backend API** | [http://localhost:8080/api/receipts](http://localhost:8080/api/receipts) |
| **Swagger UI** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |
| **H2 Console** | [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:mem:receiptdb`) |

## Számlázz.hu integráció

### Hogyan történik a nyugta létrehozás

1. A felhasználó kitölti az űrlapot a frontenden (előtag, fizetési mód, tételek)
2. A frontend JSON kérést küld a backend `/api/receipts` végpontra
3. A backend:
   - **Újraszámolja** a netto/áfa/bruttó értékeket (a backend az összegek végső forrása)
   - Generál egy **UUID hívásazonosítót** az idempotencia érdekében
   - Összeállítja az **XML kérést** a Számlázz.hu XSD sémája szerint
   - **HTTP POST** kérést küld `multipart/form-data` formátumban a `https://www.szamlazz.hu/szamla/` URL-re
   - A `action-szamla_agent_nyugta_create` form mezőben küldi az XML-t
4. Sikeres válasz esetén a nyugta adatait **elmenti a helyi H2 adatbázisba**
5. Hibás válasz esetén **nem ment** és továbbítja a hibaüzenetet a frontendnek.

### PDF letöltés folyamata

1. A felhasználó a listában vagy a részleteknél a **Letöltés** gombra kattint.
2. A backend összeállít egy `xmlnyugtaget` kérést a nyugtaszámmal.
3. A Számlázz.hu válasza vagy kapásból egy bináris PDF, vagy egy Base64 kódolt PDF-et tartalmazó XML.
4. A backend transzparensen kezeli mindkét esetet, és a frontendnek már a tiszta bináris PDF-et adja át.

### Konfiguráció

| Változó | Leírás | Alapérték |
|---|---|---|
| `SZAMLAZZ_AGENT_KEY` | Számlázz.hu Agent API kulcs | *(kötelező)* |
| `SZAMLAZZ_PDF_DOWNLOAD` | PDF letöltés engedélyezése | `false` |

## Fejlesztés

### Backend tesztek

```bash
cd backend
mvn test
```

### Frontend tesztek

```bash
cd frontend
npm test
```

## Korlátozások (v1)

- Csak **Ft/HUF** pénznem (devizás nyugta out of scope)
- Csak **numerikus ÁFA kulcsok**: 0%, 5%, 18%, 27%
- Nincs sztornó funkció
- H2 in-memory DB (adatok alkalmazás újraindítás után törlődnek)
