import { Link } from "react-router-dom";
import { formatDateTime, formatPrice } from "../utils/formatters";

function BookingCard({ booking, onCancel, cancelling }) {
  const isCancelled = booking.status === "CANCELLED";
  const eventHasPassed = new Date(booking.eventDate) < new Date();

  return (
    <article className={`booking-card ${isCancelled ? "booking-card-cancelled" : ""}`}>
      <div className="booking-card-header">
        <div>
          <h3>
            <Link to={`/events/${booking.eventId}`}>{booking.eventTitle}</Link>
          </h3>
          <p className="muted">Reference: {booking.reference}</p>
        </div>
        <span className={`badge ${isCancelled ? "badge-soldout" : "badge-confirmed"}`}>{booking.status}</span>
      </div>

      <div className="booking-card-details">
        <p>
          <strong>When:</strong> {formatDateTime(booking.eventDate)}
        </p>
        <p>
          <strong>Where:</strong> {booking.venue}
        </p>
        <p>
          <strong>Tickets:</strong> {booking.quantity}
        </p>
        <p>
          <strong>Total paid:</strong> {formatPrice(booking.totalPrice)}
        </p>
        <p className="muted">Booked on {formatDateTime(booking.bookedAt)}</p>
      </div>

      {!isCancelled && !eventHasPassed && (
        <button type="button" className="btn btn-danger btn-small" onClick={() => onCancel(booking)} disabled={cancelling}>
          {cancelling ? "Cancelling..." : "Cancel booking"}
        </button>
      )}
    </article>
  );
}

export default BookingCard;
