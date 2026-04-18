import client from "./client";
import type { Expense, MonthlySummary, MonthlyEvolution, Budget, ImportResponse, Category, AuthResponse, LlmAnalysis, UserStats, UserResponse } from "./types";

const getUserId = (): number => {
    const stored = localStorage.getItem("auth") || sessionStorage.getItem("auth");
    if (!stored) throw new Error("No autenticado");
    return JSON.parse(stored).userId;
};

export const getUserStats = async (): Promise<UserStats> => {
    const { data } = await client.get(`/users/${getUserId()}/stats`);
    return data;
};

export const updateUser = async (payload: {
    password?: string;
    currency?: string;
}): Promise<UserResponse> => {
    const { data } = await client.put(`/users/${getUserId()}`, payload);
    return data;
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
    const { data } = await client.get(`/analytics/monthly`, { params: { month } });
    return data;
};

export const getEvolution = async (months: number = 6): Promise<MonthlyEvolution[]> => {
    const { data } = await client.get(`/analytics/evolution`, { params: { months } });
    return data;
};

// Budgets
export const getBudgets = async (period: string): Promise<Budget[]> => {
    const { data } = await client.get(`/budgets`, { params: { period } });
    return data;
};

export const createBudget = async (budget: {
    categoryId: number;
    period: string;
    limitAmount: number;
}): Promise<Budget> => {
    const { data } = await client.post("/budgets", budget);
    return data;
};

export const updateBudget = async (
    budgetId: number,
    budget: { categoryId: number; period: string; limitAmount: number }
): Promise<Budget> => {
    const { data } = await client.put(`/budgets/${budgetId}`, budget);
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

// LLM Analysis
export const analyseMonth = async (period: string): Promise<LlmAnalysis> => {
    const { data } = await client.post("/llm/analyse", { period });
    return data;
};

export const getCachedAnalysis = async (period: string): Promise<LlmAnalysis | null> => {
    try {
        const { data } = await client.get("/llm/analyse", { params: { period } });
        return data;
    } catch {
        return null;
    }
};

export const deleteAnalysis = async (period: string): Promise<void> => {
    await client.delete("/llm/analyse", { params: { period } });
};