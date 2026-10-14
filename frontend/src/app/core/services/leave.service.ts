import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page } from '../models/api.model';
import { Absence, AbsenceType, LeaveRequest, LeaveStatus, LeaveType } from '../models/leave.model';
import { toParams } from './http-params';

@Injectable({ providedIn: 'root' })
export class LeaveService {
  private readonly http = inject(HttpClient);

  mine(page = 0): Observable<Page<LeaveRequest>> {
    return this.http.get<Page<LeaveRequest>>('/api/leaves/mine', { params: toParams({ page }) });
  }
  create(body: { type: LeaveType; startDate: string; endDate: string; comment: string | null }): Observable<LeaveRequest> {
    return this.http.post<LeaveRequest>('/api/leaves', body);
  }
  cancel(id: number): Observable<LeaveRequest> { return this.http.patch<LeaveRequest>(`/api/leaves/${id}/cancel`, {}); }
  search(status: LeaveStatus | '', page = 0): Observable<Page<LeaveRequest>> {
    return this.http.get<Page<LeaveRequest>>('/api/leaves', { params: toParams({ status, page }) });
  }
  decide(id: number, approve: boolean, comment: string | null): Observable<LeaveRequest> {
    return this.http.patch<LeaveRequest>(`/api/leaves/${id}/decision`, { approve, comment });
  }

  absences(employeeId: number | null, page = 0): Observable<Page<Absence>> {
    return this.http.get<Page<Absence>>('/api/absences', { params: toParams({ employeeId, page }) });
  }
  myAbsences(): Observable<Page<Absence>> { return this.http.get<Page<Absence>>('/api/absences/mine'); }
  createAbsence(body: { employeeId: number; type: AbsenceType; startDate: string; endDate: string;
                        reason: string | null; justified: boolean }): Observable<Absence> {
    return this.http.post<Absence>('/api/absences', body);
  }
  deleteAbsence(id: number): Observable<void> { return this.http.delete<void>(`/api/absences/${id}`); }
}
