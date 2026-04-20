import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiFetch } from "../../utils/api";
import NavbarPrivate from "../../components/navbar/NavbarPrivate";
import Footer from "../../components/footer/Footer";
import "./Home.css";

const BENEFICIOS = [
  { icon: "💰", texto: "10% de descuento en todas tus reservas" },
  { icon: "⚡", texto: "Acceso anticipado para reservar canchas" },
  { icon: "🏆", texto: "Canchas exclusivas para socios" },
  { icon: "🎉", texto: "Invitaciones a eventos y torneos especiales" },
  { icon: "🎧", texto: "Soporte prioritario" },
  { icon: "📅", texto: "Membresía válida por 12 meses" },
];

function Home() {
  const navigate = useNavigate();

  const [usuario, setUsuario] = useState(() => {
    try { return JSON.parse(sessionStorage.getItem("usuario")); }
    catch { return null; }
  });

  const [modalSocio,      setModalSocio]      = useState(false);
  const [modalBeneficios, setModalBeneficios] = useState(false);
  const [errorSocio,      setErrorSocio]      = useState("");

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
        ...usuario,
        rol: "SOCIO",
        fechaInicioSocio:     data.fechaInicioSocio,
        fechaVencimientoSocio: data.fechaVencimientoSocio
      };
      sessionStorage.setItem("token",   data.token);
      sessionStorage.setItem("usuario", JSON.stringify(usuarioActualizado));
      setUsuario(usuarioActualizado);
      setModalSocio(false);
    } catch (err) {
      setErrorSocio(err.message);
    }
  };

  return (
    <div className="home-page">
      <NavbarPrivate />

      <main className="home-page__main">
        <h1 className="home-page__title">Encuentra y Reserva Tu Cancha Perfecta</h1>
        <p className="home-page__subtitle">
          Explora la mejor red de sedes deportivas en Buenos Aires. ¡Tu próximo partido te espera!
        </p>

        {/* Banner NO_SOCIO */}
        {usuario?.rol === "NO_SOCIO" && (
          <section className="home-banner home-banner--promo">
            <div className="home-banner__left">
              <span className="home-banner__texto">
                ¡Desbloqueá descuentos exclusivos, prioridad en reservas y mucho más haciéndote socio!
              </span>
            </div>
            <div className="home-banner__actions">
              <button
                className="home-banner__info-btn"
                onClick={() => setModalBeneficios(true)}
                title="Ver beneficios"
              >
                ℹ
              </button>
              <button className="home-banner__button" onClick={() => setModalSocio(true)}>
                Hacerme Socio
              </button>
            </div>
          </section>
        )}

        {/* Banner SOCIO */}
        {usuario?.rol === "SOCIO" && (
          <section className="home-banner home-banner--socio">
            <div className="home-banner__texto">
              ✓ Sos socio de SportSync — disfrutá de todos tus beneficios exclusivos
            </div>
            <button className="home-banner__button home-banner__button--outline" onClick={() => setModalBeneficios(true)}>
              Ver Beneficios
            </button>
          </section>
        )}

        <section className="home-search">
          <h2 className="home-search__title">Busca tu Cancha</h2>
          <div className="home-search__row">
            <input type="text" placeholder="Buscar por sede o ubicación..." />
            <select>
              <option>Tipo de Cancha</option>
              <option>Fútbol 5</option>
              <option>Fútbol 7</option>
              <option>Fútbol 11</option>
            </select>
            <button>Buscar</button>
          </div>
          <button className="home-search__filter-btn">Filtros Avanzados</button>
        </section>

        <h2 className="home-section-title">
          Sedes Destacadas{usuario?.nombre ? `, ${usuario.nombre}` : ""}
        </h2>

        <section className="home-cards">
          <p className="home-sedes-placeholder">
            Las sedes destacadas estarán disponibles próximamente.
          </p>
        </section>

        <button className="home-page__all-sedes-btn" onClick={() => navigate("/sedes")}>
          Explorar Todas las Sedes
        </button>
      </main>

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
              <button
                className="modal-form__link-beneficios"
                onClick={() => { setModalSocio(false); setModalBeneficios(true); }}
              >
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
                {BENEFICIOS.map((b, i) => (
                  <li key={i} className="beneficios-lista__item">
                    <span className="beneficios-lista__icon">{b.icon}</span>
                    <span>{b.texto}</span>
                  </li>
                ))}
              </ul>
              {usuario?.rol === "NO_SOCIO" && (
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

      <Footer />
    </div>
  );
}

export default Home;