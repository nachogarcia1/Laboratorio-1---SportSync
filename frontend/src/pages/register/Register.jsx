import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiFetch } from "../../utils/api";
import NavbarPublic from "../../components/navbar/NavbarPublic";
import Footer from "../../components/footer/Footer";
import GoogleLoginButton from "../../components/GoogleLoginButton";
import "./Register.css";

function Register() {
  const navigate = useNavigate();
  const [error,  setError]  = useState("");

  const [form, setForm] = useState({
    nombre: "", email: "", password: "", confirmarPassword: "",
    dni: "", telefono: "", aceptaTerminos: false
  });

  const handleRegister = async (e) => {
    e.preventDefault();
    setError("");

    if (form.nombre.trim().length < 6) {
      setError("El nombre de usuario debe tener al menos 6 caracteres"); return;
    }
    if (form.dni.length < 7) {
      setError("El DNI debe tener al menos 7 dígitos"); return;
    }
    if (form.telefono && form.telefono.length < 8) {
      setError("El teléfono debe tener 8 dígitos"); return;
    }
    if (form.password !== form.confirmarPassword) {
      setError("Las contraseñas no coinciden"); return;
    }
    if (form.password.length < 6) {
      setError("La contraseña debe tener al menos 6 caracteres"); return;
    }
    if (!form.aceptaTerminos) {
      setError("Debes aceptar los términos y condiciones"); return;
    }

    try {
      await apiFetch("/usuarios/register", {
        method: "POST",
        body: JSON.stringify({
          nombre: form.nombre, email: form.email,
          password: form.password, dni: form.dni, telefono: form.telefono
        })
      });
      // Cuenta creada → ir a verificar el email con el código
      navigate("/verificar", { state: { email: form.email } });
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div className="register-page">
      <NavbarPublic />
      <main className="register-page__main">
        <div className="register-card">
          <h1 className="register-card__title">Crea tu Cuenta en SportSync</h1>

          <div className="register-card__tabs">
            <button className="register-card__tab" onClick={() => navigate("/")}>
              Iniciar sesión
            </button>
            <button className="register-card__tab register-card__tab--active">
              Registrarse
            </button>
          </div>

          <form className="register-form" onSubmit={handleRegister}>
              <label>Nombre de usuario</label>
              <input
                type="text" placeholder="Tu usuario"
                value={form.nombre} maxLength={100} required
                onChange={(e) => setForm({ ...form, nombre: e.target.value })}
              />

              <label>Correo Electrónico</label>
              <input
                type="email" placeholder="tu.email@ejemplo.com"
                value={form.email} maxLength={150} required
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />

              <label>Contraseña</label>
              <input
                type="password" placeholder="Crea una contraseña segura"
                value={form.password} maxLength={100} required
                onChange={(e) => setForm({ ...form, password: e.target.value })}
              />

              <label>Confirmar Contraseña</label>
              <input
                type="password" placeholder="Repite tu contraseña"
                value={form.confirmarPassword} maxLength={100} required
                onChange={(e) => setForm({ ...form, confirmarPassword: e.target.value })}
              />

              <label>DNI</label>
              <input
                type="text" placeholder="Ingresa tu DNI"
                value={form.dni} maxLength={12} required
                onChange={(e) => setForm({ ...form, dni: e.target.value.replace(/\D/g, "") })}
              />

              <label>Teléfono</label>
              <input
                type="text" placeholder="Ingresa tu teléfono"
                value={form.telefono} maxLength={8}
                onChange={(e) => setForm({ ...form, telefono: e.target.value.replace(/\D/g, "") })}
              />

              <div className="register-form__checkbox-row">
                <input
                  type="checkbox" checked={form.aceptaTerminos}
                  onChange={(e) => setForm({ ...form, aceptaTerminos: e.target.checked })}
                />
                <span>Acepto los Términos y Condiciones y la Política de Privacidad</span>
              </div>

              {error && <p className="form__error">{error}</p>}

              <button className="register-form__primary-btn" type="submit">
                Registrarse
              </button>

              <div className="register-form__separator">o</div>

              <GoogleLoginButton onError={setError} />

            

              <div style={{ marginTop: '40px' }} className="register-form__bottom-text">
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