import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiFetch } from "../../utils/api";
import NavbarPublic from "../../components/navbar/NavbarPublic";
import Footer from "../../components/footer/Footer";
import "./Login.css";

function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    setError("");

    try {
      const data = await apiFetch("/usuarios/login", {
        method: "POST",
        body: JSON.stringify({ email, password })
      });

      sessionStorage.setItem("token", data.token);
      sessionStorage.setItem("usuario", JSON.stringify({
        id:     data.id,
        nombre: data.nombre,
        email:  data.email,
        rol:    data.rol
      }));
      navigate("/home");
    } catch (error) {
      setError(error.message);
    }
  };

  return (
    <div className="login-page">
      <NavbarPublic />

      <main className="login-page__main">
        <div className="login-card">
          <h1 className="login-card__title">Acceso a SportSync</h1>

          <div className="login-card__tabs">
            <button className="login-card__tab login-card__tab--active">
              Iniciar Sesión
            </button>
            <button
              className="login-card__tab"
              onClick={() => navigate("/register")}
            >
              Registrarse
            </button>
          </div>

          <form className="login-form" onSubmit={handleLogin}>
            <label>Correo Electrónico</label>
            <input
              type="email"
              placeholder="Correo Electrónico"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />

            <label>Contraseña</label>
            <input
              type="password"
              placeholder="Contraseña"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />

            <div className="login-form__forgot">Olvidé mi contraseña</div>

            {error && <p className="form__error">{error}</p>}
            <button className="login-form__primary-btn" type="submit">
              Iniciar Sesión
            </button>

            <div className="login-form__separator">o</div>

            <div className="login-form__social-row">
              <button
                type="button"
                className="login-form__social-btn login-form__social-btn--google"
              >
                G Continuar con Google
              </button>

              <button
                type="button"
                className="login-form__social-btn login-form__social-btn--facebook"
              >
                f Continuar con Facebook
              </button>
            </div>
          </form>
        </div>
      </main>

      <Footer />
    </div>
  );
}

export default Login;