import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
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

// Icono azul para sede seleccionada / más cercana
const iconoCercano = new L.Icon({
  iconUrl: "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-blue.png",
  shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
  iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34],
});

// Icono verde para sede normal
const iconoNormal = new L.Icon({
  iconUrl: "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-green.png",
  shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
  iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34],
});

// Icono rojo para usuario
const iconoUsuario = new L.Icon({
  iconUrl: "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-red.png",
  shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
  iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34],
});

// Componente auxiliar para re-centrar el mapa cuando cambian los resultados
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

// Centro por defecto: Buenos Aires
const DEFAULT_CENTER = [-34.6037, -58.3816];

export default function BuscarCanchas() {
  const navigate = useNavigate();

  const [nombre,     setNombre]     = useState("");
  const [tipo,       setTipo]       = useState("");
  const [canchas,    setCanchas]    = useState([]);
  const [loading,    setLoading]    = useState(false);
  const [error,      setError]      = useState("");
  const [sedes,      setSedes]      = useState([]); // con lat/lng
  const [userPos,    setUserPos]    = useState(null); // { lat, lng }
  const [gpsError,   setGpsError]   = useState("");
  const [mapCenter,  setMapCenter]  = useState(DEFAULT_CENTER);
  const [sedeCercana, setSedeCercana] = useState(null);

  // Cargar todas las sedes al inicio para los marcadores del mapa
  useEffect(() => {
    apiFetch("/sedes")
      .then(data => setSedes(data.filter(s => s.latitud && s.longitud)))
      .catch(console.error);
    // Cargar todas las canchas habilitadas al inicio
    buscar();
  }, []);

  async function buscar(e) {
    if (e) e.preventDefault();
    setLoading(true);
    setError("");
    try {
      const params = new URLSearchParams();
      if (nombre.trim()) params.append("nombre", nombre.trim());
      if (tipo)          params.append("tipo", tipo);
      const qs = params.toString();
      const data = await apiFetch(`/canchas/buscar${qs ? "?" + qs : ""}`);
      setCanchas(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  function usarGPS() {
    setGpsError("");
    if (!navigator.geolocation) {
      setGpsError("Tu navegador no soporta geolocalización.");
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const { latitude: lat, longitude: lng } = pos.coords;
        setUserPos({ lat, lng });
        setMapCenter([lat, lng]);

        // Encontrar la sede más cercana que tenga coordenadas
        if (sedes.length > 0) {
          const sedesConDist = sedes.map(s => ({
            ...s,
            distancia: distanciaKm(lat, lng, s.latitud, s.longitud),
          }));
          sedesConDist.sort((a, b) => a.distancia - b.distancia);
          const cercana = sedesConDist[0];
          setSedeCercana(cercana);
          setMapCenter([cercana.latitud, cercana.longitud]);
        }
      },
      () => setGpsError("No se pudo obtener tu ubicación. Verificá los permisos del navegador.")
    );
  }

  // Agrupar canchas por sede para la lista
  const canchasPorSede = canchas.reduce((acc, c) => {
    const sedeId = c.sede?.id;
    if (!acc[sedeId]) acc[sedeId] = { sede: c.sede, canchas: [] };
    acc[sedeId].canchas.push(c);
    return acc;
  }, {});

  // Sedes de los resultados que tienen coordenadas (para mostrar solo esas en el mapa)
  const sedesResultado = sedes.filter(s =>
    Object.keys(canchasPorSede).map(Number).includes(s.id)
  );

  return (
    <div className="buscar-page">
      <NavbarPrivate />
      <main className="buscar-page__main">
        <h1 className="buscar-page__title">Buscar Canchas</h1>

        {/* ── Filtros ── */}
        <form className="buscar-filtros" onSubmit={buscar}>
          <input
            className="buscar-filtros__input"
            type="text"
            placeholder="Nombre de cancha..."
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
          <button type="submit" className="buscar-filtros__btn">Buscar</button>
          <button type="button" className="buscar-filtros__btn buscar-filtros__btn--gps" onClick={usarGPS}>
            📍 Cerca de mí
          </button>
        </form>

        {gpsError && <p className="buscar-error">{gpsError}</p>}
        {sedeCercana && (
          <p className="buscar-cercana">
            📍 Sede más cercana: <strong>{sedeCercana.nombre}</strong> — {sedeCercana.distancia?.toFixed(1)} km
          </p>
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

              {/* Marcadores de sedes */}
              {sedesResultado.map(sede => (
                <Marker
                  key={sede.id}
                  position={[sede.latitud, sede.longitud]}
                  icon={sedeCercana?.id === sede.id ? iconoCercano : iconoNormal}
                >
                  <Popup>
                    <strong>{sede.nombre}</strong><br />
                    {sede.direccion}<br />
                    {sedeCercana?.id === sede.id && (
                      <span style={{ color: "#2563eb" }}>
                        📍 {sedeCercana.distancia?.toFixed(1)} km de vos
                      </span>
                    )}
                  </Popup>
                </Marker>
              ))}
            </MapContainer>
          </div>

          {/* Lista de resultados */}
          <div className="buscar-resultados">
            {loading && <p className="buscar-estado">Buscando...</p>}
            {error   && <p className="buscar-estado buscar-estado--error">{error}</p>}
            {!loading && canchas.length === 0 && (
              <p className="buscar-estado">No se encontraron canchas con esos filtros.</p>
            )}

            {Object.values(canchasPorSede).map(({ sede, canchas: cs }) => (
              <div key={sede?.id} className="buscar-sede-grupo">
                <div className="buscar-sede-grupo__header">
                  <span className="buscar-sede-grupo__nombre">{sede?.nombre}</span>
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
