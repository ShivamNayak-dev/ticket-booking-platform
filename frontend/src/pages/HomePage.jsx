import { useEffect, useState } from "react";
import { fetchEvents } from "../api/eventApi";
import Alert from "../components/Alert";
import EventCard from "../components/EventCard";
import EventFilters from "../components/EventFilters";
import Loader from "../components/Loader";
import Pagination from "../components/Pagination";
import { PAGE_SIZE } from "../utils/constants";
import { getErrorMessage } from "../utils/errors";

const EMPTY_FILTERS = { category: "", from: "", to: "" };

function HomePage() {
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [page, setPage] = useState(0);

  const [events, setEvents] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // reload whenever a filter or the page changes
  useEffect(() => {
    let cancelled = false;

    async function loadEvents() {
      setLoading(true);
      setError("");
      try {
        const result = await fetchEvents({ ...filters, page, size: PAGE_SIZE });
        if (cancelled) return;
        setEvents(result.content);
        setTotalPages(result.totalPages);
        setTotalElements(result.totalElements);
      } catch (err) {
        if (!cancelled) setError(getErrorMessage(err));
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    loadEvents();

    // if the user changes a filter before the response arrives, ignore the old response
    return () => {
      cancelled = true;
    };
  }, [filters, page]);

  function handleFilterChange(name, value) {
    setFilters((previous) => ({ ...previous, [name]: value }));
    setPage(0); // a new search always starts on the first page
  }

  function handleClearFilters() {
    setFilters(EMPTY_FILTERS);
    setPage(0);
  }

  return (
    <div>
      <section className="hero">
        <h1>Find your next event</h1>
        <p>Browse concerts, matches, conferences and more. Book your tickets in seconds.</p>
      </section>

      <EventFilters filters={filters} onChange={handleFilterChange} onClear={handleClearFilters} />

      <Alert type="error" message={error} />

      {loading ? (
        <Loader text="Loading events..." />
      ) : (
        <>
          <p className="results-count">
            {totalElements} {totalElements === 1 ? "event" : "events"} found
          </p>

          {events.length === 0 && !error ? (
            <div className="empty-state">
              <h3>No events match your search</h3>
              <p>Try a different category or date range.</p>
            </div>
          ) : (
            <div className="event-grid">
              {events.map((event) => (
                <EventCard key={event.id} event={event} />
              ))}
            </div>
          )}

          <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  );
}

export default HomePage;
