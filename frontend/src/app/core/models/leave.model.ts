export type LeaveType = 'ANNUEL' | 'MALADIE' | 'EXCEPTIONNEL' | 'MATERNITE' | 'SANS_SOLDE';
export type LeaveStatus = 'EN_ATTENTE' | 'APPROUVEE' | 'REFUSEE' | 'ANNULEE';

export interface LeaveRequest {
  id: number;
  employeeId: number;
  employeeName: string;
  matricule: string;
  type: LeaveType;
  startDate: string;
  endDate: string;
  workingDays: number;
  comment: string | null;
  status: LeaveStatus;
  decisionComment: string | null;
  createdAt: string;
  processedAt: string | null;
  processedBy: string | null;
}

export type AbsenceType = 'MALADIE' | 'AUTORISEE' | 'MISSION_EXTERNE' | 'FORMATION' | 'INJUSTIFIEE';

export interface Absence {
  id: number;
  employeeId: number;
  employeeName: string;
  matricule: string;
  type: AbsenceType;
  startDate: string;
  endDate: string;
  reason: string | null;
  justified: boolean;
  recordedBy: string | null;
}
