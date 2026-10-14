import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page } from '../models/api.model';
import { DispatchOptions, DispatchRequest, Reclamation, ReclamationRequest, ReclamationStatus } from '../models/reclamation.model';
import { toParams } from './http-params';

@Injectable({ providedIn: 'root' })
export class ReclamationService {
  private readonly http = inject(HttpClient);

  // client
  mine(page = 0): Observable<Page<Reclamation>> {
    return this.http.get<Page<Reclamation>>('/api/reclamations/mine', { params: toParams({ page, size: 20 }) });
  }
  create(body: ReclamationRequest): Observable<Reclamation> { return this.http.post<Reclamation>('/api/reclamations', body); }
  cancel(id: number): Observable<Reclamation> { return this.http.patch<Reclamation>(`/api/reclamations/${id}/cancel`, {}); }

  // chef d'équipe / responsable technique
  search(status: ReclamationStatus | '' = '', page = 0): Observable<Page<Reclamation>> {
    return this.http.get<Page<Reclamation>>('/api/reclamations', { params: toParams({ status, page, size: 20 }) });
  }
  options(id: number): Observable<DispatchOptions> { return this.http.get<DispatchOptions>(`/api/reclamations/${id}/options`); }
  dispatch(id: number, body: DispatchRequest): Observable<Reclamation> {
    return this.http.post<Reclamation>(`/api/reclamations/${id}/dispatch`, body);
  }
  route(id: number, teamId: number): Observable<Reclamation> {
    return this.http.patch<Reclamation>(`/api/reclamations/${id}/team`, { teamId });
  }
}
