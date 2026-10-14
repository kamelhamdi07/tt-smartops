import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page } from '../models/api.model';
import {
  Intervention, InterventionListItem, InterventionReport, InterventionRequest, InterventionStatus, Priority,
  StatusHistory
} from '../models/intervention.model';
import { AvailabilitySlot, AvailableResources, PlanningConflict, PlanningEntry, TechnicianCandidate } from '../models/planning.model';
import { toParams } from './http-params';

export interface InterventionFilter {
  q?: string;
  status?: InterventionStatus | '';
  priority?: Priority | '';
  page?: number;
}

@Injectable({ providedIn: 'root' })
export class InterventionService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/interventions';

  search(f: InterventionFilter): Observable<Page<InterventionListItem>> {
    return this.http.get<Page<InterventionListItem>>(this.base, { params: toParams({ ...f }) });
  }
  mine(status: InterventionStatus | '' = '', page = 0): Observable<Page<InterventionListItem>> {
    return this.http.get<Page<InterventionListItem>>(`${this.base}/mine`, { params: toParams({ status, page }) });
  }
  get(id: number): Observable<Intervention> { return this.http.get<Intervention>(`${this.base}/${id}`); }
  history(id: number): Observable<StatusHistory[]> { return this.http.get<StatusHistory[]>(`${this.base}/${id}/history`); }
  create(body: InterventionRequest): Observable<Intervention> { return this.http.post<Intervention>(this.base, body); }
  update(id: number, body: InterventionRequest): Observable<Intervention> {
    return this.http.put<Intervention>(`${this.base}/${id}`, body);
  }

  candidates(id: number): Observable<TechnicianCandidate[]> {
    return this.http.get<TechnicianCandidate[]>(`${this.base}/${id}/candidates`);
  }
  assign(id: number, technicianId: number): Observable<Intervention> {
    return this.http.post<Intervention>(`${this.base}/${id}/assignments`, { technicianId });
  }
  unassign(id: number, technicianId: number): Observable<Intervention> {
    return this.http.delete<Intervention>(`${this.base}/${id}/assignments/${technicianId}`);
  }

  /** Actions de workflow : acknowledge, start, suspend, resume, complete, cancel */
  action(id: number, action: string, comment?: string): Observable<Intervention> {
    return this.http.patch<Intervention>(`${this.base}/${id}/${action}`, comment !== undefined ? { comment } : {});
  }

  report(id: number): Observable<InterventionReport> { return this.http.get<InterventionReport>(`${this.base}/${id}/report`); }
  saveReport(id: number, body: Partial<InterventionReport>): Observable<InterventionReport> {
    return this.http.put<InterventionReport>(`${this.base}/${id}/report`, body);
  }
  submitReport(id: number): Observable<InterventionReport> {
    return this.http.post<InterventionReport>(`${this.base}/${id}/report/submit`, {});
  }
  reviewReport(id: number, approve: boolean, comment: string | null): Observable<InterventionReport> {
    return this.http.post<InterventionReport>(`${this.base}/${id}/report/review`, { approve, comment });
  }

  planning(from: string, to: string, mine: boolean): Observable<PlanningEntry[]> {
    return this.http.get<PlanningEntry[]>(mine ? '/api/planning/mine' : '/api/planning', { params: toParams({ from, to }) });
  }
  conflicts(from: string, to: string): Observable<PlanningConflict[]> {
    return this.http.get<PlanningConflict[]>('/api/planning/conflicts', { params: toParams({ from, to }) });
  }
  availability(technicianId: number, date: string): Observable<AvailabilitySlot[]> {
    return this.http.get<AvailabilitySlot[]>(`/api/planning/technicians/${technicianId}/availability`, { params: toParams({ date }) });
  }
  availableResources(start: string, end: string, excludeInterventionId?: number): Observable<AvailableResources> {
    return this.http.get<AvailableResources>('/api/planning/resources/available',
      { params: toParams({ start, end, excludeInterventionId }) });
  }
}
