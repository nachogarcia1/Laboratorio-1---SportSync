import { Navigate } from "react-router-dom";

function ProtectedRoute({ children }) {
  const token = sessionStorage.getItem("token");
  const usuario = (() => {
    try { return JSON.parse(sessionStorage.getItem("usuario")); }
    catch { return null; }
  })();

  if (!token || !usuario) {
    return <Navigate to="/" replace />;
  }

  return children;
}

export default ProtectedRoute;
