export interface User {
  id: number;
  externalId: string;
  userAttributes: string; // Assuming JSON string for now
  createdAt: string;
  updatedAt: string;
}

export interface EnrollUserRequest {
  id?: number; // Optional for create, required for update
  externalId: string;
  userAttributes: string;
}

export interface EnrollUserUseCaseOut extends User {}

export interface BrowseUsersUseCaseOut {
  users: User[];
}

export interface GetTotalUserCountUseCaseOut {
  totalCount: number;
} 