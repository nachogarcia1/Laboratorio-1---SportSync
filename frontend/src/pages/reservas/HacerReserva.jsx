import { useState, useEffect } from "react"

const HacerReserva = () => {
  const [sedes, setSedes]       = useState([])
  const [sedeId, setSedeId]     = useState(null)
  const [canchas, setCanchas]   = useState([])
  const _d    = new Date();
  const today = `${_d.getFullYear()}-${String(_d.getMonth() + 1).padStart(2, "0")}-${String(_d.getDate()).padStart(2, "0")}`;
  const [fecha, setFecha]       = useState(today)

  // 1. Al cargar la página, traé las sedes
  useEffect(() => {
    fetch("http://localhost:8080/sedes")
      .then(res => res.json())
      .then(data => {
        setSedes(data)
        setSedeId(data[0]?.id)  // seleccioná la primera por defecto
      })
  }, [])

  // 2. Cada vez que cambia la sede o la fecha, traé las canchas
  useEffect(() => {
    if (!sedeId) return

    fetch(`http://localhost:8080/canchas/sede/${sedeId}`)
      .then(res => res.json())
      .then(data => setCanchas(data))
  }, [sedeId, fecha])  // ← se re-ejecuta cuando sedeId o fecha cambian

  return (
    <div>
      <h1>Canchas disponibles en {sedes.find(s => s.id === sedeId)?.nombre}</h1>

      {/* Selector de sede */}
      <select onChange={e => setSedeId(Number(e.target.value))}>
        {sedes.map(sede => (
          <option key={sede.id} value={sede.id}>{sede.nombre}</option>
        ))}
      </select>

      {/* Selector de fecha */}
      <input type="date" value={fecha} min={today} onChange={e => setFecha(e.target.value)} />

      {/* Lista de canchas */}
      {canchas.map(cancha => (
        <div key={cancha.id}>
          <h2>{cancha.nombre}</h2>
        </div>
      ))}
    </div>
  )
  
}
export default HacerReserva;


