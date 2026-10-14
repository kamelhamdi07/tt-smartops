import { Specialty } from './employee.model';
import { Priority } from './intervention.model';

export type ReclamationCategory = 'WIFI' | 'ADSL' | 'FIBRE' | 'TELEPHONE_FIXE' | 'MOBILE' | 'AUTRE';
export type ReclamationStatus = 'NOUVELLE' | 'AFFECTEE' | 'EN_COURS' | 'RESOLUE' | 'ANNULEE';

export interface Reclamation {
  id: number;
  reference: string;
  category: ReclamationCategory;
  title: string;
  description: string;
  address: string;
  phone: string;
  status: ReclamationStatus;
  clientName: string;
  clientEmail: string;
  teamId: number | null;
  teamName: string | null;
  interventionId: number | null;
  interventionReference: string | null;
  technicians: string[];
  vehiclePlate: string | null;
  plannedStart: string | null;
  plannedEnd: string | null;
  createdAt: string;
  resolvedAt: string | null;
  canDispatch: boolean;
}

export interface ReclamationRequest {
  category: ReclamationCategory;
  title: string;
  description: string;
  address: string | null;
  phone: string | null;
}

export interface DispatchRequest {
  technicianId: number;
  vehicleId: number | null;
  plannedStart: string;
  plannedEnd: string;
  priority: Priority;
}

export interface DispatchOptions {
  technicians: { id: number; fullName: string; specialty: Specialty | null }[];
  vehicles: { id: number; plateNumber: string; brandModel: string; teamName: string | null }[];
}
