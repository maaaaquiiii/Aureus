import { useEffect, useState } from "react";
import {
    BarChart, Bar, XAxis, YAxis, Tooltip,
    ResponsiveContainer, PieChart, Pie, Cell,
} from "recharts";
import { getMonthlySummary, getMonthlyExpenses, getEvolution } from "../api/services";
import type { MonthlySummary, Expense, MonthlyEvolution, CategorySummary } from "../api/types";
import styles from "./Dashboard.module.css";

// helpers
function currentMonth(): string {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
}

function fmt(amount: number, currency = "EUR"): string {
    return new Intl.NumberFormat("es-ES", { style: "currency", currency, maximumFractionDigits: 2 }).format(amount);
}

function shortMonth(period: string): string {
    const [y, m] = period.split("-");
    return new Date(Number(y), Number(m) - 1).toLocaleDateString("es-ES", { month: "short" });
}

function fullMonthLabel(month: string): string {
    const [y, m] = month.split("-");
    return new Date(Number(y), Number(m) - 1).toLocaleDateString("es-ES", { month: "long", year: "numeric" });
}

// tooltip
interface TooltipProps {
    active?: boolean;
    payload?: { value: number }[];
    label?: string;
}

function EvolutionTooltip({ active, payload, label }: TooltipProps) {
    if (!active || !payload?.length) return null;
    return (
        <div style={{
            background: "var(--color-surface)",
            border: "1px solid var(--color-border)",
            borderRadius: 8,
            padding: "8px 12px",
            boxShadow: "0 4px 16px rgba(0,0,0,0.2)",
        }}>
            <p style={{ color: "var(--color-text-muted)", fontSize: 11, margin: 0, textTransform: "capitalize" }}>{label}</p>
            <p style={{ color: "var(--color-accent)", fontSize: 13, fontWeight: 600, margin: "3px 0 0" }}>{fmt(payload[0].value)}</p>
        </div>
    );
}

// category row
function CategoryRow({ cat, max }: { cat: CategorySummary; max: number }) {
    const pct = max > 0 ? (cat.spent / max) * 100 : 0;
    return (
        <div className={styles.categoryRow}>
            <div className={styles.categoryRowHeader}>
                <span className={styles.categoryName}>{cat.category}</span>
                <span className={styles.categoryAmount}>{fmt(cat.spent)}</span>
            </div>
            <div className={styles.categoryBar}>
                <div
                    className={styles.categoryBarFill}
                    style={{ width: `${pct}%`, background: cat.color || "var(--color-accent)" }}
                />
            </div>
        </div>
    );
}

// main
const PAGE_SIZE = 10;

