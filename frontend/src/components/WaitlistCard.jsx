import { Link } from "react-router-dom";
import { formatDateTime } from "../utils/formatters";

const STATUS_TEXT = {
  WAITING: "Waiting",
  PROMOTED: "Got a ticket",
  CANCELLED: "Left the queue",
};

function WaitlistCard({ entry, onLeave, leaving }) {
  const isWaiting = entry.status === "WAITING";

  return (
    <article className="booking-card">
      <div className="booking-card-header">
        <div>
          <h3>
            <Link to={`/events/${entry.eventId}`}>{entry.eventTitle}</Link>
          </h3>
          <p className="muted">Joined on {formatDateTime(entry.joinedAt)}</p>
        </div>
        <span className={`badge ${entry.status === "PROMOTED" ? "badge-confirmed" : "badge-soldout"}`}>
          {STATUS_TEXT[entry.status] || entry.status}
        </span>
      </div>

      <div className="booking-card-details">
        <p>
          <strong>Event date:</strong> {formatDateTime(entry.eventDate)}
        </p>

        {isWaiting && (
          <p className="waitlist-position">
            Your position in the queue: <strong>#{entry.position}</strong>
          </p>
        )}

        {entry.status === "PROMOTED" && (
          <p>
            A seat opened up and a ticket was booked for you. Check <Link to="/my-bookings">My Bookings</Link>.
          </p>
        )}
      </div>

      {isWaiting && (
        <button type="button" className="btn btn-outline btn-small" onClick={() => onLeave(entry)} disabled={leaving}>
          {leaving ? "Leaving..." : "Leave waitlist"}
        </button>
      )}
    </article>
  );
}

export default WaitlistCard;
