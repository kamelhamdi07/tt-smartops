import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiError } from '../../core/models/api.model';
import { Specialty, Team } from '../../core/models/employee.model';
import { InterventionRequest, InterventionType, Priority } from '../../core/models/intervention.model';
import { AvailableResources } from '../../core/models/planning.model';
import { toApiError } from '../../core/services/api-error';
import { EmployeeService } from '../../core/services/employee.service';
import { InterventionService } from '../../core/services/intervention.service';
import { ErrorAlertComponent } from '../../shared/components/error-alert.component';
import { fromInputDateTime, toInputDateTime } from '../../shared/utils/dates';
import { INTERVENTION_TYPES, PRIORITIES, SPECIALTIES, options } from '../../shared/utils/labels';

@Component({
  selector: 'app-intervention-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, ErrorAlertComponent],
  template: `
    <div class="page">
      <div class="page-head">
        <h1>{{ id() ? 'Modifier l’intervention' : 'Nouvelle intervention' }}</h1>
        <a class="btn" [routerLink]="id() ? ['/interventions', id()] : ['/interventions']">Retour</a>
      </div>

      <form [formGroup]="form" (ngSubmit)="save()" novalidate class="page">
        <section class="card">
          <div class="card-head"><h2>Description</h2></div>
          <div class="card-body form-grid">
            <div class="field full"><label for="title">Titre</label><input id="title" class="input" formControlName="title" maxlength="200" /></div>
            <div class="field"><label for="type">Type</label>
              <select id="type" class="input" formControlName="type">
                @for (t of types; track t.value) { <option [value]="t.value">{{ t.label }}</option> }
              </select></div>
            <div class="field"><label for="priority">Priorité</label>
              <select id="priority" class="input" formControlName="priority">
                @for (p of priorities; track p.value) { <option [value]="p.value">{{ p.label }}</option> }
              </select></div>
            <div class="field"><label for="spec">Spécialité requise</label>
              <select id="spec" class="input" formControlName="requiredSpecialty">
                <option [ngValue]="null">Aucune exigence</option>
                @for (s of specialties; track s.value) { <option [ngValue]="s.value">{{ s.label }}</option> }
              </select></div>
            <div class="field full"><label for="loc">Lieu / site technique</label><input id="loc" class="input" formControlName="location" /></div>
            <div class="field full"><label for="desc">Description</label><textarea id="desc" class="input" formControlName="description" rows="4"></textarea></div>
          </div>
        </section>

        <section class="card">
          <div class="card-head"><h2>Planification et ressources</h2><span class="muted">Facultatif à la création</span></div>
          <div class="card-body form-grid">
            <div class="field"><label for="start">Début prévu</label>
              <input id="start" type="datetime-local" class="input" formControlName="plannedStart" (change)="refreshResources()" /></div>
            <div class="field"><label for="end">Fin prévue</label>
              <input id="end" type="datetime-local" class="input" formControlName="plannedEnd" (change)="refreshResources()" /></div>
            <div class="field"><label for="due">Échéance</label><input id="due" type="date" class="input" formControlName="dueDate" /></div>
            <div class="field"><label for="team">Équipe</label>
              <select id="team" class="input" formControlName="teamId">
                <option [ngValue]="null">Non définie</option>
                @for (t of teams(); track t.id) { <option [ngValue]="t.id">{{ t.name }}</option> }
              </select></div>
            <div class="field"><label for="eq">Équipement</label>
              <select id="eq" class="input" formControlName="equipmentId">
                <option [ngValue]="null">{{ resources() ? 'Aucun' : 'Choisissez d’abord le créneau' }}</option>
                @for (r of resources()?.equipment ?? []; track r.id) { <option [ngValue]="r.id">{{ r.label }}</option> }
              </select></div>
            <div class="field"><label for="veh">Véhicule</label>
              <select id="veh" class="input" formControlName="vehicleId">
                <option [ngValue]="null">{{ resources() ? 'Aucun' : 'Choisissez d’abord le créneau' }}</option>
                @for (r of resources()?.vehicles ?? []; track r.id) { <option [ngValue]="r.id">{{ r.label }}</option> }
              </select></div>
            @if (slotInvalid()) {
              <p class="field full field-error">La fin prévue doit être après le début prévu.</p>
            }
            @if (resources()) {
              <p class="field full muted">Seuls les équipements et véhicules libres sur ce créneau sont proposés.</p>
            }
          </div>
        </section>

        <app-error-alert [error]="error()" />
        <div class="row">
          <span class="spacer"></span>
          <button class="btn btn-primary" type="submit" [disabled]="form.invalid || saving()">
            {{ id() ? 'Enregistrer les modifications' : 'Créer l’intervention' }}
          </button>
        </div>
      </form>
    </div>
  `,
})
export class InterventionFormComponent implements OnInit {
  readonly id = input<string>();

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(InterventionService);
  private readonly employees = inject(EmployeeService);
  private readonly router = inject(Router);

