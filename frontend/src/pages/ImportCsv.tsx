import { useState, useEffect } from "react";
import { importCsv, deleteImportJob, deleteAllImportJobs, getImportJobs } from "../api/services";
import type { ImportResponse } from "../api/types";
import styles from "./ImportCsv.module.css";

export default function ImportCsv() {
    const [dragging, setDragging] = useState(false);
    const [loading, setLoading] = useState(false);
    const [history, setHistory] = useState<ImportResponse[]>([]);
    const [error, setError] = useState<string | null>(null);
    const [notice, setNotice] = useState<string | null>(null);
    const [deletingId, setDeletingId] = useState<number | null>(null);

    useEffect(() => {
        getImportJobs()
            .then(setHistory)
            .catch(() => setError("Error al cargar el historial."));
    }, []);

    const handleFile = async (file: File) => {
        if (!file.name.endsWith(".csv")) {
            setError("Solo se aceptan archivos .csv");
            return;
        }
        setError(null);
        setNotice(null);
        setLoading(true);
        try {
            const content = await file.text();
            const response = await importCsv(content, file.name);
            setHistory(prev => [response, ...prev]);

            // Inform the user if some rows were skipped because they already existed
            // from a different file with an overlapping date range
            if (response.skippedRows > 0) {
                setNotice(
                    `Se importaron ${response.importedRows} transacciones de "${file.name}". ` +
                    `${response.skippedRows} ${response.skippedRows === 1 ? "fila estaba duplicada" : "filas estaban duplicadas"} ` +
                    `con otro fichero ya importado y se han omitido.`
                );
            }
        } catch (e: unknown) {
            // The backend returns the error message in e.response.data.message for known errors
            // (e.g. duplicate file name) and in e.response.data for generic ones
            const axiosError = e as { response?: { data?: { message?: string } | string } };
            const data = axiosError?.response?.data;
            const message =
                typeof data === "object" && data !== null
                    ? data.message
                    : typeof data === "string"
                        ? data
                        : undefined;
            setError(message ?? "Error al importar el archivo.");
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (jobId: number) => {
        setDeletingId(jobId);
        try {
            await deleteImportJob(jobId);
            setHistory(prev => prev.filter(j => j.jobId !== jobId));
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
        } catch {
            setError("Error al limpiar el historial.");
        }
    };

    const handleDrop = (e: React.DragEvent) => {
        e.preventDefault();
        setDragging(false);
        Array.from(e.dataTransfer.files).forEach(file => handleFile(file));
    };

    const handleInputChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const files = e.target.files;
        if (!files) return;
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

            {error && <div className={styles.error}><p>{error}</p></div>}
            {notice && !error && <div className={styles.notice}><p>{notice}</p></div>}

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
                                <td className={styles.td}>
                                    {job.createdAt
                                        ? new Date(job.createdAt).toLocaleDateString("es-ES")
                                        : "—"}
                                </td>
                                <td className={styles.td}>
                                    {job.importedRows} / {job.totalRows}
                                    {job.skippedRows > 0 && (
                                        <span style={{
                                            marginLeft: "0.5rem",
                                            fontSize: "1em",
                                            color: "var(--color-text-muted)",
                                            fontWeight: 300,
                                        }}>
                                                ({job.skippedRows} duplicadas)
                                            </span>
                                    )}
                                </td>
                                <td className={styles.td}>
                                        <span className={`${styles.badge} ${job.status === "DONE" ? styles.badgeDone : styles.badgeFailed}`}>
                                            {job.status}
                                        </span>
                                </td>
                                <td className={styles.tdActions}>
                                    <button
                                        className={styles.btnDelete}
                                        onClick={() => handleDelete(job.jobId)}
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