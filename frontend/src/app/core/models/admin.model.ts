import { Role } from './auth.model';

export type AssetStatus = 'DISPONIBLE' | 'AFFECTE' | 'EN_MAINTENANCE' | 'HORS_SERVICE';

export interface UserAccount {
  id: number;
  email: string;
  role: Role;
  enabled: boolean;
  employeeId: number | null;
  employeeName: string | null;
  lastLoginAt: string | null;
  createdAt: string;
}

export interface CreateUserRequest {
  email: string;
  password: string;
  role: Role;
  employeeId: number | null;
}

export interface UpdateUserRequest {
  role: Role;
  enabled: boolean;
}

export interface DepartmentRequest {
  code: string;
  name: string;
  description: string | null;
}

export interface TeamRequest {
  name: string;
  description: string | null;
  departmentId: number;
  leaderId: number | null;
  categories: string[];
}

export interface Equipment {
  id: number;
  reference: string;
  name: string;
  type: string;
  status: AssetStatus;
  location: string | null;
  responsibleId: number | null;
  responsibleName: string | null;
  lastMaintenanceDate: string | null;
  nextMaintenanceDate: string | null;
  notes: string | null;
}

export interface EquipmentRequest {
  reference: string;
  name: string;
  type: string;
  status: AssetStatus;
  location: string | null;
  responsibleId: number | null;
  lastMaintenanceDate: string | null;
  nextMaintenanceDate: string | null;
  notes: string | null;
}

export interface Vehicle {
  id: number;
  plateNumber: string;
  brandModel: string;
  type: string;
  status: AssetStatus;
  mileage: number;
  lastMaintenanceDate: string | null;
  teamId: number | null;
  teamName: string | null;
}

export interface VehicleRequest {
  plateNumber: string;
  brandModel: string;
  type: string;
  status: AssetStatus;
  mileage: number;
  lastMaintenanceDate: string | null;
  teamId: number | null;
}
