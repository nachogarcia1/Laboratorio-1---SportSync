import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { apiFetch } from "../../utils/api";
import NavbarPrivate from "../../components/navbar/NavbarPrivate";
import Footer from "../../components/footer/Footer";
import "./Admin.css";

const NAV_ITEMS = [
  { id: "dashboard",  label: "Dashboard",           icon: "📊" },
  { id: "sedes",      label: "Gestión de Sedes",     icon: "🏢" },
  { id: "canchas",    label: "Gestión de Canchas",   icon: "⚽" },
  { id: "reservas",   label: "Supervisar Reservas",  icon: "📋" },
  { id: "usuarios",   label: "Gestión de Usuarios",  icon: "👥" },
  { id: "ratings",    label: "Historial de Ratings", icon: "⭐" },
];

const ACTIVIDAD = [
  { id: 1, tipo: "info",    texto: "Nueva Reserva: Fútbol 5 en Sede Palermo para el 28/04 a las 19:00.", accion: "Ver" },
  { id: 2, tipo: "alerta",  texto: "Alerta: Usuario 'Juan Perez' ha reportado un problema con la cancha 3 de Sede Belgrano.", accion: "Resolver" },
  { id: 3, tipo: "success", texto: "Nuevo Registro: 'María Lopez' se ha unido como socia.", accion: "Ver Perfil" },
];

