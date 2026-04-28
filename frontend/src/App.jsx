import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Login from "./pages/login/Login";
import Register from "./pages/register/Register";
import Home from "./pages/home/Home";
import ProtectedRoute from "./routes/ProtectedRoute";
import PublicRoute from "./routes/PublicRoute";
import Perfil from "./pages/perfil/Perfil";
import Admin from "./pages/admin/Admin";
import AdminRoute from "./routes/AdminRoute";
import Sedes from "./pages/sedes/Sedes";
import SedeDetalle from "./pages/sedes/SedeDetalle";
import MisReservas from "./pages/reservas/MisReservas";
import CalificarExperiencia from "./pages/calificar/CalificarExperiencia";
import MisCalificaciones from "./pages/calificaciones/MisCalificaciones";

function App() {
  return (
    <Router>
      <Routes>

        <Route path="/" element={
          <PublicRoute>
            <Login />
          </PublicRoute>
        } />
        
        <Route path="/register" element={
          <PublicRoute>
            <Register />
          </PublicRoute>
        } />

        <Route path="/home" element={
          <ProtectedRoute>
            <Home />
          </ProtectedRoute>
        } />

        <Route path="/perfil" element={
          <ProtectedRoute>
            <Perfil />
          </ProtectedRoute>
        } />

        <Route path="/admin" element={
          <AdminRoute>
            <Admin />
          </AdminRoute>
        } />

        <Route path="/sedes" element={
          <ProtectedRoute>
            <Sedes />
          </ProtectedRoute>
        } />

        <Route path="/sedes/:id" element={
          <ProtectedRoute>
            <SedeDetalle />
          </ProtectedRoute>
        } />

        <Route path="/mis-reservas" element={
          <ProtectedRoute>
            <MisReservas />
          </ProtectedRoute>
        } />

        <Route path="/calificar" element={
          <ProtectedRoute>
            <CalificarExperiencia />
          </ProtectedRoute>
        } />

        <Route path="/mis-calificaciones" element={
          <ProtectedRoute>
            <MisCalificaciones />
          </ProtectedRoute>
        } />

      </Routes>
    </Router>
  );
}

export default App;
