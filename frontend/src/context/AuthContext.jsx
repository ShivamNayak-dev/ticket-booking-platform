import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { fetchCurrentUser, loginUser, logoutUser, registerUser } from "../api/authApi";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  // true until we know whether the browser still has a valid login cookie
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;

    fetchCurrentUser()
      .then((currentUser) => {
        if (active) setUser(currentUser);
      })
      .catch(() => {
        if (active) setUser(null);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  // axiosClient fires this event when the server says the session is gone
  useEffect(() => {
    const handleExpired = () => setUser(null);
    window.addEventListener("auth:expired", handleExpired);
    return () => window.removeEventListener("auth:expired", handleExpired);
  }, []);

  const value = useMemo(() => {
    async function login(credentials) {
      const loggedInUser = await loginUser(credentials);
      setUser(loggedInUser);
      return loggedInUser;
    }

    async function register(details) {
      const newUser = await registerUser(details);
      setUser(newUser);
      return newUser;
    }

    async function logout() {
      try {
        await logoutUser();
      } finally {
        // even if the request fails we forget the user locally
        setUser(null);
      }
    }

    return { user, loading, login, register, logout };
  }, [user, loading]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside an AuthProvider");
  }
  return context;
}
