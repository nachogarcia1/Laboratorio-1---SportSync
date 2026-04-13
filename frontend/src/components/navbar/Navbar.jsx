import { Link } from "react-router-dom";
import "./Navbar.css";

function Navbar() {
  return (
    <header className="navbar">
      <div className="navbar__logo">SportSync</div>

      <nav className="navbar__links">
        <a href="#">Inicio</a>
        <a href="#">Sedes</a>
        <a href="#">Mis Reservas</a>
        <a href="#">Perfil</a>
        <Link to="/">Login/Registro</Link>
      </nav>
    </header>
  );
}

export default Navbar;