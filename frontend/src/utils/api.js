const API_URL = "http://localhost:8080";

function getAuthHeaders() {
  const token = sessionStorage.getItem("token");
  return {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {})
  };
}

export async function apiFetch(endpoint, options = {}) {
  const response = await fetch(`${API_URL}${endpoint}`, {
    ...options,
    headers: {
      ...getAuthHeaders(),
      ...options.headers
    }
  });

  const text = await response.text();
  const data = text ? JSON.parse(text) : {};

  // Sesión expirada o usuario eliminado → forzar re-login
  if (response.status === 401) {
    sessionStorage.clear();
    window.location.href = "/";
    return;
  }

  if (!response.ok) {
    const error = new Error(data.error || `Error ${response.status}`);
    error.status = response.status;
    error.body = data; // expone flags como requiereVerificacion
    throw error;
  }

  return data;
}