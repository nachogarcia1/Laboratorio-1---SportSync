import reactLogo from './assets/react.svg'
import viteLogo from './assets/vite.svg'
import heroImg from './assets/hero.png'
import './App.css'

import { useEffect, useState } from "react";

function App() {
  const [usuarios, setUsuarios] = useState([]);
  const [nombre, setNombre] = useState("");
  const [email, setEmail] = useState("");

  const cargarUsuarios = async () => {
    try {
      const response = await fetch("http://localhost:8080/usuarios");
      const data = await response.json();
      setUsuarios(data);
    } catch (error) {
      console.error("Error al cargar usuarios:", error);
    }
  };

  useEffect(() => {
    cargarUsuarios();
  }, []);

  const crearUsuario = async (e) => {
    e.preventDefault();

    try {
      const response = await fetch("http://localhost:8080/usuarios", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ nombre, email })
      });

      if (!response.ok) {
        throw new Error("No se pudo crear el usuario");
      }

      setNombre("");
      setEmail("");
      cargarUsuarios();
    } catch (error) {
      console.error("Error al crear usuario:", error);
    }
  };

  return (
    <div>
      <h1>SportSync</h1>

      <h2>Crear usuario</h2>
      <form onSubmit={crearUsuario}>
        <input
          type="text"
          placeholder="Nombre"
          value={nombre}
          onChange={(e) => setNombre(e.target.value)}
        />
        <input
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <button type="submit">Crear usuario</button>
      </form>

      <h2>Usuarios</h2>
      <ul>
        {usuarios.map((u) => (
          <li key={u.id}>
            name: {u.nombre} - mail: {u.email}
          </li>
        ))}
      </ul>
    </div>
  );
}

export default App;