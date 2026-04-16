import { useNavigate } from "react-router-dom";
import NavbarPrivate from "../../components/navbar/NavbarPrivate";
import Footer from "../../components/footer/Footer";
import "./Home.css";

function Home() {
  const usuario = (() => {
    try { return JSON.parse(sessionStorage.getItem("usuario")); }
    catch { return null; }
  })();
  const navigate = useNavigate();

  return (
    <div className="home-page">
      <NavbarPrivate />

      <main className="home-page__main">
        <h1 className="home-page__title">Encuentra y Reserva Tu Cancha Perfecta</h1>
        <p className="home-page__subtitle">
          Explora la mejor red de sedes deportivas en Buenos Aires. ¡Tu próximo partido te espera!
        </p>

        {usuario?.rol === "SOCIO" && (
        <section className="home-banner">
          <div className="home-banner__text">
            ⓘ ¡Eres socio! Disfruta de descuentos exclusivos y prioridad en reservas.
          </div>
          <button className="home-banner__button">Ver Beneficios</button>
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

      <Footer />
    </div>
  );
}

export default Home;