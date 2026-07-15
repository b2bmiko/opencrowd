/**
 * Domain model types mirroring backend DTOs.
 */

export interface User {
  id: string;
  username: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  displayName: string | null;
  status: UserStatus;
  department: string | null;
  title: string | null;
  phone: string | null;
  avatarUrl: string | null;
  externalId: string | null;
  lastLoginAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export type UserStatus = 'ACTIVE' | 'DISABLED' | 'LOCKED' | 'PENDING' | 'OFFBOARDED';

export interface Group {
  id: string;
  name: string;
  description: string | null;
  type: GroupType;
  parentId: string | null;
  ownerId: string | null;
  memberCount: number;
  createdAt: string;
  updatedAt: string;
}

export type GroupType = 'STATIC' | 'DYNAMIC';

export interface Role {
  id: string;
  name: string;
  description: string | null;
  scope: RoleScope;
  parentId: string | null;
  createdAt: string;
  updatedAt: string;
}

export type RoleScope = 'GLOBAL' | 'APPLICATION' | 'RESOURCE';

export interface Connector {
  id: string;
  connectorType: string;
  name: string;
  status: ConnectorStatus;
  healthStatus: HealthStatus | null;
  lastSyncAt: string | null;
  lastHealthAt: string | null;
  syncSchedule: string | null;
  createdAt: string;
  updatedAt: string;
}

export type ConnectorStatus = 'CONNECTED' | 'DISCONNECTED' | 'ERROR';
export type HealthStatus = 'HEALTHY' | 'DEGRADED' | 'UNHEALTHY';

export interface AuditEvent {
  id: string;
  eventType: string;
  actorId: string | null;
  actorEmail: string | null;
  targetType: string | null;
  targetId: string | null;
  action: string;
  correlationId: string | null;
  createdAt: string;
}
