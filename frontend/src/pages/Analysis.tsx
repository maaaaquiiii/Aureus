import { useState, useEffect } from "react";
import { analyseMonth, getCachedAnalysis, deleteAnalysis } from "../api/services";
import type { LlmAnalysis } from "../api/types";
import styles from "./Analysis.module.css";

function previousMonth(): string {
    const d = new Date();
    d.setMonth(d.getMonth() - 1);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
}

function currentMonth(): string {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
}

function fullMonthLabel(month: string): string {
    const [y, m] = month.split("-");
    return new Date(Number(y), Number(m) - 1)
        .toLocaleDateString("es-ES", { month: "long", year: "numeric" });
}

// Renders markdown-like text: **bold**, ### headers and newlines
function AnalysisText({ text }: { text: string }) {
    const lines = text.split("\n");
    return (
        <div className={styles.analysisText}>
            {lines.map((line, i) => {
                if (line.startsWith("### ")) {
                    return <h3 key={i} className={styles.analysisH3}>{line.replace("### ", "")}</h3>;
                }
                if (line.startsWith("## ")) {
                    return <h2 key={i} className={styles.analysisH2}>{line.replace("## ", "")}</h2>;
                }
                if (line.startsWith("# ")) {
                    return <h2 key={i} className={styles.analysisH2}>{line.replace("# ", "")}</h2>;
                }
                // Render **bold** inline
                const parts = line.split(/(\*\*[^*]+\*\*)/g);
                return (
                    <p key={i} className={line.startsWith("- ") ? styles.analysisBullet : styles.analysisParagraph}>
                        {parts.map((part, j) =>
                            part.startsWith("**") && part.endsWith("**")
                                ? <strong key={j}>{part.slice(2, -2)}</strong>
                                : part
                        )}
                    </p>
                );
            })}
        </div>
    );
}

export default function Analysis() {
    const [month, setMonth] = useState<string>(previousMonth());
    const [analysis, setAnalysis] = useState<LlmAnalysis | null>(null);
    const [loading, setLoading] = useState(false);
    const [checking, setChecking] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // Check for a cached analysis when the month changes
    useEffect(() => {
        setAnalysis(null);
        setError(null);
        setChecking(true);
        getCachedAnalysis(month)
            .then(cached => setAnalysis(cached))
            .finally(() => setChecking(false));
    }, [month]);

    const handleAnalyse = async () => {
        setLoading(true);
        setError(null);
        try {
            const result = await analyseMonth(month);
            setAnalysis(result);
        } catch {
            setError("No se pudo generar el análisis. Comprueba que tienes gastos importados para este mes.");
        } finally {
            setLoading(false);
        }
    };

    const handleRegenerate = async () => {
        setLoading(true);
        setError(null);
        try {
            await deleteAnalysis(month);
            const result = await analyseMonth(month);
            setAnalysis(result);
        } catch {
            setError("No se pudo regenerar el análisis.");
        } finally {
            setLoading(false);
        }
    };

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

    return (
        <div className={styles.page}>
            <div className={styles.pageHeader}>
                <div>
                    <h1 className={styles.pageTitle}>Análisis IA</h1>
                    <p className={styles.pageSubtitle}>Insights personalizados sobre tus finanzas</p>
                </div>
                <div className={styles.monthPicker}>
                    <button className={styles.iconBtn} onClick={prevMonth}>‹</button>
                    <span className={styles.monthLabel}>{fullMonthLabel(month)}</span>
                    <button className={styles.iconBtn} onClick={nextMonth} disabled={month === currentMonth()}>›</button>
                </div>
            </div>

            {error && <div className={styles.error}>{error}</div>}

            {checking && (
                <div className={styles.card}>
                    <div className={styles.skeleton} style={{ height: 24, width: "40%", marginBottom: "1rem" }} />
                    <div className={styles.skeleton} style={{ height: 16, width: "100%", marginBottom: "0.5rem" }} />
                    <div className={styles.skeleton} style={{ height: 16, width: "85%" }} />
                </div>
            )}

            {!checking && !analysis && !loading && (
                <div className={styles.emptyCard}>
                    <p className={styles.emptyTitle}>No hay análisis para {fullMonthLabel(month)}</p>
                    <p className={styles.emptySub}>
                        Genera un análisis con IA basado en tus gastos importados de este mes.
                    </p>
                    <button className={styles.btnPrimary} onClick={handleAnalyse}>
                        Analizar mes
                    </button>
                </div>
            )}

            {loading && (
                <div className={styles.card}>
                    <div className={styles.loadingState}>
                        <div className={styles.loadingDots}>
                            <span /><span /><span />
                        </div>
                        <p className={styles.loadingText}>Analizando tus finanzas...</p>
                    </div>
                </div>
            )}

            {!loading && analysis && (
                <div className={styles.card}>
                    <div className={styles.analysisHeader}>
                        <div className={styles.analysisMeta}>
                            <span className={styles.metaModel}>{analysis.modelUsed}</span>
                            {analysis.cached && (
                                <span className={styles.metaCached}>guardado</span>
                            )}
                            <span className={styles.metaDate}>
                                {new Date(analysis.createdAt).toLocaleDateString("es-ES", {
                                    day: "numeric", month: "short", year: "numeric"
                                })}
                            </span>
                            {analysis.tokensUsed && (
                                <span className={styles.metaTokens}>{analysis.tokensUsed} tokens</span>
                            )}
                        </div>
                        <button
                            className={styles.btnRegenerate}
                            onClick={handleRegenerate}
                            title="Regenerar análisis"
                        >
                            ↺ Regenerar
                        </button>
                    </div>
                    <AnalysisText text={analysis.analysis} />
                </div>
            )}
        </div>
    );
}