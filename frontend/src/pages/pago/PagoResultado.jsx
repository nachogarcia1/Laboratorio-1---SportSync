import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import NavbarPrivate from "../../components/navbar/NavbarPrivate";
import Footer from "../../components/footer/Footer";
import { apiFetch } from "../../utils/api";
import "./PagoResultado.css";

const INFO = {
  APROBADO:    { icon: "✓", color: "#16a34a", titulo: "¡Pago aprobado!",     texto: "Tu reserva quedó confirmada." },
  PENDIENTE:   { icon: "⏳", color: "#d97706", titulo: "Pago pendiente",      texto: "Tu pago está siendo procesado. Te confirmamos apenas se acredite." },
  RECHAZADO:   { icon: "✕", color: "#dc2626", titulo: "Pago rechazado",      texto: "El pago no se pudo completar. La reserva fue cancelada." },
  CANCELADO:   { icon: "✕", color: "#dc2626", titulo: "Pago cancelado",      texto: "Cancelaste el pago. La reserva no se confirmó." },
  VENCIDO:     { icon: "✕", color: "#dc2626", titulo: "Pago vencido",        texto: "El pago venció. La reserva fue cancelada." },
  REEMBOLSADO: { icon: "↩", color: "#6b7280", titulo: "Pago reembolsado",    texto: "El pago fue reembolsado." },
};

export default function PagoResultado() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const reservaId = params.get("reserva");
  const paymentId = params.get("payment_id") || params.get("collection_id");

  const [estado,  setEstado]  = useState(null);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState("");

  useEffect(() => {
    if (!reservaId) { setError("Falta la reserva."); setLoading(false); return; }
    // Confirma contra el backend, que consulta el estado real a Mercado Pago (no confía en la URL)
    apiFetch(`/pagos/confirmar?reservaId=${reservaId}${paymentId ? `&paymentId=${paymentId}` : ""}`, { method: "POST" })
      .then(pago => setEstado(pago?.estado))
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  }, [reservaId, paymentId]);

  const info = INFO[estado] || INFO.PENDIENTE;

  return (
    <div className="pago-page">
      <NavbarPrivate />
      <main className="pago-page__main">
        <div className="pago-card">
          {loading ? (
            <p className="pago-card__loading">Verificando el pago...</p>
          ) : error ? (
            <>
              <span className="pago-card__icon" style={{ color: "#dc2626" }}>✕</span>
              <h1 className="pago-card__titulo">No pudimos verificar el pago</h1>
              <p className="pago-card__texto">{error}</p>
            </>
          ) : (
            <>
              <span className="pago-card__icon" style={{ color: info.color }}>{info.icon}</span>
              <h1 className="pago-card__titulo" style={{ color: info.color }}>{info.titulo}</h1>
              <p className="pago-card__texto">{info.texto}</p>
            </>
          )}

          <div className="pago-card__actions">
            <button className="pago-card__btn pago-card__btn--primary" onClick={() => navigate("/mis-reservas")}>
              Ver mis reservas
            </button>
            <button className="pago-card__btn" onClick={() => navigate("/home")}>
              Volver al inicio
            </button>
          </div>
        </div>
      </main>
      <Footer />
    </div>
  );
}
