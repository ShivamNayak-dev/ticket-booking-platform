import { Link } from "react-router-dom";
import { categoryLabel, formatDateTime, formatPrice } from "../utils/formatters";

function EventCard({ event }) {
  const seatsText = event.soldOut
    ? "Sold out"
    : `${event.availableSeats} of ${event.totalSeats} seats left`;

  return (
    <article className="event-card">
      <div className="event-card-top">
        <span className={`badge badge-${event.category.toLowerCase()}`}>
          {categoryLabel(event.category)}
        </span>
        {event.soldOut && <span className="badge badge-soldout">Sold out</span>}
      </div>

      <h3 className="event-card-title">{event.title}</h3>

      <p className="event-card-meta">{formatDateTime(event.eventDate)}</p>
      <p className="event-card-meta">{event.venue}</p>

      <div className="event-card-bottom">
        <div>
          <p className="event-card-price">{formatPrice(event.price)}</p>
          <p className="event-card-seats">{seatsText}</p>
        </div>
        <Link to={`/events/${event.id}`} className="btn btn-primary btn-small">
          View details
        </Link>
      </div>
    </article>
  );
}

export default EventCard;
