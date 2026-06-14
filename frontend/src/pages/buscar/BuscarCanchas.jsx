import { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { MapContainer, TileLayer, Marker, Popup, useMap } from "react-leaflet";
import L from "leaflet";
import NavbarPrivate from "../../components/navbar/NavbarPrivate";
import Footer from "../../components/footer/Footer";
import { apiFetch } from "../../utils/api";
import "./BuscarCanchas.css";

// Leaflet necesita los iconos manualmente con Vite/Webpack
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
  iconUrl:       "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
  shadowUrl:     "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
});

const iconoCercano = new L.Icon({
  iconUrl: "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-blue.png",
  shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
  iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34],
});

const iconoNormal = new L.Icon({
  iconUrl: "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-green.png",
  shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
  iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34],
});

const iconoUsuario = new L.Icon({
  iconUrl: "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-red.png",
  shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
  iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34],
});

function RecenterMap({ center }) {
  const map = useMap();
  useEffect(() => {
    if (center) map.setView(center, 13, { animate: true });
  }, [center, map]);
  return null;
}

// Haversine: distancia en km entre dos coordenadas
function distanciaKm(lat1, lng1, lat2, lng2) {
  const R = 6371;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLng = ((lng2 - lng1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLng / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

const DEFAULT_CENTER = [-34.6037, -58.3816];

export default function BuscarCanchas() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  // Filtros iniciales desde la URL (vienen del buscador del Home)
  const [nombre,        setNombre]        = useState(searchParams.get("nombre") || "");
  const [tipo,          setTipo]          = useState(searchParams.get("tipo")   || "");
  const [todasCanchas,  setTodasCanchas]  = useState([]); // todas las canchas habilitadas
  const [sedes,         setSedes]         = useState([]); // todas las sedes con coordenadas
  const [loading,       setLoading]       = useState(false);
  const [error,         setError]         = useState("");
  const [userPos,       setUserPos]       = useState(null);
  const [gpsError,      setGpsError]      = useState("");
  const [mapCenter,     setMapCenter]     = useState(DEFAULT_CENTER);
  const [sedesCercanas, setSedesCercanas] = useState([]); // top 5 más cercanas

  // Carga inicial: todas las sedes y todas las canchas habilitadas
  useEffect(() => {
    setLoading(true);
    Promise.all([
      apiFetch("/sedes"),
      apiFetch("/canchas/buscar"),
    ])
      .then(([sedesData, canchasData]) => {
        setSedes(sedesData.filter(s => s.latitud && s.longitud));
        setTodasCanchas(canchasData);
      })
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  // ── Filtrado client-side ──────────────────────────────────────────────────

  // Canchas filtradas por tipo (si está seleccionado)
  const canchasFiltradas = tipo
    ? todasCanchas.filter(c => String(c.tipo) === tipo)
    : todasCanchas;

  // Agrupar canchas filtradas por sede
  const canchasPorSede = canchasFiltradas.reduce((acc, c) => {
    const sedeId = c.sede?.id;
    if (!sedeId) return acc;
    if (!acc[sedeId]) acc[sedeId] = { sede: c.sede, canchas: [] };
    acc[sedeId].canchas.push(c);
    return acc;
  }, {});

  // Sedes con resultados (tienen canchas del tipo seleccionado)
  const sedesConResultados = new Set(Object.keys(canchasPorSede).map(Number));

  // Filtrar por nombre de sede
  const gruposFiltrados = Object.values(canchasPorSede).filter(({ sede }) => {
    if (!nombre.trim()) return true;
    return sede?.nombre?.toLowerCase().includes(nombre.trim().toLowerCase());
  });

  // ── GPS: 5 sedes más cercanas ─────────────────────────────────────────────

  function usarGPS() {
    setGpsError("");
    setSedesCercanas([]);
    if (!navigator.geolocation) {
      setGpsError("Tu navegador no soporta geolocalización.");
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const { latitude: lat, longitude: lng } = pos.coords;
        setUserPos({ lat, lng });
        setMapCenter([lat, lng]);

        if (sedes.length > 0) {
          const conDist = sedes
            .map(s => ({ ...s, distancia: distanciaKm(lat, lng, s.latitud, s.longitud) }))
            .sort((a, b) => a.distancia - b.distancia)
            .slice(0, 5); // top 5
          setSedesCercanas(conDist);
          // Centrar el mapa en la más cercana
          setMapCenter([conDist[0].latitud, conDist[0].longitud]);
        }
      },
      () => setGpsError("No se pudo obtener tu ubicación. Verificá los permisos del navegador.")
    );
  }

  const idsCercanas = new Set(sedesCercanas.map(s => s.id));

  return (
    <div className="buscar-page">
      <NavbarPrivate />
      <main className="buscar-page__main">
        <h1 className="buscar-page__title">Buscá tu cancha</h1>

        {/* ── Filtros ── */}
        <form className="buscar-filtros" onSubmit={e => e.preventDefault()}>
          <input
            className="buscar-filtros__input"
            type="text"
            placeholder="Nombre de sede..."
            value={nombre}
            onChange={e => setNombre(e.target.value)}
          />
          <select
            className="buscar-filtros__select"
            value={tipo}
            onChange={e => setTipo(e.target.value)}
          >
            <option value="">Todos los tipos</option>
            <option value="5">Fútbol 5</option>
            <option value="7">Fútbol 7</option>
            <option value="11">Fútbol 11</option>
          </select>
          <button type="button" className="buscar-filtros__btn buscar-filtros__btn--gps" onClick={usarGPS}>
            📍 Cerca de mí
          </button>
        </form>

        {gpsError && <p className="buscar-error">{gpsError}</p>}

        {/* Resumen de 5 más cercanas */}
        {sedesCercanas.length > 0 && (
          <div className="buscar-cercanas-lista">
            <p className="buscar-cercanas-titulo">📍 Las 5 sedes más cercanas a vos:</p>
            {sedesCercanas.map((s, i) => (
              <span key={s.id} className="buscar-cercana-chip">
                {i + 1}. {s.nombre} — {s.distancia.toFixed(1)} km
              </span>
            ))}
          </div>
        )}

        {/* ── Layout: mapa + resultados ── */}
        <div className="buscar-layout">

          {/* Mapa */}
          <div className="buscar-mapa">
            <MapContainer
              center={mapCenter}
              zoom={12}
              style={{ height: "100%", width: "100%" }}
            >
              <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              />
              <RecenterMap center={mapCenter} />

              {/* Marcador del usuario */}
              {userPos && (
                <Marker position={[userPos.lat, userPos.lng]} icon={iconoUsuario}>
                  <Popup>Tu ubicación</Popup>
                </Marker>
              )}

              {/* Marcadores de todas las sedes con coordenadas */}
              {sedes.map(sede => (
                <Marker
                  key={sede.id}
                  position={[sede.latitud, sede.longitud]}
                  icon={idsCercanas.has(sede.id) ? iconoCercano : iconoNormal}
                >
                  <Popup>
                    <strong>{sede.nombre}</strong><br />
                    {sede.direccion}<br />
                    {idsCercanas.has(sede.id) && (() => {
                      const sc = sedesCercanas.find(s => s.id === sede.id);
                      return <span style={{ color: "#2563eb" }}>📍 {sc?.distancia?.toFixed(1)} km de vos</span>;
                    })()}
                    <br />
                    <button
                      style={{ marginTop: "6px", padding: "4px 10px", cursor: "pointer", background: "#16a34a", color: "#fff", border: "none", borderRadius: "4px", fontSize: "0.8rem" }}
                      onClick={() => navigate(`/sedes/${sede.id}`)}
                    >
                      Ver sede →
                    </button>
                  </Popup>
                </Marker>
              ))}
            </MapContainer>
          </div>

          {/* Lista de resultados */}
          <div className="buscar-resultados">
            {loading && <p className="buscar-estado">Cargando...</p>}
            {error   && <p className="buscar-estado buscar-estado--error">{error}</p>}
            {!loading && gruposFiltrados.length === 0 && (
              <p className="buscar-estado">No se encontraron sedes con esos filtros.</p>
            )}

            {gruposFiltrados.map(({ sede, canchas: cs }) => (
              <div key={sede?.id} className="buscar-sede-grupo">
                <div className="buscar-sede-grupo__header">
                  <span className="buscar-sede-grupo__nombre">
                    {idsCercanas.has(sede?.id) && "📍 "}
                    {sede?.nombre}
                    {idsCercanas.has(sede?.id) && (
                      <span className="buscar-cercana-badge">
                        {sedesCercanas.find(s => s.id === sede.id)?.distancia?.toFixed(1)} km
                      </span>
                    )}
                  </span>
                  <span className="buscar-sede-grupo__dir">📍 {sede?.direccion}</span>
                </div>
                {cs.map(c => (
                  <div key={c.id} className="buscar-cancha-card">
                    <div className="buscar-cancha-card__info">
                      <span className="buscar-cancha-card__nombre">{c.nombre}</span>
                      <span className="buscar-cancha-card__tipo">⚽ Fútbol {c.tipo}</span>
                      <span className="buscar-cancha-card__precio">${c.precioBase}/h</span>
                    </div>
                    <button
                      className="buscar-cancha-card__btn"
                      onClick={() => navigate(`/sedes/${sede?.id}`)}
                    >
                      Reservar
                    </button>
                  </div>
                ))}
              </div>
            ))}
          </div>

        </div>
      </main>
      <Footer />
    </div>
  );
}
