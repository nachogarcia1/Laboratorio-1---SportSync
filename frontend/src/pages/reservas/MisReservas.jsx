import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import NavbarPrivate from "../../components/navbar/NavbarPrivate";
import Footer from "../../components/footer/Footer";
import { apiFetch } from "../../utils/api";
import "./MisReservas.css";

const MESES = ["Enero","Febrero","Marzo","Abril","Mayo","Junio","Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"];

function formatFecha(fechaStr) {
  if (!fechaStr) return "";
  const [y, m, d] = fechaStr.split("-");
  return `${parseInt(d)} de ${MESES[parseInt(m) - 1]}, ${y}`;
}

function formatHora(horaStr) {
  if (!horaStr) return "";
  return horaStr.substring(0, 5);
}

function isPast(fechaStr) {
  const hoy = new Date();
  hoy.setHours(0, 0, 0, 0);
  const reservaFecha = new Date(fechaStr + "T00:00:00");
  return reservaFecha < hoy;
}

function MisReservas() {
  const navigate = useNavigate();

  const [reservas,        setReservas]        = useState([]);
  const [loading,         setLoading]         = useState(true);
  const [error,           setError]           = useState("");
  const [cancelando,      setCancelando]      = useState(null);
  const [cancelError,     setCancelError]     = useState("");
  const [detalleModal,    setDetalleModal]    = useState(null);
  const [mostrarHistorial, setMostrarHistorial] = useState(false);

  const usuario = (() => {
    try { return JSON.parse(sessionStorage.getItem("usuario")); }
    catch { return null; }
  })();

  useEffect(() => {
    if (!usuario?.id) return;
    apiFetch(`/reservas/usuario/${usuario.id}`)
      .then(data => setReservas(data))
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  const proximas    = reservas.filter(r => r.estado === "ACTIVA" && !isPast(r.fecha));
  const historial   = reservas.filter(r => r.estado !== "ACTIVA" || isPast(r.fecha));
  const completadas = historial.filter(r => isPast(r.fecha)).length;

  async function handleCancelar(reservaId) {
    if (!window.confirm("¿Estás seguro que querés cancelar esta reserva?")) return;
    setCancelando(reservaId);
    setCancelError("");
    try {
      await apiFetch(`/reservas/${reservaId}/cancelar`, {
        method: "PUT",
        body: JSON.stringify({ usuarioId: usuario.id })
      });
      setReservas(prev =>
        prev.map(r => r.id === reservaId ? { ...r, estado: "CANCELADA" } : r)
      );
    } catch (e) {
      setCancelError(e.message);
      setTimeout(() => setCancelError(""), 4000);
    } finally {
      setCancelando(null);
    }
  }

  function getStatus(r) {
    if (r.estado === "CANCELADA") return { text: "Cancelada",  color: "#ea2f2f" };
    if (isPast(r.fecha))          return { text: "Completada", color: "#6c757d" };
    return                               { text: "Confirmada", color: "#28a745" };
  }

  function getNombreCancha(r) {
    return r.cancha ? `Cancha de Fútbol ${r.cancha.tipo}` : "Cancha";
  }

  function getNombreSede(r) {
    return r.cancha?.sede ? `Sede ${r.cancha.sede.nombre}` : "";
  }

  const listaVisible = mostrarHistorial ? [...proximas, ...historial] : proximas;

  return (
    <div className="mis-reservas-page">
      <NavbarPrivate />
      <main className="mis-reservas__main">
        <h1 className="mis-reservas__title">Mis Reservas</h1>

        {usuario?.acreditado && (
          <div className="banner-socio">
            <span>ℹ ¡Eres socio! Disfruta de descuentos exclusivos en tus reservas.</span>
            <button className="banner-socio__btn">Ver Beneficios</button>
          </div>
        )}

        {cancelError && <p className="mis-reservas__error">{cancelError}</p>}

        <div className="mis-reservas__grid">

          {/* Lista */}
          <div className="mis-reservas__lista">
            <h2 className="lista-titulo">
              {mostrarHistorial ? "Todas las Reservas" : "Próximas Reservas"}
            </h2>

            {loading && <p className="mis-reservas__estado">Cargando...</p>}
            {error   && <p className="mis-reservas__estado mis-reservas__estado--error">{error}</p>}

            {!loading && listaVisible.length === 0 && (
              <p className="mis-reservas__estado">No tenés reservas próximas.</p>
            )}

            {listaVisible.map(reserva => {
              const status  = getStatus(reserva);
              const pasada  = isPast(reserva.fecha);
              return (
                <div key={reserva.id} className={`reserva-card ${pasada ? "reserva-card--pasada" : ""}`}>
                  <div className="reserva-card__img"></div>
                  <div className="reserva-card__info">
                    <h3 className="reserva-card__nombre">
                      {getNombreCancha(reserva)} - {getNombreSede(reserva)}
                    </h3>
                    <p className="reserva-card__fecha">📅 {formatFecha(reserva.fecha)}</p>
                    <p className="reserva-card__hora">⏰ {formatHora(reserva.horaInicio)} - {formatHora(reserva.horaFin)} hs</p>
                    <span className="reserva-card__status" style={{ color: status.color }}>
                      {status.text}
                    </span>
                  </div>
                  <div className="reserva-card__acciones">
                    <button
                      className="reserva-btn reserva-btn--detalle"
                      onClick={() => setDetalleModal(reserva)}
                    >
                      Ver Detalles
                    </button>
                    {reserva.estado === "ACTIVA" && !pasada && (
                      <button
                        className="reserva-btn reserva-btn--cancelar"
                        onClick={() => handleCancelar(reserva.id)}
                        disabled={cancelando === reserva.id}
                      >
                        {cancelando === reserva.id ? "Cancelando..." : "Cancelar"}
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>

          {/* Aside */}
          <aside className="mis-reservas__aside">
            <div className="aside-card">
              <h3 className="aside-title">Acciones Rápidas</h3>
              <button className="accion-btn accion-btn--primary" onClick={() => navigate("/sedes")}>
                + Nueva Reserva
              </button>
              <button
                className="accion-btn accion-btn--secondary"
                onClick={() => setMostrarHistorial(h => !h)}
              >
                📋 {mostrarHistorial ? "Próximas Reservas" : "Historial de Reservas"}
              </button>
              <button
                className="accion-btn accion-btn--secondary"
                onClick={() => navigate("/mis-calificaciones")}
              >
                ★ Mis Calificaciones
              </button>
            </div>

            <div className="aside-card">
              <h3 className="aside-title">Resumen de Actividad</h3>
              <div className="actividad-row">
                <span>Reservas Activas:</span>
                <span className="actividad-valor">{proximas.length}</span>
              </div>
              <div className="actividad-row">
                <span>Reservas Completadas:</span>
                <span className="actividad-valor">{completadas}</span>
              </div>
              <div className="actividad-row">
                <span>Puntos de Fidelidad:</span>
                <span className="actividad-valor">{completadas * 10} 🏆</span>
              </div>
            </div>
          </aside>

        </div>
      </main>

      {/* Detalle Modal */}
      {detalleModal && (
        <div className="modal-overlay" onClick={() => setDetalleModal(null)}>
          <div className="modal-box" onClick={e => e.stopPropagation()}>
            <h2 className="modal-title">Detalle de Reserva</h2>
            <div className="modal-info">
              <p><strong>Cancha:</strong> {getNombreCancha(detalleModal)}</p>
              <p><strong>Sede:</strong> {getNombreSede(detalleModal)}</p>
              <p><strong>Fecha:</strong> {formatFecha(detalleModal.fecha)}</p>
              <p><strong>Horario:</strong> {formatHora(detalleModal.horaInicio)} - {formatHora(detalleModal.horaFin)} hs</p>
              <p><strong>Estado:</strong> {getStatus(detalleModal).text}</p>
              <p><strong>Iluminación:</strong> {detalleModal.iluminacion ? "Sí" : "No"}</p>
              <p><strong>Precio total:</strong> ${detalleModal.precioTotal?.toLocaleString("es-AR")}</p>
            </div>
            <div className="modal-actions">
              {isPast(detalleModal.fecha) && detalleModal.estado === "ACTIVA" && (
                <button
                  className="modal-btn modal-btn--confirm"
                  onClick={() => {
                    setDetalleModal(null);
                    navigate("/calificar", { state: { reserva: detalleModal } });
                  }}
                >
                  Calificar Experiencia
                </button>
              )}
              <button className="modal-btn modal-btn--cancel" onClick={() => setDetalleModal(null)}>
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}

      <Footer />
    </div>
  );
}

export default MisReservas;
