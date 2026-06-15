import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiFetch } from "../../utils/api";
import NavbarPrivate from "../../components/navbar/NavbarPrivate";
import Footer from "../../components/footer/Footer";
import FormularioTarjeta from "../../components/FormularioTarjeta";
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
  const [procesandoSocio, setProcesandoSocio] = useState(false);

  // Búsqueda rápida (delega en /buscar)
  const [busqueda,     setBusqueda]     = useState("");
  const [tipoBusqueda, setTipoBusqueda] = useState("");

  const irABuscar = (conFiltros) => {
    if (!conFiltros) { navigate("/buscar"); return; }
    const params = new URLSearchParams();
    if (busqueda.trim()) params.set("nombre", busqueda.trim());
    if (tipoBusqueda)    params.set("tipo", tipoBusqueda);
    const qs = params.toString();
    navigate(qs ? `/buscar?${qs}` : "/buscar");
  };

  const formatFecha = (str) => {
    if (!str) return "—";
    const [y, m, d] = str.split("-");
    return `${d}/${m}/${y}`;
  };

  const handleAcreditar = async (datosTarjeta) => {
    setErrorSocio("");
    setProcesandoSocio(true);
    try {
      const data = await apiFetch(`/usuarios/${usuario.id}/acreditar`, {
        method: "PUT",
        body: JSON.stringify(datosTarjeta)
      });
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
    } finally {
      setProcesandoSocio(false);
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
          <form className="home-search__row" onSubmit={e => { e.preventDefault(); irABuscar(true); }}>
            <input
              type="text"
              placeholder="Buscar por sede o ubicación..."
              value={busqueda}
              onChange={e => setBusqueda(e.target.value)}
            />
            <select value={tipoBusqueda} onChange={e => setTipoBusqueda(e.target.value)}>
              <option value="">Tipo de Cancha</option>
              <option value="5">Fútbol 5</option>
              <option value="7">Fútbol 7</option>
              <option value="11">Fútbol 11</option>
            </select>
            <button type="submit">Buscar</button>
          </form>
          <button className="home-search__filter-btn" onClick={() => irABuscar(false)}>
            Filtros Avanzados
          </button>
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
              <p style={{ marginBottom: "0.75rem" }}>
                La membresía se acredita por <strong>12 meses</strong> tras abonar la cuota con tarjeta. Solo te hacés socio si el pago es aprobado.
              </p>
              <button
                className="modal-form__link-beneficios"
                onClick={() => { setModalSocio(false); setModalBeneficios(true); }}
              >
                ℹ Ver todos los beneficios
              </button>
              <FormularioTarjeta
                onPagar={handleAcreditar}
                procesando={procesandoSocio}
                error={errorSocio}
                botonTexto="Pagar y asociarme"
              />
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