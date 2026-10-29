import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { cancelBooking, fetchMyBookings } from "../api/bookingApi";
import Alert from "../components/Alert";
import BookingCard from "../components/BookingCard";
import Loader from "../components/Loader";
import { getErrorMessage } from "../utils/errors";

function MyBookingsPage() {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [cancellingId, setCancellingId] = useState(null);

  const loadBookings = useCallback(async () => {
    try {
      const data = await fetchMyBookings();
      setBookings(data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadBookings();
  }, [loadBookings]);

  async function handleCancel(booking) {
    const confirmed = window.confirm(`Cancel your booking for "${booking.eventTitle}"?`);
    if (!confirmed) {
      return;
    }

    setCancellingId(booking.id);
    setError("");
    setSuccess("");

    try {
      await cancelBooking(booking.id);
      setSuccess(`Booking ${booking.reference} was cancelled.`);
      await loadBookings();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setCancellingId(null);
    }
  }

  if (loading) {
    return <Loader text="Loading your bookings..." />;
  }

  return (
    <div>
      <h1>My bookings</h1>

      <Alert type="error" message={error} />
      <Alert type="success" message={success} />

      {bookings.length === 0 ? (
        <div className="empty-state">
          <h3>You have no bookings yet</h3>
          <p>
            <Link to="/">Browse events</Link> and book your first ticket.
          </p>
        </div>
      ) : (
        <div className="booking-list">
          {bookings.map((booking) => (
            <BookingCard key={booking.id} booking={booking} onCancel={handleCancel} cancelling={cancellingId === booking.id} />
          ))}
        </div>
      )}
    </div>
  );
}

export default MyBookingsPage;
