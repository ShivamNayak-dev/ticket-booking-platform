import { useCallback, useEffect, useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { bookTickets } from "../api/bookingApi";
import { fetchEventById } from "../api/eventApi";
import { joinWaitlist } from "../api/waitlistApi";
import Alert from "../components/Alert";
import Loader from "../components/Loader";
import { useAuth } from "../context/AuthContext";
import { MAX_TICKETS_PER_BOOKING } from "../utils/constants";
import { getErrorMessage } from "../utils/errors";
import { categoryLabel, formatDateTime, formatPrice } from "../utils/formatters";

function EventDetailsPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const location = useLocation();

  const [event, setEvent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  const [quantity, setQuantity] = useState(1);
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState({ type: "", message: "" });

  const loadEvent = useCallback(
    async (showLoader) => {
      if (showLoader) setLoading(true);
      setLoadError("");
      try {
        const data = await fetchEventById(id);
        setEvent(data);
        // keep the selected quantity inside what is still available
        setQuantity((current) => Math.max(1, Math.min(current, data.availableSeats || 1, MAX_TICKETS_PER_BOOKING)));
      } catch (err) {
        setLoadError(getErrorMessage(err));
      } finally {
        setLoading(false);
      }
    },
    [id]
  );

  useEffect(() => {
    setFeedback({ type: "", message: "" });
    loadEvent(true);
  }, [loadEvent]);

  async function handleBook(submitEvent) {
    submitEvent.preventDefault();
    setSubmitting(true);
    setFeedback({ type: "", message: "" });

    try {
      const booking = await bookTickets(event.id, quantity);
      setFeedback({
        type: "success",
        message: `Booking confirmed! Reference: ${booking.reference} (${booking.quantity} ticket${booking.quantity > 1 ? "s" : ""}).`,
      });
    } catch (err) {
      setFeedback({ type: "error", message: getErrorMessage(err) });
    } finally {
      setSubmitting(false);
      // always refresh, the seat count may have changed (also after a failed attempt)
      loadEvent(false);
    }
  }

  async function handleJoinWaitlist() {
    setSubmitting(true);
    setFeedback({ type: "", message: "" });

    try {
      const entry = await joinWaitlist(event.id);
      setFeedback({
        type: "success",
        message: `You are on the waitlist. Your position in the queue is #${entry.position}.`,
      });
    } catch (err) {
      setFeedback({ type: "error", message: getErrorMessage(err) });
    } finally {
      setSubmitting(false);
      loadEvent(false);
    }
  }

  if (loading) {
    return <Loader text="Loading event..." />;
  }

  if (loadError || !event) {
    return (
      <div>
        <Alert type="error" message={loadError || "Event not found"} />
        <Link to="/" className="btn btn-outline">
          Back to events
        </Link>
      </div>
    );
  }

  const eventHasPassed = new Date(event.eventDate) < new Date();
  const maxSelectable = Math.min(MAX_TICKETS_PER_BOOKING, event.availableSeats);
  const totalPrice = Number(event.price) * quantity;

  return (
    <div className="event-details">
      <Link to="/" className="back-link">
        &larr; Back to events
      </Link>

      <div className="event-details-layout">
        <section className="event-details-main">
          <div className="event-card-top">
            <span className={`badge badge-${event.category.toLowerCase()}`}>{categoryLabel(event.category)}</span>
            {event.soldOut && <span className="badge badge-soldout">Sold out</span>}
          </div>

          <h1>{event.title}</h1>

          <ul className="info-list">
            <li>
              <strong>Date:</strong> {formatDateTime(event.eventDate)}
            </li>
            <li>
              <strong>Venue:</strong> {event.venue}
            </li>
            <li>
              <strong>Organizer:</strong> {event.organizerName}
            </li>
            <li>
              <strong>Seats:</strong> {event.availableSeats} available out of {event.totalSeats}
            </li>
          </ul>

          <h3>About this event</h3>
          <p className="event-description">{event.description || "No description was provided for this event."}</p>
        </section>

        <aside className="booking-panel">
          <p className="booking-panel-price">{formatPrice(event.price)}</p>
          <p className="muted">per ticket</p>

          <Alert type={feedback.type || "info"} message={feedback.message} />

          {eventHasPassed ? (
            <Alert type="info" message="This event has already taken place." />
          ) : !user ? (
            <div>
              <p>Please log in to book tickets.</p>
              <Link to="/login" state={{ from: location.pathname }} className="btn btn-primary btn-block">
                Login to book
              </Link>
            </div>
          ) : event.soldOut ? (
            <div>
              <p>This event is sold out. Join the waitlist and we will book a ticket for you if a seat opens up.</p>
              <button type="button" className="btn btn-primary btn-block" onClick={handleJoinWaitlist} disabled={submitting}>
                {submitting ? "Please wait..." : "Join waitlist"}
              </button>
              <p className="muted small">
                See your place in the queue under <Link to="/my-waitlist">My Waitlist</Link>.
              </p>
            </div>
          ) : (
            <form onSubmit={handleBook}>
              <label htmlFor="quantity">Number of tickets</label>
              <select id="quantity" value={quantity} onChange={(e) => setQuantity(Number(e.target.value))}>
                {Array.from({ length: maxSelectable }, (_, index) => index + 1).map((count) => (
                  <option key={count} value={count}>
                    {count}
                  </option>
                ))}
              </select>

              <div className="booking-total">
                <span>Total</span>
                <strong>{formatPrice(totalPrice)}</strong>
              </div>

              <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
                {submitting ? "Booking..." : "Book tickets"}
              </button>
            </form>
          )}
        </aside>
      </div>
    </div>
  );
}

export default EventDetailsPage;
