import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page } from '../models/api.model';
import {
  AssetStatus, CreateUserRequest, DepartmentRequest, Equipment, EquipmentRequest, TeamRequest,
  UpdateUserRequest, UserAccount, Vehicle, VehicleRequest,
} from '../models/admin.model';
import { Department, Team } from '../models/employee.model';
import { toParams } from './http-params';

export interface AssetFilter {
  q?: string;
  status?: AssetStatus | '';
  page?: number;
  size?: number;
}

/** Référentiels gérés par l'administration : comptes, organisation, équipements, véhicules. */
@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  users(page = 0, size = 20): Observable<Page<UserAccount>> {
    return this.http.get<Page<UserAccount>>('/api/users', { params: toParams({ page, size }) });
  }
  createUser(body: CreateUserRequest): Observable<UserAccount> { return this.http.post<UserAccount>('/api/users', body); }
  updateUser(id: number, body: UpdateUserRequest): Observable<UserAccount> {
    return this.http.put<UserAccount>(`/api/users/${id}`, body);
  }

  createDepartment(body: DepartmentRequest): Observable<Department> { return this.http.post<Department>('/api/departments', body); }
  updateDepartment(id: number, body: DepartmentRequest): Observable<Department> {
    return this.http.put<Department>(`/api/departments/${id}`, body);
  }

  createTeam(body: TeamRequest): Observable<Team> { return this.http.post<Team>('/api/teams', body); }
  updateTeam(id: number, body: TeamRequest): Observable<Team> { return this.http.put<Team>(`/api/teams/${id}`, body); }

  equipment(f: AssetFilter): Observable<Page<Equipment>> {
    return this.http.get<Page<Equipment>>('/api/equipment', { params: toParams({ ...f }) });
  }
  createEquipment(body: EquipmentRequest): Observable<Equipment> { return this.http.post<Equipment>('/api/equipment', body); }
  updateEquipment(id: number, body: EquipmentRequest): Observable<Equipment> {
    return this.http.put<Equipment>(`/api/equipment/${id}`, body);
  }

  vehicles(f: AssetFilter): Observable<Page<Vehicle>> {
    return this.http.get<Page<Vehicle>>('/api/vehicles', { params: toParams({ ...f }) });
  }
  createVehicle(body: VehicleRequest): Observable<Vehicle> { return this.http.post<Vehicle>('/api/vehicles', body); }
  updateVehicle(id: number, body: VehicleRequest): Observable<Vehicle> { return this.http.put<Vehicle>(`/api/vehicles/${id}`, body); }
}
