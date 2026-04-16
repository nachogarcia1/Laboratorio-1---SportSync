import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import NavbarPrivate from "../../components/navbar/NavbarPrivate";
import Footer from "../../components/footer/Footer";
import { apiFetch } from "../../utils/api";
import "./Sedes.css";

const POR_PAGINA = 6;

function Sedes() {
  const navigate = useNavigate();
  const [sedes,   setSedes]   = useState([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState("");
  const [pagina,  setPagina]  = useState(0);

  useEffect(() => {
    apiFetch("/sedes")
      .then(data => setSedes(data))
      .catch(err  => setError(err.message))
      .finally(()  => setLoading(false));
  }, []);

  const totalPaginas = Math.ceil(sedes.length / POR_PAGINA);
  const sedesPagina  = sedes.slice(pagina * POR_PAGINA, (pagina + 1) * POR_PAGINA);

  return (
    <div className="sedes-page">
      <NavbarPrivate />
      <main className="sedes-page__main">
        <h1 className="sedes-page__title">Sedes Deportivas</h1>
        <p className="sedes-page__subtitle">
          Elegí la sede más cercana a vos y reservá tu cancha.
        </p>

        {loading && <p className="sedes-estado">Cargando sedes...</p>}
        {error   && <p className="sedes-estado sedes-estado--error">{error}</p>}

        {!loading && !error && sedes.length === 0 && (
          <p className="sedes-estado">No hay sedes disponibles por el momento.</p>
        )}

        <div className="sedes-grid">
          {sedesPagina.map((sede) => (
            <article key={sede.id} className="sede-card">
              <div className="sede-card__image"></div>
              <div className="sede-card__body">
                <h3 className="sede-card__nombre">{sede.nombre}</h3>
                <p className="sede-card__descripcion">{sede.direccion}</p>
                <div className="sede-card__footer">
                  <span className="sede-card__ubicacion">🕐 {sede.horarios}</span>
                </div>
                <button
                  className="sede-card__btn"
                  onClick={() => navigate(`/sedes/${sede.id}`)}
                >
                  Ver Detalles
                </button>
              </div>
            </article>
          ))}
        </div>

        {totalPaginas > 1 && (
          <div className="sedes-pagination">
            <button
              className="sedes-pagination__btn"
              onClick={() => setPagina(p => p - 1)}
              disabled={pagina === 0}
            >‹ Anterior</button>
            <span className="sedes-pagination__info">{pagina + 1} / {totalPaginas}</span>
            <button
              className="sedes-pagination__btn"
              onClick={() => setPagina(p => p + 1)}
              disabled={pagina === totalPaginas - 1}
            >Siguiente ›</button>
          </div>
        )}

      </main>
      <Footer />
    </div>
  );
}

export default Sedes;
