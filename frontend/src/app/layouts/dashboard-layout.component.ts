import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, interval } from 'rxjs';
import { AuthService } from '../core/auth/auth.service';
import { NotificationService } from '../core/services/notification.service';
import { LabelPipe } from '../shared/pipes/label.pipe';
import { NAVIGATION } from './navigation';

@Component({
  selector: 'app-dashboard-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, LabelPipe],
  templateUrl: './dashboard-layout.component.html',
  styleUrl: './dashboard-layout.component.scss',
})
export class DashboardLayoutComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly notifications = inject(NotificationService);

  readonly user = this.auth.user;
  readonly menuOpen = signal(false);

  readonly sections = computed(() => {
    this.auth.user(); // dépendance du computed : la session courante
    return NAVIGATION
      .map((s) => ({ ...s, items: s.items.filter((i) => this.auth.canAccess(i.roles, i.teamLeaders)) }))
      .filter((s) => s.items.length > 0);
  });

  readonly initials = computed(() =>
    (this.user()?.fullName ?? '?').split(' ').map((p) => p[0]).slice(0, 2).join('').toUpperCase());

  constructor() {
    // Les notifications sont créées par les actions des autres utilisateurs :
    // on recharge le compteur à chaque changement de page et toutes les 30 s.
    interval(30_000).pipe(takeUntilDestroyed()).subscribe(() => this.notifications.refreshUnread());
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd), takeUntilDestroyed())
      .subscribe(() => this.notifications.refreshUnread());
  }

  ngOnInit(): void {
    this.notifications.refreshUnread();
    this.router.events.subscribe(() => this.menuOpen.set(false));
  }

  logout(): void {
    this.auth.logout();
  }
}
