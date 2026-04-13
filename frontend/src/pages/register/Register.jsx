import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Navbar from "../../components/navbar/Navbar";
import Footer from "../../components/footer/Footer";
import "./Register.css";

function Register() {
  const navigate = useNavigate();

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

    if (form.password !== form.confirmarPassword) {
      alert("Las contraseñas no coinciden");
      return;
    }

    if (!form.aceptaTerminos) {
      alert("Debes aceptar los términos y condiciones");
      return;
    }

    try {
      const response = await fetch("http://localhost:8080/usuarios/register", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          nombre: form.nombre,
          email: form.email,
          password: form.password,
          dni: form.dni,
          telefono: form.telefono
        })
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.error || "Error al registrarse");
      }

      alert("Usuario registrado correctamente");
      navigate("/");
    } catch (error) {
      alert(error.message);
    }
  };

  return (
    <div className="register-page">
      <Navbar />

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
              Registrase
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
              onChange={(e) => setForm({ ...form, dni: e.target.value })}
            />

            <label>Teléfono</label>
            <input
              type="text"
              placeholder="Ingresa tu teléfono"
              value={form.telefono}
              onChange={(e) => setForm({ ...form, telefono: e.target.value })}
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