function Admin() {
  const [seccion, setSeccion]             = useState("dashboard");
  const [suspenderAbierto, setSuspender]  = useState(false);
  const [modalSede,        setModalSede]        = useState(false);
  const [modalCancha,      setModalCancha]       = useState(false);
  const [formSede,         setFormSede]          = useState({ nombre: "", direccion: "", horarios: "", error: "" });
  const [formCancha,       setFormCancha]        = useState({ sedeId: "", nombre: "", tipo: "5", precioBase: "", error: "" });
  const [sedesDisponibles, setSedesDisponibles]  = useState([]);
  const [sedes,            setSedes]             = useState([]);
  const [canchas,          setCanchas]           = useState([]);
  const [sedeCanchasId,    setSedeCanchasId]     = useState("");
  const [menuAbierto,      setMenuAbierto]       = useState(null);
  const [modalSuspender,   setModalSuspender]    = useState(false);
  const [tipoSuspension,   setTipoSuspension]    = useState("TEMPORAL");
  const [emailBusqueda,    setEmailBusqueda]     = useState("");
  const [usuarioEncontrado,setUsuarioEncontrado] = useState(null);
  const [errorBusqueda,    setErrorBusqueda]     = useState("");
  const [diasSuspension,   setDiasSuspension]    = useState(7);
  const [usuarios,         setUsuarios]          = useState([]);
  const [filtroUsuario,    setFiltroUsuario]     = useState("TODOS");
  const [emailCalificar,   setEmailCalificar]    = useState("");
  const [usuarioTarget,    setUsuarioTarget]     = useState(null);
  const [busquedaError,    setBusquedaError]     = useState("");
  const [notaUsuario,      setNotaUsuario]       = useState(0);
  const [hoverNota,        setHoverNota]         = useState(0);
  const [comentarioU,      setComentarioU]       = useState("");
  const [envioMsg,         setEnvioMsg]          = useState("");
  const [envioError,       setEnvioError]        = useState("");
  const [modalCalificarUser, setModalCalificarUser] = useState(null);

  // Modal genérico de confirmación
  const [modalConfirmar, setModalConfirmar] = useState(null);
  // { titulo, mensaje, danger, onConfirmar }

  // Modal días suspensión temporal desde ⋮
  const [modalSuspTemporal, setModalSuspTemporal] = useState(null); // userId
  const [diasModalTemp,     setDiasModalTemp]     = useState(7);

  const adminData = (() => {
    try { return JSON.parse(sessionStorage.getItem("usuario")); }
    catch { return null; }
  })();

  const stats = [
    { label: "Reservas Hoy",       valor: 25, color: "blue",   icon: "📋" },
    { label: "Nuevos Usuarios",    valor: 12, color: "green",  icon: "👥" },
    { label: "Ratings Pendientes", valor: 7,  color: "yellow", icon: "⭐" },
  ];

  const handleCrearSede = async (e) => {
    e.preventDefault();
    setFormSede(f => ({ ...f, error: "" }));
    try {
      await apiFetch("/sedes", {
        method: "POST",
        body: JSON.stringify({
          nombre: "Sede " + formSede.nombre,
          direccion: formSede.direccion,
          horarios:  formSede.horarios
        })
      });
      setModalSede(false);
      setFormSede({ nombre: "", direccion: "", horarios: "", error: "" });
    } catch (err) {
      setFormSede(f => ({ ...f, error: err.message }));
    }
  };

  const handleAbrirModalCancha = async () => {
    try {
      const data = await apiFetch("/sedes");
      setSedesDisponibles(data);
      if (data.length > 0) setFormCancha(f => ({ ...f, sedeId: data[0].id }));
    } catch (err) { console.error(err); }
    setModalCancha(true);
  };

  const handleCrearCancha = async (e) => {
    e.preventDefault();
    setFormCancha(f => ({ ...f, error: "" }));
    try {
      await apiFetch(`/canchas/sede/${formCancha.sedeId}`, {
        method: "POST",
        body: JSON.stringify({
          nombre:     formCancha.nombre,
          tipo:       parseInt(formCancha.tipo),
          precioBase: parseFloat(formCancha.precioBase)
        })
      });
      setModalCancha(false);
      setFormCancha({ sedeId: "", nombre: "", tipo: "5", precioBase: "", error: "" });
    } catch (err) {
      setFormCancha(f => ({ ...f, error: err.message }));
    }
  };

  useEffect(() => {
    if (seccion === "sedes" || seccion === "canchas") {
      apiFetch("/sedes/admin/todas")
        .then(data => {
          setSedes(data);
          if (seccion === "canchas" && data.length > 0) {
            setSedeCanchasId(String(data[0].id));
            apiFetch(`/canchas/admin/sede/${data[0].id}`).then(setCanchas).catch(console.error);
          }
        })
        .catch(console.error);
    }
    if (seccion === "usuarios") {
      apiFetch("/usuarios").then(setUsuarios).catch(console.error);
    }
  }, [seccion]);

  const handleToggleSede = async (id) => {
    try {
      const res = await apiFetch(`/sedes/${id}/toggle-activa`, { method: "PUT" });
      setSedes(prev => prev.map(s => s.id === id ? { ...s, activa: res.activa } : s));
    } catch (err) { console.error(err); }
    setMenuAbierto(null);
  };

  const handleEliminarSede = (id) => {
    setMenuAbierto(null);
    setModalConfirmar({
      titulo: "Eliminar Sede",
      mensaje: "¿Eliminar esta sede? Esta acción no se puede deshacer.",
      danger: true,
      onConfirmar: async () => {
        try {
          await apiFetch(`/sedes/${id}`, { method: "DELETE" });
          setSedes(prev => prev.filter(s => s.id !== id));
        } catch (err) { console.error(err); }
      }
    });
  };

  const cargarCanchas = async (sedeId) => {
    try {
      const data = await apiFetch(`/canchas/admin/sede/${sedeId}`);
      setCanchas(data);
    } catch (err) { console.error(err); }
  };

  const handleToggleCancha = async (id, estadoActual) => {
    const nuevoEstado = estadoActual === "HABILITADA" ? "NO_HABILITADA" : "HABILITADA";
    try {
      await apiFetch(`/canchas/${id}/estado`, {
        method: "PUT",
        body: JSON.stringify({ estado: nuevoEstado })
      });
      setCanchas(prev => prev.map(c => c.id === id ? { ...c, estado: nuevoEstado } : c));
    } catch (err) { console.error(err); }
    setMenuAbierto(null);
  };

  const handleEliminarCancha = (id) => {
    setMenuAbierto(null);
    setModalConfirmar({
      titulo: "Eliminar Cancha",
      mensaje: "¿Eliminar esta cancha? Esta acción no se puede deshacer.",
      danger: true,
      onConfirmar: async () => {
        try {
          await apiFetch(`/canchas/${id}`, { method: "DELETE" });
          setCanchas(prev => prev.filter(c => c.id !== id));
        } catch (err) { console.error(err); }
      }
    });
  };

  const abrirModalSuspender = (tipo) => {
    setTipoSuspension(tipo);
    setEmailBusqueda("");
    setUsuarioEncontrado(null);
    setErrorBusqueda("");
    setDiasSuspension(7);
    setSuspender(false);
    setModalSuspender(true);
  };

  const handleBuscarUsuario = async (e) => {
    if (e?.preventDefault) e.preventDefault();
    setErrorBusqueda("");
    setUsuarioEncontrado(null);
    try {
      const data = await apiFetch(`/usuarios/buscar?email=${encodeURIComponent(emailBusqueda)}`);
      setUsuarioEncontrado(data);
    } catch (err) {
      setErrorBusqueda(err.message);
    }
  };

  const handleConfirmarSuspension = async () => {
    if (!usuarioEncontrado) return;
    try {
      await apiFetch(`/usuarios/${usuarioEncontrado.id}/suspender`, {
        method: "PUT",
        body: JSON.stringify({ tipo: tipoSuspension, dias: diasSuspension })
      });
      setModalSuspender(false);
      if (seccion === "usuarios") apiFetch("/usuarios").then(setUsuarios).catch(console.error);
    } catch (err) {
      setErrorBusqueda(err.message);
    }
  };

  const handleSuspenderTemporal = (id) => {
    setMenuAbierto(null);
    setDiasModalTemp(7);
    setModalSuspTemporal(id);
  };

  const handleConfirmarSuspTemporal = async () => {
    if (isNaN(diasModalTemp) || diasModalTemp <= 0) return;
    try {
      await apiFetch(`/usuarios/${modalSuspTemporal}/suspender`, {
        method: "PUT",
        body: JSON.stringify({ tipo: "TEMPORAL", dias: diasModalTemp })
      });
      apiFetch("/usuarios").then(setUsuarios).catch(console.error);
    } catch (err) { console.error(err); }
    setModalSuspTemporal(null);
  };

  const handleSuspenderPermanente = (id) => {
    setMenuAbierto(null);
    setModalConfirmar({
      titulo: "Suspender Permanentemente",
      mensaje: "¿Suspender permanentemente a este usuario? No podrá acceder a la plataforma.",
      danger: true,
      onConfirmar: async () => {
        try {
          await apiFetch(`/usuarios/${id}/suspender`, {
            method: "PUT",
            body: JSON.stringify({ tipo: "PERMANENTE" })
          });
          apiFetch("/usuarios").then(setUsuarios).catch(console.error);
        } catch (err) { console.error(err); }
      }
    });
  };

  const handleRehabilitarUsuario = (id) => {
    setMenuAbierto(null);
    setModalConfirmar({
      titulo: "Rehabilitar Usuario",
      mensaje: "¿Rehabilitar a este usuario? Recuperará el acceso normal a la plataforma.",
      danger: false,
      onConfirmar: async () => {
        try {
          await apiFetch(`/usuarios/${id}/rehabilitar`, { method: "PUT" });
          apiFetch("/usuarios").then(setUsuarios).catch(console.error);
        } catch (err) { console.error(err); }
      }
    });
  };

  const handleEliminarUsuario = (id) => {
    setMenuAbierto(null);
    setModalConfirmar({
      titulo: "Eliminar Usuario",
      mensaje: "¿Eliminar permanentemente a este usuario? Esta acción no se puede deshacer.",
      danger: true,
      onConfirmar: async () => {
        try {
          await apiFetch(`/usuarios/${id}`, { method: "DELETE" });
          setUsuarios(prev => prev.filter(u => u.id !== id));
        } catch (err) { console.error(err); }
      }
    });
  };

  const handleCalificarUsuario = async (e) => {
    e.preventDefault();
    if (notaUsuario === 0) { setEnvioError("Seleccioná una nota."); return; }
    setEnvioError("");
    try {
      await apiFetch("/criticas/usuarios", {
        method: "POST",
        body: JSON.stringify({
          adminId:    adminData.id,
          usuarioId:  usuarioTarget.id,
          nota:       notaUsuario,
          comentario: comentarioU.trim() || null
        })
      });
      setEnvioMsg(`Calificación enviada para ${usuarioTarget.nombre}.`);
      setNotaUsuario(0);
      setComentarioU("");
      setUsuarioTarget(null);
      setEmailCalificar("");
    } catch (err) {
      setEnvioError(err.message);
    }
  };

  const handleBuscarParaCalificar = async (e) => {
    e.preventDefault();
    setBusquedaError("");
    setUsuarioTarget(null);
    setEnvioMsg("");
    setEnvioError("");
    try {
      const data = await apiFetch(`/usuarios/buscar?email=${encodeURIComponent(emailCalificar)}`);
      setUsuarioTarget(data);
    } catch (err) {
      setBusquedaError(err.message);
    }
  };

  const handleCalificarDesdeMenu = async (e) => {
    e.preventDefault();
    if (notaUsuario === 0) { setEnvioError("Seleccioná una nota."); return; }
    setEnvioError("");
    try {
      await apiFetch("/criticas/usuarios", {
        method: "POST",
        body: JSON.stringify({
          adminId:    adminData.id,
          usuarioId:  modalCalificarUser.id,
          nota:       notaUsuario,
          comentario: comentarioU.trim() || null
        })
      });
      setEnvioMsg(`Calificación enviada para ${modalCalificarUser.nombre}.`);
      setNotaUsuario(0);
      setComentarioU("");
      setModalCalificarUser(null);
    } catch (err) {
      setEnvioError(err.message);
    }
  };

  return (
    <div className="admin-page">
      <NavbarPrivate />
      <main className="admin-page__main">
        <h1 className="admin-page__title">Panel de Administrador</h1>
        <div className="admin-layout">

          <aside className="admin-sidebar">
            <h3 className="admin-sidebar__title">Navegación</h3>
            <nav className="admin-nav">
              {NAV_ITEMS.map((item) => (
                <button
                  key={item.id}
                  className={`admin-nav__item ${seccion === item.id ? "admin-nav__item--active" : ""}`}
                  onClick={() => setSeccion(item.id)}
                >
                  <span>{item.icon}</span>
                  <span>{item.label}</span>
                </button>
              ))}
            </nav>
          </aside>

          <div className="admin-content">

            {seccion === "dashboard" && (
              <>
                <section className="admin-section">
                  <h2 className="admin-section__title">Dashboard General</h2>
                  <div className="admin-stats-grid">
                    {stats.map((s) => (
                      <div key={s.label} className={`admin-stat-card admin-stat-card--${s.color}`}>
                        <span className="admin-stat-card__icon">{s.icon}</span>
                        <div>
                          <p className="admin-stat-card__label">{s.label}</p>
                          <p className="admin-stat-card__valor">{s.valor}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                </section>

                <section className="admin-section">
                  <h2 className="admin-section__title">Actividad Reciente</h2>
                  <div className="admin-actividad">
                    {ACTIVIDAD.map((a) => (
                      <div key={a.id} className={`admin-actividad__item admin-actividad__item--${a.tipo}`}>
                        <span className="admin-actividad__texto">{a.texto}</span>
                        <button className={`admin-actividad__btn admin-actividad__btn--${a.tipo}`}>{a.accion}</button>
                      </div>
                    ))}
                  </div>
                </section>

                <section className="admin-section">
                  <h2 className="admin-section__title">Acciones Rápidas</h2>
                  <div className="admin-acciones-grid">
                    <button className="admin-accion-btn admin-accion-btn--blue" onClick={() => setModalSede(true)}>+ Añadir Sede</button>
                    <button className="admin-accion-btn admin-accion-btn--blue" onClick={handleAbrirModalCancha}>+ Añadir Cancha</button>
                    <div className="admin-accion-expandable">
                      <button
                        className="admin-accion-btn admin-accion-btn--gray"
                        onClick={() => setSuspender(!suspenderAbierto)}
                      >
                        🚫 Suspender Usuario {suspenderAbierto ? "▲" : "▼"}
                      </button>
                      {suspenderAbierto && (
                        <div className="admin-accion-expandable__opciones">
                          <button className="admin-accion-sub-btn" onClick={() => abrirModalSuspender("TEMPORAL")}>Suspender temporalmente</button>
                          <button className="admin-accion-sub-btn admin-accion-sub-btn--danger" onClick={() => abrirModalSuspender("PERMANENTE")}>Suspender permanentemente</button>
                        </div>
                      )}
                    </div>
                    <button className="admin-accion-btn admin-accion-btn--gray">🚫 Rechazar Reserva</button>
                    <button className="admin-accion-btn admin-accion-btn--gray">📊 Ver Métricas</button>
                    <button className="admin-accion-btn admin-accion-btn--gray">✉ Enviar Notificación</button>
                  </div>
                </section>
              </>
            )}

            {seccion === "sedes" && (
              <section className="admin-section">
                <div className="admin-section__header">
                  <h2 className="admin-section__title">Gestión de Sedes</h2>
                  <button className="admin-accion-btn admin-accion-btn--blue" onClick={() => setModalSede(true)}>+ Añadir Sede</button>
                </div>
                {sedes.length === 0 && <p className="admin-empty">No hay sedes registradas.</p>}
                <div className="admin-lista">
                  {sedes.map(sede => (
                    <div key={sede.id} className="admin-lista__item">
                      <div className="admin-lista__info">
                        <span className="admin-lista__nombre">{sede.nombre}</span>
                        <span className={`admin-lista__badge ${sede.activa ? "badge--activa" : "badge--inactiva"}`}>
                          {sede.activa ? "Activa" : "Inactiva"}
                        </span>
                      </div>
                      <div className="admin-lista__menu">
                        <button
                          className="admin-lista__menu-btn"
                          onClick={() => setMenuAbierto(menuAbierto === `sede-${sede.id}` ? null : `sede-${sede.id}`)}
                        >⋮</button>
                        {menuAbierto === `sede-${sede.id}` && (
                          <div className="admin-dropdown">
                            <button onClick={() => handleToggleSede(sede.id)}>
                              {sede.activa ? "Deshabilitar" : "Habilitar"}
                            </button>
                            <button className="admin-dropdown__danger" onClick={() => handleEliminarSede(sede.id)}>
                              Eliminar
                            </button>
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </section>
            )}

            {seccion === "canchas" && (
              <section className="admin-section">
                <div className="admin-section__header">
                  <h2 className="admin-section__title">Gestión de Canchas</h2>
                  <button className="admin-accion-btn admin-accion-btn--blue" onClick={handleAbrirModalCancha}>+ Añadir Cancha</button>
                </div>
                <div className="admin-sede-selector">
                  <label>Seleccionar sede:</label>
                  <select
                    value={sedeCanchasId}
                    onChange={e => { setSedeCanchasId(e.target.value); cargarCanchas(e.target.value); }}
                  >
                    {sedes.map(s => <option key={s.id} value={s.id}>{s.nombre}</option>)}
                  </select>
                </div>
                {canchas.length === 0 && sedeCanchasId && <p className="admin-empty">No hay canchas en esta sede.</p>}
                <div className="admin-lista">
                  {canchas.map(cancha => (
                    <div key={cancha.id} className="admin-lista__item">
                      <div className="admin-lista__info">
                        <span className="admin-lista__nombre">{cancha.nombre}</span>
                        <span className="admin-lista__subtipo">Fútbol {cancha.tipo}</span>
                        <span className={`admin-lista__badge ${cancha.estado === "HABILITADA" ? "badge--activa" : "badge--inactiva"}`}>
                          {cancha.estado === "HABILITADA" ? "Habilitada" : "Deshabilitada"}
                        </span>
                      </div>
                      <div className="admin-lista__menu">
                        <button
                          className="admin-lista__menu-btn"
                          onClick={() => setMenuAbierto(menuAbierto === `cancha-${cancha.id}` ? null : `cancha-${cancha.id}`)}
                        >⋮</button>
                        {menuAbierto === `cancha-${cancha.id}` && (
                          <div className="admin-dropdown">
                            <button onClick={() => handleToggleCancha(cancha.id, cancha.estado)}>
                              {cancha.estado === "HABILITADA" ? "Deshabilitar" : "Habilitar"}
                            </button>
                            <button className="admin-dropdown__danger" onClick={() => handleEliminarCancha(cancha.id)}>
                              Eliminar
                            </button>
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </section>
            )}

            {seccion === "usuarios" && (
              <section className="admin-section">
                <h2 className="admin-section__title">Gestión de Usuarios</h2>
                <div className="admin-filtros">
                  {[
                    { key: "TODOS",                 label: "Todos" },
                    { key: "ACTIVO",                label: "Activos" },
                    { key: "SUSPENDIDO_TEMPORAL",   label: "Susp. temporal" },
                    { key: "SUSPENDIDO_PERMANENTE", label: "Susp. permanente" },
                  ].map(f => (
                    <button
                      key={f.key}
                      className={`admin-filtro-btn ${filtroUsuario === f.key ? "admin-filtro-btn--active" : ""}`}
                      onClick={() => setFiltroUsuario(f.key)}
                    >{f.label}</button>
                  ))}
                </div>
                <div className="admin-lista">
                  {usuarios
                    .filter(u => filtroUsuario === "TODOS" || u.estado === filtroUsuario)
                    .map(u => (
                      <div key={u.id} className="admin-lista__item">
                        <div className="admin-lista__info">
                          <span className="admin-lista__nombre">{u.nombre}</span>
                          <span className="admin-lista__subtipo">{u.email}</span>
                          <span className="admin-lista__subtipo">DNI: {u.dni}</span>
                          <span className={`admin-lista__badge ${
                            u.estado === "ACTIVO" ? "badge--activa" :
                            u.estado === "SUSPENDIDO_TEMPORAL" ? "badge--temporal" : "badge--inactiva"
                          }`}>
                            {u.estado === "ACTIVO" ? "Activo" :
                             u.estado === "SUSPENDIDO_TEMPORAL" ? `Susp. hasta ${u.suspendidoHasta}` : "Susp. perm."}
                          </span>
                        </div>
                        <div className="admin-lista__menu">
                          <button
                            className="admin-lista__menu-btn"
                            onClick={() => setMenuAbierto(menuAbierto === `usuario-${u.id}` ? null : `usuario-${u.id}`)}
                          >⋮</button>
                          {menuAbierto === `usuario-${u.id}` && (
                            <div className="admin-dropdown">
                              {u.estado === "ACTIVO" ? (
                                <>
                                  <button onClick={() => handleSuspenderTemporal(u.id)}>Suspender temporalmente</button>
                                  <button onClick={() => handleSuspenderPermanente(u.id)}>Suspender permanentemente</button>
                                </>
                              ) : (
                                <button onClick={() => handleRehabilitarUsuario(u.id)}>Rehabilitar</button>
                              )}
                              <button onClick={() => {
                                setModalCalificarUser(u);
                                setNotaUsuario(0);
                                setComentarioU("");
                                setEnvioError("");
                                setEnvioMsg("");
                                setMenuAbierto(null);
                              }}>Calificar</button>
                              <button className="admin-dropdown__danger" onClick={() => handleEliminarUsuario(u.id)}>Eliminar</button>
                            </div>
                          )}
                        </div>
                      </div>
                    ))}
                </div>
              </section>
            )}

            {seccion === "ratings" && (
              <section className="admin-section">
                <h2 className="admin-section__title">Calificar Usuario</h2>
                <form className="admin-buscar-form" onSubmit={handleBuscarParaCalificar}>
                  <label>Buscar usuario por email</label>
                  <div style={{ display: "flex", gap: "0.5rem" }}>
                    <input
                      type="email" placeholder="usuario@email.com"
                      value={emailCalificar}
                      onChange={e => setEmailCalificar(e.target.value)}
                      required
                      style={{ flex: 1, padding: "0.5rem", borderRadius: "6px", border: "1px solid #ccc" }}
                    />
                    <button type="submit" className="modal-btn-save" style={{ whiteSpace: "nowrap" }}>Buscar</button>
                  </div>
                  {busquedaError && <p className="form__error">{busquedaError}</p>}
                </form>
                {usuarioTarget && (
                  <form onSubmit={handleCalificarUsuario} style={{ marginTop: "1.5rem" }}>
                    <p style={{ marginBottom: "0.75rem" }}>
                      Calificando a: <strong>{usuarioTarget.nombre}</strong> ({usuarioTarget.email})
                    </p>
                    <label>Nota</label>
                    <div style={{ fontSize: "2rem", marginBottom: "1rem" }}>
                      {[1,2,3,4,5].map(n => (
                        <span key={n}
                          style={{ cursor: "pointer", color: n <= (hoverNota || notaUsuario) ? "#f59e0b" : "#d1d5db" }}
                          onMouseEnter={() => setHoverNota(n)}
                          onMouseLeave={() => setHoverNota(0)}
                          onClick={() => setNotaUsuario(n)}
                        >★</span>
                      ))}
                    </div>
                    <label>Comentario (opcional)</label>
                    <textarea
                      value={comentarioU} onChange={e => setComentarioU(e.target.value)}
                      rows={3} placeholder="Motivo de la calificación..."
                      style={{ width: "100%", padding: "0.5rem", borderRadius: "6px", border: "1px solid #ccc", marginBottom: "1rem" }}
                    />
                    {envioError && <p className="form__error">{envioError}</p>}
                    <div style={{ display: "flex", gap: "0.5rem" }}>
                      <button type="button" className="modal-btn-cancel" onClick={() => setUsuarioTarget(null)}>Cancelar</button>
                      <button type="submit" className="modal-btn-save">Enviar Calificación</button>
                    </div>
                  </form>
                )}
                {envioMsg && <p style={{ color: "#22c55e", marginTop: "1rem", fontWeight: 600 }}>{envioMsg}</p>}
              </section>
            )}

            {seccion !== "dashboard" && seccion !== "sedes" && seccion !== "canchas" && seccion !== "usuarios" && seccion !== "ratings" && (
              <div className="admin-wip"><p>Sección en desarrollo</p></div>
            )}

          </div>
        </div>
      </main>

      {/* Modal Añadir Sede */}
      {modalSede && (
        <div className="modal-overlay" onClick={() => setModalSede(false)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">Añadir Sede</h2>
              <button className="modal-close" onClick={() => setModalSede(false)}>✕</button>
            </div>
            <form className="modal-form" onSubmit={handleCrearSede}>
              <label>Nombre</label>
              <div className="modal-input-prefix">
                <span className="modal-input-prefix__text">Sede</span>
                <input type="text" placeholder="Ej: Palermo" value={formSede.nombre} maxLength={80} required
                  onChange={e => setFormSede({ ...formSede, nombre: e.target.value })} />
              </div>
              <label>Dirección</label>
              <input type="text" placeholder="Ej: Av. Santa Fe 1234" value={formSede.direccion} maxLength={150} required
                onChange={e => setFormSede({ ...formSede, direccion: e.target.value })} />
              <label>Horarios</label>
              <input type="text" placeholder="Ej: Lunes a Domingo 8:00 - 22:00" value={formSede.horarios} maxLength={100} required
                onChange={e => setFormSede({ ...formSede, horarios: e.target.value })} />
              {formSede.error && <p className="form__error">{formSede.error}</p>}
              <div className="modal-actions">
                <button type="button" className="modal-btn-cancel" onClick={() => setModalSede(false)}>Cancelar</button>
                <button type="submit" className="modal-btn-save">Crear Sede</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Añadir Cancha */}
      {modalCancha && (
        <div className="modal-overlay" onClick={() => setModalCancha(false)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">Añadir Cancha</h2>
              <button className="modal-close" onClick={() => setModalCancha(false)}>✕</button>
            </div>
            <form className="modal-form" onSubmit={handleCrearCancha}>
              <label>Sede</label>
              <select value={formCancha.sedeId} required onChange={e => setFormCancha({ ...formCancha, sedeId: e.target.value })}>
                {sedesDisponibles.map(s => <option key={s.id} value={s.id}>{s.nombre}</option>)}
              </select>
              <label>Nombre</label>
              <input type="text" placeholder="Ej: Cancha Principal" value={formCancha.nombre} maxLength={80} required
                onChange={e => setFormCancha({ ...formCancha, nombre: e.target.value })} />
              <label>Tipo</label>
              <select value={formCancha.tipo} onChange={e => setFormCancha({ ...formCancha, tipo: e.target.value })}>
                <option value="5">Fútbol 5</option>
                <option value="7">Fútbol 7</option>
                <option value="11">Fútbol 11</option>
              </select>
              <label>Precio Base</label>
              <input type="number" placeholder="Ej: 1500" value={formCancha.precioBase} required min="0"
                onChange={e => setFormCancha({ ...formCancha, precioBase: e.target.value })} />
              {formCancha.error && <p className="form__error">{formCancha.error}</p>}
              <div className="modal-actions">
                <button type="button" className="modal-btn-cancel" onClick={() => setModalCancha(false)}>Cancelar</button>
                <button type="submit" className="modal-btn-save">Crear Cancha</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Suspender Usuario (desde dashboard) */}
      {modalSuspender && (
        <div className="modal-overlay" onClick={() => setModalSuspender(false)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">
                {tipoSuspension === "TEMPORAL" ? "Suspender temporalmente" : "Suspender permanentemente"}
              </h2>
              <button className="modal-close" onClick={() => setModalSuspender(false)}>✕</button>
            </div>
            <div className="modal-form">
              <label>Email del usuario</label>
              <div style={{ display: "flex", gap: "0.5rem" }}>
                <input type="email" placeholder="usuario@email.com" value={emailBusqueda}
                  onChange={e => setEmailBusqueda(e.target.value)}
                  onKeyDown={e => e.key === "Enter" && handleBuscarUsuario()} />
                <button type="button" className="modal-btn-save" onClick={handleBuscarUsuario}>Buscar</button>
              </div>
              {errorBusqueda && <p className="form__error">{errorBusqueda}</p>}
              {usuarioEncontrado && (
                <div className="usuario-confirmacion">
                  <p><strong>Nombre:</strong> {usuarioEncontrado.nombre}</p>
                  <p><strong>DNI:</strong> {usuarioEncontrado.dni}</p>
                  <p><strong>Estado actual:</strong> {usuarioEncontrado.estado}</p>
                </div>
              )}
              {tipoSuspension === "TEMPORAL" && usuarioEncontrado && (
                <>
                  <label>Días de suspensión</label>
                  <input type="number" min="1" value={diasSuspension}
                    onChange={e => setDiasSuspension(parseInt(e.target.value))} />
                </>
              )}
              <div className="modal-actions">
                <button type="button" className="modal-btn-cancel" onClick={() => setModalSuspender(false)}>Cancelar</button>
                <button type="button" className="modal-btn-save" onClick={handleConfirmarSuspension} disabled={!usuarioEncontrado}>
                  Confirmar
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Modal Suspender Temporal desde ⋮ */}
      {modalSuspTemporal && (
        <div className="modal-overlay" onClick={() => setModalSuspTemporal(null)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">Suspender Temporalmente</h2>
              <button className="modal-close" onClick={() => setModalSuspTemporal(null)}>✕</button>
            </div>
            <div className="modal-form">
              <label>Días de suspensión</label>
              <input type="number" min="1" value={diasModalTemp}
                onChange={e => setDiasModalTemp(parseInt(e.target.value))} />
              <div className="modal-actions">
                <button type="button" className="modal-btn-cancel" onClick={() => setModalSuspTemporal(null)}>Cancelar</button>
                <button type="button" className="modal-btn-save" onClick={handleConfirmarSuspTemporal}>Confirmar</button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Modal Confirmación Genérico */}
      {modalConfirmar && (
        <div className="modal-overlay" onClick={() => setModalConfirmar(null)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">{modalConfirmar.titulo}</h2>
              <button className="modal-close" onClick={() => setModalConfirmar(null)}>✕</button>
            </div>
            <div className="modal-form">
              <p style={{ marginBottom: "1.5rem" }}>{modalConfirmar.mensaje}</p>
              <div className="modal-actions">
                <button type="button" className="modal-btn-cancel" onClick={() => setModalConfirmar(null)}>Cancelar</button>
                <button
                  type="button"
                  className="modal-btn-save"
                  style={modalConfirmar.danger ? { background: "#ef4444" } : {}}
                  onClick={async () => {
                    await modalConfirmar.onConfirmar();
                    setModalConfirmar(null);
                  }}
                >
                  Confirmar
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Modal Calificar Usuario desde ⋮ */}
      {modalCalificarUser && (
        <div className="modal-overlay" onClick={() => setModalCalificarUser(null)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">Calificar Usuario</h2>
              <button className="modal-close" onClick={() => setModalCalificarUser(null)}>✕</button>
            </div>
            <form className="modal-form" onSubmit={handleCalificarDesdeMenu}>
              <p style={{ marginBottom: "0.75rem" }}>
                Calificando a: <strong>{modalCalificarUser.nombre}</strong> ({modalCalificarUser.email})
              </p>
              <label>Nota</label>
              <div style={{ fontSize: "2rem", marginBottom: "1rem" }}>
                {[1,2,3,4,5].map(n => (
                  <span key={n}
                    style={{ cursor: "pointer", color: n <= (hoverNota || notaUsuario) ? "#f59e0b" : "#d1d5db" }}
                    onMouseEnter={() => setHoverNota(n)}
                    onMouseLeave={() => setHoverNota(0)}
                    onClick={() => setNotaUsuario(n)}
                  >★</span>
                ))}
              </div>
              <label>Comentario (opcional)</label>
              <textarea value={comentarioU} onChange={e => setComentarioU(e.target.value)}
                rows={3} placeholder="Motivo de la calificación..." />
              {envioError && <p className="form__error">{envioError}</p>}
              {envioMsg && <p style={{ color: "#22c55e", fontWeight: 600 }}>{envioMsg}</p>}
              <div className="modal-actions">
                <button type="button" className="modal-btn-cancel" onClick={() => setModalCalificarUser(null)}>Cancelar</button>
                <button type="submit" className="modal-btn-save">Enviar Calificación</button>
              </div>
            </form>
          </div>
        </div>
      )}

      <Footer />
    </div>
  );
}

export default Admin;