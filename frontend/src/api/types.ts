export interface User {
    id: number;
    email: string;
    name: string;
    currency: string;
}

export interface AuthResponse {
    token: string;
    userId: number;
    email: string;
    name: string;
}

export interface Category {
    id: number;
    name: string;
    icon: string;
    color: string;
}

export interface Expense {
    id: number;
    userId: number;
    category: string;
    categoryColor: string;
    amount: number;
    currency: string;
    incurredOn: string;
    description: string;
    source: string;
}

export interface MonthlySummary {
    userId: number;
    period: string;
    totalSpent: number;
    totalBudget: number;
    averagePerDay: number;
    transactionCount: number;
    categories: CategorySummary[];
}

export interface CategorySummary {
    category: string;
    color: string;
    spent: number;
    budget: number;
    remaining: number;
}

export interface MonthlyEvolution {
    period: string;
    totalSpent: number;
    transactionCount: number;
}

export interface Budget {
    id: number;
    userId: number;
    category: string;
    categoryColor: string;
    period: string;
    limitAmount: number;
}

export interface ImportResponse {
    jobId: number;
    status: string;
    totalRows: number;
    importedRows: number;
    skippedRows: number;
    errorDetail: string | null;
    fileName: string;
    createdAt: string;
}

export interface LlmAnalysis {
    id: number;
    period: string;
    analysis: string;
    modelUsed: string;
    tokensUsed: number;
    createdAt: string;
    cached: boolean;
}