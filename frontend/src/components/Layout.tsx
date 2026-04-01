import { useState, useEffect } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import styles from "./Layout.module.css";

const navItems = [
    { to: "/dashboard", label: "Dashboard" },
    { to: "/import", label: "Importar CSV" },
    { to: "/analysis", label: "Análisis" },
    { to: "/budgets", label: "Presupuestos" },
];

export default function Layout() {
    const { logout, user } = useAuth();
    const navigate = useNavigate();

    const [isLight, setIsLight] = useState<boolean>(() => {
        return localStorage.getItem("theme") === "light";
    });

    useEffect(() => {
        const html = document.documentElement;
        if (isLight) {
            html.classList.add("light");
        } else {
            html.classList.remove("light");
        }
        localStorage.setItem("theme", isLight ? "light" : "dark");
    }, [isLight]);

    const handleLogout = () => {
        logout();
        navigate("/auth");
    };

    return (
        <div className={styles.root}>
            <header className={styles.header}>
                <nav className={styles.nav}>
                    <span className={styles.wordmark}>Aureus</span>
                    <div className={styles.divider} />
                    <div className={styles.navLinks}>
                        {navItems.map((item) => (
                            <NavLink
                                key={item.to}
                                to={item.to}
                                className={({ isActive }) =>
                                    `${styles.navLink} ${isActive ? styles.navLinkActive : ""}`
                                }
                            >
                                {item.label}
                            </NavLink>
                        ))}
                    </div>
                    <div className={styles.navRight}>
                        <button
                            className={styles.themeToggle}
                            onClick={() => setIsLight(prev => !prev)}
                            title={isLight ? "Modo oscuro" : "Modo claro"}
                            aria-label="Cambiar tema"
                        >
                            {isLight ? "○" : "●"}
                        </button>
                        <span className={styles.userName}>{user?.name}</span>
                        <button
                            className={styles.logoutBtn}
                            onClick={handleLogout}
                            title="Cerrar sesión"
                        >
                            Salir
                        </button>
                    </div>
                </nav>
            </header>
            <main className={styles.main}>
                <Outlet />
            </main>
        </div>
    );
}