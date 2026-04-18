import { useEffect, useState } from "react";
import { useAuth } from "../context/useAuth";
import { getUserStats, updateUser } from "../api/services";
import type { UserStats } from "../api/types";
import styles from "./Profile.module.css";

function EyeOpen() {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
            <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"/>
            <circle cx="12" cy="12" r="3"/>
        </svg>
    );
}

function EyeClosed() {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
            <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
            <line x1="1" y1="1" x2="23" y2="23"/>
        </svg>
    );
}

function fmt(amount: number): string {
    return new Intl.NumberFormat("es-ES", {
        style: "currency",
        currency: "EUR",
        maximumFractionDigits: 2,
    }).format(amount);
}

export default function Profile() {
    const { user } = useAuth();

    const [stats, setStats] = useState<UserStats | null>(null);
    const [statsLoading, setStatsLoading] = useState(true);

    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [currency, setCurrency] = useState(user?.currency ?? "EUR");
    const [showPassword, setShowPassword] = useState(false);
    const [saving, setSaving] = useState(false);
    const [success, setSuccess] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        getUserStats()
            .then(setStats)
            .catch(() => {})
            .finally(() => setStatsLoading(false));
    }, []);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        setSuccess(null);

        if (password && password !== confirmPassword) {
            setError("Las contraseñas no coinciden.");
            return;
        }
        if (password && password.length < 6) {
            setError("La contraseña debe tener al menos 6 caracteres.");
            return;
        }

        const payload: { password?: string; currency?: string } = {};
        if (password) payload.password = password;
        if (currency !== user?.currency) payload.currency = currency;

        if (Object.keys(payload).length === 0) {
            setError("No has modificado ningún campo.");
            return;
        }

        setSaving(true);
        try {
            await updateUser(payload);
            setSuccess("Perfil actualizado correctamente.");
            setPassword("");
            setConfirmPassword("");
        } catch {
            setError("No se pudo actualizar el perfil.");
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className={styles.page}>
            <div className={styles.pageHeader}>
                <h1 className={styles.pageTitle}>Mi perfil</h1>
                <p className={styles.pageSubtitle}>Información de tu cuenta</p>
            </div>

            {/* stats */}
            <div className={styles.statsGrid}>
                {statsLoading ? (
                    [0, 1, 2, 3].map(i => (
                        <div key={i} className={styles.skeleton} style={{ height: 100 }} />
                    ))
                ) : stats ? (
                    <>
                        <div className={styles.statCard}>
                            <p className={styles.statLabel}>Total gastado</p>
                            <p className={styles.statValue}>{fmt(stats.totalSpent)}</p>
                            <p className={styles.statSub}>acumulado</p>
                        </div>
                        <div className={styles.statCard}>
                            <p className={styles.statLabel}>Transacciones</p>
                            <p className={styles.statValue}>{stats.totalExpenses}</p>
                            <p className={styles.statSub}>registradas</p>
                        </div>
                        <div className={styles.statCard}>
                            <p className={styles.statLabel}>Importaciones</p>
                            <p className={styles.statValue}>{stats.totalImports}</p>
                            <p className={styles.statSub}>archivos CSV</p>
                        </div>
                        <div className={styles.statCard}>
                            <p className={styles.statLabel}>Miembro desde</p>
                            <p className={styles.statValue} style={{ fontSize: "1.25rem", textTransform: "capitalize" }}>
                                {stats.memberSince}
                            </p>
                            <p className={styles.statSub}>{user?.email}</p>
                        </div>
                    </>
                ) : null}
            </div>

            {/* edit form */}
            <div className={styles.card}>
                <p className={styles.cardTitle}>Editar cuenta</p>

                {success && <div className={styles.success}>{success}</div>}
                {error && <div className={styles.error}>{error}</div>}

                <form className={styles.form} onSubmit={handleSubmit}>
                    <div className={styles.field}>
                        <label className={styles.label}>Nombre</label>
                        <span className={styles.readonlyValue}>{user?.name}</span>
                    </div>

                    <div className={styles.field}>
                        <label className={styles.label}>Email</label>
                        <span className={styles.readonlyValue}>{user?.email}</span>
                    </div>

                    <div className={styles.field}>
                        <label className={styles.label}>Moneda</label>
                        <select
                            className={styles.select}
                            value={currency}
                            onChange={e => setCurrency(e.target.value)}
                        >
                            <option value="EUR">EUR — Euro</option>
                            <option value="USD">USD — Dólar</option>
                            <option value="GBP">GBP — Libra</option>
                        </select>
                    </div>

                    <div className={styles.field}>
                        <label className={styles.label}>Nueva contraseña</label>
                        <div className={styles.passwordWrapper}>
                            <input
                                type={showPassword ? "text" : "password"}
                                className={styles.input}
                                placeholder="Dejar en blanco para no cambiar"
                                value={password}
                                onChange={e => setPassword(e.target.value)}
                                autoComplete="new-password"
                            />
                            <button
                                type="button"
                                className={styles.eyeBtn}
                                onClick={() => setShowPassword(v => !v)}
                                tabIndex={-1}
                            >
                                {showPassword ? <EyeOpen /> : <EyeClosed />}
                            </button>
                        </div>
                    </div>

                    {password && (
                        <div className={styles.field}>
                            <label className={styles.label}>Confirmar contraseña</label>
                            <div className={styles.passwordWrapper}>
                                <input
                                    type={showPassword ? "text" : "password"}
                                    className={styles.input}
                                    placeholder="Repite la nueva contraseña"
                                    value={confirmPassword}
                                    onChange={e => setConfirmPassword(e.target.value)}
                                    autoComplete="new-password"
                                />
                            </div>
                        </div>
                    )}

                    <div className={styles.formActions}>
                        <button
                            type="submit"
                            className={styles.btnPrimary}
                            disabled={saving}
                        >
                            {saving ? "Guardando..." : "Guardar cambios"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}