  readonly types = options(INTERVENTION_TYPES);
  readonly priorities = options(PRIORITIES);
  readonly specialties = options(SPECIALTIES);
  readonly teams = signal<Team[]>([]);
  readonly resources = signal<AvailableResources | null>(null);
  readonly slotInvalid = signal(false);
  readonly saving = signal(false);
  readonly error = signal<ApiError | null>(null);

  readonly form = this.fb.group({
    title: ['', Validators.required],
    type: ['MAINTENANCE' as InterventionType, Validators.required],
    priority: ['NORMALE' as Priority, Validators.required],
    requiredSpecialty: [null as Specialty | null],
    location: ['', Validators.required],
    description: [''],
    plannedStart: [''],
    plannedEnd: [''],
    dueDate: [''],
    teamId: [null as number | null],
    equipmentId: [null as number | null],
    vehicleId: [null as number | null],
  });

  ngOnInit(): void {
    this.form.controls.equipmentId.disable();
    this.form.controls.vehicleId.disable();
    this.employees.teams().subscribe((t) => this.teams.set(t));
    const id = this.id();
    if (id) {
      this.service.get(+id).subscribe((i) => {
        this.form.patchValue({
          title: i.title, type: i.type, priority: i.priority, requiredSpecialty: i.requiredSpecialty,
          location: i.location, description: i.description ?? '', plannedStart: toInputDateTime(i.plannedStart),
          plannedEnd: toInputDateTime(i.plannedEnd), dueDate: i.dueDate ?? '', teamId: i.teamId,
          equipmentId: i.equipmentId, vehicleId: i.vehicleId,
        });
        this.refreshResources();
      });
    }
  }

  refreshResources(): void {
    const start = fromInputDateTime(this.form.controls.plannedStart.value);
    const end = fromInputDateTime(this.form.controls.plannedEnd.value);
    this.slotInvalid.set(!!start && !!end && end <= start);
    if (!start || !end || end <= start) {
      this.resources.set(null);
      this.form.patchValue({ equipmentId: null, vehicleId: null });
      this.form.controls.equipmentId.disable();
      this.form.controls.vehicleId.disable();
      return;
    }
    this.form.controls.equipmentId.enable();
    this.form.controls.vehicleId.enable();
    const id = this.id();
    this.service.availableResources(start, end, id ? +id : undefined).subscribe((r) => {
      this.resources.set(r);
      // On retire une sélection devenue indisponible sur le nouveau créneau
      const eq = this.form.controls.equipmentId.value;
      if (eq && !r.equipment.some((x) => x.id === eq)) this.form.controls.equipmentId.setValue(null);
      const veh = this.form.controls.vehicleId.value;
      if (veh && !r.vehicles.some((x) => x.id === veh)) this.form.controls.vehicleId.setValue(null);
    });
  }

  save(): void {
    const v = this.form.getRawValue();
    const body: InterventionRequest = {
      title: v.title!, type: v.type!, priority: v.priority!, requiredSpecialty: v.requiredSpecialty,
      location: v.location!, description: v.description || null,
      plannedStart: fromInputDateTime(v.plannedStart), plannedEnd: fromInputDateTime(v.plannedEnd),
      dueDate: v.dueDate || null, teamId: v.teamId, equipmentId: v.equipmentId, vehicleId: v.vehicleId,
    };
    if ((body.plannedStart === null) !== (body.plannedEnd === null)) {
      this.error.set({ status: 400, message: 'Renseignez à la fois le début et la fin du créneau, ou aucun des deux.' });
      return;
    }
    const id = this.id();
    this.saving.set(true);
    this.error.set(null);
    (id ? this.service.update(+id, body) : this.service.create(body)).subscribe({
      next: (i) => this.router.navigate(['/interventions', i.id]),
      error: (e) => { this.error.set(toApiError(e)); this.saving.set(false); },
    });
  }
}
