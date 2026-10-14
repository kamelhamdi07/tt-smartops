import { EmployeeSummary } from './employee.model';
import { InterventionStatus, Priority } from './intervention.model';

export type ConflictType = 'INACTIF' | 'SPECIALITE' | 'CONGE' | 'ABSENCE' | 'INTERVENTION' | 'EQUIPEMENT' | 'VEHICULE';

export interface Conflict {
  type: ConflictType;
  message: string;
  from: string | null;
  to: string | null;
}

export interface TechnicianCandidate {
  technician: EmployeeSummary;
  specialtyMatch: boolean;
  sameTeam: boolean;
  available: boolean;
  alreadyAssigned: boolean;
  conflicts: Conflict[];
}

export type SlotState = 'DISPONIBLE' | 'OCCUPE' | 'CONGE' | 'ABSENT';

export interface AvailabilitySlot {
  start: string;
  end: string;
  state: SlotState;
  label: string;
}

export interface PlanningEntry {
  interventionId: number;
  reference: string;
  title: string;
  priority: Priority;
  status: InterventionStatus;
  location: string;
  start: string;
  end: string;
  teamName: string | null;
  technicians: string[];
  vehicle: string | null;
  equipment: string | null;
}

export interface PlanningConflict {
  type: ConflictType;
  message: string;
  interventionIds: number[];
}

export interface AvailableResource { id: number; label: string; }
export interface AvailableResources { equipment: AvailableResource[]; vehicles: AvailableResource[]; }
