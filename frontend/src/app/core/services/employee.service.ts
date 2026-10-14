import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page } from '../models/api.model';
import { Department, Employee, EmployeeRequest, EmployeeStatus, EmployeeSummary, Team } from '../models/employee.model';
import { toParams } from './http-params';

export interface EmployeeFilter {
  q?: string;
  departmentId?: number | null;
  teamId?: number | null;
  status?: EmployeeStatus | '';
  technician?: boolean | null;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class EmployeeService {
  private readonly http = inject(HttpClient);

  search(f: EmployeeFilter): Observable<Page<Employee>> {
    return this.http.get<Page<Employee>>('/api/employees', { params: toParams({ ...f }) });
  }
  get(id: number): Observable<Employee> { return this.http.get<Employee>(`/api/employees/${id}`); }
  me(): Observable<Employee> { return this.http.get<Employee>('/api/employees/me'); }
  technicians(): Observable<EmployeeSummary[]> { return this.http.get<EmployeeSummary[]>('/api/employees/technicians'); }
  create(body: EmployeeRequest): Observable<Employee> { return this.http.post<Employee>('/api/employees', body); }
  update(id: number, body: EmployeeRequest): Observable<Employee> { return this.http.put<Employee>(`/api/employees/${id}`, body); }
  changeStatus(id: number, status: EmployeeStatus): Observable<Employee> {
    return this.http.patch<Employee>(`/api/employees/${id}/status`, { status });
  }

  departments(): Observable<Department[]> { return this.http.get<Department[]>('/api/departments'); }
  teams(departmentId?: number | null): Observable<Team[]> {
    return this.http.get<Team[]>('/api/teams', { params: toParams({ departmentId }) });
  }
}
