import { useState } from "react";
import type { ReactNode } from "react";
import type { AuthResponse } from "../api/types";
import { AuthContext } from "./AuthContextDef";
import { setAuthToken } from "../api/client";

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<AuthResponse | null>(() => {
        const stored = localStorage.getItem("auth") || sessionStorage.getItem("auth");
        if (stored) {
            const parsed = JSON.parse(stored);
            setAuthToken(parsed.token);
            return parsed;
        }
        return null;
    });

    const token = user?.token ?? null;

    const login = (data: AuthResponse, remember: boolean) => {
        setUser(data);
        setAuthToken(data.token);
        if (remember) {
            localStorage.setItem("auth", JSON.stringify(data));
        } else {
            sessionStorage.setItem("auth", JSON.stringify(data));
        }
    };

    const logout = () => {
        setUser(null);
        setAuthToken(null);
        localStorage.removeItem("auth");
        sessionStorage.removeItem("auth");
    };

    return (
        <AuthContext.Provider value={{ user, token, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
}