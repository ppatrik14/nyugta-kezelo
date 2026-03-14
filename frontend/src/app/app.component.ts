import { Component } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, MatToolbarModule, MatButtonModule, MatIconModule],
  template: `
    <mat-toolbar color="primary" class="app-toolbar">
      <mat-icon class="toolbar-icon">receipt_long</mat-icon>
      <span class="toolbar-title" routerLink="/receipts">Nyugta Kezelő</span>
      <span class="spacer"></span>
      <button mat-button routerLink="/receipts">
        <mat-icon>list</mat-icon>
        Nyugták
      </button>
      <button mat-raised-button routerLink="/receipts/new">
        <mat-icon>add</mat-icon>
        Új nyugta
      </button>
    </mat-toolbar>
    <main class="content">
      <router-outlet />
    </main>
  `,
  styles: [`
    .app-toolbar {
      position: sticky;
      top: 0;
      z-index: 100;
    }
    .toolbar-icon {
      margin-right: 8px;
    }
    .toolbar-title {
      cursor: pointer;
      font-weight: 500;
    }
    .spacer {
      flex: 1 1 auto;
    }
    .content {
      max-width: 1200px;
      margin: 24px auto;
      padding: 0 16px;
    }
    button {
      margin-left: 8px;
    }
  `]
})
export class AppComponent {
  title = 'Nyugta Kezelő';
}
