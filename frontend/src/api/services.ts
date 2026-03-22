import client from "./client";
import type {
    Expense,
    MonthlySummary,
    MonthlyEvolution,
    Budget,
    ImportResponse,
} from "./types";

const USER_ID = 1; // TODO: hardcoded until login is implemented

// Expenses

export const getMonthlyExpenses = async (month: string): Promise<Expense[]> => {
    const { data } = await client.get(`/expenses/users/${USER_ID}/monthly`, {
        params: { month },
    });
    return data;
};

// Analytics

export const getMonthlySummary = async (month: string): Promise<MonthlySummary> => {
    const { data } = await client.get(`/analytics/users/${USER_ID}/monthly`, {
        params: { month },
    });
    return data;
};

export const getEvolution = async (months: number = 6): Promise<MonthlyEvolution[]> => {
    const { data } = await client.get(`/analytics/users/${USER_ID}/evolution`, {
        params: { months },
    });
    return data;
};

// Budgets

export const getBudgets = async (period: string): Promise<Budget[]> => {
    const { data } = await client.get(`/budgets/users/${USER_ID}`, {
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
        userId: USER_ID,
        ...budget,
    });
    return data;
};

export const updateBudget = async (
    budgetId: number,
    budget: { categoryId: number; period: string; limitAmount: number }
): Promise<Budget> => {
    const { data } = await client.put(`/budgets/${budgetId}`, {
        userId: USER_ID,
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
        userId: USER_ID,
        source: "revolut",
        csvContent,
        fileName,
    });
    return data;
};