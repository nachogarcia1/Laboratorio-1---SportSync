import { useState, useEffect } from "react";
import { apiFetch } from "../../utils/api";
import NavbarPrivate from "../../components/navbar/NavbarPrivate";
import Footer from "../../components/footer/Footer";
import "./Perfil.css";

function Perfil() {
  const [usuario, setUsuario] = useState(() => {
    try { return JSON.parse(sessionStorage.getItem("usuario")); }
    catch { return null; }
  });

  const [modalSocio,            setModalSocio]            = useState(false);
  const [modalBeneficios,       setModalBeneficios]       = useState(false);
  const [modalConfirmarCancelar, setModalConfirmarCancelar] = useState(false);
  const [errorSocio,            setErrorSocio]            = useState("");
  const [modalAbierto,          setModalAbierto]          = useState(false);
  const [form, setForm] = useState({
    nombre: usuario?.nombre || "", email: usuario?.email || "",
    dni: "", telefono: "", error: ""
  });

  const [reservas,       setReservas]       = useState([]);
  const [criticasCancha, setCriticasCancha] = useState([]);
  const [calificaciones, setCalificaciones] = useState([]);
  const [loadingStats,   setLoadingStats]   = useState(true);

  const [modalCalificar,     setModalCalificar]     = useState(null);
  const [notaCancha,         setNotaCancha]         = useState(0);
  const [notaStaff,          setNotaStaff]          = useState(0);
  const [notaServicios,      setNotaServicios]      = useState(0);
  const [comentarioCalificar, setComentarioCalificar] = useState("");
  const [errorCalificar,     setErrorCalificar]     = useState("");
  const [enviando,           setEnviando]           = useState(false);

  useEffect(() => {
    if (!usuario) return;
    Promise.allSettled([
      apiFetch(`/reservas/usuario/${usuario.id}`),
      apiFetch(`/criticas/canchas/usuario/${usuario.id}`),
      apiFetch(`/criticas/usuarios/${usuario.id}`)
    ])
      .then(([res, criticas, califs]) => {
        if (res.status      === "fulfilled") setReservas(res.value);
        if (criticas.status === "fulfilled") setCriticasCancha(criticas.value);
        if (califs.status   === "fulfilled") setCalificaciones(califs.value);
      })
      .finally(() => setLoadingStats(false));
  }, []);

  const criticasMap = new Map(criticasCancha.map(c => [c.reservaId, c]));

  const ratingPromedio = calificaciones.length
    ? (calificaciones.reduce((acc, c) => acc + c.nota, 0) / calificaciones.length).toFixed(1)
    : "—";

  const esTerminada = (r) => new Date(`${r.fecha}T${r.horaFin}`) < new Date();

  const handleEnviarCalificacion = async (e) => {
    e.preventDefault();
    if (!notaCancha || !notaStaff || !notaServicios) {
      setErrorCalificar("Completá las tres notas."); return;
    }
    setEnviando(true);
    setErrorCalificar("");
    try {
      await apiFetch("/criticas/canchas", {
        method: "POST",
        body: JSON.stringify({
          usuarioId:    usuario.id,
          canchaId:     modalCalificar.cancha.id,
          reservaId:    modalCalificar.id,
          notaCancha, notaStaff, notaServicios,
          comentario:   comentarioCalificar.trim() || null
        })
      });
      const criticas = await apiFetch(`/criticas/canchas/usuario/${usuario.id}`);
      setCriticasCancha(criticas);
      setModalCalificar(null);
      setNotaCancha(0); setNotaStaff(0); setNotaServicios(0);
      setComentarioCalificar("");
    } catch (err) {
      setErrorCalificar(err.message);
    } finally {
      setEnviando(false);
    }
  };

  const renderEstrellas = (rating) =>
    Array.from({ length: 5 }, (_, i) => (
      <span key={i} className={i < rating ? "estrella--llena" : "estrella--vacia"}>★</span>
    ));

  const handleAbrirModal = async () => {
    try {
      const data = await apiFetch(`/usuarios/${usuario.id}`);
      setForm({
        nombre: data.nombre || "", email: data.email || "",
        dni: data.dni || "", telefono: data.telefono || "", error: ""
      });
      setModalAbierto(true);
    } catch (error) {
      console.error("Error al cargar perfil:", error);
    }
  };

  const handleCerrarModal = () => setModalAbierto(false);

  const handleGuardar = async (e) => {
    e.preventDefault();
    setForm((f) => ({ ...f, error: "" }));

    if (form.nombre.trim().length < 2) {
      setForm((f) => ({ ...f, error: "El nombre debe tener al menos 2 caracteres" })); return;
    }
    if (form.dni && form.dni.length < 7) {
      setForm((f) => ({ ...f, error: "El DNI debe tener al menos 7 dígitos" })); return;
    }
    if (form.telefono && form.telefono.length < 7) {
      setForm((f) => ({ ...f, error: "El teléfono debe tener al menos 7 dígitos" })); return;
    }

    try {
      await apiFetch(`/usuarios/${usuario.id}`, {
        method: "PUT",
        body: JSON.stringify({
          nombre: form.nombre, email: form.email,
          dni: form.dni, telefono: form.telefono
        })
      });
      sessionStorage.setItem("usuario", JSON.stringify({
        ...usuario, nombre: form.nombre, email: form.email
      }));
      setModalAbierto(false);
    } catch (error) {
      setForm((f) => ({ ...f, error: error.message }));
    }
  };

  const formatFecha = (str) => {
    if (!str) return "—";
    const [y, m, d] = str.split("-");
    return `${d}/${m}/${y}`;
  };

  const handleAcreditar = async () => {
    setErrorSocio("");
    try {
      const data = await apiFetch(`/usuarios/${usuario.id}/acreditar`, { method: "PUT" });
      const usuarioActualizado = {
        ...usuario, rol: "SOCIO",
        fechaInicioSocio: data.fechaInicioSocio,
        fechaVencimientoSocio: data.fechaVencimientoSocio
      };
      sessionStorage.setItem("token", data.token);
      sessionStorage.setItem("usuario", JSON.stringify(usuarioActualizado));
      setUsuario(usuarioActualizado);
      setModalSocio(false);
    } catch (err) {
      setErrorSocio(err.message);
    }
  };

  const handleCancelarSocio = async () => {
    try {
      const data = await apiFetch(`/usuarios/${usuario.id}/cancelar-socio`, { method: "PUT" });
      const usuarioActualizado = {
        ...usuario, rol: "NO_SOCIO",
        fechaInicioSocio: null, fechaVencimientoSocio: null
      };
      sessionStorage.setItem("token", data.token);
      sessionStorage.setItem("usuario", JSON.stringify(usuarioActualizado));
      setUsuario(usuarioActualizado);
      setModalConfirmarCancelar(false);
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="perfil-page">
      <NavbarPrivate />
      <main className="perfil-page__main">
        <h1 className="perfil-page__title">Perfil de Usuario</h1>
        <div className="perfil-layout">

          <aside className="perfil-card">
            <div className="perfil-avatar">
              <span>{usuario?.nombre?.charAt(0).toUpperCase()}</span>
            </div>
            <h2 className="perfil-nombre">{usuario?.nombre}</h2>
            <div className="perfil-info-row">
              <span>✉</span><span>{usuario?.email}</span>
            </div>
            <div className="perfil-info-row">
              <span>📱</span>
              <span className="perfil-info--placeholder">No disponible</span>
            </div>
            <button className="perfil-edit-btn" onClick={handleAbrirModal}>Editar Perfil</button>
            <hr className="perfil-divider" />

            <div className="perfil-socio">
              {usuario?.rol === "SOCIO" ? (
                <>
                  <h4 className="perfil-socio__title">Membresía Activa</h4>
                  <p className="perfil-socio__info">✓ Socio desde: <strong>{formatFecha(usuario.fechaInicioSocio)}</strong></p>
                  <p className="perfil-socio__info">✓ Válida hasta: <strong>{formatFecha(usuario.fechaVencimientoSocio)}</strong></p>
                  <p className="perfil-socio__info">✓ Descuentos exclusivos activos</p>
                  <button className="perfil-socio__btn perfil-socio__btn--info" onClick={() => setModalBeneficios(true)}>
                    ℹ Ver beneficios
                  </button>
                  <button
                    className="perfil-socio__btn perfil-socio__btn--cancelar"
                    onClick={() => setModalConfirmarCancelar(true)}
                  >
                    Cancelar membresía
                  </button>
                </>
              ) : (
                <>
                  <p className="perfil-socio__promo">¡Aprovechá los beneficios exclusivos siendo socio!</p>
                  <div className="perfil-socio__actions">
                    <button className="perfil-socio__btn" onClick={() => setModalSocio(true)}>
                      Hacerme Socio
                    </button>
                    <button className="perfil-socio__info-btn" onClick={() => setModalBeneficios(true)} title="Ver beneficios">
                      ℹ
                    </button>
                  </div>
                </>
              )}
            </div>
          </aside>

          <div className="perfil-content">
            <section className="perfil-stats">
              <h3 className="perfil-section-title">Estadísticas</h3>
              <div className="perfil-stats__grid">
                <div className="perfil-stat-card perfil-stat-card--blue">
                  <span className="perfil-stat-card__icon">📋</span>
                  <div>
                    <p className="perfil-stat-card__label">Total Reservas</p>
                    <p className="perfil-stat-card__value">{loadingStats ? "…" : reservas.length}</p>
                  </div>
                </div>
                <div className="perfil-stat-card perfil-stat-card--green">
                  <span className="perfil-stat-card__icon">⭐</span>
                  <div>
                    <p className="perfil-stat-card__label">Tu Rating Promedio</p>
                    <p className="perfil-stat-card__value">{loadingStats ? "…" : ratingPromedio}</p>
                  </div>
                </div>
              </div>
            </section>

            <section className="perfil-ratings">
              <h3 className="perfil-section-title">Mis Reservas</h3>
              <div className="perfil-ratings__list">
                {loadingStats && <p className="perfil-ratings__msg">Cargando...</p>}
                {!loadingStats && reservas.length === 0 && (
                  <p className="perfil-ratings__msg">No tenés reservas todavía.</p>
                )}
                {!loadingStats && reservas.map((r) => {
                  const critica = criticasMap.get(r.id);
                  const terminada = esTerminada(r);
                  const promedio = critica
                    ? Math.round((critica.notaCancha + critica.notaStaff + critica.notaServicios) / 3)
                    : null;
                  return (
                    <div key={r.id} className="perfil-rating-item">
                      <div className="perfil-rating-item__info">
                        <span className="perfil-rating-item__cancha">{r.cancha?.nombre}</span>
                        <span className="perfil-rating-item__sede">{formatFecha(r.fecha)}</span>
                        <span className="perfil-rating-item__fecha">{r.horaInicio} — {r.horaFin}</span>
                      </div>
                      {critica
                        ? <div className="perfil-rating-item__estrellas">{renderEstrellas(promedio)}</div>
                        : terminada
                          ? <button className="perfil-calificar-btn" onClick={() => { setModalCalificar(r); setErrorCalificar(""); }}>Calificar</button>
                          : null
                      }
                    </div>
                  );
                })}
              </div>
            </section>
          </div>
        </div>
      </main>

      {/* Modal Editar Perfil */}
      {modalAbierto && (
        <div className="modal-overlay" onClick={handleCerrarModal}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">Editar Perfil</h2>
              <button className="modal-close" onClick={handleCerrarModal}>✕</button>
            </div>
            <form className="modal-form" onSubmit={handleGuardar}>
              <label>Nombre Completo</label>
              <input
                type="text" value={form.nombre} maxLength={100} required
                onChange={(e) => setForm({ ...form, nombre: e.target.value })}
              />
              <label>Correo Electrónico</label>
              <input
                type="email" value={form.email} maxLength={150} required
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />
              <label>DNI</label>
              <input
                type="text" placeholder="Ingresa tu DNI"
                value={form.dni} maxLength={12}
                onChange={(e) => setForm({ ...form, dni: e.target.value.replace(/\D/g, "") })}
              />
              <label>Teléfono</label>
              <input
                type="text" placeholder="Ingresa tu teléfono"
                value={form.telefono} maxLength={20}
                onChange={(e) => setForm({ ...form, telefono: e.target.value.replace(/\D/g, "") })}
              />
              {form.error && <p className="form__error">{form.error}</p>}
              <div className="modal-actions">
                <button type="button" className="modal-btn-cancel" onClick={handleCerrarModal}>Cancelar</button>
                <button type="submit" className="modal-btn-save">Guardar Cambios</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Confirmar Cancelar Membresía */}
      {modalConfirmarCancelar && (
        <div className="modal-overlay" onClick={() => setModalConfirmarCancelar(false)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">Cancelar Membresía</h2>
              <button className="modal-close" onClick={() => setModalConfirmarCancelar(false)}>✕</button>
            </div>
            <div className="modal-form">
              <p style={{ marginBottom: "1rem" }}>
                ¿Estás seguro? Perderás todos los beneficios de socio al cancelar tu membresía.
              </p>
              <div className="modal-actions">
                <button className="modal-btn-cancel" onClick={() => setModalConfirmarCancelar(false)}>
                  Volver
                </button>
                <button className="modal-btn-save" style={{ background: "#ef4444" }} onClick={handleCancelarSocio}>
                  Sí, cancelar membresía
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Modal Hacerme Socio */}
      {modalSocio && (
        <div className="modal-overlay" onClick={() => setModalSocio(false)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">Hacerme Socio</h2>
              <button className="modal-close" onClick={() => setModalSocio(false)}>✕</button>
            </div>
            <div className="modal-form">
              <p style={{ marginBottom: "1rem" }}>
                Al confirmar, tu cuenta se acreditará como socio y tendrás acceso inmediato a todos los beneficios por <strong>12 meses</strong>.
              </p>
              <button className="modal-form__link-beneficios" onClick={() => { setModalSocio(false); setModalBeneficios(true); }}>
                ℹ Ver todos los beneficios
              </button>
              {errorSocio && <p className="form__error">{errorSocio}</p>}
              <div className="modal-actions">
                <button className="modal-btn-cancel" onClick={() => setModalSocio(false)}>Cancelar</button>
                <button className="modal-btn-save" onClick={handleAcreditar}>Confirmar</button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Modal Beneficios */}
      {modalBeneficios && (
        <div className="modal-overlay" onClick={() => setModalBeneficios(false)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">Beneficios de Ser Socio</h2>
              <button className="modal-close" onClick={() => setModalBeneficios(false)}>✕</button>
            </div>
            <div className="modal-form">
              <ul className="beneficios-lista">
                {[
                  { icon: "💰", texto: "10% de descuento en todas tus reservas" },
                  { icon: "⚡", texto: "Acceso anticipado para reservar canchas" },
                  { icon: "🏆", texto: "Canchas exclusivas para socios" },
                  { icon: "🎉", texto: "Invitaciones a eventos y torneos especiales" },
                  { icon: "🎧", texto: "Soporte prioritario" },
                  { icon: "📅", texto: "Membresía válida por 12 meses" },
                ].map((b, i) => (
                  <li key={i} className="beneficios-lista__item">
                    <span className="beneficios-lista__icon">{b.icon}</span>
                    <span>{b.texto}</span>
                  </li>
                ))}
              </ul>
              {usuario?.rol !== "SOCIO" && (
                <div className="modal-actions">
                  <button className="modal-btn-cancel" onClick={() => setModalBeneficios(false)}>Cerrar</button>
                  <button className="modal-btn-save" onClick={() => { setModalBeneficios(false); setModalSocio(true); }}>
                    Hacerme Socio
                  </button>
                </div>
              )}
              {usuario?.rol === "SOCIO" && (
                <div className="modal-actions">
                  <button className="modal-btn-save" onClick={() => setModalBeneficios(false)}>Cerrar</button>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {modalCalificar && (
        <div className="modal-overlay" onClick={() => setModalCalificar(null)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">Calificar reserva</h2>
              <button className="modal-close" onClick={() => setModalCalificar(null)}>✕</button>
            </div>
            <form className="modal-form" onSubmit={handleEnviarCalificacion}>
              <p style={{ marginBottom: "1rem", color: "#4c5562" }}>
                <strong>{modalCalificar.cancha?.nombre}</strong> — {formatFecha(modalCalificar.fecha)}
              </p>
              {[
                { label: "Cancha", val: notaCancha, set: setNotaCancha },
                { label: "Staff",  val: notaStaff,  set: setNotaStaff  },
                { label: "Servicios", val: notaServicios, set: setNotaServicios }
              ].map(({ label, val, set }) => (
                <div key={label} style={{ marginBottom: "0.75rem" }}>
                  <label>{label}</label>
                  <div style={{ fontSize: "1.75rem" }}>
                    {[1,2,3,4,5].map(n => (
                      <span key={n} style={{ cursor: "pointer", color: n <= val ? "#f59e0b" : "#d1d5db" }}
                        onClick={() => set(n)}>★</span>
                    ))}
                  </div>
                </div>
              ))}
              <label>Comentario (opcional)</label>
              <textarea
                value={comentarioCalificar}
                onChange={e => setComentarioCalificar(e.target.value)}
                rows={3} placeholder="Contanos tu experiencia..."
                style={{ width: "100%", padding: "0.5rem", borderRadius: "6px", border: "1px solid #ccc", marginBottom: "1rem" }}
              />
              {errorCalificar && <p className="form__error">{errorCalificar}</p>}
              <div className="modal-actions">
                <button type="button" className="modal-btn-cancel" onClick={() => setModalCalificar(null)}>Cancelar</button>
                <button type="submit" className="modal-btn-save" disabled={enviando}>
                  {enviando ? "Enviando..." : "Enviar Calificación"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <Footer />
    </div>
  );
}

export default Perfil;