import { Routes } from '@angular/router';
import { ReceiptListComponent } from './pages/receipt-list/receipt-list.component';
import { ReceiptDetailComponent } from './pages/receipt-detail/receipt-detail.component';
import { ReceiptCreateComponent } from './pages/receipt-create/receipt-create.component';

export const routes: Routes = [
  { path: '', redirectTo: 'receipts', pathMatch: 'full' },
  { path: 'receipts', component: ReceiptListComponent },
  { path: 'receipts/new', component: ReceiptCreateComponent },
  { path: 'receipts/:id', component: ReceiptDetailComponent },
];
