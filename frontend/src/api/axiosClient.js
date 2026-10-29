import axios from "axios";

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
  // send the HttpOnly auth cookie with every request
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 15000,
});

// If the session expires while the user is on a page, tell the rest of the app
// so it can clear the logged-in user. Calls to /auth/* are ignored because a
// failed login is also a 401 and is handled by the login page itself.
axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response ? error.response.status : null;
    const url = error.config && error.config.url ? error.config.url : "";

    if (status === 401 && !url.startsWith("/auth/")) {
      window.dispatchEvent(new Event("auth:expired"));
    }
    return Promise.reject(error);
  }
);

export default axiosClient;
