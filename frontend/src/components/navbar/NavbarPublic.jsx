import { Link } from "react-router-dom";
import "./Navbar.css";

function NavbarPublic() {
  return (
    <header className="navbar">
      <div className="navbar__logo">SportSync</div>
      <nav className="navbar__links">
        <Link to="/">Login/Registro</Link>
      </nav>
    </header>
  );
}

export default NavbarPublic;
