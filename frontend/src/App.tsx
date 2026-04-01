import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { useAuth } from "./context/useAuth";
import Dashboard from "./pages/Dashboard";
import ImportCsv from "./pages/ImportCsv";
import Analysis from "./pages/Analysis";
import Budgets from "./pages/Budgets";
import Auth from "./pages/Auth";
import Layout from "./components/Layout";

function PrivateRoute({ children }: { children: React.ReactNode }) {
    const { token } = useAuth();
    return token ? <>{children}</> : <Navigate to="/auth" replace />;
}

function App() {
    return (
        <AuthProvider>
            <BrowserRouter>
                <Routes>
                    <Route path="/auth" element={<Auth />} />
                    <Route
                        path="/"
                        element={
                            <PrivateRoute>
                                <Layout />
                            </PrivateRoute>
                        }
                    >
                        <Route index element={<Navigate to="/dashboard" replace />} />
                        <Route path="dashboard" element={<Dashboard />} />
                        <Route path="import" element={<ImportCsv />} />
                        <Route path="analysis" element={<Analysis />} />
                        <Route path="budgets" element={<Budgets />} />
                    </Route>
                </Routes>
            </BrowserRouter>
        </AuthProvider>
    );
}

export default App;