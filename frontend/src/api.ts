import axios, { type AxiosInstance } from 'axios'
import type {
  AuthResponse,
  ApiPage,
  Lease,
  MaintenanceComment,
  MaintenanceRequest,
  Notice,
  NoticePage,
  OwnerDashboard,
  Property,
  Rent,
  Role,
  TenantRents,
  TenantDetails,
  User,
} from './types'

const baseURL = import.meta.env.VITE_API_BASE_URL || ''

function client(token?: string): AxiosInstance {
  return axios.create({
    baseURL,
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  })
}

export function errorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const body = error.response?.data as { message?: string; error?: string } | undefined
    return body?.message || body?.error || error.message || 'The request could not be completed.'
  }
  return error instanceof Error ? error.message : 'The request could not be completed.'
}

export const api = {
  async register(input: { name: string; email: string; password: string; phone: string; role: Role }) {
    const response = await client().post<User>('/api/auth/register', input)
    return response.data
  },
  async login(email: string, password: string) {
    const response = await client().post<AuthResponse>('/api/auth/login', { email, password })
    return response.data
  },
  async profile(token: string) {
    return (await client(token).get<User>('/api/profile')).data
  },
  async updateProfile(token: string, body: Pick<User, 'name' | 'email' | 'phone'>) {
    return (await client(token).put<User>('/api/profile', body)).data
  },
  async properties(token: string) {
    return (await client(token).get<Property[]>('/api/properties')).data
  },
  async createProperty(token: string, body: Partial<Property>) {
    return (await client(token).post<Property>('/api/properties', body)).data
  },
  async updateProperty(token: string, id: number, body: Partial<Property>) {
    return (await client(token).put<Property>(`/api/properties/${id}`, body)).data
  },
  async deleteProperty(token: string, id: number) {
    await client(token).delete(`/api/properties/${id}`)
  },
  async leases(token: string) {
    return (await client(token).get<Lease[]>('/api/leases')).data
  },
  async createLease(token: string, body: Partial<Lease> & { tenantCode: string }) {
    return (await client(token).post<Lease>('/api/leases', body)).data
  },
  async updateLease(token: string, lease: Lease) {
    return (await client(token).put<Lease>(`/api/leases/${lease.id}`, lease)).data
  },
  async endLease(token: string, id: number) {
    return (await client(token).put<Lease>(`/api/leases/${id}/end`)).data
  },
  async findTenant(token: string, code: string) {
    return (await client(token).get<User>('/api/tenants/search', { params: { code } })).data
  },
  async tenantDetails(token: string, id: number) {
    return (await client(token).get<TenantDetails>(`/api/tenants/${id}`)).data
  },
  async dashboard(token: string) {
    return (await client(token).get<OwnerDashboard>('/api/dashboard/owner')).data
  },
  async generateRent(token: string) {
    return (await client(token).post<string>('/api/rents/generate')).data
  },
  async approvals(token: string) {
    return (await client(token).get<Rent[]>('/api/rents/approvals')).data
  },
  async leaseHistory(token: string, leaseId: number) {
    return (await client(token).get<Rent[]>(`/api/rents/lease/${leaseId}`)).data
  },
  async approveRent(token: string, id: number) {
    return (await client(token).put<Rent>(`/api/rents/${id}/approve`)).data
  },
  async rejectRent(token: string, id: number) {
    return (await client(token).put<Rent>(`/api/rents/${id}/reject`)).data
  },
  async tenantLeases(token: string) {
    return (await client(token).get<Lease[]>('/api/me/lease')).data
  },
  async tenantRents(token: string) {
    return (await client(token).get<TenantRents>('/api/me/rents')).data
  },
  async payOnline(token: string, id: number) {
    return (await client(token).post<Rent>(`/api/rents/${id}/pay/online`)).data
  },
  async payCash(token: string, id: number) {
    return (await client(token).post<Rent>(`/api/rents/${id}/pay/cash`)).data
  },
  async ownerNotices(token: string) {
    return (await client(token).get<Notice[]>('/api/notices')).data
  },
  async tenantNotices(token: string) {
    return (await client(token).get<Notice[]>('/api/me/notices')).data
  },
  async ownerNoticePage(token: string, params: { page: number; size: number; search?: string; propertyId?: number }) {
    return (await client(token).get<NoticePage>('/api/notices/page', { params })).data
  },
  async tenantNoticePage(token: string, params: { page: number; size: number; search?: string; propertyId?: number }) {
    return (await client(token).get<NoticePage>('/api/me/notices/page', { params })).data
  },
  async createNotice(token: string, body: { title: string; message: string; propertyId?: number }) {
    return (await client(token).post<Notice>('/api/notices', body)).data
  },
  async updateNotice(token: string, id: number, body: { title: string; message: string; propertyId?: number }) {
    return (await client(token).put<Notice>(`/api/notices/${id}`, body)).data
  },
  async deleteNotice(token: string, id: number) {
    await client(token).delete(`/api/notices/${id}`)
  },
  async propertyMaintenance(token: string, id: number) {
    return (await client(token).get<MaintenanceRequest[]>(`/api/maintenance/property/${id}`)).data
  },
  async maintenancePage(token: string, params: { page: number; size: number; search?: string; propertyId?: number }) {
    return (await client(token).get<ApiPage<MaintenanceRequest>>('/api/maintenance/page', { params })).data
  },
  async maintenanceRequest(token: string, id: number) {
    return (await client(token).get<MaintenanceRequest>(`/api/maintenance/${id}`)).data
  },
  async createMaintenance(token: string, body: { propertyId: number; title: string; description: string; priority: string }) {
    return (await client(token).post<MaintenanceRequest>('/api/maintenance', body)).data
  },
  async updateMaintenance(token: string, id: number, body: { title: string; description: string; priority: string }) {
    return (await client(token).put<MaintenanceRequest>(`/api/maintenance/${id}`, body)).data
  },
  async cancelMaintenance(token: string, id: number) {
    return (await client(token).put<MaintenanceRequest>(`/api/maintenance/${id}/cancel`)).data
  },
  async updateMaintenanceStatus(token: string, id: number, status: MaintenanceRequest['status']) {
    return (await client(token).put<MaintenanceRequest>(`/api/maintenance/${id}/status`, { status })).data
  },
  async comments(token: string, id: number) {
    return (await client(token).get<MaintenanceComment[]>(`/api/maintenance/${id}/comments`)).data
  },
  async addComment(token: string, id: number, comment: string) {
    return (await client(token).post<MaintenanceComment>(`/api/maintenance/${id}/comments`, { comment })).data
  },
}