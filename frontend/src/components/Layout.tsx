import { useEffect, useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import styles from "./Layout.module.css";

const navItems = [
    { to: "/dashboard", label: "Dashboard"    },
    { to: "/import",    label: "Importar CSV" },
    { to: "/analysis",  label: "Análisis"     },
    { to: "/budgets",   label: "Presupuestos" },
];

function applyTheme(isLight: boolean) {
    const html = document.documentElement;
    if (isLight) {
        html.classList.add("light");
        html.classList.remove("dark");
    } else {
        html.classList.remove("light");
        html.classList.remove("dark");
    }
}

export default function Layout() {
    const [isLight, setIsLight] = useState<boolean>(() => {
        // Aplicar síncronamente en la inicialización
        const saved = localStorage.getItem("theme") === "light";
        applyTheme(saved);
        return saved;
    });

    useEffect(() => {
        applyTheme(isLight);
        localStorage.setItem("theme", isLight ? "light" : "dark");
    }, [isLight]);

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
                    <button
                        className={styles.themeToggle}
                        onClick={() => setIsLight(prev => !prev)}
                        title={isLight ? "Modo oscuro" : "Modo claro"}
                        aria-label="Cambiar tema"
                    >
                        {isLight ? "☽" : "☀"}
                    </button>
                </nav>
            </header>
            <main className={styles.main}>
                <Outlet />
            </main>
        </div>
    );
}