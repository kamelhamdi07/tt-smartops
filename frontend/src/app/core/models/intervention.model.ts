import { Specialty } from './employee.model';

export type InterventionType = 'MAINTENANCE' | 'REPARATION' | 'DEPANNAGE' | 'INSTALLATION' | 'CONTROLE'
  | 'REMPLACEMENT_EQUIPEMENT' | 'INTERVENTION_RESEAU';
export type Priority = 'BASSE' | 'NORMALE' | 'HAUTE' | 'CRITIQUE';
export type InterventionStatus = 'NOUVELLE' | 'PLANIFIEE' | 'AFFECTEE' | 'EN_COURS' | 'SUSPENDUE'
  | 'TERMINEE' | 'VALIDEE' | 'ANNULEE';

export interface InterventionListItem {
  id: number;
  reference: string;
  title: string;
  type: InterventionType;
  priority: Priority;
  location: string;
  plannedStart: string | null;
  plannedEnd: string | null;
  status: InterventionStatus;
  late: boolean;
  teamName: string | null;
}

export interface AssignedTechnician {
  id: number;
  fullName: string;
  specialty: Specialty | null;
  assignedAt: string;
  acknowledgedAt: string | null;
}

export interface Intervention extends InterventionListItem {
  description: string | null;
  requiredSpecialty: Specialty | null;
  dueDate: string | null;
  teamId: number | null;
  equipmentId: number | null;
  equipmentName: string | null;
  vehicleId: number | null;
  vehiclePlate: string | null;
  createdBy: string | null;
  createdAt: string;
  actualStart: string | null;
  actualEnd: string | null;
  cancelReason: string | null;
  technicians: AssignedTechnician[];
}

export interface InterventionRequest {
  title: string;
  description: string | null;
  type: InterventionType;
  priority: Priority;
  location: string;
  requiredSpecialty: Specialty | null;
  plannedStart: string | null;
  plannedEnd: string | null;
  dueDate: string | null;
  teamId: number | null;
  equipmentId: number | null;
  vehicleId: number | null;
}

export interface StatusHistory {
  fromStatus: InterventionStatus | null;
  toStatus: InterventionStatus;
  changedBy: string;
  changedAt: string;
  comment: string | null;
}

export type ReportStatus = 'BROUILLON' | 'SOUMIS' | 'A_CORRIGER' | 'VALIDE';
export type ReportResult = 'RESOLU' | 'PARTIELLEMENT_RESOLU' | 'NON_RESOLU';

export interface InterventionReport {
  id: number;
  interventionId: number;
  authorName: string;
  workStart: string | null;
  workEnd: string | null;
  problemFound: string | null;
  diagnosis: string | null;
  solutionApplied: string | null;
  materialsUsed: string | null;
  replacedParts: string | null;
  observations: string | null;
  result: ReportResult | null;
  status: ReportStatus;
  submittedAt: string | null;
  reviewComment: string | null;
  reviewedBy: string | null;
}
