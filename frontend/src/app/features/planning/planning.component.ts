import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/models/api.model';
import { PlanningConflict, PlanningEntry } from '../../core/models/planning.model';
import { toApiError } from '../../core/services/api-error';
import { InterventionService } from '../../core/services/intervention.service';
import { ErrorAlertComponent } from '../../shared/components/error-alert.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { addDays, startOfWeek, toIsoDate, toIsoDateTime } from '../../shared/utils/dates';

interface Day { date: Date; iso: string; entries: PlanningEntry[]; }

@Component({
  selector: 'app-planning',
  standalone: true,
  imports: [DatePipe, RouterLink, StatusBadgeComponent, ErrorAlertComponent],
  template: `
    <div class="page">
      <div class="page-head">
        <div>
          <h1>{{ mine ? 'Mon planning' : 'Planning des interventions' }}</h1>
          <p>Semaine du {{ weekStart() | date: 'd MMMM' }} au {{ weekEnd() | date: 'd MMMM y' }}</p>
        </div>
        <div class="row">
          <button class="btn" (click)="shift(-7)">Semaine précédente</button>
          <button class="btn" (click)="today()">Cette semaine</button>
          <button class="btn" (click)="shift(7)">Semaine suivante</button>
        </div>
      </div>

      <app-error-alert [error]="error()" />

      @if (!mine && conflicts().length) {
        <section class="card conflicts">
          <div class="card-head"><h2>{{ conflicts().length }} conflit(s) détecté(s) cette semaine</h2></div>
          <ul>
            @for (c of conflicts(); track $index) {
              <li>
                <span class="badge danger">{{ c.type }}</span> {{ c.message }}
                @for (id of c.interventionIds; track id) { <a [routerLink]="['/interventions', id]">voir</a> }
              </li>
            }
          </ul>
        </section>
      }

      <div class="week">
        @for (d of days(); track d.iso) {
          <section class="day card" [class.is-today]="d.iso === todayIso">
            <header>
              <span class="dow">{{ d.date | date: 'EEEE' }}</span>
              <span class="num">{{ d.date | date: 'd MMM' }}</span>
            </header>
            <div class="entries">
              @for (e of d.entries; track e.interventionId) {
                <a class="entry" [class]="'entry prio-' + e.priority" [routerLink]="['/interventions', e.interventionId]">
                  <span class="time">{{ e.start | date: 'HH:mm' }}–{{ e.end | date: 'HH:mm' }}</span>
                  <strong>{{ e.title }}</strong>
                  <span class="meta">{{ e.location }}</span>
                  @if (e.technicians.length) { <span class="meta">{{ e.technicians.join(', ') }}</span> }
                  @else { <span class="meta unassigned">Non affectée</span> }
                  <app-badge [value]="e.status" />
                </a>
              } @empty {
                <span class="free">Aucune intervention</span>
              }
            </div>
          </section>
        }
      </div>
    </div>
  `,
  styles: [`
    .week { display: grid; grid-template-columns: repeat(7, minmax(150px, 1fr)); gap: 10px; overflow-x: auto; }
    .day { display: flex; flex-direction: column; min-height: 320px; }
    .day header { padding: 10px 12px; border-bottom: 1px solid var(--line); display: flex; flex-direction: column; }
    .day.is-today header { border-bottom: 2px solid var(--blue); }
    .dow { font-weight: 600; text-transform: capitalize; }
    .num { color: var(--ink-3); font-size: 12px; }
    .entries { display: flex; flex-direction: column; gap: 8px; padding: 10px; }
    .entry {
      display: flex; flex-direction: column; gap: 3px; padding: 8px 10px; border-radius: var(--radius);
      background: #f7f9fb; border-left: 3px solid var(--ink-3); color: var(--ink); text-decoration: none; font-size: 13px;
    }
    .entry:hover { background: var(--blue-soft); text-decoration: none; }
    .entry.prio-CRITIQUE { border-left-color: var(--danger); }
    .entry.prio-HAUTE { border-left-color: var(--warn); }
    .entry.prio-NORMALE { border-left-color: var(--blue); }
    .time { font-weight: 600; color: var(--ink-2); }
    .meta { color: var(--ink-2); font-size: 12px; }
    .unassigned { color: var(--warn); }
    .free { color: var(--ink-3); font-size: 12px; padding: 4px 2px; }
    .conflicts { border-color: #efc4c4; }
    .conflicts ul { margin: 0; padding: 12px 20px 16px 36px; display: flex; flex-direction: column; gap: 6px; }
    .conflicts a { margin-left: 6px; }
  `],
})
export class PlanningComponent implements OnInit {
  private readonly service = inject(InterventionService);
  readonly mine = inject(AuthService).hasRole('TECHNICIEN');

  readonly weekStart = signal(startOfWeek(new Date()));
  readonly weekEnd = computed(() => addDays(this.weekStart(), 6));
  readonly entries = signal<PlanningEntry[]>([]);
  readonly conflicts = signal<PlanningConflict[]>([]);
  readonly error = signal<ApiError | null>(null);
  readonly todayIso = toIsoDate(new Date());

  readonly days = computed<Day[]>(() => {
    const start = this.weekStart();
    return Array.from({ length: 7 }, (_, n) => {
      const date = addDays(start, n);
      const iso = toIsoDate(date);
      return { date, iso, entries: this.entries().filter((e) => e.start.substring(0, 10) === iso) };
    });
  });

  ngOnInit(): void { this.load(); }

  shift(days: number): void {
    this.weekStart.set(addDays(this.weekStart(), days));
    this.load();
  }

  today(): void {
    this.weekStart.set(startOfWeek(new Date()));
    this.load();
  }

  private load(): void {
    const from = toIsoDateTime(this.weekStart());
    const to = toIsoDateTime(addDays(this.weekStart(), 7));
    this.error.set(null);
    this.service.planning(from, to, this.mine).subscribe({
      next: (e) => this.entries.set(e),
      error: (err) => this.error.set(toApiError(err)),
    });
    if (!this.mine) {
      this.service.conflicts(from, to).subscribe({ next: (c) => this.conflicts.set(c), error: () => this.conflicts.set([]) });
    }
  }
}
