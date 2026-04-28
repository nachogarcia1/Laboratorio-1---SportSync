import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import NavbarPrivate from "../../components/navbar/NavbarPrivate";
import Footer from "../../components/footer/Footer";
import { apiFetch } from "../../utils/api";
import "./MisCalificaciones.css";

const MESES = ["Enero","Febrero","Marzo","Abril","Mayo","Junio","Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"];

function formatFecha(fechaStr) {
  if (!fechaStr) return "—";
  const d = new Date(fechaStr);
  return `${d.getDate()} de ${MESES[d.getMonth()]}, ${d.getFullYear()}`;
}

function Estrellas({ valor }) {
  return (
    <span className="mc-estrellas">
      {[1,2,3,4,5].map(n => (
        <span key={n} className={n <= valor ? "mc-estrella--llena" : "mc-estrella--vacia"}>★</span>
      ))}
    </span>
  );
}

function MisCalificaciones() {
  const navigate  = useNavigate();
  const [criticas, setCriticas] = useState([]);
  const [loading,  setLoading]  = useState(true);
  const [error,    setError]    = useState("");

  const usuario = (() => {
    try { return JSON.parse(sessionStorage.getItem("usuario")); }
    catch { return null; }
  })();

  useEffect(() => {
    if (!usuario) { navigate("/"); return; }
    apiFetch(`/criticas/canchas/usuario/${usuario.id}`)
      .then(data => setCriticas(data))
      .catch(err  => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="mc-page">
      <NavbarPrivate />
      <main className="mc-main">
        <h1 className="mc-title">Mis Calificaciones</h1>

        {loading && <p className="mc-msg">Cargando...</p>}
        {error   && <p className="mc-msg mc-msg--error">{error}</p>}

        {!loading && !error && criticas.length === 0 && (
          <div className="mc-empty">
            <p>Todavía no calificaste ninguna reserva.</p>
            <button className="mc-btn" onClick={() => navigate("/mis-reservas")}>
              Ver Mis Reservas
            </button>
          </div>
        )}

        {!loading && criticas.length > 0 && (
          <div className="mc-lista">
            {criticas.map(c => {
              const promedio = ((c.notaCancha + c.notaStaff + c.notaServicios) / 3).toFixed(1);
              return (
                <div key={c.id} className="mc-card">
                  <div className="mc-card__header">
                    <div>
                      <p className="mc-card__cancha">{c.nombreCancha} — {c.nombreSede}</p>
                      <span className="mc-card__fecha">{formatFecha(c.fecha)}</span>
                    </div>
                    <span className="mc-card__promedio">Promedio: {promedio} ★</span>
                  </div>

                  <div className="mc-card__criterios">
                    <div className="mc-criterio">
                      <span className="mc-criterio__label">Cancha</span>
                      <Estrellas valor={c.notaCancha} />
                    </div>
                    <div className="mc-criterio">
                      <span className="mc-criterio__label">Staff</span>
                      <Estrellas valor={c.notaStaff} />
                    </div>
                    <div className="mc-criterio">
                      <span className="mc-criterio__label">Servicios</span>
                      <Estrellas valor={c.notaServicios} />
                    </div>
                  </div>

                  {c.comentario && (
                    <p className="mc-card__comentario">"{c.comentario}"</p>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </main>
      <Footer />
    </div>
  );
}

export default MisCalificaciones;
