import { useState } from "react";
import type { ReactNode } from "react";
import type { AuthResponse } from "../api/types";
import { AuthContext } from "./AuthContextDef";

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<AuthResponse | null>(() => {
        const stored = localStorage.getItem("auth");
        return stored ? JSON.parse(stored) : null;
    });

    const token = user?.token ?? null;

    const login = (data: AuthResponse) => {
        setUser(data);
        localStorage.setItem("auth", JSON.stringify(data));
    };

    const logout = () => {
        setUser(null);
        localStorage.removeItem("auth");
    };

    return (
        <AuthContext.Provider value={{ user, token, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
}