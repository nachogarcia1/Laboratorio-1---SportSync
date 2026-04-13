import { Navigate } from "react-router-dom";

function AdminRoute({ children }) {
  const token = sessionStorage.getItem("token");
  const usuario = (() => {
    try { return JSON.parse(sessionStorage.getItem("usuario")); }
    catch { return null; }
  })();

  if (!token || !usuario) {
    return <Navigate to="/" replace />;
  }

  if (usuario.rol !== "ADMIN") {
    return <Navigate to="/home" replace />;
  }

  return children;
}

export default AdminRoute;
