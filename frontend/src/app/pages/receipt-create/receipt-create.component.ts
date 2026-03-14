import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ReceiptService } from '../../services/receipt.service';
import { CreateReceiptRequest, ReceiptItem } from '../../models/receipt.model';

/**
 * Új nyugta létrehozás komponens.
 * Angular Material alapú űrlap dinamikus tételsorokkal.
 * A frontend segít a kitöltésnél (automatikus számítás),
 * de a backend az összegek végső forrása.
 */
@Component({
  selector: 'app-receipt-create',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatDividerModule, MatSnackBarModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="page-header">
      <button mat-button routerLink="/receipts">
        <mat-icon>arrow_back</mat-icon>
        Vissza a listához
      </button>
      <h1>Új nyugta</h1>
    </div>

    <form [formGroup]="form" (ngSubmit)="onSubmit()">
      <!-- Fejléc adatok -->
      <mat-card class="form-card">
        <mat-card-header>
          <mat-card-title>Fejléc adatok</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <div class="form-row">
            <mat-form-field appearance="outline">
              <mat-label>Előtag</mat-label>
              <input matInput formControlName="elotag" placeholder="NYGTA" (input)="formatPrefix()" />
              @if (form.get('elotag')?.hasError('required')) {
                <mat-error>Az előtag kötelező</mat-error>
              }
              @if (form.get('elotag')?.hasError('pattern')) {
                <mat-error>Csak nagybetű és szám megengedett</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Fizetési mód</mat-label>
              <mat-select formControlName="fizmod">
                @for (option of paymentMethods; track option) {
                  <mat-option [value]="option">{{ option }}</mat-option>
                }
              </mat-select>
              @if (form.get('fizmod')?.hasError('required')) {
                <mat-error>A fizetési mód kötelező</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Pénznem</mat-label>
              <mat-select formControlName="penznem">
                <mat-option value="HUF">HUF</mat-option>
              </mat-select>
              @if (form.get('penznem')?.hasError('required')) {
                <mat-error>A pénznem kötelező</mat-error>
              }
            </mat-form-field>
          </div>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Megjegyzés (opcionális)</mat-label>
            <textarea matInput formControlName="megjegyzes" rows="2"></textarea>
          </mat-form-field>
        </mat-card-content>
      </mat-card>

      <!-- Tételek -->
      <mat-card class="form-card">
        <mat-card-header>
          <mat-card-title>Tételek</mat-card-title>
        </mat-card-header>
        <mat-card-content formArrayName="tetelek">
          @for (item of tetelek.controls; track item; let i = $index) {
            <div class="item-row" [formGroupName]="i">
              <div class="item-header">
                <span class="item-number">{{ i + 1 }}. tétel</span>
                @if (tetelek.length > 1) {
                  <button mat-icon-button color="warn" type="button" (click)="removeItem(i)"
                          matTooltip="Tétel törlése">
                    <mat-icon>delete</mat-icon>
                  </button>
                }
              </div>

              <div class="form-row">
                <mat-form-field appearance="outline" class="flex-2">
                  <mat-label>Megnevezés</mat-label>
                  <input matInput formControlName="megnevezes" />
                  @if (item.get('megnevezes')?.hasError('required')) {
                    <mat-error>Kötelező</mat-error>
                  }
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>Mennyiség</mat-label>
                  <input matInput type="number" formControlName="mennyiseg"
                         (input)="recalculateItem(i)" />
                  @if (item.get('mennyiseg')?.hasError('required')) {
                    <mat-error>Kötelező</mat-error>
                  }
                  @if (item.get('mennyiseg')?.hasError('min')) {
                    <mat-error>Pozitív szám kell</mat-error>
                  }
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>Egység</mat-label>
                  <input matInput formControlName="mennyisegiEgyseg" placeholder="db" />
                  @if (item.get('mennyisegiEgyseg')?.hasError('required')) {
                    <mat-error>Kötelező</mat-error>
                  }
                </mat-form-field>
              </div>

              <div class="form-row">
                <mat-form-field appearance="outline">
                  <mat-label>Nettó egységár</mat-label>
                  <input matInput type="number" formControlName="nettoEgysegar"
                         (input)="recalculateItem(i)" />
                  @if (item.get('nettoEgysegar')?.hasError('required')) {
                    <mat-error>Kötelező</mat-error>
                  }
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>ÁFA kulcs</mat-label>
                  <mat-select formControlName="afakulcs" (selectionChange)="recalculateItem(i)">
                    @for (vat of vatRates; track vat) {
                      <mat-option [value]="vat.value">{{ vat.label }}</mat-option>
                    }
                  </mat-select>
                  @if (item.get('afakulcs')?.hasError('required')) {
                    <mat-error>Kötelező</mat-error>
                  }
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>Nettó</mat-label>
                  <input matInput type="number" formControlName="netto" readonly />
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>ÁFA</mat-label>
                  <input matInput type="number" formControlName="afa" readonly />
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>Bruttó</mat-label>
                  <input matInput type="number" formControlName="brutto" readonly />
                </mat-form-field>
              </div>

              @if (i < tetelek.length - 1) {
                <mat-divider></mat-divider>
              }
            </div>
          }

          <button mat-stroked-button type="button" (click)="addItem()" class="add-item-btn">
            <mat-icon>add</mat-icon>
            Tétel hozzáadása
          </button>

          <!-- Végösszeg -->
          <div class="totals">
            <mat-divider></mat-divider>
            <div class="totals-grid">
              <div class="total-item">
                <label>Összesen nettó:</label>
                <span>{{ getTotalNetto() | number:'1.0-2' }} {{ form.get('penznem')?.value }}</span>
              </div>
              <div class="total-item">
                <label>Összesen ÁFA:</label>
                <span>{{ getTotalAfa() | number:'1.0-2' }} {{ form.get('penznem')?.value }}</span>
              </div>
              <div class="total-item total-highlight">
                <label>Összesen bruttó:</label>
                <span>{{ getTotalBrutto() | number:'1.0-2' }} {{ form.get('penznem')?.value }}</span>
              </div>
            </div>
          </div>
        </mat-card-content>
      </mat-card>

      <!-- Submit -->
      <div class="form-actions">
        <button mat-button type="button" routerLink="/receipts">Mégse</button>
        <button mat-raised-button color="primary" type="submit"
                [disabled]="form.invalid || submitting">
          @if (submitting) {
            <mat-spinner diameter="20"></mat-spinner>
          } @else {
            <mat-icon>save</mat-icon>
            Nyugta mentése
          }
        </button>
      </div>
    </form>
  `,
  styles: [`
    .page-header {
      margin-bottom: 16px;
    }
    .page-header h1 {
      margin: 8px 0 0 0;
      font-size: 24px;
      font-weight: 500;
    }
    .form-card {
      margin-bottom: 16px;
    }
    .form-row {
      display: flex;
      gap: 16px;
      flex-wrap: wrap;
    }
    .form-row mat-form-field {
      flex: 1;
      min-width: 150px;
    }
    .flex-2 {
      flex: 2 !important;
    }
    .full-width {
      width: 100%;
    }
    .item-row {
      padding: 16px 0;
    }
    .item-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
    }
    .item-number {
      font-weight: 500;
      color: #666;
    }
    .add-item-btn {
      margin-top: 16px;
      width: 100%;
    }
    .totals {
      margin-top: 24px;
    }
    .totals-grid {
      display: flex;
      justify-content: flex-end;
      gap: 32px;
      padding: 16px 0;
    }
    .total-item {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 4px;
    }
    .total-item label {
      font-size: 12px;
      color: #666;
    }
    .total-item span {
      font-size: 16px;
    }
    .total-highlight span {
      font-size: 20px;
      font-weight: 700;
      color: #1976d2;
    }
    .form-actions {
      display: flex;
      justify-content: flex-end;
      gap: 16px;
      margin-top: 16px;
      margin-bottom: 32px;
    }
  `]
})
export class ReceiptCreateComponent {
  submitting = false;

  paymentMethods = ['készpénz', 'bankkártya', 'átutalás'];
  vatRates = [
    { value: '27', label: '27%' },
    { value: '18', label: '18%' },
    { value: '5', label: '5%' },
    { value: '0', label: '0% (mentes)' },
  ];

  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private receiptService: ReceiptService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.form = this.fb.group({
      elotag: ['NYGTA', [Validators.required, Validators.pattern(/^[A-Z0-9]+$/)]],
      fizmod: ['készpénz', Validators.required],
      penznem: ['Ft', Validators.required],
      megjegyzes: [''],
      tetelek: this.fb.array([this.createItemGroup()])
    });
  }

  /**
   * Előtag formázása: nagybetűsítés és szóközök eltávolítása real-time.
   */
  formatPrefix(): void {
    const control = this.form.get('elotag');
    if (control && control.value) {
      const formatted = control.value.toUpperCase().replace(/\s/g, '');
      if (control.value !== formatted) {
        control.patchValue(formatted, { emitEvent: false });
      }
    }
  }

  get tetelek(): FormArray {
    return this.form.get('tetelek') as FormArray;
  }

  createItemGroup(): FormGroup {
    return this.fb.group({
      megnevezes: ['', Validators.required],
      mennyiseg: [1, [Validators.required, Validators.min(0.0001)]],
      mennyisegiEgyseg: ['db', Validators.required],
      nettoEgysegar: [0, [Validators.required, Validators.min(0)]],
      afakulcs: ['27', Validators.required],
      netto: [{ value: 0, disabled: false }],
      afa: [{ value: 0, disabled: false }],
      brutto: [{ value: 0, disabled: false }],
    });
  }

  addItem(): void {
    this.tetelek.push(this.createItemGroup());
  }

  removeItem(index: number): void {
    if (this.tetelek.length > 1) {
      this.tetelek.removeAt(index);
    }
  }

  /**
   * Tétel netto/afa/brutto automatikus újraszámolása.
   * Ez csak segítség a kitöltésnél – a backend az igazság forrása.
   */
  recalculateItem(index: number): void {
    const item = this.tetelek.at(index);
    const mennyiseg = Number(item.get('mennyiseg')?.value) || 0;
    const nettoEgysegar = Number(item.get('nettoEgysegar')?.value) || 0;
    const afakulcsStr = item.get('afakulcs')?.value || '0';
    const afakulcs = Number(afakulcsStr);

    const netto = Math.round(mennyiseg * nettoEgysegar * 100) / 100;
    const afa = Math.round(netto * afakulcs / 100 * 100) / 100;
    const brutto = Math.round((netto + afa) * 100) / 100;

    item.patchValue({ netto, afa, brutto }, { emitEvent: false });
  }

  getTotalNetto(): number {
    return this.tetelek.controls.reduce((sum, item) => sum + (Number(item.get('netto')?.value) || 0), 0);
  }

  getTotalAfa(): number {
    return this.tetelek.controls.reduce((sum, item) => sum + (Number(item.get('afa')?.value) || 0), 0);
  }

  getTotalBrutto(): number {
    return this.tetelek.controls.reduce((sum, item) => sum + (Number(item.get('brutto')?.value) || 0), 0);
  }

  onSubmit(): void {
    if (this.form.invalid || this.submitting) return;

    this.submitting = true;

    const formValue = this.form.getRawValue();
    const request: CreateReceiptRequest = {
      elotag: formValue.elotag,
      fizmod: formValue.fizmod,
      penznem: formValue.penznem,
      megjegyzes: formValue.megjegyzes || undefined,
      tetelek: formValue.tetelek.map((item: any): ReceiptItem => ({
        megnevezes: item.megnevezes,
        mennyiseg: item.mennyiseg,
        mennyisegiEgyseg: item.mennyisegiEgyseg,
        nettoEgysegar: item.nettoEgysegar,
        afakulcs: item.afakulcs,
        netto: item.netto,
        afa: item.afa,
        brutto: item.brutto,
      }))
    };

    this.receiptService.createReceipt(request).subscribe({
      next: (created) => {
        this.snackBar.open('Nyugta sikeresen létrehozva!', 'OK', { duration: 3000 });
        this.router.navigate(['/receipts', created.id]);
      },
      error: (err) => {
        this.submitting = false;
        const message = err.error?.message || 'Hiba történt a nyugta létrehozásakor.';
        this.snackBar.open(message, 'Bezár', { duration: 5000 });
      }
    });
  }
}
