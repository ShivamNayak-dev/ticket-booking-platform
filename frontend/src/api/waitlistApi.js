import axiosClient from "./axiosClient";

export async function joinWaitlist(eventId) {
  const response = await axiosClient.post("/waitlist", { eventId });
  return response.data.data;
}

export async function fetchMyWaitlist() {
  const response = await axiosClient.get("/waitlist/my");
  return response.data.data;
}

export async function leaveWaitlist(entryId) {
  const response = await axiosClient.delete(`/waitlist/${entryId}`);
  return response.data.data;
}
