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

  const HORA_APERTURA = parseInt(sede?.horaApertura) || 8;
  const HORA_CIERRE   = parseInt(sede?.horaCierre) || 22;

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
  const [antelacionMin,     setAntelacionMin]     = useState(30);

  // Booking modal
  const [modal,         setModal]         = useState(null);
  const [iluminacion,   setIluminacion]   = useState(false);
  const [bookingStatus, setBookingStatus] = useState(null);
  const [bookingError,  setBookingError]  = useState("");
  const [descuentosMap,  setDescuentosMap]  = useState({});
  const [precioModal,    setPrecioModal]    = useState(null);
  const [loadingPrecio,  setLoadingPrecio]  = useState(false);
  const [extras,         setExtras]         = useState([]);   // ítems disponibles en la sede
  const [cantidades,     setCantidades]     = useState({});   // { itemId: cantidad }

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
      apiFetch(`/criticas/canchas/${c.id}/rating`)
        .then(data => setRatingsMap(prev => ({ ...prev, [c.id]: data.rating })))
        .catch(() => setRatingsMap(prev => ({ ...prev, [c.id]: 0 })));
    });
  }, [canchas]);

  useEffect(() => {
    if (canchas.length === 0) return;
    canchas.forEach(c => {
      apiFetch(`/reservas/descuentos?canchaId=${c.id}`)
        .then(data => {
          const mapa = {};
          data.forEach(d => { mapa[d.hora] = d.descuentoPorcentaje; });
          setDescuentosMap(prev => ({ ...prev, [c.id]: mapa }));
        })
        .catch(() => {});
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

  // Antelación mínima (la misma que valida el backend) para grisar turnos
  useEffect(() => {
    apiFetch("/reservas/config")
      .then(cfg => { if (cfg?.antelacionMinimaMinutos != null) setAntelacionMin(cfg.antelacionMinimaMinutos); })
      .catch(() => {});
  }, []);

  // Extras disponibles en esta sede (propios + globales)
  useEffect(() => {
    if (!id) return;
    apiFetch(`/equipamiento?sedeId=${id}`)
      .then(data => setExtras(Array.isArray(data) ? data : []))
      .catch(() => setExtras([]));
  }, [id]);

  const fecha = new Date();
  fecha.setDate(fecha.getDate() + offset);
  const fechaFormateada = `${DIAS[fecha.getDay()]} ${fecha.getDate()} de ${MESES[fecha.getMonth()]}`;

  // "Ahora" en minutos del día, según la zona horaria de la sede.
  function minutosAhoraEnSede() {
    const tz = sede?.zonaHoraria || "America/Argentina/Buenos_Aires";
    const ahoraSede = new Date(new Date().toLocaleString("en-US", { timeZone: tz }));
    return ahoraSede.getHours() * 60 + ahoraSede.getMinutes();
  }

  function getSlotsDisponibles(canchasGrupo) {
    const slots = [];
    // Solo el día de hoy (offset 0) tiene turnos vencidos; los días futuros, no.
    const esHoy = offset === 0;
    const limiteMin = esHoy ? minutosAhoraEnSede() + antelacionMin : -1;

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
        // Deshabilitado si el turno arranca antes de "ahora + antelación" (en tz de la sede).
        const vencido = esHoy && (h * 60) < limiteMin;
        slots.push({ ini, fin, canchaId: canchaLibre.id, vencido });
      }
    }
    return slots;
  }

  function getDescuentoSlot(canchaId, hora) {
    return descuentosMap[canchaId]?.[hora] || 0;
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
    setPrecioModal(null);
    setCantidades({});
  }

  // ¿El turno permite iluminación? Solo desde las 18:00.
  function permiteIluminacion(horaInicio) {
    return parseInt(horaInicio) >= 18;
  }

  // Total de extras (ítems) seleccionados
  function totalExtras() {
    const total = extras.reduce((acc, it) => acc + (cantidades[it.id] || 0) * it.precioPorUnidad, 0);
    return precioModal?.esSocio ? Math.round(total * 0.9) : total;
  }

  function totalExtrasOriginal() {
    return extras.reduce((acc, it) => acc + (cantidades[it.id] || 0) * it.precioPorUnidad, 0);
  }

  function setCantidad(itemId, cantidad, stock) {
    const c = Math.max(0, Math.min(cantidad, stock));
    setCantidades(prev => ({ ...prev, [itemId]: c }));
  }

  async function handleAbrirModal(canchaId, tipo, horaInicio, horaFin) {
    setModal({ canchaId, tipo, horaInicio, horaFin });
    setPrecioModal(null);
    setCantidades({});
    setIluminacion(false);
    setLoadingPrecio(true);
    try {
      const data = await apiFetch(`/reservas/precio-preview?canchaId=${canchaId}&hora=${horaInicio}&usuarioId=${usuario?.id}`);
      setPrecioModal(data);
    } catch {
    } finally {
      setLoadingPrecio(false);
    }
  }

  async function handleReservar() {
    if (!modal || !usuario) return;
    setBookingStatus("loading");
    setBookingError("");
    try {
      const equipamiento = extras
        .filter(it => (cantidades[it.id] || 0) > 0)
        .map(it => ({ itemId: it.id, cantidad: cantidades[it.id] }));
      await apiFetch("/reservas", {
        method: "POST",
        body: JSON.stringify({
          usuarioId: usuario.id,
          canchaId: modal.canchaId,
          fecha: toFechaISO(offset),
          horaInicio: modal.horaInicio + ":00",
          horaFin: modal.horaFin + ":00",
          iluminacion: iluminacion && permiteIluminacion(modal.horaInicio),
          equipamiento
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
                              {slots.map(({ ini, fin, canchaId, vencido }) => {
                                const descuento = getDescuentoSlot(canchaId, ini);
                                return (
                                  <div key={ini} className={`turno-row${vencido ? " turno-row--vencido" : ""}`}>
                                    <span className="turno-hora">{ini}</span>
                                    {vencido ? (
                                      <span className="turno-badge turno-badge--vencido">Horario no disponible</span>
                                    ) : descuento > 0 ? (
                                      <span className="turno-badge turno-badge--descuento">🏷️ -{descuento}% dto.</span>
                                    ) : (
                                      <span className="turno-badge turno-badge--normal">{fin}</span>
                                    )}
                                    <button
                                      className="turno-reservar-btn"
                                      disabled={vencido}
                                      onClick={() => !vencido && handleAbrirModal(canchaId, tipoNum, ini, fin)}
                                    >
                                      {vencido ? "No disponible" : "Reservar"}
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

                    {loadingPrecio && <p className="modal-precio-cargando">Calculando precio...</p>}

                    {precioModal && (
                      <div className="modal-precio">
                        {/* Precio base con descuento inteligente */}
                        {precioModal.descuentoPorcentaje > 0 ? (
                          <>
                            <p>Precio cancha: <span className="precio-tachado">${precioModal.precioBase.toLocaleString("es-AR")}</span></p>
                            <p className="modal-precio__descuento">Descuento horario -{precioModal.descuentoPorcentaje}%: -${(precioModal.precioBase - precioModal.precioTrasPrecioInteligente).toLocaleString("es-AR")}</p>
                            {precioModal.esSocio && <p>Precio intermedio: <strong>${precioModal.precioTrasPrecioInteligente.toLocaleString("es-AR")}</strong></p>}
                          </>
                        ) : precioModal.esSocio ? (
                          <p>Precio cancha: <span className="precio-tachado">${precioModal.precioBase.toLocaleString("es-AR")}</span></p>
                        ) : (
                          <p>Precio cancha: <strong>${precioModal.precioFinal.toLocaleString("es-AR")}</strong></p>
                        )}

                        {/* Descuento socio */}
                        {precioModal.esSocio && (
                          <p className="modal-precio__descuento">🎟️ Descuento socio -10%: -${(precioModal.precioTrasPrecioInteligente - precioModal.precioFinal).toLocaleString("es-AR")}</p>
                        )}

                        {/* Subtotal cancha */}
                        {(precioModal.descuentoPorcentaje > 0 || precioModal.esSocio) && (
                          <p>Subtotal cancha: <strong>${precioModal.precioFinal.toLocaleString("es-AR")}</strong></p>
                        )}

                        {/* Extras */}
                        {totalExtrasOriginal() > 0 && (
                          <p className="modal-precio__extra">
                            Extras:{" "}
                            {precioModal.esSocio ? (
                              <>
                                <span className="precio-tachado">${totalExtrasOriginal().toLocaleString("es-AR")}</span>
                                {" → "}<strong>${totalExtras().toLocaleString("es-AR")}</strong>
                                <span className="modal-precio__socio-badge"> (−10% socio)</span>
                              </>
                            ) : (
                              <strong>+${totalExtras().toLocaleString("es-AR")}</strong>
                            )}
                          </p>
                        )}

                        {/* Iluminación */}
                        {iluminacion && permiteIluminacion(modal.horaInicio) && (
                          <p className="modal-precio__extra">
                            Iluminación:{" "}
                            {precioModal.esSocio ? (
                              <>
                                <span className="precio-tachado">$500</span>
                                {" → "}<strong>$450</strong>
                                <span className="modal-precio__socio-badge"> (−10% socio)</span>
                              </>
                            ) : (
                              <strong>+$500</strong>
                            )}
                          </p>
                        )}

                        <p className="modal-precio__total">
                          Total: <strong>${(
                            precioModal.precioFinal +
                            totalExtras() +
                            (iluminacion && permiteIluminacion(modal.horaInicio) ? (precioModal.esSocio ? 450 : 500) : 0)
                          ).toLocaleString("es-AR")}</strong>
                        </p>
                      </div>
                    )}
                  </div>

                  {/* ── Extras ── */}
                  <div className="modal-extras">
                    <p className="modal-extras__titulo">Extras</p>
                    {extras.length === 0 ? (
                      <p className="modal-extras__vacio">No hay extras disponibles en esta sede.</p>
                    ) : (
                      extras.map(it => (
                        <div key={it.id} className="modal-extra-row">
                          <span className="modal-extra-row__nombre">{it.nombre}</span>
                          <span className="modal-extra-row__precio">${it.precioPorUnidad.toLocaleString("es-AR")} c/u</span>
                          <div className="modal-extra-row__qty">
                            <button type="button"
                              onClick={() => setCantidad(it.id, (cantidades[it.id] || 0) - 1, it.stock)}
                              disabled={(cantidades[it.id] || 0) <= 0}>−</button>
                            <span>{cantidades[it.id] || 0}</span>
                            <button type="button"
                              onClick={() => setCantidad(it.id, (cantidades[it.id] || 0) + 1, it.stock)}
                              disabled={(cantidades[it.id] || 0) >= it.stock}>+</button>
                          </div>
                        </div>
                      ))
                    )}
                  </div>

                  {/* Iluminación: solo desde las 18:00 */}
                  {permiteIluminacion(modal.horaInicio) ? (
                    <label className="modal-iluminacion">
                      <input
                        type="checkbox"
                        checked={iluminacion}
                        onChange={e => setIluminacion(e.target.checked)}
                      />
                      {precioModal?.esSocio ? (
                        <>Iluminación (<span className="precio-tachado">$500</span> → $450 <span className="modal-precio__socio-badge">−10% socio</span>)</>
                      ) : (
                        <>Iluminación (+$500)</>
                      )}
                    </label>
                  ) : (
                    <p className="modal-iluminacion modal-iluminacion--nodisp">
                      💡 La iluminación está disponible solo para turnos desde las 18:00.
                    </p>
                  )}
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
