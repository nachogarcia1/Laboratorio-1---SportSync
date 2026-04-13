import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiFetch } from "../../utils/api";
import NavbarPublic from "../../components/navbar/NavbarPublic";
import Footer from "../../components/footer/Footer";
import "./Register.css";

function Register() {
  const navigate = useNavigate();
  const [error, setError] = useState("");

  const [form, setForm] = useState({
    nombre: "",
    email: "",
    password: "",
    confirmarPassword: "",
    dni: "",
    telefono: "",
    aceptaTerminos: false
  });

  const handleRegister = async (e) => {
    e.preventDefault();
    setError("");

    if (form.password !== form.confirmarPassword) {
      setError("Las contraseñas no coinciden");
      return;
    }

    if (form.password.length < 6) {
      setError("La contraseña debe tener al menos 6 caracteres");
      return;
    }

    if (!form.aceptaTerminos) {
      setError("Debes aceptar los términos y condiciones");
      return;
    }

    try {
      await apiFetch("/usuarios/register", {
        method: "POST",
        body: JSON.stringify({
          nombre: form.nombre,
          email: form.email,
          password: form.password,
          dni: form.dni,
          telefono: form.telefono
        })
      });

      alert("Usuario registrado correctamente");
      navigate("/");
    } catch (error) {
      setError(error.message);
    }
  };

  return (
    <div className="register-page">
      <NavbarPublic />

      <main className="register-page__main">
        <div className="register-card">
          <h1 className="register-card__title">Crea tu Cuenta en SportSync</h1>

          <div className="register-card__tabs">
            <button
              className="register-card__tab"
              onClick={() => navigate("/")}
            >
              Iniciar sesión
            </button>
            <button className="register-card__tab register-card__tab--active">
              Registrarse
            </button>
          </div>

          <form className="register-form" onSubmit={handleRegister}>
            <label>Nombre Completo</label>
            <input
              type="text"
              placeholder="Tu Nombre Apellido"
              value={form.nombre}
              onChange={(e) => setForm({ ...form, nombre: e.target.value })}
            />

            <label>Correo Electrónico</label>
            <input
              type="email"
              placeholder="tu.email@ejemplo.com"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
            />

            <label>Contraseña</label>
            <input
              type="password"
              placeholder="Crea una contraseña segura"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
            />

            <label>Confirmar Contraseña</label>
            <input
              type="password"
              placeholder="Repite tu contraseña"
              value={form.confirmarPassword}
              onChange={(e) =>
                setForm({ ...form, confirmarPassword: e.target.value })
              }
            />

            <label>DNI</label>
            <input
              type="text"
              placeholder="Ingresa tu DNI"
              value={form.dni}
              onChange={(e) => {
                const valor = e.target.value.replace(/\D/g, "");
                setForm({ ...form, dni: valor });
              }}
            />
            

            <label>Teléfono</label>
            <input
              type="text"
              placeholder="Ingresa tu teléfono"
              value={form.telefono}
              onChange={(e) => {
                const valor = e.target.value.replace(/\D/g, "");
                setForm({ ...form, telefono: valor });
              }}
            />

            <div className="register-form__checkbox-row">
              <input
                type="checkbox"
                checked={form.aceptaTerminos}
                onChange={(e) =>
                  setForm({ ...form, aceptaTerminos: e.target.checked })
                }
              />
              <span>
                Acepto los Términos y Condiciones y la Política de Privacidad
              </span>
            </div>

            {error && <p className="form__error">{error}</p>}
            <button className="register-form__primary-btn" type="submit">
              Registrarse
            </button>

            <div className="register-form__separator">o</div>

            <button
              type="button"
              className="register-form__social-btn register-form__social-btn--google"
            >
              G Registrarse con Google
            </button>

            <button
              type="button"
              className="register-form__social-btn register-form__social-btn--facebook"
            >
              f Registrarse con Facebook
            </button>

            <div className="register-form__bottom-text">
              ¿Ya tienes una cuenta?{" "}
              <span onClick={() => navigate("/")}>Inicia Sesión aquí</span>
            </div>
          </form>
        </div>
      </main>

      <Footer />
    </div>
  );
}

export default Register;