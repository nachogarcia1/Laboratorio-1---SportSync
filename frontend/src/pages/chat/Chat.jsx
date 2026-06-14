import { useState, useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";
import NavbarPrivate from "../../components/navbar/NavbarPrivate";
import Footer from "../../components/footer/Footer";
import { apiFetch } from "../../utils/api";
import "./Chat.css";

function Chat() {
  const usuario = (() => {
    try { return JSON.parse(sessionStorage.getItem("usuario")); }
    catch { return null; }
  })();

  const [mensajes,  setMensajes]  = useState([]);
  const [texto,     setTexto]     = useState("");
  const [conectado, setConectado] = useState(false);
  const clientRef = useRef(null);
  const bottomRef = useRef(null);

  useEffect(() => {
    if (!usuario) return;

    apiFetch(`/chat/historial/${usuario.id}`)
      .then(data => setMensajes(data))
      .catch(() => {});

    const client = new Client({
      brokerURL: "ws://localhost:8080/ws",
      onConnect: () => {
        setConectado(true);
        client.subscribe(`/topic/chat/usuario/${usuario.id}`, (frame) => {
          const msg = JSON.parse(frame.body);
          setMensajes(prev =>
            prev.some(m => m.id === msg.id) ? prev : [...prev, msg]
          );
        });
      },
      onDisconnect: () => setConectado(false),
    });

    client.activate();
    clientRef.current = client;
    return () => { client.deactivate(); };
  }, []);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [mensajes]);

  function enviar() {
    if (!texto.trim() || !clientRef.current?.connected) return;
    clientRef.current.publish({
      destination: "/app/chat/enviar",
      body: JSON.stringify({ usuarioId: usuario.id, contenido: texto.trim() }),
    });
    setTexto("");
  }

  function handleKey(e) {
    if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); enviar(); }
  }

  return (
    <div className="chat-page">
      <NavbarPrivate />
      <main className="chat-main">
        <div className="chat-box">
          <div className="chat-header">
            <h2>💬 Chat con Soporte</h2>
            <span className={`chat-status ${conectado ? "chat-status--ok" : "chat-status--off"}`}>
              {conectado ? "● Conectado" : "○ Conectando..."}
            </span>
          </div>

          <div className="chat-mensajes">
            {mensajes.length === 0 && (
              <p className="chat-vacio">Escribí tu consulta y te responderemos pronto.</p>
            )}
            {mensajes.map((m, i) => (
              <div
                key={m.id || i}
                className={`chat-burbuja ${m.remitente === "USUARIO" ? "chat-burbuja--usuario" : "chat-burbuja--admin"}`}
              >
                <span className="chat-burbuja__quien">{m.remitente === "USUARIO" ? "Vos" : "Soporte"}</span>
                <p className="chat-burbuja__texto">{m.contenido}</p>
                <span className="chat-burbuja__hora">
                  {new Date(m.timestamp).toLocaleTimeString("es-AR", { hour: "2-digit", minute: "2-digit" })}
                </span>
              </div>
            ))}
            <div ref={bottomRef} />
          </div>

          <div className="chat-input-row">
            <textarea
              className="chat-input"
              placeholder="Escribí tu mensaje... (Enter para enviar)"
              value={texto}
              onChange={e => setTexto(e.target.value)}
              onKeyDown={handleKey}
              rows={2}
            />
            <button className="chat-send-btn" onClick={enviar} disabled={!conectado || !texto.trim()}>
              Enviar
            </button>
          </div>
        </div>
      </main>
      <Footer />
    </div>
  );
}

export default Chat;