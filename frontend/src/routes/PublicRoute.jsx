import { useEffect } from "react";

function PublicRoute({ children }) {
  useEffect(() => {
    sessionStorage.clear();
  }, []);

  return children;
}

export default PublicRoute;