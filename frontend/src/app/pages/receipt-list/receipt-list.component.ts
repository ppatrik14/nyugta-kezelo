import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ReceiptService } from '../../services/receipt.service';
import { Receipt } from '../../models/receipt.model';

/**
 * Nyugta lista komponens – a landing page.
 * Megjeleníti az összes nyugtát táblázatban: nyugtaszám, keltezés, nettó, bruttó.
 */
@Component({
  selector: 'app-receipt-list',
  standalone: true,
  imports: [
    CommonModule, RouterLink,
    MatTableModule, MatButtonModule, MatIconModule, MatCardModule, MatProgressSpinnerModule
  ],
  template: `
    <div class="page-header">
      <h1>Nyugták</h1>
      <button mat-raised-button color="primary" routerLink="/receipts/new">
        <mat-icon>add</mat-icon>
        Új nyugta
      </button>
    </div>

    <!-- Betöltés állapot -->
    @if (loading) {
      <div class="loading">
        <mat-spinner diameter="40"></mat-spinner>
        <p>Nyugták betöltése...</p>
      </div>
    }

    <!-- Hiba állapot -->
    @if (error) {
      <mat-card class="error-card">
        <mat-card-content>
          <mat-icon color="warn">error</mat-icon>
          <span>{{ error }}</span>
        </mat-card-content>
      </mat-card>
    }

    <!-- Üres lista -->
    @if (!loading && !error && receipts.length === 0) {
      <mat-card class="empty-card">
        <mat-card-content>
          <mat-icon>receipt_long</mat-icon>
          <p>Még nincsenek nyugták az adatbázisban.</p>
          <button mat-raised-button color="primary" routerLink="/receipts/new">
            Első nyugta létrehozása
          </button>
        </mat-card-content>
      </mat-card>
    }

    <!-- Nyugta táblázat -->
    @if (!loading && receipts.length > 0) {
      <mat-card>
        <table mat-table [dataSource]="receipts" class="receipt-table">
          <!-- Nyugtaszám -->
          <ng-container matColumnDef="nyugtaszam">
            <th mat-header-cell *matHeaderCellDef>Nyugtaszám</th>
            <td mat-cell *matCellDef="let r">
              <a [routerLink]="['/receipts', r.id]" class="receipt-link">{{ r.nyugtaszam }}</a>
            </td>
          </ng-container>

          <!-- Keltezés -->
          <ng-container matColumnDef="kelt">
            <th mat-header-cell *matHeaderCellDef>Keltezés</th>
            <td mat-cell *matCellDef="let r">{{ r.kelt }}</td>
          </ng-container>

          <!-- Fizetési mód -->
          <ng-container matColumnDef="fizmod">
            <th mat-header-cell *matHeaderCellDef>Fizetési mód</th>
            <td mat-cell *matCellDef="let r">{{ r.fizmod }}</td>
          </ng-container>

          <!-- Nettó -->
          <ng-container matColumnDef="totalNetto">
            <th mat-header-cell *matHeaderCellDef class="right-align">Nettó</th>
            <td mat-cell *matCellDef="let r" class="right-align">
              {{ r.totalNetto | number:'1.0-2' }} {{ r.penznem }}
            </td>
          </ng-container>

          <!-- Bruttó -->
          <ng-container matColumnDef="totalBrutto">
            <th mat-header-cell *matHeaderCellDef class="right-align">Bruttó</th>
            <td mat-cell *matCellDef="let r" class="right-align bold">
              {{ r.totalBrutto | number:'1.0-2' }} {{ r.penznem }}
            </td>
          </ng-container>

          <!-- Műveletek -->
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef class="center-align">Műveletek</th>
            <td mat-cell *matCellDef="let r" class="center-align">
              <button mat-icon-button color="accent" (click)="downloadReceipt($event, r)" title="PDF letöltése">
                <mat-icon>file_download</mat-icon>
              </button>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"
              class="clickable-row"
              [routerLink]="['/receipts', row.id]"></tr>
        </table>
      </mat-card>
    }
  `,
  styles: [`
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
    }
    .page-header h1 {
      margin: 0;
      font-size: 24px;
      font-weight: 500;
    }
    .loading {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
      padding: 48px;
      color: #666;
    }
    .error-card mat-card-content,
    .empty-card mat-card-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      padding: 48px 24px;
      text-align: center;
    }
    .empty-card mat-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: #999;
    }
    .receipt-table {
      width: 100%;
    }
    .right-align {
      text-align: right;
    }
    .center-align {
      text-align: center;
    }
    .bold {
      font-weight: 600;
    }
    .clickable-row {
      cursor: pointer;
    }
    .clickable-row:hover {
      background-color: rgba(0,0,0,0.04);
    }
    .receipt-link {
      color: #1976d2;
      text-decoration: none;
      font-weight: 500;
    }
    .receipt-link:hover {
      text-decoration: underline;
    }
  `]
})
export class ReceiptListComponent implements OnInit {
  receipts: Receipt[] = [];
  loading = true;
  error = '';
  displayedColumns = ['nyugtaszam', 'kelt', 'fizmod', 'totalNetto', 'totalBrutto', 'actions'];

  constructor(private receiptService: ReceiptService) {}

  ngOnInit(): void {
    this.receiptService.getReceipts().subscribe({
      next: (data) => {
        this.receipts = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Nem sikerült betölteni a nyugtákat. Kérjük, próbáld újra később.';
        this.loading = false;
        console.error('Hiba a nyugták betöltésekor:', err);
      }
    });
  }

  downloadReceipt(event: MouseEvent, receipt: Receipt): void {
    event.stopPropagation(); // Ne navigáljon el a sorra kattintás miatt
    
    this.receiptService.downloadPdf(receipt.id).subscribe({
      next: (blob) => {
        if (blob.type !== 'application/pdf') {
          console.error('A kapott fájl nem PDF:', blob.type);
          alert('Nem sikerült a PDF letöltése. Az API hibaüzenetet küldött.');
          return;
        }

        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `nyugta_${receipt.nyugtaszam}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Hiba a PDF letöltésekor:', err);
        alert('Nem sikerült a PDF letöltése. Kérjük, próbáld újra később.');
      }
    });
  }
}
