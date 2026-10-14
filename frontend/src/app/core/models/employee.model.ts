export type EmployeeStatus = 'ACTIF' | 'SUSPENDU' | 'INACTIF';
export type Specialty = 'RESEAU' | 'FIBRE_OPTIQUE' | 'TRANSMISSION' | 'RADIO_MOBILE' | 'ENERGIE' | 'CLIMATISATION' | 'INFORMATIQUE';

export interface Employee {
  id: number;
  matricule: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  phone: string | null;
  jobTitle: string;
  hireDate: string;
  status: EmployeeStatus;
  technician: boolean;
  specialty: Specialty | null;
  departmentId: number | null;
  departmentName: string | null;
  teamId: number | null;
  teamName: string | null;
}

export interface EmployeeSummary {
  id: number;
  matricule: string;
  fullName: string;
  specialty: Specialty | null;
  teamId: number | null;
  teamName: string | null;
}

export interface EmployeeRequest {
  matricule: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  jobTitle: string;
  hireDate: string;
  technician: boolean;
  specialty: Specialty | null;
  departmentId: number | null;
  teamId: number | null;
}

export interface Department {
  id: number;
  code: string;
  name: string;
  description: string | null;
  teamCount: number;
  employeeCount: number;
}

export interface Team {
  id: number;
  name: string;
  description: string | null;
  departmentId: number;
  departmentName: string;
  leaderId: number | null;
  leaderName: string | null;
  memberCount: number;
  /** Catégories de réclamations clients routées vers l'équipe. */
  categories: string[];
}
