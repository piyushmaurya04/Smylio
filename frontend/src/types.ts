export type Role = 'OWNER' | 'TENANT'

export interface User {
  id: number
  name: string
  email: string
  phone?: string | null
  role: Role
  tenantCode?: string | null
  createdAt?: string | null
}

export interface AuthResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
  user: User
}

export interface Property {
  id: number
  ownerId: number
  title: string
  address: string
  type: string
  rentAmount: number
  status: 'AVAILABLE' | 'OCCUPIED'
  description: string
  amenities: string[]
  createdAt: string
}

export interface Lease {
  id: number
  propertyId: number
  propertyTitle: string
  amenities: string[]
  tenantId: number
  tenantName: string
  tenantCode: string
  ownerName: string
  ownerEmail: string
  ownerPhone?: string | null
  startDate: string
  endDate?: string | null
  monthlyRent: number
  fineGraceDays: number
  finePerDay: number
  status: 'ACTIVE' | 'ENDED'
}

export interface TenantDetails {
  id: number
  name: string
  email: string
  phone: string | null
  tenantCode: string
  createdAt: string
  maintenanceRequestCount: number
  maintenanceRequestsByProperty: Record<number, number>
  leases: Lease[]
}

export interface Rent {
  id: number
  leaseId: number
  forMonth: string
  amount: number
  fine: number
  dueDate: string
  paidDate?: string | null
  method?: 'ONLINE' | 'CASH' | null
  transactionId?: string | null
  status: 'PENDING' | 'LATE' | 'AWAITING_APPROVAL' | 'PAID'
}

export interface TenantRents {
  totalDue: number
  nextDueDate?: string | null
  breakdown: Rent[]
  history: Rent[]
}

export interface OwnerDashboard {
  tenantCount: number
  pendingRent: number
  openMaintenanceCount: number
}

export interface MaintenanceRequest {
  id: number
  propertyId: number
  propertyTitle: string
  tenantId: number
  tenantName: string
  title: string
  description: string
  priority: 'LOW' | 'MEDIUM' | 'HIGH'
  status: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CANCELLED'
  createdAt: string
  resolvedAt?: string | null
}

export interface ApiPage<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface MaintenanceComment {
  id: number
  requestId: number
  userId: number
  userName: string
  comment: string
  createdAt: string
}

export interface Notice {
  id: number
  ownerId: number
  propertyId?: number | null
  propertyTitle?: string | null
  title: string
  message: string
  createdAt: string
}

export interface NoticePage {
  content: Notice[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}