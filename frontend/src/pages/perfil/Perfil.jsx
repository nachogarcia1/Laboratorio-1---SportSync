import { useState } from "react";
import { apiFetch } from "../../utils/api";
import NavbarPrivate from "../../components/navbar/NavbarPrivate";
import Footer from "../../components/footer/Footer";
import "./Perfil.css";

function Perfil() {
  const usuario = (() => {
    try { return JSON.parse(sessionStorage.getItem("usuario")); }
    catch { return null; }
  })();

  const [tabActiva, setTabActiva] = useState("canchas");

  const [modalAbierto, setModalAbierto] = useState(false);
  const [form, setForm] = useState({
    nombre:   usuario?.nombre || "",
    email:    usuario?.email  || "",
    dni:      "",
    telefono: "",
    error:    ""
  });

  // Hardcoded visual — se conecta al backend después
  const stats = {
    totalReservas: 12,
    ratingPromedio: 4.3
  };

  const calificaciones = [
    { id: 1, cancha: "Fútbol 5", sede: "Sede Palermo",   fecha: "25/04/2024", rating: 5 },
    { id: 2, cancha: "Fútbol 7", sede: "Sede Belgrano",  fecha: "21/04/2024", rating: 4 },
    { id: 3, cancha: "Fútbol 5", sede: "Sede Caballito", fecha: "15/04/2024", rating: 4 },
  ];

  const renderEstrellas = (rating) =>
    Array.from({ length: 5 }, (_, i) => (
      <span key={i} className={i < rating ? "estrella--llena" : "estrella--vacia"}>★</span>
    ));



  const handleAbrirModal = async () => {
    try {
      const data = await apiFetch(`/usuarios/${usuario.id}`);
      setForm({
        nombre:   data.nombre   || "",
        email:    data.email    || "",
        dni:      data.dni      || "",
        telefono: data.telefono || "",
        error:    ""
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
    try {
      const data = await apiFetch(`/usuarios/${usuario.id}`, {
        method: "PUT",
        body: JSON.stringify({
          nombre:   form.nombre,
          email:    form.email,
          dni:      form.dni,
          telefono: form.telefono
        })
      });
      // Actualizar sessionStorage con los nuevos datos
      sessionStorage.setItem("usuario", JSON.stringify({
        ...usuario,
        nombre: form.nombre,
        email:  form.email
      }));
      setModalAbierto(false);
    } catch (error) {
      setForm((f) => ({ ...f, error: error.message }));
    }
  };


  return (
    <div className="perfil-page">
      <NavbarPrivate />

      <main className="perfil-page__main">
        <h1 className="perfil-page__title">Perfil de Usuario</h1>

        <div className="perfil-layout">

          {/* Sidebar izquierdo */}
          <aside className="perfil-card">
            <div className="perfil-avatar">
              <span>{usuario?.nombre?.charAt(0).toUpperCase()}</span>
            </div>

            <h2 className="perfil-nombre">{usuario?.nombre}</h2>

            <div className="perfil-info-row">
              <span>✉</span>
              <span>{usuario?.email}</span>
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
                  <h4 className="perfil-socio__title">Datos de Socio</h4>
                  <p className="perfil-socio__info">✓ Membresía activa</p>
                  <p className="perfil-socio__info">✓ Descuentos exclusivos</p>
                  <p className="perfil-socio__info">✓ Prioridad en reservas</p>
                </>
              ) : (
                <>
                  <p className="perfil-socio__promo">
                    ¡Aprovechá los beneficios exclusivos siendo socio!
                  </p>
                  <button className="perfil-socio__btn">
                    Hacerme Socio
                  </button>
                </>
              )}
            </div>
          </aside>

          {/* Contenido derecho */}
          <div className="perfil-content">

            <section className="perfil-stats">
              <h3 className="perfil-section-title">Estadísticas</h3>
              <div className="perfil-stats__grid">
                <div className="perfil-stat-card perfil-stat-card--blue">
                  <span className="perfil-stat-card__icon">📋</span>
                  <div>
                    <p className="perfil-stat-card__label">Total Reservas</p>
                    <p className="perfil-stat-card__value">{stats.totalReservas}</p>
                  </div>
                </div>
                <div className="perfil-stat-card perfil-stat-card--green">
                  <span className="perfil-stat-card__icon">⭐</span>
                  <div>
                    <p className="perfil-stat-card__label">Tu Rating Promedio</p>
                    <p className="perfil-stat-card__value">{stats.ratingPromedio}</p>
                  </div>
                </div>
              </div>
            </section>

            <section className="perfil-ratings">
              <h3 className="perfil-section-title">Mis Calificaciones</h3>

              <div className="perfil-ratings__tabs">
                <button
                  className={`perfil-tab ${tabActiva === "canchas" ? "perfil-tab--active" : ""}`}
                  onClick={() => setTabActiva("canchas")}
                >
                  Canchas
                </button>
                <button
                  className={`perfil-tab ${tabActiva === "sedes" ? "perfil-tab--active" : ""}`}
                  onClick={() => setTabActiva("sedes")}
                >
                  Sedes
                </button>
              </div>

              <div className="perfil-ratings__list">
                {calificaciones.map((cal) => (
                  <div key={cal.id} className="perfil-rating-item">
                    <div className="perfil-rating-item__info">
                      <span className="perfil-rating-item__cancha">{cal.cancha}</span>
                      <span className="perfil-rating-item__sede">{cal.sede}</span>
                      <span className="perfil-rating-item__fecha">{cal.fecha}</span>
                    </div>
                    <div className="perfil-rating-item__estrellas">
                      {renderEstrellas(cal.rating)}
                    </div>
                  </div>
                ))}
              </div>
            </section>

          </div>
        </div>
      </main>

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
                type="text"
                value={form.nombre}
                onChange={(e) => setForm({ ...form, nombre: e.target.value })}
              />

              <label>Correo Electrónico</label>
              <input
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />

              <label>DNI</label>
              <input
                type="text"
                placeholder="Ingresa tu DNI"
                value={form.dni}
                onChange={(e) => setForm({ ...form, dni: e.target.value.replace(/\D/g, "") })}
              />

              <label>Teléfono</label>
              <input
                type="text"
                placeholder="Ingresa tu teléfono"
                value={form.telefono}
                onChange={(e) => setForm({ ...form, telefono: e.target.value.replace(/\D/g, "") })}
              />

              {form.error && <p className="form__error">{form.error}</p>}

              <div className="modal-actions">
                <button type="button" className="modal-btn-cancel" onClick={handleCerrarModal}>
                  Cancelar
                </button>
                <button type="submit" className="modal-btn-save">
                  Guardar Cambios
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
