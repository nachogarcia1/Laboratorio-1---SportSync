import { useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { apiFetch } from "../utils/api";

const CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID;

/**
 * Botón "Continuar con Google" usando Google Identity Services.
 * El script GIS se carga en index.html. Al recibir el id_token de Google,
 * lo manda al backend (/usuarios/oauth/google), guarda la sesión y entra.
 * Si no hay VITE_GOOGLE_CLIENT_ID configurado, no renderiza nada.
 */
export default function GoogleLoginButton({ onError }) {
  const navigate = useNavigate();
  const divRef = useRef(null);

  useEffect(() => {
    if (!CLIENT_ID) return;
    let cancelled = false;

    async function handleCredential(response) {
      try {
        const data = await apiFetch("/usuarios/oauth/google", {
          method: "POST",
          body: JSON.stringify({ idToken: response.credential })
        });
        sessionStorage.setItem("token", data.token);
        sessionStorage.setItem("usuario", JSON.stringify({
          id: data.id, nombre: data.nombre, email: data.email, rol: data.rol
        }));
        navigate("/home");
      } catch (err) {
        onError?.(err.message);
      }
    }

    function init() {
      if (cancelled) return;
      // El script GIS carga async; reintentar hasta que esté disponible
      if (!window.google?.accounts?.id) {
        setTimeout(init, 200);
        return;
      }
      window.google.accounts.id.initialize({
        client_id: CLIENT_ID,
        callback: handleCredential
      });
      if (divRef.current) {
        window.google.accounts.id.renderButton(divRef.current, {
          theme: "outline",
          size: "large",
          width: 320,
          text: "continue_with"
        });
      }
    }
    init();
    return () => { cancelled = true; };
  }, [navigate, onError]);

  if (!CLIENT_ID) return null;
  return <div ref={divRef} style={{ display: "flex", justifyContent: "center", marginTop: 8 }} />;
}
