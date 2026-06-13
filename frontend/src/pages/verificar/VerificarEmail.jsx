import { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { apiFetch } from "../../utils/api";
import NavbarPublic from "../../components/navbar/NavbarPublic";
import Footer from "../../components/footer/Footer";
import "./VerificarEmail.css";

const REENVIO_SEGUNDOS = 60;

function VerificarEmail() {
  const navigate = useNavigate();
  const location = useLocation();
  const email = location.state?.email || "";

  const [codigo,    setCodigo]    = useState("");
  const [error,     setError]     = useState("");
  const [info,      setInfo]      = useState("");
  const [enviando,  setEnviando]  = useState(false);
  const [cooldown,  setCooldown]  = useState(0); // segundos hasta poder reenviar

  // Si llegaron acá sin email, volver al login
  useEffect(() => {
    if (!email) navigate("/");
  }, [email, navigate]);

  // Cuenta regresiva del cooldown de reenvío
  useEffect(() => {
    if (cooldown <= 0) return;
    const t = setInterval(() => setCooldown((s) => s - 1), 1000);
    return () => clearInterval(t);
  }, [cooldown]);

  const handleVerificar = async (e) => {
    e.preventDefault();
    setError(""); setInfo("");
    if (codigo.length !== 6) {
      setError("El código tiene 6 dígitos."); return;
    }
    try {
      setEnviando(true);
      const data = await apiFetch("/usuarios/verificar", {
        method: "POST",
        body: JSON.stringify({ email, codigo })
      });
      // Verificado → auto-login con el token devuelto
      sessionStorage.setItem("token", data.token);
      sessionStorage.setItem("usuario", JSON.stringify({
        id: data.id, nombre: data.nombre, email: data.email, rol: data.rol
      }));
      navigate("/home");
    } catch (err) {
      setError(err.message);
    } finally {
      setEnviando(false);
    }
  };

  const handleReenviar = async () => {
    setError(""); setInfo("");
    try {
      await apiFetch("/usuarios/reenviar-codigo", {
        method: "POST",
        body: JSON.stringify({ email })
      });
      setInfo("Te reenviamos el código a tu email.");
      setCooldown(REENVIO_SEGUNDOS);
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div className="verif-page">
      <NavbarPublic />
      <main className="verif-page__main">
        <div className="verif-card">
          <span className="verif-card__logo">SportSync</span>
          <h1 className="verif-card__title">Verificá tu cuenta</h1>
          <p className="verif-card__subtitle">
            Ingresá el código de 6 dígitos que enviamos a<br />
            <strong>{email}</strong>
          </p>

          <form className="verif-form" onSubmit={handleVerificar}>
            <input
              className="verif-form__code"
              type="text"
              inputMode="numeric"
              placeholder="______"
              maxLength={6}
              value={codigo}
              onChange={(e) => setCodigo(e.target.value.replace(/\D/g, ""))}
              autoFocus
            />

            {error && <p className="verif-form__error">{error}</p>}
            {info  && <p className="verif-form__info">{info}</p>}

            <button className="verif-form__btn" type="submit" disabled={enviando || codigo.length !== 6}>
              {enviando ? "Verificando..." : "Verificar"}
            </button>
          </form>

          <div className="verif-card__resend">
            ¿No te llegó?{" "}
            {cooldown > 0 ? (
              <span className="verif-card__resend-wait">Reenviar en {cooldown}s</span>
            ) : (
              <button className="verif-card__resend-btn" onClick={handleReenviar}>
                Reenviar código
              </button>
            )}
          </div>

          <p className="verif-card__foot">Si no te registraste en SportSync, ignorá el email.</p>
        </div>
      </main>
      <Footer />
    </div>
  );
}

export default VerificarEmail;
