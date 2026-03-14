import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { ReceiptListComponent } from './receipt-list.component';
import { Receipt } from '../../models/receipt.model';

describe('ReceiptListComponent', () => {
  let component: ReceiptListComponent;
  let fixture: ComponentFixture<ReceiptListComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReceiptListComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideAnimationsAsync()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ReceiptListComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('létre kell jönnie', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne('/api/receipts');
    req.flush([]);
    expect(component).toBeTruthy();
  });

  it('nyugta lista renderelése mock adatokkal', () => {
    const mockReceipts: Receipt[] = [
      {
        id: 1, szamlazzId: '100001', nyugtaszam: 'NYGTA-2026-001',
        hivasAzonosito: 'id-001', elotag: 'NYGTA', fizmod: 'készpénz',
        penznem: 'Ft', kelt: '2026-03-01', tipus: 'NY', stornozott: false,
        megjegyzes: '', teszt: true, totalNetto: 10000, totalAfa: 2700,
        totalBrutto: 12700, createdAt: '2026-03-01T10:00:00', tetelek: []
      },
      {
        id: 2, szamlazzId: '100002', nyugtaszam: 'NYGTA-2026-002',
        hivasAzonosito: 'id-002', elotag: 'NYGTA', fizmod: 'bankkártya',
        penznem: 'Ft', kelt: '2026-03-05', tipus: 'NY', stornozott: false,
        megjegyzes: '', teszt: true, totalNetto: 15000, totalAfa: 4050,
        totalBrutto: 19050, createdAt: '2026-03-05T14:30:00', tetelek: []
      }
    ];

    fixture.detectChanges();
    const req = httpMock.expectOne('/api/receipts');
    req.flush(mockReceipts);
    fixture.detectChanges();

    expect(component.receipts.length).toBe(2);
    expect(component.loading).toBeFalse();
    expect(component.error).toBe('');
  });

  it('üres lista esetén üzenet megjelenítés', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne('/api/receipts');
    req.flush([]);
    fixture.detectChanges();

    expect(component.receipts.length).toBe(0);
    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('Még nincsenek nyugták');
  });

  it('hiba esetén hibaüzenet megjelenítés', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne('/api/receipts');
    req.error(new ProgressEvent('error'));
    fixture.detectChanges();

    expect(component.error).toContain('Nem sikerült');
    expect(component.loading).toBeFalse();
  });
});
