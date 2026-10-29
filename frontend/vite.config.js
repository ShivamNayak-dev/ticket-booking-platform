import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// During development every request that starts with /api is forwarded to the
// Spring Boot server. That way the browser thinks frontend and backend share one
// origin, so the HttpOnly cookie works without any extra CORS fiddling.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
