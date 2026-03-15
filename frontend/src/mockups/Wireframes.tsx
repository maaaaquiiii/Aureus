import { useState } from "react";

const screens = ["Dashboard", "Importar CSV", "Análisis LLM", "Presupuestos"];

const mockCategories = [
    { name: "Restaurantes", amount: 340, budget: 200, color: "#f87171" },
    { name: "Transporte", amount: 89, budget: 150, color: "#60a5fa" },
    { name: "Supermercado", amount: 210, budget: 250, color: "#34d399" },
    { name: "Ocio", amount: 175, budget: 100, color: "#fbbf24" },
    { name: "Suscripciones", amount: 45, budget: 60, color: "#a78bfa" },
];

const mockImports = [
    { file: "revolut-feb-2025.csv", date: "01/03/2025", rows: 47, status: "DONE" },
    { file: "revolut-ene-2025.csv", date: "02/02/2025", rows: 53, status: "DONE" },
    { file: "revolut-dic-2024.csv", date: "01/01/2025", rows: 38, status: "FAILED" },
];

const mockAnalysis = `Durante marzo de 2025 has gastado un total de 859 €, lo que supone un incremento del 12% respecto al mes anterior.

Tu mayor gasto ha sido en Restaurantes (340 €), superando en un 70% el presupuesto que tenías definido. Te recomiendo revisar este hábito si quieres ajustarte a tu planificación mensual.

Por el contrario, has estado por debajo del presupuesto en Transporte y Supermercado, lo que compensa parcialmente el exceso anterior.

El gasto en Ocio (175 €) también supera el límite establecido (100 €). Considera si se trata de un gasto puntual o un patrón a controlar.`;

// ── Subcomponentes ──────────────────────────────────────────

function Navbar({ active, setActive }: { active: string; setActive: (s: string) => void }) {
    return (
        <div style={{
            background: "#1e1e2e", borderBottom: "1px solid #2a2a3e",
            display: "flex", alignItems: "center", padding: "0 24px",
            height: 52, gap: 8, flexShrink: 0,
        }}>
      <span style={{ color: "#fbbf24", fontWeight: 700, fontSize: 16, marginRight: 24, letterSpacing: "0.05em" }}>
        ✦ AUREUS
      </span>
            {screens.map((s) => (
                <button key={s} onClick={() => setActive(s)} style={{
                    background: active === s ? "#2a2a3e" : "transparent",
                    border: "none", color: active === s ? "#e2e8f0" : "#64748b",
                    padding: "6px 14px", borderRadius: 6, cursor: "pointer",
                    fontSize: 13, fontWeight: active === s ? 600 : 400,
                }}>{s}</button>
            ))}
        </div>
    );
}

function BarChart({ categories }: { categories: typeof mockCategories }) {
    const max = Math.max(...categories.map((c) => Math.max(c.amount, c.budget)));
    return (
        <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
            {categories.map((c) => (
                <div key={c.name}>
                    <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}>
                        <span style={{ fontSize: 12, color: "#94a3b8" }}>{c.name}</span>
                        <span style={{ fontSize: 12, color: c.amount > c.budget ? "#f87171" : "#34d399", fontWeight: 600 }}>
              {c.amount} € / {c.budget} €
            </span>
                    </div>
                    <div style={{ position: "relative", height: 8, background: "#2a2a3e", borderRadius: 4 }}>
                        <div style={{
                            position: "absolute", height: "100%", borderRadius: 4,
                            width: `${(c.amount / max) * 100}%`,
                            background: c.amount > c.budget ? "#f87171" : c.color,
                        }} />
                        <div style={{
                            position: "absolute", height: "100%", width: 2,
                            left: `${(c.budget / max) * 100}%`,
                            background: "#64748b",
                        }} />
                    </div>
                </div>
            ))}
        </div>
    );
}

