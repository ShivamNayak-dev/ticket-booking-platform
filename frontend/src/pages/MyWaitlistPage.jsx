import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { fetchMyWaitlist, leaveWaitlist } from "../api/waitlistApi";
import Alert from "../components/Alert";
import Loader from "../components/Loader";
import WaitlistCard from "../components/WaitlistCard";
import { getErrorMessage } from "../utils/errors";

function MyWaitlistPage() {
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [leavingId, setLeavingId] = useState(null);

  const loadEntries = useCallback(async () => {
    try {
      const data = await fetchMyWaitlist();
      setEntries(data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadEntries();
  }, [loadEntries]);

  async function handleLeave(entry) {
    const confirmed = window.confirm(`Leave the waitlist for "${entry.eventTitle}"? You will lose your place in the queue.`);
    if (!confirmed) {
      return;
    }

    setLeavingId(entry.id);
    setError("");
    setSuccess("");

    try {
      await leaveWaitlist(entry.id);
      setSuccess("You left the waitlist.");
      await loadEntries();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLeavingId(null);
    }
  }

  if (loading) {
    return <Loader text="Loading your waitlist..." />;
  }

  return (
    <div>
      <h1>My waitlist</h1>
      <p className="muted">Events you are queueing for. Your position moves up as people ahead of you get tickets or leave.</p>

      <Alert type="error" message={error} />
      <Alert type="success" message={success} />

      {entries.length === 0 ? (
        <div className="empty-state">
          <h3>You are not on any waitlist</h3>
          <p>
            When an event is sold out you can join its waitlist from the <Link to="/">event page</Link>.
          </p>
        </div>
      ) : (
        <div className="booking-list">
          {entries.map((entry) => (
            <WaitlistCard key={entry.id} entry={entry} onLeave={handleLeave} leaving={leavingId === entry.id} />
          ))}
        </div>
      )}
    </div>
  );
}

export default MyWaitlistPage;
