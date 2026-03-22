import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Dashboard from "./pages/Dashboard";
import ImportCsv from "./pages/ImportCsv";
import Analysis from "./pages/Analysis";
import Budgets from "./pages/Budgets";
import Layout from "./components/Layout";

function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<Layout />}>
                    <Route index element={<Navigate to="/dashboard" replace />} />
                    <Route path="dashboard" element={<Dashboard />} />
                    <Route path="import" element={<ImportCsv />} />
                    <Route path="analysis" element={<Analysis />} />
                    <Route path="budgets" element={<Budgets />} />
                </Route>
            </Routes>
        </BrowserRouter>
    );
}

export default App;