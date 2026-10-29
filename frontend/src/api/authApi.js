import axiosClient from "./axiosClient";

export async function registerUser(payload) {
  const response = await axiosClient.post("/auth/register", payload);
  return response.data.data;
}

export async function loginUser(payload) {
  const response = await axiosClient.post("/auth/login", payload);
  return response.data.data;
}

export async function logoutUser() {
  await axiosClient.post("/auth/logout");
}

export async function fetchCurrentUser() {
  const response = await axiosClient.get("/auth/me");
  return response.data.data;
}
