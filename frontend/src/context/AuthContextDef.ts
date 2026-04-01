import { createContext } from "react";
import type { AuthResponse } from "../api/types";

interface AuthContextType {
    user: AuthResponse | null;
    token: string | null;
    login: (data: AuthResponse) => void;
    logout: () => void;
}

export const AuthContext = createContext<AuthContextType | null>(null);