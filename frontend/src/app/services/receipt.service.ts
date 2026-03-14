import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Receipt, CreateReceiptRequest } from '../models/receipt.model';

/**
 * Nyugta HTTP service – kommunikáció a backend REST API-val.
 */
@Injectable({
  providedIn: 'root'
})
export class ReceiptService {
  private readonly apiUrl = '/api/receipts';

  constructor(private http: HttpClient) {}

  /** Összes nyugta listázása */
  getReceipts(): Observable<Receipt[]> {
    return this.http.get<Receipt[]>(this.apiUrl);
  }

  /** Nyugta részletek lekérdezése ID alapján */
  getReceipt(id: number): Observable<Receipt> {
    return this.http.get<Receipt>(`${this.apiUrl}/${id}`);
  }

  /** Új nyugta létrehozása */
  createReceipt(request: CreateReceiptRequest): Observable<Receipt> {
    return this.http.post<Receipt>(this.apiUrl, request);
  }

  /** Nyugta PDF letöltése blob-ként */
  downloadPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/pdf`, {
      responseType: 'blob'
    });
  }
}
