import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { ReceiptCreateComponent } from './receipt-create.component';

describe('ReceiptCreateComponent', () => {
  let component: ReceiptCreateComponent;
  let fixture: ComponentFixture<ReceiptCreateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReceiptCreateComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideAnimationsAsync()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ReceiptCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('létre kell jönnie', () => {
    expect(component).toBeTruthy();
  });

  it('az űrlap alapértelmezett értékekkel indul', () => {
    expect(component.form.get('elotag')?.value).toBe('NYGTA');
    expect(component.form.get('fizmod')?.value).toBe('készpénz');
    expect(component.form.get('penznem')?.value).toBe('Ft');
  });

  it('legalább 1 tétel van alapból', () => {
    expect(component.tetelek.length).toBe(1);
  });

  it('tétel hozzáadása növeli a tételek számát', () => {
    component.addItem();
    expect(component.tetelek.length).toBe(2);
  });

  it('tétel törlése csökkenti a tételek számát', () => {
    component.addItem();
    expect(component.tetelek.length).toBe(2);
    component.removeItem(1);
    expect(component.tetelek.length).toBe(1);
  });

  it('az utolsó tétel nem törölhető', () => {
    component.removeItem(0);
    expect(component.tetelek.length).toBe(1);
  });

  it('automatikus összeg számítás helyes', () => {
    const item = component.tetelek.at(0);
    item.patchValue({ mennyiseg: 2, nettoEgysegar: 10000, afakulcs: '27' });
    component.recalculateItem(0);

    expect(item.get('netto')?.value).toBe(20000);
    expect(item.get('afa')?.value).toBe(5400);
    expect(item.get('brutto')?.value).toBe(25400);
  });

  it('automatikus összeg számítás 5%-os ÁFA-val', () => {
    const item = component.tetelek.at(0);
    item.patchValue({ mennyiseg: 1, nettoEgysegar: 4000, afakulcs: '5' });
    component.recalculateItem(0);

    expect(item.get('netto')?.value).toBe(4000);
    expect(item.get('afa')?.value).toBe(200);
    expect(item.get('brutto')?.value).toBe(4200);
  });

  it('a submit gomb letiltva érvénytelen űrlap esetén', () => {
    component.form.patchValue({ elotag: '' });
    fixture.detectChanges();

    expect(component.form.invalid).toBeTrue();
  });

  it('érvénytelen előtag pattern – kisbetűk nem megengedettek', () => {
    component.form.patchValue({ elotag: 'nygta' });
    expect(component.form.get('elotag')?.hasError('pattern')).toBeTrue();
  });

  it('végösszeg számítás több tétellel', () => {
    component.tetelek.at(0).patchValue({
      mennyiseg: 1, nettoEgysegar: 10000, afakulcs: '27'
    });
    component.recalculateItem(0);

    component.addItem();
    component.tetelek.at(1).patchValue({
      mennyiseg: 2, nettoEgysegar: 5000, afakulcs: '27'
    });
    component.recalculateItem(1);

    expect(component.getTotalNetto()).toBe(20000);
    expect(component.getTotalBrutto()).toBe(25400);
  });
});
