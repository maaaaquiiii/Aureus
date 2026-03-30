import { useEffect, useState } from "react";
import { getBudgets, createBudget, updateBudget, deleteBudget, getCategories } from "../api/services";
import type { Budget, Category } from "../api/types";
import styles from "./Budgets.module.css";

// ─── helpers ──────────────────────────────────────────────────────────────────

function currentPeriod(): string {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
}

function fmt(amount: number): string {
    return new Intl.NumberFormat("es-ES", { style: "currency", currency: "EUR", maximumFractionDigits: 2 }).format(amount);
}

function periodLabel(period: string): string {
    const [y, m] = period.split("-");
    return new Date(Number(y), Number(m) - 1).toLocaleDateString("es-ES", { month: "long", year: "numeric" });
}

// ─── form type ────────────────────────────────────────────────────────────────

interface BudgetForm {
    categoryId: number | "";
    period: string;
    limitAmount: number | "";
}

const EMPTY_FORM: BudgetForm = { categoryId: "", period: currentPeriod(), limitAmount: "" };

// ─── budget modal ─────────────────────────────────────────────────────────────

interface ModalProps {
    title: string;
    onClose: () => void;
    onSubmit: () => void;
    form: BudgetForm;
    setForm: (f: BudgetForm) => void;
    saving: boolean;
    error: string | null;
    categories: Category[];
}

function BudgetModal({ title, onClose, onSubmit, form, setForm, saving, error, categories }: ModalProps) {
    return (
        <div className={styles.overlay} onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
            <div className={styles.modal}>
                <div className={styles.modalHeader}>
                    <h2 className={styles.modalTitle}>{title}</h2>
                    <button className={styles.modalClose} onClick={onClose}>✕</button>
                </div>

                {error && <div className={styles.formError}>{error}</div>}

                <div className={styles.formFields}>
                    <div>
                        <label className={styles.fieldLabel}>Categoría</label>
                        <select
                            value={form.categoryId}
                            onChange={(e) => setForm({ ...form, categoryId: e.target.value === "" ? "" : Number(e.target.value) })}
                            className={styles.fieldInput}
                        >
                            <option value="">Selecciona una categoría</option>
                            {categories.map((cat) => (
                                <option key={cat.id} value={cat.id}>{cat.name}</option>
                            ))}
                        </select>
                    </div>
                    <div>
                        <label className={styles.fieldLabel}>Período</label>
                        <input
                            type="month"
                            value={form.period}
                            onChange={(e) => setForm({ ...form, period: e.target.value })}
                            className={styles.fieldInput}
                        />
                    </div>
                    <div>
                        <label className={styles.fieldLabel}>Límite (€)</label>
                        <input
                            type="number"
                            value={form.limitAmount}
                            onChange={(e) => setForm({ ...form, limitAmount: e.target.value === "" ? "" : Number(e.target.value) })}
                            placeholder="300"
                            min={0}
                            className={styles.fieldInput}
                        />
                    </div>
                </div>

                <div className={styles.modalFooter}>
                    <button className={styles.btnCancel} onClick={onClose}>Cancelar</button>
                    <button className={styles.btnPrimary} onClick={onSubmit} disabled={saving}>
                        {saving ? "Guardando..." : "Guardar"}
                    </button>
                </div>
            </div>
        </div>
    );
}

// ─── confirm modal ────────────────────────────────────────────────────────────

function ConfirmModal({ onClose, onConfirm, saving }: { onClose: () => void; onConfirm: () => void; saving: boolean }) {
    return (
        <div className={styles.overlay} onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
            <div className={styles.modal} style={{ width: 360 }}>
                <div className={styles.modalHeader}>
                    <h2 className={styles.modalTitle}>¿Eliminar presupuesto?</h2>
                    <button className={styles.modalClose} onClick={onClose}>✕</button>
                </div>
                <p style={{ color: "var(--color-text-secondary)", fontSize: "0.875rem", margin: "0 0 1.75rem", fontWeight: 300 }}>
                    Esta acción no se puede deshacer.
                </p>
                <div className={styles.modalFooter}>
                    <button className={styles.btnCancel} onClick={onClose}>Cancelar</button>
                    <button className={styles.btnDelete} onClick={onConfirm} disabled={saving}>
                        {saving ? "Eliminando..." : "Eliminar"}
                    </button>
                </div>
            </div>
        </div>
    );
}

// ─── budget row ───────────────────────────────────────────────────────────────

interface BudgetRowProps {
    budget: Budget;
    onEdit: () => void;
    onDelete: () => void;
}

function BudgetRow({ budget, onEdit, onDelete }: BudgetRowProps) {
    return (
        <div className={styles.budgetRow}>
            <div className={styles.dot} style={{ background: budget.categoryColor || "var(--color-accent)" }} />
            <div className={styles.budgetInfo}>
                <p className={styles.budgetCategory}>{budget.category}</p>
                <p className={styles.budgetPeriod}>{periodLabel(budget.period)}</p>
            </div>
            <div className={styles.budgetAmount}>
                <p className={styles.budgetAmountValue}>{fmt(budget.limitAmount)}</p>
                <p className={styles.budgetAmountLabel}>límite mensual</p>
            </div>
            <div className={styles.rowActions}>
                <button className={styles.btnSecondary} onClick={onEdit}>Editar</button>
                <button className={styles.btnDanger} onClick={onDelete}>Eliminar</button>
            </div>
        </div>
    );
}

