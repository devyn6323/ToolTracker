export type Role = 'OWNER' | 'ADMIN' | 'MANAGER' | 'EMPLOYEE';
export type ToolStatus = 'AVAILABLE' | 'CHECKED_OUT' | 'OVERDUE' | 'MAINTENANCE' | 'DAMAGED' | 'LOST' | 'RETIRED';
export type ToolCondition = 'NEW' | 'GOOD' | 'FAIR' | 'POOR' | 'DAMAGED' | 'MISSING';

export interface UserSummary {
  id: string;
  name: string;
  email: string;
  role: Role;
  active: boolean;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  companyId: string;
  companyName: string;
  user: UserSummary;
}

export interface Tool {
  id: string;
  assetNumber: string;
  name: string;
  category?: string;
  manufacturer?: string;
  model?: string;
  serialNumber?: string;
  purchaseDate?: string;
  condition: ToolCondition;
  status: ToolStatus;
  currentLocation?: string;
  qrCodeValue: string;
  photoUrl?: string;
  notes?: string;
  createdAt: string;
  checkedOutTo?: UserSummary;
  expectedReturnAt?: string;
}

export interface ToolTransaction {
  id: string;
  toolId: string;
  user: UserSummary;
  transactionType: 'CHECKOUT' | 'RETURN' | 'TRANSFER' | 'MAINTENANCE' | 'CONDITION_UPDATE';
  jobName?: string;
  location?: string;
  conditionAtCheckout?: ToolCondition;
  conditionAtReturn?: ToolCondition;
  checkedOutAt: string;
  expectedReturnAt?: string;
  returnedAt?: string;
  notes?: string;
  tool?: Tool;
}

export interface ActivityItem extends Omit<ToolTransaction, 'tool'> {
  toolName: string;
  assetNumber: string;
  occurredAt: string;
}

export interface DashboardData {
  counts: Record<ToolStatus, number>;
  recentActivity: ActivityItem[];
}

export type AuthStackParams = {
  Login: undefined;
  Register: undefined;
};

export type AppStackParams = {
  Main: undefined;
  ToolDetail: { toolId: string };
  ToolForm: { toolId?: string } | undefined;
  Checkout: { tool: Tool };
  Return: { tool: Tool };
  Transfer: { tool: Tool };
  Scanner: undefined;
  Employees: undefined;
  Settings: undefined;
};

export type TabParams = {
  Home: undefined;
  Inventory: undefined;
  Scan: undefined;
  MyTools: undefined;
  Activity: undefined;
};
