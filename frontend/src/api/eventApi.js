import axiosClient from "./axiosClient";

export async function fetchEvents(filters) {
  // only send the filters that actually have a value
  const params = {};
  if (filters.category) params.category = filters.category;
  if (filters.from) params.from = filters.from;
  if (filters.to) params.to = filters.to;
  params.page = filters.page;
  params.size = filters.size;

  const response = await axiosClient.get("/events", { params });
  return response.data.data;
}

export async function fetchEventById(id) {
  const response = await axiosClient.get(`/events/${id}`);
  return response.data.data;
}

export async function createEvent(payload) {
  const response = await axiosClient.post("/events", payload);
  return response.data.data;
}
