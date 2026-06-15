import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import NavbarPrivate from "../../components/navbar/NavbarPrivate";
import Footer from "../../components/footer/Footer";
import { apiFetch } from "../../utils/api";
import "./PagoResultado.css";

/**
 * Checkout SIMULADO (modo desarrollo). Reemplaza al de Mercado Pago cuando no hay
 * credenciales reales: deja elegir manualmente el resultado del pago para demostrar
 * la máquina de estados completa.
 */
export default function PagoSimular() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const reservaId = params.get("reserva");

  const [pago,     setPago]    = useState(null);
  const [enviando, setEnviando] = useState(false);
  const [error,    setError]   = useState("");

  useEffect(() => {
    if (!reservaId) { setError("Falta la reserva."); return; }
    apiFetch(`/pagos/reserva/${reservaId}`).then(setPago).catch(e => setError(e.message));
  }, [reservaId]);

  const simular = async (resultado) => {
    setEnviando(true); setError("");
    try {
      await apiFetch(`/pagos/simular?reservaId=${reservaId}&resultado=${resultado}`, { method: "POST" });
      navigate(`/pago/resultado?reserva=${reservaId}`);
    } catch (e) {
      setError(e.message);
      setEnviando(false);
    }
  };

  return (
    <div className="pago-page">
      <NavbarPrivate />
      <main className="pago-page__main">
        <div className="pago-card">
          <span className="pago-card__icon" style={{ color: "#2f66e8" }}>$</span>
          <h1 className="pago-card__titulo">Checkout simulado</h1>
          <p className="pago-card__texto">
            Modo desarrollo (sin Mercado Pago real).{pago ? ` Monto: $${pago.monto?.toLocaleString("es-AR")}.` : ""} Elegí el resultado para probar el flujo:
          </p>

          {error && <p className="pago-card__texto" style={{ color: "#dc2626" }}>{error}</p>}

          <div className="pago-card__actions">
            <button className="pago-card__btn pago-card__btn--primary" disabled={enviando}
              onClick={() => simular("APROBADO")}>
              {enviando ? "Procesando..." : "Simular pago APROBADO"}
            </button>
            <button className="pago-card__btn" disabled={enviando}
              onClick={() => simular("RECHAZADO")}>
              Simular pago RECHAZADO
            </button>
          </div>
        </div>
      </main>
      <Footer />
    </div>
  );
}
