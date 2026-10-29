import axiosClient from "./axiosClient";

export async function bookTickets(eventId, quantity) {
  const response = await axiosClient.post("/bookings", { eventId, quantity });
  return response.data.data;
}

export async function fetchMyBookings() {
  const response = await axiosClient.get("/bookings/my");
  return response.data.data;
}

export async function cancelBooking(bookingId) {
  const response = await axiosClient.post(`/bookings/${bookingId}/cancel`);
  return response.data.data;
}
