import { useState } from "react";
import { Link } from "react-router-dom";
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

  const stats = [
    { label: "Reservas Hoy",       valor: 25, color: "blue",   icon: "📋" },
    { label: "Nuevos Usuarios",    valor: 12, color: "green",  icon: "👥" },
    { label: "Ratings Pendientes", valor: 7,  color: "yellow", icon: "⭐" },
  ];

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
                    <button className="admin-accion-btn admin-accion-btn--blue">+ Añadir Sede</button>
                    <button className="admin-accion-btn admin-accion-btn--blue">+ Añadir Cancha</button>

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

      <Footer />
    </div>
  );
}

export default Admin;