function MonthLine() {
    const data = [520, 610, 780, 690, 920, 859];
    const months = ["Oct", "Nov", "Dic", "Ene", "Feb", "Mar"];
    const max = Math.max(...data);
    const min = Math.min(...data);
    const w = 340, h = 80, pad = 10;
    const points = data.map((v, i) => {
        const x = pad + (i / (data.length - 1)) * (w - pad * 2);
        const y = h - pad - ((v - min) / (max - min)) * (h - pad * 2);
        return `${x},${y}`;
    }).join(" ");
    return (
        <svg width={w} height={h} style={{ overflow: "visible" }}>
            <polyline points={points} fill="none" stroke="#fbbf24" strokeWidth={2} />
            {data.map((v, i) => {
                const x = pad + (i / (data.length - 1)) * (w - pad * 2);
                const y = h - pad - ((v - min) / (max - min)) * (h - pad * 2);
                return (
                    <g key={i}>
                        <circle cx={x} cy={y} r={4} fill="#fbbf24" />
                        <text x={x} y={h + 14} textAnchor="middle" fontSize={10} fill="#64748b">{months[i]}</text>
                    </g>
                );
            })}
        </svg>
    );
}

function Dashboard() {
    const total = mockCategories.reduce((s, c) => s + c.amount, 0);
    const totalBudget = mockCategories.reduce((s, c) => s + c.budget, 0);
    return (
        <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
            <div style={{ display: "flex", gap: 16 }}>
                {[
                    { label: "Gasto total", value: `${total} €`, sub: "marzo 2025", color: "#fbbf24" },
                    { label: "Presupuesto", value: `${totalBudget} €`, sub: "definido", color: "#60a5fa" },
                    { label: "Desviación", value: `+${total - totalBudget} €`, sub: "sobre el límite", color: "#f87171" },
                    { label: "Importaciones", value: "3", sub: "este mes", color: "#34d399" },
                ].map((card) => (
                    <div key={card.label} style={{
                        flex: 1, background: "#1e1e2e", border: "1px solid #2a2a3e",
                        borderRadius: 10, padding: "14px 16px",
                    }}>
                        <div style={{ fontSize: 11, color: "#64748b", marginBottom: 4 }}>{card.label}</div>
                        <div style={{ fontSize: 22, fontWeight: 700, color: card.color }}>{card.value}</div>
                        <div style={{ fontSize: 11, color: "#475569", marginTop: 2 }}>{card.sub}</div>
                    </div>
                ))}
            </div>
            <div style={{ display: "flex", gap: 16 }}>
                <div style={{ flex: 1.2, background: "#1e1e2e", border: "1px solid #2a2a3e", borderRadius: 10, padding: 16 }}>
                    <div style={{ fontSize: 12, color: "#94a3b8", fontWeight: 600, marginBottom: 16 }}>GASTO VS PRESUPUESTO</div>
                    <BarChart categories={mockCategories} />
                </div>
                <div style={{ flex: 1, background: "#1e1e2e", border: "1px solid #2a2a3e", borderRadius: 10, padding: 16 }}>
                    <div style={{ fontSize: 12, color: "#94a3b8", fontWeight: 600, marginBottom: 16 }}>EVOLUCIÓN MENSUAL</div>
                    <MonthLine />
                    <div style={{ marginTop: 28, fontSize: 11, color: "#475569" }}>Últimos 6 meses</div>
                </div>
            </div>
        </div>
    );
}

