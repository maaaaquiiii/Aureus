import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import { login as loginService, register as registerService } from "../api/services";
import styles from "./Auth.module.css";

function EyeOpen() {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
            <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"/>
            <circle cx="12" cy="12" r="3"/>
        </svg>
    );
}

function EyeClosed() {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
            <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
            <line x1="1" y1="1" x2="23" y2="23"/>
        </svg>
    );
}

export default function Auth() {
    const { login } = useAuth();
    const navigate = useNavigate();
    const [isLogin, setIsLogin] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);
    const [remember, setRemember] = useState(false);
    const [showLoginPassword, setShowLoginPassword] = useState(false);
    const [showRegisterPassword, setShowRegisterPassword] = useState(false);
    const [loginForm, setLoginForm] = useState({ email: "", password: "" });
    const [registerForm, setRegisterForm] = useState({
        email: "",
        name: "",
        password: "",
        currency: "EUR",
    });

    const toggle = (toLogin: boolean) => {
        if (isLogin === toLogin) return;
        setError(null);
        setIsLogin(toLogin);
    };

    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        setLoading(true);
        try {
            const data = await loginService(loginForm.email, loginForm.password);
            login(data, remember);
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
            login(data, false);
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
                    <svg
                        width="56"
                        height="56"
                        viewBox="0 0 56 56"
                        fill="none"
                        xmlns="http://www.w3.org/2000/svg"
                        className={styles.logoSvg}
                    >
                        <circle cx="28" cy="28" r="26" stroke="var(--color-logo)" strokeWidth="1.2"/>
                        <text
                            x="28"
                            y="36"
                            textAnchor="middle"
                            fill="var(--color-logo)"
                            fontSize="22"
                            fontFamily="'Red Hat Display', sans-serif"
                            fontWeight="300"
                            letterSpacing="2"
                        >A</text>
                    </svg>
                    <h1 className={styles.logoName}>Aureus</h1>
                    <p className={styles.logoSub}>Finanzas personales</p>
                </div>
                {/* Card */}
                <div className={styles.card}>
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
                        {/* Forms */}
                        <div className={styles.formWrapper}>
                            {isLogin ? (
                                <form key="login" className={`${styles.form} ${styles.formSlide}`} onSubmit={handleLogin}>
                                    <div className={styles.field}>
                                        <label className={styles.label}>Email</label>
                                        <input
                                            type="email"
                                            className={styles.input}
                                            value={loginForm.email}
                                            onChange={(e) =>
                                                setLoginForm({ ...loginForm, email: e.target.value })
                                            }
                                            required
                                        />
                                    </div>
                                    <div className={styles.field}>
                                        <label className={styles.label}>Contraseña</label>
                                        <div className={styles.passwordWrapper}>
                                            <input
                                                type={showLoginPassword ? "text" : "password"}
                                                className={styles.input}
                                                value={loginForm.password}
                                                onChange={(e) =>
                                                    setLoginForm({ ...loginForm, password: e.target.value })
                                                }
                                                required
                                            />
                                            <button
                                                type="button"
                                                className={styles.eyeBtn}
                                                onClick={() => setShowLoginPassword(v => !v)}
                                                tabIndex={-1}
                                            >
                                                {showLoginPassword ? <EyeOpen /> : <EyeClosed />}
                                            </button>
                                        </div>
                                    </div>
                                    <div className={styles.rememberRow}>
                                        <label className={styles.rememberLabel}>
                                            <input
                                                type="checkbox"
                                                checked={remember}
                                                onChange={(e) => setRemember(e.target.checked)}
                                                className={styles.rememberCheck}
                                            />
                                            Recuérdame
                                        </label>
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
                                <form key="register" className={`${styles.form} ${styles.formSlide}`} onSubmit={handleRegister}>
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
                                        <div className={styles.passwordWrapper}>
                                            <input
                                                type={showRegisterPassword ? "text" : "password"}
                                                className={styles.input}
                                                placeholder="Mínimo 6 caracteres"
                                                value={registerForm.password}
                                                onChange={(e) =>
                                                    setRegisterForm({ ...registerForm, password: e.target.value })
                                                }
                                                required
                                                minLength={6}
                                            />
                                            <button
                                                type="button"
                                                className={styles.eyeBtn}
                                                onClick={() => setShowRegisterPassword(v => !v)}
                                                tabIndex={-1}
                                            >
                                                {showRegisterPassword ? <EyeOpen /> : <EyeClosed />}
                                            </button>
                                        </div>
                                    </div>
                                    <div className={styles.field}>
                                        <label className={styles.label}>Moneda</label>
                                        <select
                                            className={styles.select}
                                            value={registerForm.currency}
                                            onChange={(e) =>
                                                setRegisterForm({ ...registerForm, currency: e.target.value })
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
                </div>

                <p className={styles.footer}>Aureus · Gestión financiera personal</p>
            </div>
        </div>
    );
}