import { useState, useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import NavbarPrivate from "../../components/navbar/NavbarPrivate";
import Footer from "../../components/footer/Footer";
import { apiFetch } from "../../utils/api";
import "./CalificarExperiencia.css";

const MESES = ["Enero","Febrero","Marzo","Abril","Mayo","Junio","Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"];

function formatFecha(fechaStr) {
  if (!fechaStr) return "";
  const [y, m, d] = fechaStr.split("-");
  return `${parseInt(d)} de ${MESES[parseInt(m) - 1]}, ${y}`;
}

function formatHora(horaStr) {
  return horaStr ? horaStr.substring(0, 5) : "";
}

function StarRating({ value, onChange }) {
  const [hover, setHover] = useState(0);
  return (
    <div className="stars">
      {[1, 2, 3, 4, 5].map(n => (
        <span
          key={n}
          className={`star ${n <= (hover || value) ? "star--filled" : ""}`}
          onMouseEnter={() => setHover(n)}
          onMouseLeave={() => setHover(0)}
          onClick={() => onChange(n)}
        >
          ★
        </span>
      ))}
    </div>
  );
}

function CalificarExperiencia() {
  const { state } = useLocation();
  const navigate  = useNavigate();
  const reserva   = state?.reserva;

  const [notaCancha,    setNotaCancha]    = useState(0);
  const [notaStaff,     setNotaStaff]     = useState(0);
  const [notaServicios, setNotaServicios] = useState(0);
  const [comentario,    setComentario]    = useState("");
  const [status,        setStatus]        = useState(null); // null | "loading" | "success" | "error"
  const [errorMsg,      setErrorMsg]      = useState("");

  const usuario = (() => {
    try { return JSON.parse(sessionStorage.getItem("usuario")); }
    catch { return null; }
  })();

  useEffect(() => {
    if (!reserva) return;
    apiFetch(`/criticas/canchas/reservas/${reserva.id}`)
      .then(data => { if (data.yaCalificada) setStatus("success"); })
      .catch(() => {});
  }, []);

  if (!reserva) {
    return (
      <div className="calificar-page">
        <NavbarPrivate />
        <main className="calificar__main">
          <p className="calificar__error">No se encontró información de la reserva.</p>
          <button className="calificar__back-btn" onClick={() => navigate("/mis-reservas")}>
            Volver a Mis Reservas
          </button>
        </main>
        <Footer />
      </div>
    );
  }

  const notaPromedio = Math.round((notaCancha + notaStaff + notaServicios) / 3) || 0;

  async function handleEnviar(e) {
    e.preventDefault();
    if (notaCancha === 0 || notaStaff === 0 || notaServicios === 0) {
      setErrorMsg("Por favor completá todas las calificaciones.");
      return;
    }
    setStatus("loading");
    setErrorMsg("");
    try {
      await apiFetch("/criticas/canchas", {
        method: "POST",
        body: JSON.stringify({
          usuarioId:  usuario.id,
          canchaId:   reserva.cancha?.id ?? reserva.canchaId,
          reservaId:  reserva.id,
          nota:       notaPromedio,
          comentario: comentario.trim() || null
        })
      });
      setStatus("success");
    } catch (e) {
      setErrorMsg(e.message);
      setStatus("error");
    }
  }

  const nombreCancha = reserva.cancha
    ? `Cancha de Fútbol ${reserva.cancha.tipo}`
    : "Cancha";
  const nombreSede = reserva.cancha?.sede?.nombre
    ? `Sede ${reserva.cancha.sede.nombre}`
    : "";

  return (
    <div className="calificar-page">
      <NavbarPrivate />
      <main className="calificar__main">
        <h1 className="calificar__title">Califica tu Experiencia</h1>

        <div className="calificar__card">
          <p className="calificar__subtitle">Ayúdanos a mejorar tu experiencia en SportSync.</p>

          <div className="calificar__reserva-info">
            <h3>Reserva Reciente</h3>
            <div className="calificar__reserva-card">
              <div className="calificar__reserva-img"></div>
              <div>
                <p className="calificar__reserva-nombre">{nombreCancha} - {nombreSede}</p>
                <p className="calificar__reserva-dato">📅 {formatFecha(reserva.fecha)}</p>
                <p className="calificar__reserva-dato">⏰ {formatHora(reserva.horaInicio)} - {formatHora(reserva.horaFin)} hs</p>
              </div>
            </div>
          </div>

          {status === "success" ? (
            <div className="calificar__success">
              <span className="calificar__success-icon">✓</span>
              <p>¡Gracias por tu calificación!</p>
              <button className="calificar__submit-btn" onClick={() => navigate("/mis-reservas")}>
                Volver a Mis Reservas
              </button>
            </div>
          ) : (
            <form onSubmit={handleEnviar}>
              <div className="calificar__criterio">
                <label>¿Cómo calificarías la cancha?</label>
                <StarRating value={notaCancha} onChange={setNotaCancha} />
              </div>
              <div className="calificar__criterio">
                <label>¿Cómo calificarías la atención del staff?</label>
                <StarRating value={notaStaff} onChange={setNotaStaff} />
              </div>
              <div className="calificar__criterio">
                <label>¿Cómo calificarías los servicios adicionales?</label>
                <StarRating value={notaServicios} onChange={setNotaServicios} />
              </div>

              <div className="calificar__comentario">
                <label>Comentarios Adicionales (Opcional)</label>
                <textarea
                  placeholder="Cuéntanos más sobre tu experiencia..."
                  value={comentario}
                  onChange={e => setComentario(e.target.value)}
                  rows={4}
                />
              </div>

              {errorMsg && <p className="calificar__error">{errorMsg}</p>}

              <button
                type="submit"
                className="calificar__submit-btn"
                disabled={status === "loading"}
              >
                {status === "loading" ? "Enviando..." : "Enviar Calificación"}
              </button>
            </form>
          )}
        </div>
      </main>
      <Footer />
    </div>
  );
}

export default CalificarExperiencia;