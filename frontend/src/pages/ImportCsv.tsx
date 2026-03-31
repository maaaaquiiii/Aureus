import { useState } from "react";
import { importCsv, deleteImportJob, deleteAllImportJobs } from "../api/services";
import type { ImportResponse } from "../api/types";
import styles from "./ImportCsv.module.css";

interface ImportJob {
    jobId: number;
    fileName: string;
    status: string;
    totalRows: number;
    importedRows: number;
    errorDetail: string | null;
    date: string;
}

const HISTORY_KEY = "aureus_import_history";

function loadHistory(): ImportJob[] {
    try {
        const saved = localStorage.getItem(HISTORY_KEY);
        return saved ? JSON.parse(saved) : [];
    } catch {
        return [];
    }
}

function saveHistory(history: ImportJob[]) {
    localStorage.setItem(HISTORY_KEY, JSON.stringify(history));
}

export default function ImportCsv() {
    const [dragging, setDragging] = useState(false);
    const [loading, setLoading] = useState(false);
    const [history, setHistory] = useState<ImportJob[]>(loadHistory);
    const [error, setError] = useState<string | null>(null);
    const [deletingId, setDeletingId] = useState<number | null>(null);

    const addToHistory = (job: ImportJob) => {
        setHistory(prev => {
            const updated = [job, ...prev];
            saveHistory(updated);
            return updated;
        });
    };

    const handleFile = async (file: File) => {
        if (!file.name.endsWith(".csv")) {
            setError("Solo se aceptan archivos .csv");
            return;
        }
        const alreadyImported = history.some(
            job => job.fileName === file.name && job.status === "DONE"
        );
        if (alreadyImported) {
            setError(`El archivo "${file.name}" ya fue importado anteriormente.`);
            return;
        }
        setError(null);
        setLoading(true);
        try {
            const content = await file.text();
            const response: ImportResponse = await importCsv(content, file.name);
            addToHistory({
                jobId: response.jobId,
                fileName: file.name,
                status: response.status,
                totalRows: response.totalRows,
                importedRows: response.importedRows,
                errorDetail: response.errorDetail,
                date: new Date().toLocaleDateString("es-ES"),
            });
        } catch {
            setError("Error al importar el archivo. ¿Está el backend arrancado?");
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (job: ImportJob) => {
        setDeletingId(job.jobId);
        try {
            await deleteImportJob(job.jobId);
            setHistory(prev => {
                const updated = prev.filter(j => j.jobId !== job.jobId);
                saveHistory(updated);
                return updated;
            });
        } catch {
            setError("Error al eliminar la importación.");
        } finally {
            setDeletingId(null);
        }
    };

    const handleClearAll = async () => {
        try {
            await deleteAllImportJobs();
            setHistory([]);
            saveHistory([]);
        } catch {
            setError("Error al limpiar el historial.");
        }
    };

    const handleDrop = (e: React.DragEvent) => {
        e.preventDefault();
        setDragging(false);
        const files = e.dataTransfer.files;
        if (!files || files.length === 0) return;
        Array.from(files).forEach(file => handleFile(file));
    };

    const handleInputChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const files = e.target.files;
        if (!files || files.length === 0) return;
        for (const file of Array.from(files)) {
            await handleFile(file);
        }
    };

    return (
        <div className={styles.page}>
            <div className={styles.pageHeader}>
                <h1 className={styles.pageTitle}>Importar CSV</h1>
                <p className={styles.pageSubtitle}>Sube tu exportación de Revolut en formato CSV</p>
            </div>

            <div
                onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
                onDragLeave={() => setDragging(false)}
                onDrop={handleDrop}
                className={`${styles.dropZone} ${dragging ? styles.dropZoneActive : ""}`}
            >
                <div className={styles.dropIcon}>📂</div>
                <p className={styles.dropTitle}>Arrastra tu CSV de Revolut aquí</p>
                <p className={styles.dropSubtitle}>
                    Formato: Type, Product, Started Date, Description, Amount, Currency...
                </p>
                <label className={styles.fileLabel}>
                    <span className={styles.btnPrimary}>
                        {loading ? "Importando..." : "Seleccionar archivo"}
                    </span>
                    <input
                        type="file"
                        accept=".csv"
                        multiple
                        className={styles.fileInput}
                        onChange={handleInputChange}
                        disabled={loading}
                    />
                </label>
            </div>

            {error && (
                <div className={styles.error}>
                    <p>{error}</p>
                </div>
            )}

            <div className={styles.card}>
                <div className={styles.cardHeader}>
                    <p className={styles.cardTitle}>HISTORIAL DE IMPORTACIONES</p>
                    {history.length > 0 && (
                        <button className={styles.btnClear} onClick={handleClearAll}>
                            Limpiar todo
                        </button>
                    )}
                </div>
                {history.length === 0 ? (
                    <p className={styles.emptyText}>No hay importaciones registradas</p>
                ) : (
                    <table className={styles.table}>
                        <thead>
                        <tr>
                            {["Archivo", "Fecha", "Filas", "Estado", ""].map((h, i) => (
                                <th key={i} className={styles.th}>{h}</th>
                            ))}
                        </tr>
                        </thead>
                        <tbody>
                        {history.map((job) => (
                            <tr key={job.jobId} className={styles.tableRow}>
                                <td className={styles.tdFile}>{job.fileName}</td>
                                <td className={styles.td}>{job.date}</td>
                                <td className={styles.td}>{job.importedRows} / {job.totalRows}</td>
                                <td className={styles.td}>
                                        <span className={`${styles.badge} ${job.status === "DONE" ? styles.badgeDone : styles.badgeFailed}`}>
                                            {job.status}
                                        </span>
                                </td>
                                <td className={styles.tdActions}>
                                    <button
                                        className={styles.btnDelete}
                                        onClick={() => handleDelete(job)}
                                        disabled={deletingId === job.jobId}
                                    >
                                        {deletingId === job.jobId ? "..." : "Eliminar"}
                                    </button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                )}
            </div>

            <div className={styles.card}>
                <p className={styles.cardTitle}>¿CÓMO EXPORTAR DESDE REVOLUT?</p>
                <ol className={styles.stepsList}>
                    <li>Abre la app de Revolut</li>
                    <li>Ve a la sección de transacciones</li>
                    <li>Pulsa el icono de exportar arriba a la derecha</li>
                    <li>Selecciona formato CSV y el rango de fechas</li>
                    <li>Descarga el archivo y súbelo aquí</li>
                </ol>
            </div>
        </div>
    );
}