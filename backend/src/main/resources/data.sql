-- Teszt nyugta adatok (csak dev profilban töltődnek be)
-- 2 minta nyugta az acceptance criteria szerint

-- Nyugta 1: Készpénzes vásárlás
INSERT INTO receipt (id, szamlazz_id, nyugtaszam, hivas_azonosito, elotag, fizmod, penznem, kelt, tipus, stornozott, megjegyzes, teszt, total_netto, total_afa, total_brutto, created_at)
VALUES (1001, '100001', 'NYGTA-2026-001', 'seed-001', 'NYGTA', 'készpénz', 'Ft', '2026-03-01', 'NY', false, 'Teszt nyugta - készpénzes', true, 35000.00, 9450.00, 44450.00, '2026-03-01T10:00:00');

INSERT INTO receipt_item (id, receipt_id, megnevezes, mennyiseg, mennyisegi_egyseg, netto_egysegar, afakulcs, netto, afa, brutto)
VALUES (1001, 1001, 'Háztartási mosógép', 1.0, 'db', 25000.00, '27', 25000.00, 6750.00, 31750.00);

INSERT INTO receipt_item (id, receipt_id, megnevezes, mennyiseg, mennyisegi_egyseg, netto_egysegar, afakulcs, netto, afa, brutto)
VALUES (1002, 1001, 'Szállítási díj', 1.0, 'db', 10000.00, '27', 10000.00, 2700.00, 12700.00);

-- Nyugta 2: Bankkártyás vásárlás
INSERT INTO receipt (id, szamlazz_id, nyugtaszam, hivas_azonosito, elotag, fizmod, penznem, kelt, tipus, stornozott, megjegyzes, teszt, total_netto, total_afa, total_brutto, created_at)
VALUES (1002, '100002', 'NYGTA-2026-002', 'seed-002', 'NYGTA', 'bankkártya', 'Ft', '2026-03-05', 'NY', false, 'Teszt nyugta - bankkártyás', true, 15000.00, 4050.00, 19050.00, '2026-03-05T14:30:00');

INSERT INTO receipt_item (id, receipt_id, megnevezes, mennyiseg, mennyisegi_egyseg, netto_egysegar, afakulcs, netto, afa, brutto)
VALUES (1003, 1002, 'Laptop hűtő', 2.0, 'db', 5000.00, '27', 10000.00, 2700.00, 12700.00);

INSERT INTO receipt_item (id, receipt_id, megnevezes, mennyiseg, mennyisegi_egyseg, netto_egysegar, afakulcs, netto, afa, brutto)
VALUES (1004, 1002, 'USB kábel', 5.0, 'db', 1000.00, '27', 5000.00, 1350.00, 6350.00);

-- Nyugta 3: Átutalásos vásárlás 5%-os ÁFA kulccsal
INSERT INTO receipt (id, szamlazz_id, nyugtaszam, hivas_azonosito, elotag, fizmod, penznem, kelt, tipus, stornozott, megjegyzes, teszt, total_netto, total_afa, total_brutto, created_at)
VALUES (1003, '100003', 'NYGTA-2026-003', 'seed-003', 'NYGTA', 'átutalás', 'Ft', '2026-03-10', 'NY', false, 'Teszt nyugta - könyv rendelés', true, 8000.00, 400.00, 8400.00, '2026-03-10T09:15:00');

INSERT INTO receipt_item (id, receipt_id, megnevezes, mennyiseg, mennyisegi_egyseg, netto_egysegar, afakulcs, netto, afa, brutto)
VALUES (1005, 1003, 'Programozási alapismeretek könyv', 2.0, 'db', 4000.00, '5', 8000.00, 400.00, 8400.00);
