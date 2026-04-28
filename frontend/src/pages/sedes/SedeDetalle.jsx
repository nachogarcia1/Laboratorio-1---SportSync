import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import NavbarPrivate from "../../components/navbar/NavbarPrivate";
import Footer from "../../components/footer/Footer";
import { apiFetch } from "../../utils/api";
import "./SedeDetalle.css";

const DIAS  = ["Domingo","Lunes","Martes","Miércoles","Jueves","Viernes","Sábado"];
const MESES = ["Enero","Febrero","Marzo","Abril","Mayo","Junio","Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"];


function horaStr(h) {
  return `${String(h).padStart(2, "0")}:00`;
}

function toFechaISO(o) {
  const d = new Date();
  d.setDate(d.getDate() + o);
  const yyyy = d.getFullYear();
  const mm   = String(d.getMonth() + 1).padStart(2, "0");
  const dd   = String(d.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

function SedeDetalle() {
  const { id }   = useParams();
  const navigate = useNavigate();

  const [sede,    setSede]    = useState(null);
  const [canchas, setCanchas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState("");
  const [filtro,  setFiltro]  = useState(null);
  const [offset,  setOffset]  = useState(0);
  const [expandidos, setExpandidos] = useState(new Set());

  // Availability & ratings
  const [disponibilidadMap, setDisponibilidadMap] = useState({});
  const [ratingsMap,        setRatingsMap]        = useState({});
  const [loadingGrupos,     setLoadingGrupos]     = useState(new Set());
  const [refreshKey,        setRefreshKey]        = useState(0);

  // Booking modal
  const [modal,         setModal]         = useState(null);
  const [iluminacion,   setIluminacion]   = useState(false);
  const [bookingStatus, setBookingStatus] = useState(null);
  const [bookingError,  setBookingError]  = useState("");

  const usuario = (() => {
    try { return JSON.parse(sessionStorage.getItem("usuario")); }
    catch { return null; }
  })();


  // Load sede + canchas once
  useEffect(() => {
    Promise.all([
      apiFetch(`/sedes/${id}`),
      apiFetch(`/canchas/sede/${id}`)
    ])
      .then(([sedeData, canchasData]) => {
        setSede(sedeData);
        setCanchas(canchasData);
        setExpandidos(new Set([...new Set(canchasData.map(c => c.tipo))]));
      })
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  }, [id]);

  // Fetch ratings once when canchas are loaded
  useEffect(() => {
    if (canchas.length === 0) return;
    canchas.forEach(c => {
      apiFetch(`/feedback/canchas/${c.id}/rating`)
        .then(data => setRatingsMap(prev => ({ ...prev, [c.id]: data.rating })))
        .catch(() => setRatingsMap(prev => ({ ...prev, [c.id]: 0 })));
    });
  }, [canchas]);

  // Fetch availability when canchas loaded, date changes, or booking made
  useEffect(() => {
    if (canchas.length === 0) return;
    const fechaStr = toFechaISO(offset);
    const tipos = [...new Set(canchas.map(c => c.tipo))];

    tipos.forEach(tipo => {
      const canchasGrupo = canchas.filter(c => c.tipo === tipo);
      setLoadingGrupos(prev => new Set([...prev, tipo]));

      Promise.all(
        canchasGrupo.map(c =>
          apiFetch(`/reservas/disponibilidad?canchaId=${c.id}&fecha=${fechaStr}`)
            .then(data => ({ canchaId: c.id, ocupados: data }))
            .catch(() => ({ canchaId: c.id, ocupados: [] }))
        )
      )
        .then(results => {
          setDisponibilidadMap(prev => {
            const next = { ...prev };
            results.forEach(({ canchaId, ocupados }) => { next[canchaId] = ocupados; });
            return next;
          });
        })
        .finally(() => {
          setLoadingGrupos(prev => {
            const next = new Set(prev);
            next.delete(tipo);
            return next;
          });
        });
    });
  }, [canchas, offset, refreshKey]);

  const fecha = new Date();
  fecha.setDate(fecha.getDate() + offset);
  const fechaFormateada = `${DIAS[fecha.getDay()]} ${fecha.getDate()} de ${MESES[fecha.getMonth()]}`;

  function getSlotsDisponibles(canchasGrupo) {
    const HORA_APERTURA = parseInt(sede?.horaApertura) || 8;
    const HORA_CIERRE   = parseInt(sede?.horaCierre)   || 22;
    const slots = [];
    for (let h = HORA_APERTURA; h < HORA_CIERRE; h++) {
      const ini = horaStr(h);
      const fin = horaStr(h + 1);
      const canchaLibre = canchasGrupo.find(c => {
        const ocupados = disponibilidadMap[c.id] || [];
        return !ocupados.some(r => {
          const rIni = r.horaInicio ? r.horaInicio.substring(0, 5) : "";
          return rIni === ini;
        });
      });
      if (canchaLibre) {
        slots.push({ ini, fin, canchaId: canchaLibre.id });
      }
    }
    return slots;
  }

  function getBadge(horaInicio) {
    const h = parseInt(horaInicio.split(":")[0], 10);
    if (usuario?.acreditado && h >= 18) return "socio";
    if (h >= 18 && h <= 20) return "popular";
    return "normal";
  }

  function getGrupoRating(canchasGrupo) {
    const ratings = canchasGrupo
      .map(c => ratingsMap[c.id])
      .filter(r => r != null && r > 0);
    if (ratings.length === 0) return null;
    return (ratings.reduce((a, b) => a + b, 0) / ratings.length).toFixed(1);
  }

  const gruposMap = canchas.reduce((acc, cancha) => {
    if (!acc[cancha.tipo]) acc[cancha.tipo] = [];
    acc[cancha.tipo].push(cancha);
    return acc;
  }, {});

  const grupos = Object.entries(gruposMap)
    .sort(([a], [b]) => Number(a) - Number(b))
    .filter(([tipo]) => !filtro || Number(tipo) === filtro);

  const toggleExpandido = (tipo) => {
    setExpandidos(prev => {
      const next = new Set(prev);
      next.has(tipo) ? next.delete(tipo) : next.add(tipo);
      return next;
    });
  };

  function closeModal() {
    setModal(null);
    setBookingStatus(null);
    setBookingError("");
    setIluminacion(false);
  }

  async function handleReservar() {
    if (!modal || !usuario) return;
    setBookingStatus("loading");
    setBookingError("");
    try {
      await apiFetch("/reservas", {
        method: "POST",
        body: JSON.stringify({
          usuarioId: usuario.id,
          canchaId: modal.canchaId,
          fecha: toFechaISO(offset),
          horaInicio: modal.horaInicio + ":00",
          horaFin: modal.horaFin + ":00",
          iluminacion,
          equipamiento: []
        })
      });
      setBookingStatus("success");
      setTimeout(() => {
        closeModal();
        setRefreshKey(k => k + 1);
      }, 1500);
    } catch (e) {
      setBookingError(e.message);
      setBookingStatus("error");
    }
  }

  return (
    <div className="sede-detalle-page">
      <NavbarPrivate />
      <main className="sede-detalle__main">

        {loading && <p className="sede-estado">Cargando...</p>}
        {error   && <p className="sede-estado sede-estado--error">{error}</p>}

        {!loading && sede && (
          <>
            <div className="sede-detalle__header">
              <h1 className="sede-detalle__title">
                Canchas <span className="sede-detalle__verde">Disponibles</span> en {sede.nombre}
              </h1>
              <button className="sede-detalle__cambiar-btn" onClick={() => navigate("/sedes")}>
                🏟 Cambiar zona ›
              </button>
            </div>

            <div className="sede-detalle__filters">
              <div className="tipo-tabs">
                {[5, 7, 11].map(tipo => (
                  <button
                    key={tipo}
                    className={`tipo-tab ${filtro === tipo ? "tipo-tab--active" : ""}`}
                    onClick={() => setFiltro(filtro === tipo ? null : tipo)}
                  >
                    ⚽ Fútbol {tipo}
                  </button>
                ))}
              </div>
              <div className="fecha-selector">
                <span>📅</span>
                <span className="fecha-selector__texto">{fechaFormateada}</span>
                <button
                  className="fecha-nav-btn"
                  onClick={() => setOffset(o => Math.max(0, o - 1))}
                  disabled={offset === 0}
                >‹</button>
                <button
                  className="fecha-nav-btn"
                  onClick={() => setOffset(o => o + 1)}
                >›</button>
              </div>
            </div>

            {grupos.length === 0 && (
              <p className="sede-estado">No hay canchas disponibles.</p>
            )}

            <div className="canchas-list">
              {grupos.map(([tipo, canchasGrupo]) => {
                const tipoNum   = Number(tipo);
                const isLoading = loadingGrupos.has(tipoNum);
                const slots     = getSlotsDisponibles(canchasGrupo);
                const rating    = getGrupoRating(canchasGrupo);

                return (
                  <div key={tipo} className="cancha-card">
                    {expandidos.has(tipoNum) && (
                      <div className="cancha-card__body">
                        <div className="cancha-card__image"></div>
                        <div className="cancha-card__info">
                          <div className="cancha-card__title-row">
                            <h3 className="cancha-card__nombre">Fútbol {tipo}</h3>
                            {rating && (
                              <span className="cancha-card__rating">⭐ {rating}</span>
                            )}
                          </div>

                          {isLoading ? (
                            <p className="cancha-turnos-placeholder">Cargando turnos...</p>
                          ) : slots.length === 0 ? (
                            <p className="cancha-turnos-placeholder">No hay turnos disponibles para este día.</p>
                          ) : (
                            <div className="cancha-card__turnos">
                              {slots.map(({ ini, fin, canchaId }) => {
                                const badge = getBadge(ini);
                                return (
                                  <div key={ini} className="turno-row">
                                    <span className="turno-hora">{ini}</span>
                                    <span className={`turno-badge turno-badge--${badge}`}>
                                      {badge === "normal"  && fin}
                                      {badge === "popular" && "Turno Popular +5% de dto."}
                                      {badge === "socio"   && "Descuento Socio -10%"}
                                    </span>
                                    <button
                                      className="turno-reservar-btn"
                                      onClick={() => setModal({ canchaId, tipo: tipoNum, horaInicio: ini, horaFin: fin })}
                                    >
                                      Reservar
                                    </button>
                                  </div>
                                );
                              })}
                            </div>
                          )}
                        </div>
                      </div>
                    )}
                    <div
                      className="cancha-card__footer"
                      onClick={() => toggleExpandido(tipoNum)}
                    >
                      <div className="cancha-card__footer-left">
                        <span>x{canchasGrupo.length} {canchasGrupo.length === 1 ? "Cancha" : "Canchas"}</span>
                        <span>💲 Desde ${canchasGrupo[0].precioBase.toLocaleString("es-AR")}</span>
                      </div>
                      <span className="cancha-card__footer-tipo">Fútbol {tipo}</span>
                      <span className="cancha-card__chevron">
                        {expandidos.has(tipoNum) ? "▲" : "▼"}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          </>
        )}

        {/* Booking Modal */}
        {modal && (
          <div className="modal-overlay" onClick={() => { if (bookingStatus !== "loading") closeModal(); }}>
            <div className="modal-box" onClick={e => e.stopPropagation()}>
              {bookingStatus === "success" ? (
                <div className="modal-success">
                  <span className="modal-success__icon">✓</span>
                  <p>¡Reserva confirmada!</p>
                </div>
              ) : (
                <>
                  <h2 className="modal-title">Confirmar Reserva</h2>
                  <div className="modal-info">
                    <p><strong>Cancha:</strong> Fútbol {modal.tipo} — {sede?.nombre}</p>
                    <p><strong>Fecha:</strong> {fechaFormateada}</p>
                    <p><strong>Horario:</strong> {modal.horaInicio} - {modal.horaFin} hs</p>
                  </div>
                  <label className="modal-iluminacion">
                    <input
                      type="checkbox"
                      checked={iluminacion}
                      onChange={e => setIluminacion(e.target.checked)}
                    />
                    Iluminación (+$500)
                  </label>
                  {bookingStatus === "error" && (
                    <p className="modal-error">{bookingError}</p>
                  )}
                  <div className="modal-actions">
                    <button
                      className="modal-btn modal-btn--cancel"
                      onClick={closeModal}
                      disabled={bookingStatus === "loading"}
                    >
                      Cancelar
                    </button>
                    <button
                      className="modal-btn modal-btn--confirm"
                      onClick={handleReservar}
                      disabled={bookingStatus === "loading"}
                    >
                      {bookingStatus === "loading" ? "Reservando..." : "Confirmar"}
                    </button>
                  </div>
                </>
              )}
            </div>
          </div>
        )}

      </main>
      <Footer />
    </div>
  );
}

export default SedeDetalle;