function ImportScreen() {
    const [dragging, setDragging] = useState(false);
    return (
        <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
            <div
                onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
                onDragLeave={() => setDragging(false)}
                style={{
                    border: `2px dashed ${dragging ? "#fbbf24" : "#2a2a3e"}`,
                    borderRadius: 12, padding: "40px 24px", textAlign: "center",
                    background: dragging ? "#1e1e2e" : "transparent", transition: "all 0.2s", cursor: "pointer",
                }}>
                <div style={{ fontSize: 32, marginBottom: 12 }}>📂</div>
                <div style={{ color: "#e2e8f0", fontWeight: 600, marginBottom: 6 }}>Arrastra tu CSV de Revolut aquí</div>
                <div style={{ color: "#475569", fontSize: 12, marginBottom: 16 }}>Formato: date, category, amount, description</div>
                <button style={{
                    background: "#fbbf24", color: "#0f0f1a", border: "none",
                    borderRadius: 6, padding: "8px 20px", fontWeight: 700, fontSize: 13, cursor: "pointer",
                }}>Seleccionar archivo</button>
            </div>
            <div style={{ background: "#1e1e2e", border: "1px solid #2a2a3e", borderRadius: 10, padding: 16 }}>
                <div style={{ fontSize: 12, color: "#94a3b8", fontWeight: 600, marginBottom: 14 }}>HISTORIAL DE IMPORTACIONES</div>
                <table style={{ width: "100%", borderCollapse: "collapse" }}>
                    <thead>
                    <tr>
                        {["Archivo", "Fecha", "Filas", "Estado"].map((h) => (
                            <th key={h} style={{ textAlign: "left", fontSize: 11, color: "#475569", paddingBottom: 8, borderBottom: "1px solid #2a2a3e" }}>{h}</th>
                        ))}
                    </tr>
                    </thead>
                    <tbody>
                    {mockImports.map((imp, i) => (
                        <tr key={i}>
                            <td style={{ padding: "10px 0", fontSize: 12, color: "#e2e8f0" }}>{imp.file}</td>
                            <td style={{ fontSize: 12, color: "#64748b" }}>{imp.date}</td>
                            <td style={{ fontSize: 12, color: "#64748b" }}>{imp.rows}</td>
                            <td>
                  <span style={{
                      fontSize: 11, fontWeight: 600, padding: "2px 8px", borderRadius: 4,
                      background: imp.status === "DONE" ? "#14532d" : "#450a0a",
                      color: imp.status === "DONE" ? "#34d399" : "#f87171",
                  }}>{imp.status}</span>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}

function AnalysisScreen() {
    const [loading, setLoading] = useState(false);
    const [shown, setShown] = useState(true);
    const handleGenerate = () => {
        setShown(false); setLoading(true);
        setTimeout(() => { setLoading(false); setShown(true); }, 1800);
    };
    return (
        <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
            <div style={{ display: "flex", gap: 12, alignItems: "center" }}>
                <select style={{
                    background: "#1e1e2e", border: "1px solid #2a2a3e", color: "#e2e8f0",
                    borderRadius: 6, padding: "8px 12px", fontSize: 13,
                }}>
                    <option>Marzo 2025</option>
                    <option>Febrero 2025</option>
                    <option>Enero 2025</option>
                </select>
                <button onClick={handleGenerate} style={{
                    background: "#fbbf24", color: "#0f0f1a", border: "none",
                    borderRadius: 6, padding: "8px 20px", fontWeight: 700, fontSize: 13, cursor: "pointer",
                }}>
                    {loading ? "Analizando..." : "✦ Generar análisis"}
                </button>
                <span style={{ fontSize: 11, color: "#475569" }}>Modelo: gpt-4.1 · Cacheado</span>
            </div>
            <div style={{ background: "#1e1e2e", border: "1px solid #2a2a3e", borderRadius: 10, padding: 20, minHeight: 200 }}>
                <div style={{ fontSize: 12, color: "#94a3b8", fontWeight: 600, marginBottom: 16 }}>✦ ANÁLISIS GENERADO POR IA</div>
                {loading ? (
                    <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
                        {[100, 80, 90, 60].map((w, i) => (
                            <div key={i} style={{ height: 12, borderRadius: 4, background: "#2a2a3e", width: `${w}%` }} />
                        ))}
                    </div>
                ) : shown ? (
                    <p style={{ color: "#cbd5e1", fontSize: 13, lineHeight: 1.8, margin: 0, whiteSpace: "pre-line" }}>
                        {mockAnalysis}
                    </p>
                ) : null}
            </div>
            <div style={{ background: "#1e1e2e", border: "1px solid #2a2a3e", borderRadius: 10, padding: 16 }}>
                <div style={{ fontSize: 12, color: "#94a3b8", fontWeight: 600, marginBottom: 10 }}>PROMPT ENVIADO A GPT</div>
                <pre style={{ color: "#475569", fontSize: 11, margin: 0, whiteSpace: "pre-wrap", lineHeight: 1.6 }}>
{`Analiza los gastos del usuario para marzo 2025:
- Total gastado: 859 €
- Presupuesto total: 760 €
- Desglose: Restaurantes 340€ (límite 200€), 
  Transporte 89€ (límite 150€)...

Dame un análisis en español, conciso y accionable.`}
        </pre>
            </div>
        </div>
    );
}

function BudgetsScreen() {
    return (
        <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <span style={{ color: "#94a3b8", fontSize: 12, fontWeight: 600 }}>PRESUPUESTOS — MARZO 2025</span>
                <button style={{
                    background: "#fbbf24", color: "#0f0f1a", border: "none",
                    borderRadius: 6, padding: "6px 14px", fontWeight: 700, fontSize: 12, cursor: "pointer",
                }}>+ Nuevo presupuesto</button>
            </div>
            {mockCategories.map((c) => {
                const pct = Math.min((c.amount / c.budget) * 100, 100);
                const over = c.amount > c.budget;
                return (
                    <div key={c.name} style={{
                        background: "#1e1e2e", border: `1px solid ${over ? "#450a0a" : "#2a2a3e"}`,
                        borderRadius: 10, padding: 16,
                    }}>
                        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 10 }}>
                            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                                <div style={{ width: 10, height: 10, borderRadius: "50%", background: c.color }} />
                                <span style={{ color: "#e2e8f0", fontSize: 13, fontWeight: 600 }}>{c.name}</span>
                                {over && (
                                    <span style={{ fontSize: 10, background: "#450a0a", color: "#f87171", padding: "2px 6px", borderRadius: 4, fontWeight: 700 }}>
                    EXCEDIDO
                  </span>
                                )}
                            </div>
                            <div style={{ display: "flex", gap: 16, alignItems: "center" }}>
                                <span style={{ fontSize: 12, color: "#64748b" }}>{c.amount} € gastado</span>
                                <span style={{ fontSize: 12, color: "#94a3b8", fontWeight: 600 }}>Límite: {c.budget} €</span>
                                <button style={{
                                    background: "transparent", border: "1px solid #2a2a3e", color: "#64748b",
                                    borderRadius: 4, padding: "3px 10px", fontSize: 11, cursor: "pointer",
                                }}>Editar</button>
                            </div>
                        </div>
                        <div style={{ height: 6, background: "#2a2a3e", borderRadius: 3 }}>
                            <div style={{ height: "100%", borderRadius: 3, width: `${pct}%`, background: over ? "#f87171" : c.color }} />
                        </div>
                        <div style={{ fontSize: 11, color: over ? "#f87171" : "#475569", marginTop: 6 }}>
                            {over ? `${c.amount - c.budget} € por encima del límite` : `Quedan ${c.budget - c.amount} €`}
                        </div>
                    </div>
                );
            })}
        </div>
    );
}

// ── Componente principal ─────────────────────────────────────

export default function Wireframes() {
    const [active, setActive] = useState("Dashboard");
    return (
        <div style={{
            background: "#0f0f1a", minHeight: "100vh",
            fontFamily: "monospace", display: "flex", flexDirection: "column",
        }}>
            <Navbar active={active} setActive={setActive} />
            <div style={{ padding: 24, flex: 1, maxWidth: 960, width: "100%", margin: "0 auto" }}>
                <div style={{ marginBottom: 20 }}>
                    <div style={{ fontSize: 18, fontWeight: 700, color: "#e2e8f0" }}>{active}</div>
                    <div style={{ fontSize: 11, color: "#475569", marginTop: 2 }}>
                        Aureus · Wireframe — datos de ejemplo
                    </div>
                </div>
                {active === "Dashboard"     && <Dashboard />}
                {active === "Importar CSV"  && <ImportScreen />}
                {active === "Análisis LLM"  && <AnalysisScreen />}
                {active === "Presupuestos"  && <BudgetsScreen />}
            </div>
        </div>
    );
}