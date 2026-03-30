import { useEffect, useState } from "react";
import { importCsv } from "../api/services";
import type { ImportResponse } from "../api/types";

interface ImportJob {
    jobId: number;
    fileName: string;
    status: string;
    totalRows: number;
    importedRows: number;
    errorDetail: string | null;
    date: string;
}

export default function ImportCsv() {
    const [dragging, setDragging] = useState(false);
    const [loading, setLoading] = useState(false);
    const [history, setHistory] = useState<ImportJob[]>([]);
    const [error, setError] = useState<string | null>(null);

    const handleFile = async (file: File) => {
        if (!file.name.endsWith(".csv")) {
            setError("Solo se aceptan archivos .csv");
            return;
        }

        setError(null);
        setLoading(true);

        try {
            const content = await file.text();
            const response: ImportResponse = await importCsv(content, file.name);

            setHistory((prev) => [
                {
                    jobId: response.jobId,
                    fileName: file.name,
                    status: response.status,
                    totalRows: response.totalRows,
                    importedRows: response.importedRows,
                    errorDetail: response.errorDetail,
                    date: new Date().toLocaleDateString("es-ES"),
                },
                ...prev,
            ]);
        } catch (e) {
            setError("Error al importar el archivo. ¿Está el backend arrancado?");
        } finally {
            setLoading(false);
        }
    };

    const handleDrop = (e: React.DragEvent) => {
        e.preventDefault();
        setDragging(false);
        const file = e.dataTransfer.files[0];
        if (file) handleFile(file);
    };

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (file) handleFile(file);
    };

    return (
        <div className="flex flex-col gap-5">

            {/* Cabecera */}
            <div>
                <h1 className="text-[#e2e8f0] text-xl font-bold">Importar CSV</h1>
                <p className="text-[#475569] text-xs mt-1">
                    Sube tu exportación de Revolut en formato CSV
                </p>
            </div>

            {/* Zona drag & drop */}
            <div
                onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
                onDragLeave={() => setDragging(false)}
                onDrop={handleDrop}
                className={`border-2 border-dashed rounded-xl p-10 text-center transition-all ${
                    dragging
                        ? "border-[#fbbf24] bg-[#1e1e2e]"
                        : "border-[#2a2a3e] hover:border-[#475569]"
                }`}
            >
                <div className="text-4xl mb-3">📂</div>
                <p className="text-[#e2e8f0] font-semibold text-sm mb-1">
                    Arrastra tu CSV de Revolut aquí
                </p>
                <p className="text-[#475569] text-xs mb-4">
                    Formato: Type, Product, Started Date, Description, Amount, Currency...
                </p>
                <label className="cursor-pointer">
                    <span className="bg-[#fbbf24] text-[#0f0f1a] font-bold text-sm px-5 py-2 rounded-lg hover:bg-[#f59e0b] transition-colors">
                        {loading ? "Importando..." : "Seleccionar archivo"}
                    </span>
                    <input
                        type="file"
                        accept=".csv"
                        className="hidden"
                        onChange={handleInputChange}
                        disabled={loading}
                    />
                </label>
            </div>

            {/* Error */}
            {error && (
                <div className="bg-[#450a0a] border border-[#f87171] rounded-xl p-3">
                    <p className="text-[#f87171] text-sm">{error}</p>
                </div>
            )}

            {/* Historial */}
            <div className="bg-[#1e1e2e] border border-[#2a2a3e] rounded-xl p-4">
                <p className="text-[#94a3b8] text-xs font-semibold mb-4 tracking-wider">
                    HISTORIAL DE IMPORTACIONES
                </p>
                {history.length === 0 ? (
                    <p className="text-[#475569] text-xs">
                        No hay importaciones en esta sesión
                    </p>
                ) : (
                    <table className="w-full">
                        <thead>
                        <tr>
                            {["Archivo", "Fecha", "Filas", "Estado"].map((h) => (
                                <th
                                    key={h}
                                    className="text-left text-xs text-[#475569] pb-3 border-b border-[#2a2a3e] font-normal"
                                >
                                    {h}
                                </th>
                            ))}
                        </tr>
                        </thead>
                        <tbody>
                        {history.map((job) => (
                            <tr key={job.jobId} className="border-b border-[#2a2a3e] last:border-0">
                                <td className="py-3 text-xs text-[#e2e8f0]">{job.fileName}</td>
                                <td className="py-3 text-xs text-[#64748b]">{job.date}</td>
                                <td className="py-3 text-xs text-[#64748b]">
                                    {job.importedRows} / {job.totalRows}
                                </td>
                                <td className="py-3">
                                        <span className={`text-xs font-bold px-2 py-0.5 rounded ${
                                            job.status === "DONE"
                                                ? "bg-[#14532d] text-[#34d399]"
                                                : "bg-[#450a0a] text-[#f87171]"
                                        }`}>
                                            {job.status}
                                        </span>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                )}
            </div>

            {/* Nota */}
            <div className="bg-[#1e1e2e] border border-[#2a2a3e] rounded-xl p-4">
                <p className="text-[#94a3b8] text-xs font-semibold mb-2 tracking-wider">
                    ¿CÓMO EXPORTAR DESDE REVOLUT?
                </p>
                <ol className="text-[#64748b] text-xs flex flex-col gap-1 list-decimal list-inside">
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