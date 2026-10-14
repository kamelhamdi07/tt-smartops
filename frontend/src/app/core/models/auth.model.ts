export type Role = 'ADMIN' | 'RH' | 'RESPONSABLE_TECHNIQUE' | 'TECHNICIEN' | 'EMPLOYE' | 'CLIENT';

export interface UserInfo {
  id: number;
  email: string;
  role: Role;
  employeeId: number | null;
  fullName: string;
  /** Dirige au moins une équipe : traite les réclamations clients de son équipe. */
  teamLeader: boolean;
}

export interface AuthResponse {
  token: string;
  expiresInSeconds: number;
  user: UserInfo;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  phone: string;
  address: string;
  password: string;
}
