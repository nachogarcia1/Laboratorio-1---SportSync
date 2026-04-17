import { useState } from "react";
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
    } catch (err) {
      console.error(err);
    }
    setModalCancha(true);
  };

  const handleCrearCancha = async (e) => {
    e.preventDefault();
    setFormCancha(f => ({ ...f, error: "" }));
    try {
      await apiFetch(`/canchas/sede/${formCancha.sedeId}`, {
        method: "POST",
        body: JSON.stringify({
          nombre:    formCancha.nombre,
          tipo:      parseInt(formCancha.tipo),
          precioBase: parseFloat(formCancha.precioBase)
        })
      });
      setModalCancha(false);
      setFormCancha({ sedeId: "", nombre: "", tipo: "5", precioBase: "", error: "" });
    } catch (err) {
      setFormCancha(f => ({ ...f, error: err.message }));
    }
  };


  return (
    <div className="admin-page">
      <NavbarPrivate />

      <main className="admin-page__main">
        <h1 className="admin-page__title">Panel de Administrador</h1>

        <div className="admin-layout">

          {/* ── Sidebar ── */}
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

          {/* ── Contenido ── */}
          <div className="admin-content">

            {seccion === "dashboard" && (
              <>
                {/* Stats */}
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

                {/* Actividad reciente */}
                <section className="admin-section">
                  <h2 className="admin-section__title">Actividad Reciente</h2>
                  <div className="admin-actividad">
                    {ACTIVIDAD.map((a) => (
                      <div key={a.id} className={`admin-actividad__item admin-actividad__item--${a.tipo}`}>
                        <span className="admin-actividad__texto">{a.texto}</span>
                        <button className={`admin-actividad__btn admin-actividad__btn--${a.tipo}`}>
                          {a.accion}
                        </button>
                      </div>
                    ))}
                  </div>
                </section>

                {/* Acciones rápidas */}
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
                          <button className="admin-accion-sub-btn">Suspender temporalmente</button>
                          <button className="admin-accion-sub-btn admin-accion-sub-btn--danger">Suspender permanentemente</button>
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

            {seccion !== "dashboard" && (
              <div className="admin-wip">
                <p>🚧 Sección en desarrollo</p>
              </div>
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
                <input
                  type="text"
                  placeholder="Ej: Palermo"
                  value={formSede.nombre}
                  onChange={e => setFormSede({ ...formSede, nombre: e.target.value })}
                  required
                />
              </div>
              <label>Dirección</label>
              <input
                type="text"
                placeholder="Ej: Av. Santa Fe 1234"
                value={formSede.direccion}
                onChange={e => setFormSede({ ...formSede, direccion: e.target.value })}
                required
              />
              <label>Horarios</label>
              <input
                type="text"
                placeholder="Ej: Lunes a Domingo 8:00 - 22:00"
                value={formSede.horarios}
                onChange={e => setFormSede({ ...formSede, horarios: e.target.value })}
                required
              />
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
              <select
                value={formCancha.sedeId}
                onChange={e => setFormCancha({ ...formCancha, sedeId: e.target.value })}
                required
              >
                {sedesDisponibles.map(s => (
                  <option key={s.id} value={s.id}>{s.nombre}</option>
                ))}
              </select>
              <label>Nombre</label>
              <input
                type="text"
                placeholder="Ej: Cancha Principal"
                value={formCancha.nombre}
                onChange={e => setFormCancha({ ...formCancha, nombre: e.target.value })}
                required
              />
              <label>Tipo</label>
              <select
                value={formCancha.tipo}
                onChange={e => setFormCancha({ ...formCancha, tipo: e.target.value })}
              >
                <option value="5">Fútbol 5</option>
                <option value="7">Fútbol 7</option>
                <option value="11">Fútbol 11</option>
              </select>
              <label>Precio Base</label>
              <input
                type="number"
                placeholder="Ej: 1500"
                value={formCancha.precioBase}
                onChange={e => setFormCancha({ ...formCancha, precioBase: e.target.value })}
                required
                min="0"
              />
              {formCancha.error && <p className="form__error">{formCancha.error}</p>}
              <div className="modal-actions">
                <button type="button" className="modal-btn-cancel" onClick={() => setModalCancha(false)}>Cancelar</button>
                <button type="submit" className="modal-btn-save">Crear Cancha</button>
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
