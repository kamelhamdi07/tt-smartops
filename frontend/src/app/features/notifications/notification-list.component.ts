import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Page } from '../../core/models/api.model';
import { AppNotification } from '../../core/models/misc.model';
import { NotificationService } from '../../core/services/notification.service';
import { PagerComponent } from '../../shared/components/pager.component';

@Component({
  selector: 'app-notification-list',
  standalone: true,
  imports: [DatePipe, PagerComponent],
  template: `
    <div class="page">
      <div class="page-head">
        <h1>Notifications</h1>
        <button class="btn" (click)="markAll()" [disabled]="service.unread() === 0">Tout marquer comme lu</button>
      </div>
      <section class="card">
        @if (page(); as p) {
          @if (p.content.length === 0) {
            <div class="empty"><strong>Aucune notification</strong>Les affectations, validations et décisions apparaîtront ici.</div>
          } @else {
            <ul class="list">
              @for (n of p.content; track n.id) {
                <li [class.unread]="!n.read">
                  <button class="item" (click)="open(n)">
                    <span class="title">{{ n.title }}</span>
                    <span class="msg">{{ n.message }}</span>
                    <span class="time">{{ n.createdAt | date: 'dd/MM/yyyy HH:mm' }}</span>
                  </button>
                </li>
              }
            </ul>
            <app-pager [page]="p.page" [totalPages]="p.totalPages" [totalElements]="p.totalElements" (pageChange)="load($event)" />
          }
        } @else { <div class="loading">Chargement…</div> }
      </section>
    </div>
  `,
  styles: [`
    .list { list-style: none; margin: 0; padding: 0; }
    .list li { border-bottom: 1px solid var(--line); border-left: 3px solid transparent; }
    .list li.unread { border-left-color: var(--blue); background: #fafcfe; }
    .item { all: unset; display: grid; grid-template-columns: 1fr auto; gap: 2px 16px; padding: 14px 20px; width: 100%; box-sizing: border-box; cursor: pointer; }
    .item:focus-visible { outline: 2px solid var(--blue); }
    .title { font-weight: 600; }
    .msg { grid-column: 1; color: var(--ink-2); }
    .time { grid-column: 2; grid-row: 1; color: var(--ink-3); font-size: 12px; }
  `],
})
export class NotificationListComponent implements OnInit {
  readonly service = inject(NotificationService);
  private readonly router = inject(Router);
  readonly page = signal<Page<AppNotification> | null>(null);

  ngOnInit(): void { this.load(0); }

  load(page: number): void {
    this.service.list(page).subscribe((p) => this.page.set(p));
  }

  open(n: AppNotification): void {
    const go = () => { if (n.link) this.router.navigateByUrl(n.link); };
    if (n.read) { go(); return; }
    this.service.markRead(n.id).subscribe(() => { n.read = true; go(); });
  }

  markAll(): void {
    this.service.markAllRead().subscribe(() => this.load(this.page()?.page ?? 0));
  }
}
