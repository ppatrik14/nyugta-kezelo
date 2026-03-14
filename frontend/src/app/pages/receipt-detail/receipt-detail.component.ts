import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ReceiptService } from '../../services/receipt.service';
import { Receipt } from '../../models/receipt.model';

/**
 * Nyugta részletek komponens – egy nyugta összes tárolt adatát megjeleníti.
 */
@Component({
  selector: 'app-receipt-detail',
  standalone: true,
  imports: [
    CommonModule, RouterLink,
    MatCardModule, MatButtonModule, MatIconModule, MatDividerModule,
    MatTableModule, MatChipsModule, MatProgressSpinnerModule
  ],
  template: `
    <div class="page-header">
      <button mat-button routerLink="/receipts">
        <mat-icon>arrow_back</mat-icon>
        Vissza a listához
      </button>
      @if (receipt) {
        <button mat-raised-button color="accent" (click)="downloadReceipt()">
          <mat-icon>file_download</mat-icon>
          PDF letöltése
        </button>
      }
    </div>

    @if (loading) {
      <div class="loading">
        <mat-spinner diameter="40"></mat-spinner>
      </div>
    }

    @if (error) {
      <mat-card class="error-card">
        <mat-card-content>
          <mat-icon color="warn">error</mat-icon>
          <span>{{ error }}</span>
        </mat-card-content>
      </mat-card>
    }

    @if (receipt) {
      <!-- Alapadatok kártya -->
      <mat-card class="detail-card">
        <mat-card-header>
          <mat-card-title>{{ receipt.nyugtaszam }}</mat-card-title>
          <mat-card-subtitle>Nyugta részletek</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <div class="detail-grid">
            <div class="detail-item">
              <label>Nyugtaszám</label>
              <span>{{ receipt.nyugtaszam }}</span>
            </div>
            <div class="detail-item">
              <label>Számlázz.hu ID</label>
              <span>{{ receipt.szamlazzId || '–' }}</span>
            </div>
            <div class="detail-item">
              <label>Előtag</label>
              <span>{{ receipt.elotag }}</span>
            </div>
            <div class="detail-item">
              <label>Keltezés</label>
              <span>{{ receipt.kelt }}</span>
            </div>
            <div class="detail-item">
              <label>Fizetési mód</label>
              <span>{{ receipt.fizmod }}</span>
            </div>
            <div class="detail-item">
              <label>Pénznem</label>
              <span>{{ receipt.penznem }}</span>
            </div>
            <div class="detail-item">
              <label>Típus</label>
              <mat-chip-set>
                <mat-chip [highlighted]="receipt.tipus === 'NY'">
                  {{ receipt.tipus === 'NY' ? 'Nyugta' : 'Sztornó' }}
                </mat-chip>
              </mat-chip-set>
            </div>
            <div class="detail-item">
              <label>Teszt</label>
              <span>{{ receipt.teszt ? 'Igen' : 'Nem' }}</span>
            </div>
            @if (receipt.megjegyzes) {
              <div class="detail-item full-width">
                <label>Megjegyzés</label>
                <span>{{ receipt.megjegyzes }}</span>
              </div>
            }
            <div class="detail-item">
              <label>Hívásazonosító</label>
              <span class="monospace">{{ receipt.hivasAzonosito }}</span>
            </div>
          </div>
        </mat-card-content>
      </mat-card>

      <!-- Tételek táblázat -->
      <mat-card class="detail-card">
        <mat-card-header>
          <mat-card-title>Tételek</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <table mat-table [dataSource]="receipt.tetelek" class="items-table">
            <ng-container matColumnDef="megnevezes">
              <th mat-header-cell *matHeaderCellDef>Megnevezés</th>
              <td mat-cell *matCellDef="let item">{{ item.megnevezes }}</td>
            </ng-container>
            <ng-container matColumnDef="mennyiseg">
              <th mat-header-cell *matHeaderCellDef class="right-align">Mennyiség</th>
              <td mat-cell *matCellDef="let item" class="right-align">
                {{ item.mennyiseg }} {{ item.mennyisegiEgyseg }}
              </td>
            </ng-container>
            <ng-container matColumnDef="nettoEgysegar">
              <th mat-header-cell *matHeaderCellDef class="right-align">Nettó egységár</th>
              <td mat-cell *matCellDef="let item" class="right-align">
                {{ item.nettoEgysegar | number:'1.0-2' }} {{ receipt.penznem }}
              </td>
            </ng-container>
            <ng-container matColumnDef="afakulcs">
              <th mat-header-cell *matHeaderCellDef class="right-align">ÁFA kulcs</th>
              <td mat-cell *matCellDef="let item" class="right-align">{{ item.afakulcs }}%</td>
            </ng-container>
            <ng-container matColumnDef="netto">
              <th mat-header-cell *matHeaderCellDef class="right-align">Nettó</th>
              <td mat-cell *matCellDef="let item" class="right-align">
                {{ item.netto | number:'1.0-2' }}
              </td>
            </ng-container>
            <ng-container matColumnDef="afa">
              <th mat-header-cell *matHeaderCellDef class="right-align">ÁFA</th>
              <td mat-cell *matCellDef="let item" class="right-align">
                {{ item.afa | number:'1.0-2' }}
              </td>
            </ng-container>
            <ng-container matColumnDef="brutto">
              <th mat-header-cell *matHeaderCellDef class="right-align">Bruttó</th>
              <td mat-cell *matCellDef="let item" class="right-align bold">
                {{ item.brutto | number:'1.0-2' }}
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="itemColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: itemColumns;"></tr>
          </table>
        </mat-card-content>
      </mat-card>

      <!-- Összesítő kártya -->
      <mat-card class="detail-card summary-card">
        <mat-card-content>
          <div class="summary-grid">
            <div class="summary-item">
              <label>Összesen nettó</label>
              <span>{{ receipt.totalNetto | number:'1.0-2' }} {{ receipt.penznem }}</span>
            </div>
            <div class="summary-item">
              <label>Összesen ÁFA</label>
              <span>{{ receipt.totalAfa | number:'1.0-2' }} {{ receipt.penznem }}</span>
            </div>
            <div class="summary-item total">
              <label>Összesen bruttó</label>
              <span>{{ receipt.totalBrutto | number:'1.0-2' }} {{ receipt.penznem }}</span>
            </div>
          </div>
        </mat-card-content>
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
    .loading {
      display: flex;
      justify-content: center;
      padding: 48px;
    }
    .error-card mat-card-content {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 24px;
    }
    .detail-card {
      margin-bottom: 16px;
    }
    .detail-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
      padding-top: 16px;
    }
    .detail-item {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .detail-item label {
      font-size: 12px;
      color: #666;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .detail-item span {
      font-size: 14px;
    }
    .full-width {
      grid-column: 1 / -1;
    }
    .monospace {
      font-family: monospace;
      font-size: 12px !important;
    }
    .items-table {
      width: 100%;
    }
    .right-align {
      text-align: right;
    }
    .bold {
      font-weight: 600;
    }
    .summary-card {
      background-color: #f5f5f5;
    }
    .summary-grid {
      display: flex;
      justify-content: flex-end;
      gap: 32px;
    }
    .summary-item {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 4px;
    }
    .summary-item label {
      font-size: 12px;
      color: #666;
    }
    .summary-item span {
      font-size: 16px;
    }
    .summary-item.total span {
      font-size: 20px;
      font-weight: 700;
      color: #1976d2;
    }
  `]
})
export class ReceiptDetailComponent implements OnInit {
  receipt: Receipt | null = null;
  loading = true;
  error = '';
  itemColumns = ['megnevezes', 'mennyiseg', 'nettoEgysegar', 'afakulcs', 'netto', 'afa', 'brutto'];

  constructor(
    private route: ActivatedRoute,
    private receiptService: ReceiptService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (isNaN(id)) {
      this.error = 'Érvénytelen nyugta azonosító.';
      this.loading = false;
      return;
    }

    this.receiptService.getReceipt(id).subscribe({
      next: (data) => {
        this.receipt = data;
        this.loading = false;
      },
      error: (err) => {
        if (err.status === 404) {
          this.error = 'Nyugta nem található.';
        } else {
          this.error = 'Nem sikerült betölteni a nyugta adatait.';
        }
        this.loading = false;
      }
    });
  }

  downloadReceipt(): void {
    if (!this.receipt) return;
    
    this.receiptService.downloadPdf(this.receipt.id).subscribe({
      next: (blob) => {
        if (blob.type !== 'application/pdf') {
          alert('Nem sikerült a PDF letöltése. Az API hibaüzenetet küldött.');
          return;
        }
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `nyugta_${this.receipt?.nyugtaszam}.pdf`;
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
