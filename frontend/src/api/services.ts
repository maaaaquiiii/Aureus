import client from "./client";
import type {Expense, MonthlySummary, MonthlyEvolution, Budget, ImportResponse, Category, AuthResponse} from "./types";

const getUserId = (): number => {
    const stored = localStorage.getItem("auth") || sessionStorage.getItem("auth");
    if (!stored) throw new Error("No autenticado");
    return JSON.parse(stored).userId;
};

// Login
export const login = async (email: string, password: string): Promise<AuthResponse> => {
    const { data } = await client.post("/auth/login", { email, password });
    return data;
};

// Logout
export const register = async (
    email: string,
    name: string,
    password: string,
    currency: string = "EUR"
): Promise<AuthResponse> => {
    const { data } = await client.post("/auth/register", { email, name, password, currency });
    return data;
};

// Expenses
export const getMonthlyExpenses = async (month: string): Promise<Expense[]> => {
    const { data } = await client.get(`/expenses/users/${getUserId()}/monthly`, {
        params: { month },
    });
    return data;
};

// Analytics
export const getMonthlySummary = async (month: string): Promise<MonthlySummary> => {
    const { data } = await client.get(`/analytics/users/${getUserId()}/monthly`, {
        params: { month },
    });
    return data;
};

export const getEvolution = async (months: number = 6): Promise<MonthlyEvolution[]> => {
    const { data } = await client.get(`/analytics/users/${getUserId()}/evolution`, {
        params: { months },
    });
    return data;
};

// Budgets
export const getBudgets = async (period: string): Promise<Budget[]> => {
    const { data } = await client.get(`/budgets/users/${getUserId()}`, {
        params: { period },
    });
    return data;
};

export const createBudget = async (budget: {
    categoryId: number;
    period: string;
    limitAmount: number;
}): Promise<Budget> => {
    const { data } = await client.post("/budgets", {
        userId: getUserId(),
        ...budget,
    });
    return data;
};

export const updateBudget = async (
    budgetId: number,
    budget: { categoryId: number; period: string; limitAmount: number }
): Promise<Budget> => {
    const { data } = await client.put(`/budgets/${budgetId}`, {
        userId: getUserId(),
        ...budget,
    });
    return data;
};

export const deleteBudget = async (budgetId: number): Promise<void> => {
    await client.delete(`/budgets/${budgetId}`);
};

// Imports
export const importCsv = async (
    csvContent: string,
    fileName: string
): Promise<ImportResponse> => {
    const { data } = await client.post("/imports", {
        userId: getUserId(),
        source: "revolut",
        csvContent,
        fileName,
    });
    return data;
};

export const getImportJobs = async (): Promise<ImportResponse[]> => {
    const { data } = await client.get(`/imports/users/${getUserId()}`);
    return data;
};

export const deleteImportJob = async (jobId: number): Promise<void> => {
    await client.delete(`/imports/${jobId}`);
};

export const deleteAllImportJobs = async (): Promise<void> => {
    await client.delete(`/imports/users/${getUserId()}`);
};

// Categories
export const getCategories = async (): Promise<Category[]> => {
    const { data } = await client.get("/categories");
    return data;
};

export const updateExpenseCategory = async (
    expenseId: number,
    categoryId: number
): Promise<void> => {
    await client.patch(`/expenses/${expenseId}/category`, { categoryId });
};