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
import BuscarCanchas from "./pages/buscar/BuscarCanchas";
import VerificarEmail from "./pages/verificar/VerificarEmail";
import PagoResultado from "./pages/pago/PagoResultado";
import PagoSimular from "./pages/pago/PagoSimular";
import PagoTarjeta from "./pages/pago/PagoTarjeta";
import Chat from "./pages/chat/Chat";

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

        <Route path="/verificar" element={
          <PublicRoute>
            <VerificarEmail />
          </PublicRoute>
        } />

        <Route path="/home" element={
          <ProtectedRoute>
            <Home />
          </ProtectedRoute>
        } />

        <Route path="/pago/resultado" element={
          <ProtectedRoute>
            <PagoResultado />
          </ProtectedRoute>
        } />

        <Route path="/pago/simular" element={
          <ProtectedRoute>
            <PagoSimular />
          </ProtectedRoute>
        } />

        <Route path="/pago/tarjeta" element={
          <ProtectedRoute>
            <PagoTarjeta />
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

        <Route path="/buscar" element={
          <ProtectedRoute>
            <BuscarCanchas />
          </ProtectedRoute>
        } />

        <Route path="/chat" element={
          <ProtectedRoute>
            <Chat />
          </ProtectedRoute>
        } />

      </Routes>
    </Router>
  );
}

export default App;
