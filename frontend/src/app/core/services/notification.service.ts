import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Page } from '../models/api.model';
import { AppNotification } from '../models/misc.model';
import { toParams } from './http-params';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  readonly unread = signal(0);

  refreshUnread(): void {
    this.http.get<{ count: number }>('/api/notifications/unread-count')
      .subscribe({ next: (r) => this.unread.set(r.count), error: () => {} });
  }
  list(page = 0): Observable<Page<AppNotification>> {
    return this.http.get<Page<AppNotification>>('/api/notifications', { params: toParams({ page }) });
  }
  markRead(id: number): Observable<void> {
    return this.http.patch<void>(`/api/notifications/${id}/read`, {}).pipe(tap(() => this.refreshUnread()));
  }
  markAllRead(): Observable<void> {
    return this.http.patch<void>('/api/notifications/read-all', {}).pipe(tap(() => this.unread.set(0)));
  }
}
