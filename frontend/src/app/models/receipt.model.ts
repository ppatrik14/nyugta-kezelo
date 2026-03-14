/**
 * Nyugta modellek – a backend REST API DTO-jainak TypeScript megfelelői.
 */

/** Nyugta lista és részletek válasz */
export interface Receipt {
  id: number;
  szamlazzId: string;
  nyugtaszam: string;
  hivasAzonosito: string;
  elotag: string;
  fizmod: string;
  penznem: string;
  kelt: string;
  tipus: string;
  stornozott: boolean;
  megjegyzes: string;
  teszt: boolean;
  totalNetto: number;
  totalAfa: number;
  totalBrutto: number;
  createdAt: string;
  tetelek: ReceiptItem[];
}

/** Nyugta tétel */
export interface ReceiptItem {
  megnevezes: string;
  mennyiseg: number;
  mennyisegiEgyseg: string;
  nettoEgysegar: number;
  afakulcs: string;
  netto: number;
  afa: number;
  brutto: number;
}

/** Nyugta létrehozási kérés */
export interface CreateReceiptRequest {
  elotag: string;
  fizmod: string;
  penznem: string;
  megjegyzes?: string;
  tetelek: ReceiptItem[];
}

/** Backend hiba válasz */
export interface ErrorResponse {
  status: number;
  error: string;
  message: string;
}
