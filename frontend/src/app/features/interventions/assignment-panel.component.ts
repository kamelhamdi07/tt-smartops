import { DatePipe } from '@angular/common';
import { Component, OnChanges, inject, input, output, signal } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ApiError } from '../../core/models/api.model';
import { Intervention } from '../../core/models/intervention.model';
import { AvailabilitySlot, TechnicianCandidate } from '../../core/models/planning.model';
import { toApiError } from '../../core/services/api-error';
import { InterventionService } from '../../core/services/intervention.service';
import { ErrorAlertComponent } from '../../shared/components/error-alert.component';
import { LabelPipe } from '../../shared/pipes/label.pipe';

interface Row extends TechnicianCandidate { slots: AvailabilitySlot[]; }

/**
 * Aide à la décision pour l'affectation : pour chaque technicien, adéquation à l'intervention,
 * conflits détectés et frise de disponibilité de la journée. Le responsable décide.
 */
@Component({
  selector: 'app-assignment-panel',
  standalone: true,
  imports: [DatePipe, LabelPipe, ErrorAlertComponent],
  template: `
    <section class="card">
      <div class="card-head">
        <div>
          <h2>Affectation des techniciens</h2>
          <span class="muted">Journée du {{ intervention().plannedStart | date: 'EEEE d MMMM' }} · créneau
            {{ intervention().plannedStart | date: 'HH:mm' }}–{{ intervention().plannedEnd | date: 'HH:mm' }}</span>
        </div>
        <div class="legend" aria-hidden="true">
          <span><i class="s-free"></i>Disponible</span><span><i class="s-busy"></i>Occupé</span>
          <span><i class="s-off"></i>Congé / absence</span><span><i class="s-target"></i>Créneau demandé</span>
        </div>
      </div>

      <app-error-alert [error]="error()" />

      @if (loading()) {
        <div class="loading">Analyse des disponibilités…</div>
      } @else if (rows().length === 0) {
        <div class="empty"><strong>Aucun technicien actif</strong>Ajoutez des techniciens dans le module Employés.</div>
      } @else {
        <ul class="candidates">
          @for (r of rows(); track r.technician.id) {
            <li [class.unavailable]="!r.available && !r.alreadyAssigned">
              <div class="who">
                <strong>{{ r.technician.fullName }}</strong>
                <span class="muted">{{ r.technician.specialty | label }}{{ r.technician.teamName ? ' · ' + r.technician.teamName : '' }}</span>
                <span class="tags">
                  @if (r.alreadyAssigned) { <span class="badge signal">Affecté</span> }
                  @else if (r.available) { <span class="badge ok">Disponible</span> }
                  @else { <span class="badge danger">Indisponible</span> }
                  @if (r.sameTeam) { <span class="badge">Même équipe</span> }
                </span>
              </div>

              <div class="timeline" [attr.aria-label]="'Disponibilité de ' + r.technician.fullName">
                @for (s of r.slots; track s.start) {
                  <span [class]="'slot ' + slotClass(s)" [style.left.%]="pos(r.slots, s.start)"
                        [style.width.%]="pos(r.slots, s.end) - pos(r.slots, s.start)"
                        [attr.title]="(s.start | date: 'HH:mm') + '–' + (s.end | date: 'HH:mm') + ' · ' + s.label"></span>
                }
                @if (r.slots.length) {
                  <span class="target" [style.left.%]="pos(r.slots, intervention().plannedStart!)"
                        [style.width.%]="pos(r.slots, intervention().plannedEnd!) - pos(r.slots, intervention().plannedStart!)"></span>
                  <span class="hours"><span>{{ r.slots[0].start | date: 'HH:mm' }}</span><span>{{ r.slots[r.slots.length - 1].end | date: 'HH:mm' }}</span></span>
                }
              </div>

              <div class="act">
                @if (r.alreadyAssigned) {
                  <button class="btn btn-sm btn-danger" (click)="unassign(r)" [disabled]="busy()">Retirer</button>
                } @else {
                  <button class="btn btn-sm btn-primary" (click)="assign(r)" [disabled]="!r.available || busy()">Affecter</button>
                }
              </div>

              @if (!r.alreadyAssigned && r.conflicts.length) {
                <ul class="conflicts">@for (c of r.conflicts; track c.message) { <li>{{ c.message }}</li> }</ul>
              }
            </li>
          }
        </ul>
      }
    </section>
  `,
  styleUrl: './assignment-panel.component.scss',
})
export class AssignmentPanelComponent implements OnChanges {
  readonly intervention = input.required<Intervention>();
  readonly changed = output<Intervention>();

  private readonly service = inject(InterventionService);
  readonly rows = signal<Row[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal<ApiError | null>(null);

  ngOnChanges(): void {
    this.load();
  }

  load(): void {
    const i = this.intervention();
    const day = i.plannedStart!.substring(0, 10);
    this.loading.set(true);
    this.service.candidates(i.id).subscribe({
      next: (candidates) => {
        if (candidates.length === 0) { this.rows.set([]); this.loading.set(false); return; }
        forkJoin(candidates.map((c) => this.service.availability(c.technician.id, day).pipe(catchError(() => of([])))))
          .subscribe((slots) => {
            this.rows.set(candidates.map((c, idx) => ({ ...c, slots: slots[idx] })));
            this.loading.set(false);
          });
      },
      error: (e) => { this.error.set(toApiError(e)); this.loading.set(false); },
    });
  }

  assign(r: Row): void {
    this.run(this.service.assign(this.intervention().id, r.technician.id));
  }

  unassign(r: Row): void {
    if (!confirm(`Retirer ${r.technician.fullName} de cette intervention ?`)) return;
    this.run(this.service.unassign(this.intervention().id, r.technician.id));
  }

  /** Position (%) d'un instant dans la journée de travail affichée. */
  pos(slots: AvailabilitySlot[], at: string): number {
    const start = new Date(slots[0].start).getTime();
    const end = new Date(slots[slots.length - 1].end).getTime();
    const t = Math.min(Math.max(new Date(at).getTime(), start), end);
    return ((t - start) / (end - start)) * 100;
  }

  slotClass(s: AvailabilitySlot): string {
    return { DISPONIBLE: 's-free', OCCUPE: 's-busy', CONGE: 's-off', ABSENT: 's-off' }[s.state];
  }

  private run(obs: ReturnType<InterventionService['assign']>): void {
    this.busy.set(true);
    this.error.set(null);
    obs.subscribe({
      next: (i) => { this.busy.set(false); this.changed.emit(i); },
      error: (e) => { this.error.set(toApiError(e)); this.busy.set(false); },
    });
  }
}
