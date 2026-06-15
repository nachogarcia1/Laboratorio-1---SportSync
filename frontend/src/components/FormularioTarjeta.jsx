import { useState } from "react";
import "./FormularioTarjeta.css";

/**
 * Formulario de tarjeta reutilizable (pago de reserva e inscripción a socio).
 * Hace validación liviana de UX; la validación autoritativa (Luhn, vencimiento, etc.)
 * la hace el backend, que devuelve el resultado. Nunca expone datos sensibles fuera del submit.
 */
export default function FormularioTarjeta({ onPagar, procesando, error, botonTexto = "Pagar", montoTexto }) {
  const [titular,     setTitular]     = useState("");
  const [numero,      setNumero]      = useState("");
  const [vencimiento, setVencimiento] = useState("");
  const [cvv,         setCvv]         = useState("");
  const [tipo,        setTipo]        = useState("TARJETA_CREDITO");
  const [errorLocal,  setErrorLocal]  = useState("");

  // Formatea el número en grupos de 4 mientras se tipea
  const onNumeroChange = (e) => {
    const limpio = e.target.value.replace(/[^\d]/g, "").slice(0, 19);
    setNumero(limpio.replace(/(.{4})/g, "$1 ").trim());
  };
  // Inserta la barra del MM/AA automáticamente
  const onVencChange = (e) => {
    let v = e.target.value.replace(/[^\d]/g, "").slice(0, 4);
    if (v.length >= 3) v = v.slice(0, 2) + "/" + v.slice(2);
    setVencimiento(v);
  };

  const submit = (e) => {
    e.preventDefault();
    setErrorLocal("");
    const numeroLimpio = numero.replace(/\s/g, "");
    if (!titular.trim())              { setErrorLocal("Ingresá el nombre del titular."); return; }
    if (numeroLimpio.length < 13)     { setErrorLocal("El número de tarjeta es muy corto."); return; }
    if (!/^\d{2}\/\d{2,4}$/.test(vencimiento)) { setErrorLocal("Fecha de expiración inválida (MM/AA)."); return; }
    if (!/^\d{3,4}$/.test(cvv))       { setErrorLocal("El CVV debe tener 3 o 4 dígitos."); return; }
    onPagar({ titular: titular.trim(), numero: numeroLimpio, cvv, vencimiento, tipo });
  };

  return (
    <form className="tarjeta-form" onSubmit={submit}>
      <label>Tipo de tarjeta</label>
      <select value={tipo} onChange={e => setTipo(e.target.value)}>
        <option value="TARJETA_CREDITO">Crédito</option>
        <option value="TARJETA_DEBITO">Débito</option>
      </select>

      <label>Nombre del titular</label>
      <input type="text" value={titular} placeholder="Como figura en la tarjeta"
        maxLength={60} onChange={e => setTitular(e.target.value)} />

      <label>Número de tarjeta</label>
      <input type="text" inputMode="numeric" value={numero}
        placeholder="1234 5678 9012 3456" onChange={onNumeroChange} />

      <div className="tarjeta-form__row">
        <div>
          <label>Vencimiento</label>
          <input type="text" inputMode="numeric" value={vencimiento}
            placeholder="MM/AA" maxLength={7} onChange={onVencChange} />
        </div>
        <div>
          <label>CVV</label>
          <input type="password" inputMode="numeric" value={cvv}
            placeholder="123" maxLength={4}
            onChange={e => setCvv(e.target.value.replace(/[^\d]/g, "").slice(0, 4))} />
        </div>
      </div>

      {(errorLocal || error) && <p className="tarjeta-form__error">{errorLocal || error}</p>}

      <button type="submit" className="tarjeta-form__btn" disabled={procesando}>
        {procesando ? "Procesando..." : (montoTexto ? `${botonTexto} ${montoTexto}` : botonTexto)}
      </button>

      <p className="tarjeta-form__nota">🔒 Pago simulado. No ingreses datos reales de tarjeta.</p>
    </form>
  );
}
