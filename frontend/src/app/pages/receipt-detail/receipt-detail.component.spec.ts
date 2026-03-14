import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { ReceiptDetailComponent } from './receipt-detail.component';
import { Receipt } from '../../models/receipt.model';

describe('ReceiptDetailComponent', () => {
  let component: ReceiptDetailComponent;
  let fixture: ComponentFixture<ReceiptDetailComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReceiptDetailComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideAnimationsAsync(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: (key: string) => '1' } }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ReceiptDetailComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('létre kell jönnie', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne('/api/receipts/1');
    req.flush({});
    expect(component).toBeTruthy();
  });

  it('nyugta részletek megjelenítése mock adatokkal', () => {
    const mockReceipt: Receipt = {
      id: 1, szamlazzId: '100001', nyugtaszam: 'NYGTA-2026-001',
      hivasAzonosito: 'id-001', elotag: 'NYGTA', fizmod: 'készpénz',
      penznem: 'Ft', kelt: '2026-03-01', tipus: 'NY', stornozott: false,
      megjegyzes: 'Teszt megjegyzés', teszt: true,
      totalNetto: 10000, totalAfa: 2700, totalBrutto: 12700,
      createdAt: '2026-03-01T10:00:00',
      tetelek: [
        {
          megnevezes: 'Teszt termék', mennyiseg: 1, mennyisegiEgyseg: 'db',
          nettoEgysegar: 10000, afakulcs: '27', netto: 10000, afa: 2700, brutto: 12700
        }
      ]
    };

    fixture.detectChanges();
    const req = httpMock.expectOne('/api/receipts/1');
    req.flush(mockReceipt);
    fixture.detectChanges();

    expect(component.receipt).toBeTruthy();
    expect(component.receipt?.nyugtaszam).toBe('NYGTA-2026-001');
    expect(component.loading).toBeFalse();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('NYGTA-2026-001');
    expect(compiled.textContent).toContain('Teszt megjegyzés');
  });

  it('404 hiba esetén hibaüzenet', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne('/api/receipts/1');
    req.flush({ status: 404 }, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();

    expect(component.error).toContain('nem található');
    expect(component.loading).toBeFalse();
  });
});
