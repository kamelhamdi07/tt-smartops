import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/models/api.model';
import { Intervention, StatusHistory } from '../../core/models/intervention.model';
import { toApiError } from '../../core/services/api-error';
import { InterventionService } from '../../core/services/intervention.service';
import { ErrorAlertComponent } from '../../shared/components/error-alert.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { LabelPipe } from '../../shared/pipes/label.pipe';
import { AssignmentPanelComponent } from './assignment-panel.component';
import { ReportPanelComponent } from './report-panel.component';

interface Action { key: string; label: string; primary?: boolean; danger?: boolean; needsComment?: string; }

@Component({
  selector: 'app-intervention-detail',
  standalone: true,
  imports: [RouterLink, DatePipe, LabelPipe, StatusBadgeComponent, ErrorAlertComponent,
            AssignmentPanelComponent, ReportPanelComponent],
  templateUrl: './intervention-detail.component.html',
  styleUrl: './intervention-detail.component.scss',
})
export class InterventionDetailComponent implements OnInit {
  readonly id = input.required<string>();

  private readonly service = inject(InterventionService);
  private readonly auth = inject(AuthService);

  readonly item = signal<Intervention | null>(null);
  readonly history = signal<StatusHistory[]>([]);
  readonly error = signal<ApiError | null>(null);
  readonly busy = signal(false);

  readonly isManager = this.auth.hasRole('RESPONSABLE_TECHNIQUE');
  readonly isTechnician = this.auth.hasRole('TECHNICIEN');

  readonly myAssignment = computed(() => {
    const me = this.auth.user()?.employeeId;
    return this.item()?.technicians.find((t) => t.id === me) ?? null;
  });

  readonly canEdit = computed(() =>
    this.isManager && ['NOUVELLE', 'PLANIFIEE', 'AFFECTEE'].includes(this.item()?.status ?? ''));

  readonly showAssignment = computed(() =>
    this.isManager && !!this.item()?.plannedStart && ['PLANIFIEE', 'AFFECTEE'].includes(this.item()!.status));

  readonly showReport = computed(() =>
    ['EN_COURS', 'SUSPENDUE', 'TERMINEE', 'VALIDEE'].includes(this.item()?.status ?? ''));

  /** Actions disponibles selon le rôle et le statut : reflet de la machine à états du backend. */
  readonly actions = computed<Action[]>(() => {
    const i = this.item();
    if (!i) return [];
    const a: Action[] = [];
    if (this.isTechnician && this.myAssignment()) {
      if (!this.myAssignment()!.acknowledgedAt && ['AFFECTEE', 'EN_COURS'].includes(i.status)) {
        a.push({ key: 'acknowledge', label: 'Prendre connaissance' });
      }
      if (i.status === 'AFFECTEE') a.push({ key: 'start', label: 'Démarrer l’intervention', primary: true });
      if (i.status === 'EN_COURS') {
        a.push({ key: 'suspend', label: 'Suspendre', needsComment: 'Raison de la suspension :' });
        a.push({ key: 'complete', label: 'Marquer comme terminée', primary: true });
      }
      if (i.status === 'SUSPENDUE') a.push({ key: 'resume', label: 'Reprendre', primary: true });
    }
    if (this.isManager) {
      if (i.status === 'EN_COURS') a.push({ key: 'suspend', label: 'Suspendre', needsComment: 'Raison de la suspension :' });
      if (i.status === 'SUSPENDUE') a.push({ key: 'resume', label: 'Reprendre' });
      if (['NOUVELLE', 'PLANIFIEE', 'AFFECTEE', 'SUSPENDUE'].includes(i.status)) {
        a.push({ key: 'cancel', label: 'Annuler l’intervention', danger: true, needsComment: 'Motif d’annulation (obligatoire) :' });
      }
    }
    return a;
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.service.get(+this.id()).subscribe({
      next: (i) => this.item.set(i),
      error: (e) => this.error.set(toApiError(e)),
    });
    this.service.history(+this.id()).subscribe({ next: (h) => this.history.set(h), error: () => {} });
  }

  onChanged(i?: Intervention): void {
    if (i) this.item.set(i);
    this.load();
  }

  run(a: Action): void {
    let comment: string | undefined;
    if (a.needsComment) {
      const value = prompt(a.needsComment, '');
      if (value === null) return;
      comment = value;
    }
    this.busy.set(true);
    this.error.set(null);
    this.service.action(+this.id(), a.key, comment).subscribe({
      next: (i) => { this.busy.set(false); this.onChanged(i); },
      error: (e) => { this.error.set(toApiError(e)); this.busy.set(false); },
    });
  }
}
