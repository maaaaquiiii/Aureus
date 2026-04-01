import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import { login as loginService, register as registerService } from "../api/services";
import styles from "./Auth.module.css";

export default function Auth() {
    const { login } = useAuth();
    const navigate = useNavigate();
    const [isLogin, setIsLogin] = useState(true);
    const [flipping, setFlipping] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);

    const [loginForm, setLoginForm] = useState({ email: "", password: "" });
    const [registerForm, setRegisterForm] = useState({
        email: "",
        name: "",
        password: "",
        currency: "EUR",
    });

    const toggle = (toLogin: boolean) => {
        if (flipping || isLogin === toLogin) return;
        setFlipping(true);
        setError(null);
        setTimeout(() => {
            setIsLogin(toLogin);
            setFlipping(false);
        }, 300);
    };

    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        setLoading(true);
        try {
            const data = await loginService(loginForm.email, loginForm.password);
            login(data);
            navigate("/dashboard");
        } catch {
            setError("Email o contraseña incorrectos");
        } finally {
            setLoading(false);
        }
    };

    const handleRegister = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        setLoading(true);
        try {
            const data = await registerService(
                registerForm.email,
                registerForm.name,
                registerForm.password,
                registerForm.currency
            );
            login(data);
            navigate("/dashboard");
        } catch {
            setError("No se pudo completar el registro. El email puede que ya esté en uso.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className={styles.page}>
            <div>
                {/* Logo */}
                <div className={styles.logo}>
                    <div className={styles.logoMark}>
                        <span>A</span>
                    </div>
                    <h1 className={styles.logoName}>Aureus</h1>
                    <p className={styles.logoSub}>Finanzas personales</p>
                </div>

                {/* Card */}
                <div className={`${styles.card} ${flipping ? styles.flipping : ""}`}>
                    <div className={styles.accentBar} />
                    <div className={styles.cardInner}>

                        {/* Toggle */}
                        <div className={styles.toggle}>
                            <button
                                className={`${styles.toggleBtn} ${isLogin ? styles.toggleActive : ""}`}
                                onClick={() => toggle(true)}
                            >
                                Iniciar sesión
                            </button>
                            <button
                                className={`${styles.toggleBtn} ${!isLogin ? styles.toggleActive : ""}`}
                                onClick={() => toggle(false)}
                            >
                                Registrarse
                            </button>
                        </div>

                        {/* Error */}
                        {error && <div className={styles.error}>{error}</div>}

                        {/* Login */}
                        {isLogin ? (
                            <form className={styles.form} onSubmit={handleLogin}>
                                <div className={styles.field}>
                                    <label className={styles.label}>Email</label>
                                    <input
                                        type="email"
                                        className={styles.input}
                                        placeholder="tu@email.com"
                                        value={loginForm.email}
                                        onChange={(e) =>
                                            setLoginForm({ ...loginForm, email: e.target.value })
                                        }
                                        required
                                    />
                                </div>
                                <div className={styles.field}>
                                    <label className={styles.label}>Contraseña</label>
                                    <input
                                        type="password"
                                        className={styles.input}
                                        placeholder="••••••••"
                                        value={loginForm.password}
                                        onChange={(e) =>
                                            setLoginForm({ ...loginForm, password: e.target.value })
                                        }
                                        required
                                    />
                                </div>
                                <button
                                    type="submit"
                                    className={styles.submitBtn}
                                    disabled={loading}
                                >
                                    {loading ? "Entrando..." : "Entrar"}
                                </button>
                            </form>
                        ) : (
                            <form className={styles.form} onSubmit={handleRegister}>
                                <div className={styles.field}>
                                    <label className={styles.label}>Nombre</label>
                                    <input
                                        type="text"
                                        className={styles.input}
                                        value={registerForm.name}
                                        onChange={(e) =>
                                            setRegisterForm({ ...registerForm, name: e.target.value })
                                        }
                                        required
                                    />
                                </div>
                                <div className={styles.field}>
                                    <label className={styles.label}>Email</label>
                                    <input
                                        type="email"
                                        className={styles.input}
                                        placeholder="example@email.com"
                                        value={registerForm.email}
                                        onChange={(e) =>
                                            setRegisterForm({ ...registerForm, email: e.target.value })
                                        }
                                        required
                                    />
                                </div>
                                <div className={styles.field}>
                                    <label className={styles.label}>Contraseña</label>
                                    <input
                                        type="password"
                                        className={styles.input}
                                        placeholder="Mínimo 6 caracteres"
                                        value={registerForm.password}
                                        onChange={(e) =>
                                            setRegisterForm({
                                                ...registerForm,
                                                password: e.target.value,
                                            })
                                        }
                                        required
                                        minLength={6}
                                    />
                                </div>
                                <div className={styles.field}>
                                    <label className={styles.label}>Moneda</label>
                                    <select
                                        className={styles.select}
                                        value={registerForm.currency}
                                        onChange={(e) =>
                                            setRegisterForm({
                                                ...registerForm,
                                                currency: e.target.value,
                                            })
                                        }
                                    >
                                        <option value="EUR">EUR — Euro</option>
                                        <option value="USD">USD — Dólar</option>
                                        <option value="GBP">GBP — Libra</option>
                                    </select>
                                </div>
                                <button
                                    type="submit"
                                    className={styles.submitBtn}
                                    disabled={loading}
                                >
                                    {loading ? "Creando cuenta..." : "Crear cuenta"}
                                </button>
                            </form>
                        )}
                    </div>
                </div>

                <p className={styles.footer}>Aureus · Gestión financiera personal</p>
            </div>
        </div>
    );
}