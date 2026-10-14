import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/models/api.model';
import { Chart, Dashboard } from '../../core/models/misc.model';
import { toApiError } from '../../core/services/api-error';
import { NAVIGATION } from '../../layouts/navigation';
import { DashboardService } from '../../core/services/dashboard.service';
import { ErrorAlertComponent } from '../../shared/components/error-alert.component';
import { LabelPipe } from '../../shared/pipes/label.pipe';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, LabelPipe, ErrorAlertComponent],
  template: `
    <div class="page">
      <div class="page-head">
        <div>
          <h1>Bonjour {{ firstName() }}</h1>
          <p>{{ auth.role() | label }} · situation au {{ today }}</p>
        </div>
      </div>

      @if (quickAdd().length) {
        <section class="card quick">
          <div class="card-head"><h2>Ajout rapide</h2></div>
          <div class="card-body row">
            @for (q of quickAdd(); track q.path) { <a class="btn" [routerLink]="q.path">+ {{ q.label }}</a> }
          </div>
        </section>
      }

      <app-error-alert [error]="error()" />

      @if (data(); as d) {
        <div class="kpis">
          @for (k of d.kpis; track k.key) {
            @if (k.link) {
              <button type="button" [class]="'kpi card tone-' + k.tone" (click)="go(k.link)">
                <span class="kpi-value">{{ k.value }}</span>
                <span class="kpi-label">{{ k.label }}</span>
              </button>
            } @else {
              <div [class]="'kpi card static tone-' + k.tone">
                <span class="kpi-value">{{ k.value }}</span>
                <span class="kpi-label">{{ k.label }}</span>
              </div>
            }
          }
        </div>

        @if (d.charts.length) {
          <div class="grid-2">
            @for (c of d.charts; track c.key) {
              <section class="card">
                <div class="card-head"><h2>{{ c.title }}</h2></div>
                <div class="card-body">
                  @if (c.points.length === 0) {
                    <p class="muted">Pas encore de données.</p>
                  } @else {
                    <ul class="bars">
                      @for (p of c.points; track p.label) {
                        <li>
                          <span class="bar-label">{{ p.label | label }}</span>
                          <span class="bar-track"><span class="bar" [style.width.%]="percent(c, p.value)"></span></span>
                          <span class="bar-value">{{ p.value }}</span>
                        </li>
                      }
                    </ul>
                  }
                </div>
              </section>
            }
          </div>
        }
      } @else if (!error()) {
        <div class="loading">Chargement des indicateurs…</div>
      }
    </div>
  `,
  styles: [`
    .kpis { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 14px; }
    .kpi { display: flex; flex-direction: column; gap: 4px; padding: 18px 20px; color: var(--ink); text-align: left; font: inherit; cursor: pointer;
           border-top: 3px solid var(--line); }
    .kpi:hover { border-color: var(--ink-3); }
    .kpi.static { cursor: default; }
    .kpi-value { font-size: 30px; font-weight: 600; line-height: 1.1; }
    .kpi-label { color: var(--ink-2); }
    .tone-info { border-top-color: var(--blue); }
    .tone-success { border-top-color: var(--ok); }
    .tone-warning { border-top-color: var(--warn); }
    .tone-danger { border-top-color: var(--danger); }
    .tone-danger .kpi-value { color: var(--danger); }
    .bars { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 10px; }
    .bars li { display: grid; grid-template-columns: 150px 1fr 40px; gap: 12px; align-items: center; }
    .bar-label { color: var(--ink-2); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .bar-track { height: 10px; background: #edf1f5; border-radius: 5px; overflow: hidden; }
    .bar { display: block; height: 100%; background: var(--blue); border-radius: 5px; }
    .bar-value { text-align: right; font-weight: 600; }
  `],
})
export class DashboardComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly service = inject(DashboardService);
  private readonly router = inject(Router);

  readonly data = signal<Dashboard | null>(null);
  readonly error = signal<ApiError | null>(null);
  /** Raccourcis vers les pages de saisie de la section Administration accessibles au rôle courant. */
  readonly quickAdd = computed(() => {
    const role = this.auth.role();
    const sections = NAVIGATION.filter((s) => s.title === 'Administration' || s.title === 'Espace client');
    return sections.flatMap((s) => s.items).filter((i) => role !== null && i.roles.includes(role));
  });
  readonly today = new Date().toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' });

  firstName(): string {
    return (this.auth.user()?.fullName ?? '').split(' ')[0];
  }

  ngOnInit(): void {
    this.service.get().subscribe({ next: (d) => this.data.set(d), error: (e) => this.error.set(toApiError(e)) });
  }

  go(link: string): void {
    this.router.navigateByUrl(link);
  }

  percent(chart: Chart, value: number): number {
    const max = Math.max(...chart.points.map((p) => p.value), 1);
    return Math.round((value / max) * 100);
  }
}
