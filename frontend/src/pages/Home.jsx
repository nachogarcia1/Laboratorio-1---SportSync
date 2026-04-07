function Home() {
  const usuario = JSON.parse(localStorage.getItem("usuario"));

  return (
    <div>
      <h1>Encuentra y Reserva Tu Cancha Perfecta</h1>
      <p>Bienvenido{usuario ? `, ${usuario.nombre}` : ""}</p>
    </div>
  );
}

export default Home;