// ─── main ─────────────────────────────────────────────────────────────────────

export default function Budgets() {
    const [period, setPeriod] = useState(currentPeriod());
    const [budgets, setBudgets] = useState<Budget[]>([]);
    const [categories, setCategories] = useState<Category[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const [showCreate, setShowCreate] = useState(false);
    const [editTarget, setEditTarget] = useState<Budget | null>(null);
    const [deleteTarget, setDeleteTarget] = useState<Budget | null>(null);
    const [form, setForm] = useState<BudgetForm>(EMPTY_FORM);
    const [saving, setSaving] = useState(false);
    const [formError, setFormError] = useState<string | null>(null);

    useEffect(() => {
        const fetchCategories = async () => {
            try { setCategories(await getCategories()); } catch { /* no bloqueante */ }
        };
        fetchCategories();
    }, []);

    useEffect(() => {
        const fetchBudgets = async () => {
            setLoading(true);
            setError(null);
            try { setBudgets(await getBudgets(period)); }
            catch { setError("No se pudieron cargar los presupuestos."); }
            finally { setLoading(false); }
        };
        fetchBudgets();
    }, [period]);

    function prevPeriod() {
        const [y, m] = period.split("-").map(Number);
        const d = new Date(y, m - 2);
        setPeriod(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`);
    }

    function nextPeriod() {
        const [y, m] = period.split("-").map(Number);
        const d = new Date(y, m);
        const next = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
        if (next <= currentPeriod()) setPeriod(next);
    }

    function openCreate() {
        setForm({ ...EMPTY_FORM, period });
        setFormError(null);
        setShowCreate(true);
    }

    async function handleCreate() {
        if (form.categoryId === "" || form.limitAmount === "") { setFormError("Rellena todos los campos."); return; }
        setSaving(true); setFormError(null);
        try {
            const created = await createBudget({ categoryId: Number(form.categoryId), period: form.period, limitAmount: Number(form.limitAmount) });
            setBudgets(prev => [...prev, created]);
            setShowCreate(false);
        } catch { setFormError("Error al crear el presupuesto."); }
        finally { setSaving(false); }
    }

    function openEdit(budget: Budget) {
        const match = categories.find(c => c.name === budget.category);
        setForm({ categoryId: match ? match.id : "", period: budget.period, limitAmount: budget.limitAmount });
        setFormError(null);
        setEditTarget(budget);
    }

    async function handleEdit() {
        if (!editTarget || form.categoryId === "" || form.limitAmount === "") { setFormError("Rellena todos los campos."); return; }
        setSaving(true); setFormError(null);
        try {
            const updated = await updateBudget(editTarget.id, { categoryId: Number(form.categoryId), period: form.period, limitAmount: Number(form.limitAmount) });
            setBudgets(prev => prev.map(b => b.id === editTarget.id ? updated : b));
            setEditTarget(null);
        } catch { setFormError("Error al actualizar el presupuesto."); }
        finally { setSaving(false); }
    }

    async function handleDelete() {
        if (!deleteTarget) return;
        setSaving(true);
        try {
            await deleteBudget(deleteTarget.id);
            setBudgets(prev => prev.filter(b => b.id !== deleteTarget.id));
            setDeleteTarget(null);
        } catch { setError("Error al eliminar el presupuesto."); }
        finally { setSaving(false); }
    }

    return (
        <div className={styles.page}>
            <div className={styles.pageHeader}>
                <div>
                    <h1 className={styles.pageTitle}>Presupuestos</h1>
                    <p className={styles.pageSubtitle}>Límites de gasto por categoría</p>
                </div>
                <div className={styles.headerActions}>
                    <div className={styles.periodPicker}>
                        <button className={styles.iconBtn} onClick={prevPeriod}>‹</button>
                        <span className={styles.periodLabel}>{periodLabel(period)}</span>
                        <button className={styles.iconBtn} onClick={nextPeriod} disabled={period === currentPeriod()}>›</button>
                    </div>
                    <button className={styles.btnPrimary} onClick={openCreate}>+ Nuevo</button>
                </div>
            </div>

            {error && <div className={styles.error}>{error}</div>}

            {loading && (
                <div className={styles.list}>
                    {[0,1,2].map(i => <div key={i} className={styles.skeleton} style={{ height: 68 }} />)}
                </div>
            )}

            {!loading && budgets.length > 0 && (
                <div className={styles.list}>
                    {budgets.map(budget => (
                        <BudgetRow key={budget.id} budget={budget} onEdit={() => openEdit(budget)} onDelete={() => setDeleteTarget(budget)} />
                    ))}
                </div>
            )}

            {!loading && !error && budgets.length === 0 && (
                <div className={styles.emptyState}>
                    <p className={styles.emptyTitle}>No hay presupuestos para este período.</p>
                    <p className={styles.emptySub}>Crea uno con el botón + Nuevo.</p>
                </div>
            )}

            {showCreate && <BudgetModal title="Nuevo presupuesto" form={form} setForm={setForm} onClose={() => setShowCreate(false)} onSubmit={handleCreate} saving={saving} error={formError} categories={categories} />}
            {editTarget && <BudgetModal title="Editar presupuesto" form={form} setForm={setForm} onClose={() => setEditTarget(null)} onSubmit={handleEdit} saving={saving} error={formError} categories={categories} />}
            {deleteTarget && <ConfirmModal onClose={() => setDeleteTarget(null)} onConfirm={handleDelete} saving={saving} />}
        </div>
    );
}