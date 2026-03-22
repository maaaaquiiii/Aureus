import { NavLink, Outlet } from "react-router-dom";

const navItems = [
    { to: "/dashboard", label: "Dashboard" },
    { to: "/import",    label: "Importar CSV" },
    { to: "/analysis",  label: "Análisis LLM" },
    { to: "/budgets",   label: "Presupuestos" },
];

export default function Layout() {
    return (
        <div className="bg-[#0f0f1a] min-h-screen flex flex-col font-mono">
            <nav className="bg-[#1e1e2e] border-b border-[#2a2a3e] flex items-center px-6 h-13 gap-2 shrink-0">
                <span className="text-[#fbbf24] font-bold text-base mr-6 tracking-wider">
                    ✦ AUREUS
                </span>
                {navItems.map((item) => (
                    <NavLink
                        key={item.to}
                        to={item.to}
                        className={({ isActive }) =>
                            `px-3 py-1.5 rounded-md text-sm no-underline transition-colors ${
                                isActive
                                    ? "bg-[#2a2a3e] text-[#e2e8f0] font-semibold"
                                    : "text-[#64748b] hover:text-[#94a3b8]"
                            }`
                        }
                    >
                        {item.label}
                    </NavLink>
                ))}
            </nav>
            <main className="p-6 flex-1 w-full">
                <Outlet />
            </main>
        </div>
    );
}