export default function Dashboard() {
    const [month, setMonth] = useState(currentMonth());
    const [summary, setSummary] = useState<MonthlySummary | null>(null);
    const [expenses, setExpenses] = useState<Expense[]>([]);
    const [evolution, setEvolution] = useState<MonthlyEvolution[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [page, setPage] = useState(0);
    const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
    const [sortBy, setSortBy] = useState<"date" | "amount" | "category" | "description">("date");
    const [sortDir, setSortDir] = useState<"asc" | "desc">("desc");

    useEffect(() => {
        const fetchData = async () => {
            setLoading(true);
            setError(null);
            try {
                const [s, e, ev] = await Promise.all([
                    getMonthlySummary(month),
                    getMonthlyExpenses(month),
                    getEvolution(6),
                ]);
                setSummary(s);
                setExpenses(e);
                setEvolution(ev);
                setPage(0);
                setSelectedCategory(null);
            } catch {
                setError("No se pudieron cargar los datos. ¿Está el backend levantado?");
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, [month]);

    function prevMonth() {
        const [y, m] = month.split("-").map(Number);
        const d = new Date(y, m - 2);
        setMonth(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`);
    }

    function nextMonth() {
        const [y, m] = month.split("-").map(Number);
        const d = new Date(y, m);
        const next = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
        if (next <= currentMonth()) setMonth(next);
    }

    // filters & short
    const uniqueCategories = [...new Set(expenses.map(e => e.category))].sort();

    const filteredExpenses = (selectedCategory
            ? expenses.filter(e => e.category === selectedCategory)
            : expenses
    ).slice().sort((a, b) => {
        let comparison = 0;
        switch (sortBy) {
            case "date":
                comparison = a.incurredOn.localeCompare(b.incurredOn);
                break;
            case "amount":
                comparison = a.amount - b.amount;
                break;
            case "category":
                comparison = a.category.localeCompare(b.category);
                break;
            case "description":
                comparison = (a.description || "").localeCompare(b.description || "");
                break;
        }
        return sortDir === "asc" ? comparison : -comparison;
    });

    const totalPages = Math.ceil(filteredExpenses.length / PAGE_SIZE);
    const pagedExpenses = filteredExpenses.slice(
        page * PAGE_SIZE,
        page * PAGE_SIZE + PAGE_SIZE
    );

    const pieData = summary?.categories ?? [];
    const maxSpent = Math.max(...(summary?.categories.map(c => c.spent) ?? [1]));
    const budgetPct = summary && summary.totalBudget > 0
        ? Math.min((summary.totalSpent / summary.totalBudget) * 100, 100)
        : null;

    return (
        <div className={styles.page}>

            {/* header */}
            <div className={styles.pageHeader}>
                <div>
                    <h1 className={styles.pageTitle}>Dashboard</h1>
                    <p className={styles.pageSubtitle}>Resumen financiero personal</p>
                </div>
                <div className={styles.monthPicker}>
                    <button className={styles.iconBtn} onClick={prevMonth}>‹</button>
                    <span className={styles.monthLabel}>{fullMonthLabel(month)}</span>
                    <button className={styles.iconBtn} onClick={nextMonth} disabled={month === currentMonth()}>›</button>
                </div>
            </div>

            {/* error */}
            {error && <div className={styles.error}>{error}</div>}

            {/* kpi skeleton */}
            {loading && (
                <div className={styles.kpiGrid}>
                    {[0, 1, 2, 3].map(i => (
                        <div key={i} className={styles.skeleton} style={{ height: 104 }} />
                    ))}
                </div>
            )}

            {/* kpi cards */}
            {!loading && summary && (
                <div className={styles.kpiGrid}>
                    <div className={styles.kpiCard} style={{ animationDelay: "0ms" }}>
                        <p className={styles.kpiLabel}>Total gastado</p>
                        <p className={styles.kpiValue}>{fmt(summary.totalSpent)}</p>
                    </div>
                    <div className={styles.kpiCard} style={{ animationDelay: "60ms" }}>
                        <p className={styles.kpiLabel}>Media diaria</p>
                        <p className={styles.kpiValue}>{fmt(summary.averagePerDay)}</p>
                    </div>
                    <div className={styles.kpiCard} style={{ animationDelay: "120ms" }}>
                        <p className={styles.kpiLabel}>Transacciones</p>
                        <p className={styles.kpiValue}>{summary.transactionCount}</p>
                        <p className={styles.kpiSub}>{fullMonthLabel(month)}</p>
                    </div>
                    {budgetPct !== null && (
                        <div className={styles.kpiCard} style={{ animationDelay: "180ms" }}>
                            <p className={styles.kpiLabel}>Presupuesto usado</p>
                            <p
                                className={styles.kpiValue}
                                style={{ color: budgetPct > 90 ? "var(--color-danger)" : "var(--color-accent)" }}
                            >
                                {budgetPct.toFixed(0)}%
                            </p>
                            <div className={styles.budgetBar}>
                                <div
                                    className={styles.budgetBarFill}
                                    style={{
                                        width: `${budgetPct}%`,
                                        background: budgetPct > 90 ? "var(--color-danger)" : "var(--color-accent)"
                                    }}
                                />
                            </div>
                            <p className={styles.kpiSub}>{fmt(summary.totalBudget)} total</p>
                        </div>
                    )}
                </div>
            )}

            {/* charts */}
            {!loading && (
                <div className={styles.chartsRow}>
                    <div className={styles.card}>
                        <p className={styles.cardTitle}>Evolución mensual</p>
                        {evolution.length > 0 ? (
                            <ResponsiveContainer width="100%" height={190}>
                                <BarChart
                                    data={evolution.map(e => ({ ...e, label: shortMonth(e.period) }))}
                                    barSize={22}
                                >
                                    <XAxis
                                        dataKey="label"
                                        tick={{ fill: "var(--color-text-muted)", fontSize: 15 }}
                                        axisLine={false}
                                        tickLine={false}
                                    />
                                    <YAxis hide />
                                    <Tooltip content={<EvolutionTooltip />} cursor={false} />
                                    <Bar dataKey="totalSpent" fill="var(--color-accent)" radius={[3, 3, 0, 0]} />
                                </BarChart>
                            </ResponsiveContainer>
                        ) : (
                            <p className={styles.emptyChart}>Sin datos de evolución</p>
                        )}
                    </div>

                    <div className={styles.card}>
                        <p className={styles.cardTitle}>Por categoría</p>
                        {pieData.length > 0 ? (
                            <div style={{ display: "flex", gap: "1.25rem", alignItems: "flex-start" }}>
                                <PieChart width={100} height={100}>
                                    <Pie
                                        data={pieData}
                                        dataKey="spent"
                                        cx={46}
                                        cy={46}
                                        innerRadius={28}
                                        outerRadius={46}
                                        strokeWidth={2}
                                        stroke="var(--color-surface)"
                                    >
                                        {pieData.map((entry, i) => (
                                            <Cell key={i} fill={entry.color || "var(--color-accent)"} />
                                        ))}
                                    </Pie>
                                </PieChart>
                                <div className={styles.categoryList}>
                                    {[...pieData]
                                        .sort((a, b) => b.spent - a.spent)
                                        .map((cat, i) => (
                                            <CategoryRow key={i} cat={cat} max={maxSpent} />
                                        ))}
                                </div>
                            </div>
                        ) : (
                            <p className={styles.emptyChart}>Sin categorías este mes</p>
                        )}
                    </div>
                </div>
            )}

            {/* table */}
            {!loading && expenses.length > 0 && (
                <div className={styles.tableCard}>
                    <div className={styles.tableHeader}>
                        <p className={styles.cardTitle} style={{ margin: 0 }}>Gastos del mes</p>
                        <div className={styles.tableControls}>
                            <div className={styles.tableFilters}>
                                <div className={styles.selectWrapper}>
                                    <select
                                        className={styles.filterSelect}
                                        value={selectedCategory ?? ""}
                                        onChange={e => {
                                            setSelectedCategory(e.target.value || null);
                                            setPage(0);
                                        }}
                                    >
                                        <option value="">Todas las categorías</option>
                                        {uniqueCategories.map(cat => (
                                            <option key={cat} value={cat}>{cat}</option>
                                        ))}
                                    </select>
                                </div>
                                <div className={styles.selectWrapper}>
                                    <select
                                        className={styles.filterSelect}
                                        value={sortBy}
                                        onChange={e => {
                                            setSortBy(e.target.value as typeof sortBy);
                                            setPage(0);
                                        }}
                                    >
                                        <option value="date">Ordenar por fecha</option>
                                        <option value="amount">Ordenar por importe</option>
                                        <option value="category">Ordenar por categoría</option>
                                        <option value="description">Ordenar por descripción</option>
                                    </select>
                                </div>
                                <button
                                    className={styles.sortDirBtn}
                                    onClick={() => setSortDir(d => d === "asc" ? "desc" : "asc")}
                                    title={sortDir === "asc" ? "Ascendente" : "Descendente"}
                                >
                                    {sortDir === "asc" ? "↑" : "↓"}
                                </button>
                            </div>
                            <span className={styles.tableCount}>
                                {filteredExpenses.length} transacciones
                            </span>
                        </div>
                    </div>
                    <table className={styles.table}>
                        <thead>
                        <tr>
                            <th>Fecha</th>
                            <th>Descripción</th>
                            <th>Categoría</th>
                            <th>Importe</th>
                        </tr>
                        </thead>
                        <tbody>
                        {pagedExpenses.map((exp) => (
                            <tr key={exp.id} className={styles.tableRow}>
                                <td className={styles.tdDate}>
                                    {new Date(exp.incurredOn).toLocaleDateString("es-ES", {
                                        day: "2-digit",
                                        month: "short"
                                    })}
                                </td>
                                <td className={styles.tdDesc}>{exp.description || "—"}</td>
                                <td className={styles.tdCategory}>
                                        <span
                                            className={styles.categoryBadge}
                                            style={{
                                                background: `${exp.categoryColor}22`,
                                                color: exp.categoryColor || "var(--color-accent)"
                                            }}
                                        >
                                            {exp.category}
                                        </span>
                                </td>
                                <td className={styles.tdAmount}>{fmt(exp.amount, exp.currency)}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                    {totalPages > 1 && (
                        <div className={styles.pagination}>
                            <button
                                className={styles.paginationBtn}
                                onClick={() => setPage(p => Math.max(0, p - 1))}
                                disabled={page === 0}
                            >← Anterior</button>
                            <span className={styles.paginationLabel}>{page + 1} / {totalPages}</span>
                            <button
                                className={styles.paginationBtn}
                                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                                disabled={page === totalPages - 1}
                            >Siguiente →</button>
                        </div>
                    )}
                </div>
            )}

            {/* empty */}
            {!loading && !error && expenses.length === 0 && (
                <div className={styles.emptyState}>
                    <p className={styles.emptyTitle}>No hay gastos para este mes.</p>
                    <p className={styles.emptySub}>Importa un CSV de Revolut para empezar.</p>
                </div>
            )}
        </div>
    );
}