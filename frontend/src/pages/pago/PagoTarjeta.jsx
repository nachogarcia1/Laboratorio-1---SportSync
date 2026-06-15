import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import NavbarPrivate from "../../components/navbar/NavbarPrivate";
import Footer from "../../components/footer/Footer";
import FormularioTarjeta from "../../components/FormularioTarjeta";
import { apiFetch } from "../../utils/api";
import "./PagoResultado.css";

/** Pago con tarjeta de una reserva (pasarela mock). Al aprobarse va a /pago/resultado. */
export default function PagoTarjeta() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const reservaId = params.get("reserva");

  const [procesando, setProcesando] = useState(false);
  const [error,      setError]      = useState("");

  const pagar = async (datos) => {
    setError(""); setProcesando(true);
    try {
      const res = await apiFetch(`/pagos/tarjeta?reservaId=${reservaId}`, {
        method: "POST",
        body: JSON.stringify(datos)
      });
      if (res.estado === "APROBADO") {
        navigate(`/pago/resultado?reserva=${reservaId}`);
      } else {
        // RECHAZADO o DATOS_INVALIDOS → mostrar mensaje y permitir reintentar
        setError(res.mensaje || "El pago no se pudo completar.");
        setProcesando(false);
      }
    } catch (e) {
      setError(e.message);
      setProcesando(false);
    }
  };

  return (
    <div className="pago-page">
      <NavbarPrivate />
      <main className="pago-page__main">
        <div className="pago-card">
          <span className="pago-card__icon" style={{ color: "#2f66e8" }}>💳</span>
          <h1 className="pago-card__titulo">Pago con tarjeta</h1>
          <p className="pago-card__texto">Ingresá los datos de tu tarjeta para confirmar la reserva.</p>
          <FormularioTarjeta onPagar={pagar} procesando={procesando} error={error} botonTexto="Pagar reserva" />
        </div>
      </main>
      <Footer />
    </div>
  );
}
