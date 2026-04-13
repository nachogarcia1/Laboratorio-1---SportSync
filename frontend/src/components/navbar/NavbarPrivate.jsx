import { Link, useNavigate } from "react-router-dom";
import "./Navbar.css";

function NavbarPrivate() {
  const navigate = useNavigate();
  const usuario = (() => {
    try { return JSON.parse(sessionStorage.getItem("usuario")); }
    catch { return null; }
  })();

  const handleLogout = () => {
    sessionStorage.clear();
    navigate("/");
  };

  return (
    <header className="navbar">
      <Link to="/home" className="navbar__logo">SportSync</Link>
      <nav className="navbar__links">
        <Link to="/home">Inicio</Link>
        <a href="#">Sedes</a>
        <a href="#">Mis Reservas</a>
        <Link to="/perfil">Perfil</Link>
        {usuario?.rol === "ADMIN" && (
            <Link to="/admin">Panel Admin</Link>
        )}
        <button className="navbar__logout-btn" onClick={handleLogout}>
          Cerrar Sesión
        </button>
      </nav>
    </header>
  );
}

export default NavbarPrivate;
