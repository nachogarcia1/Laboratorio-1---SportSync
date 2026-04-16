import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import NavbarPrivate from "../../components/navbar/NavbarPrivate";
import Footer from "../../components/footer/Footer";
import { apiFetch } from "../../utils/api";
import "./SedeDetalle.css";

const DIAS  = ["Domingo","Lunes","Martes","Miércoles","Jueves","Viernes","Sábado"];
const MESES = ["Enero","Febrero","Marzo","Abril","Mayo","Junio","Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"];

function SedeDetalle() {
  const { id }   = useParams();
  const navigate = useNavigate();

  const [sede,    setSede]    = useState(null);
  const [canchas, setCanchas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState("");
  const [filtro,  setFiltro]  = useState(null);
  const [offset,  setOffset]  = useState(0);
  const [expandidos, setExpandidos] = useState(new Set());

  useEffect(() => {
    Promise.all([
      apiFetch(`/sedes/${id}`),
      apiFetch(`/canchas/sede/${id}`)
    ])
      .then(([sedeData, canchasData]) => {
        setSede(sedeData);
        setCanchas(canchasData);
        const tipos = [...new Set(canchasData.map(c => c.tipo))];
        setExpandidos(new Set(tipos));
      })
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  }, [id]);

  const fecha = new Date();
  fecha.setDate(fecha.getDate() + offset);
  const fechaFormateada = `${DIAS[fecha.getDay()]} ${fecha.getDate()} de ${MESES[fecha.getMonth()]}`;

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
              {grupos.map(([tipo, canchasGrupo]) => (
                <div key={tipo} className="cancha-card">
                  {expandidos.has(Number(tipo)) && (
                    <div className="cancha-card__body">
                      <div className="cancha-card__image"></div>
                      <div className="cancha-card__info">
                        <div className="cancha-card__title-row">
                          <h3 className="cancha-card__nombre">Fútbol {tipo}</h3>
                          <span className="cancha-card__precio">
                            Desde ${canchasGrupo[0].precioBase.toLocaleString("es-AR")}
                          </span>
                        </div>
                        <p className="cancha-turnos-placeholder">
                          📅 Seleccioná una fecha para ver los turnos disponibles.
                        </p>
                      </div>
                    </div>
                  )}
                  <div
                    className="cancha-card__footer"
                    onClick={() => toggleExpandido(Number(tipo))}
                  >
                    <div className="cancha-card__footer-left">
                      <span>x{canchasGrupo.length} {canchasGrupo.length === 1 ? "Cancha" : "Canchas"}</span>
                    </div>
                    <span className="cancha-card__footer-tipo">Fútbol {tipo}</span>
                    <span className="cancha-card__chevron">
                      {expandidos.has(Number(tipo)) ? "▲" : "▼"}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </>
        )}

      </main>
      <Footer />
    </div>
  );
}

export default SedeDetalle;
