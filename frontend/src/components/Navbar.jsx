import { Link, NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate("/");
  }

  return (
    <header className="navbar">
      <div className="container navbar-inner">
        <Link to="/" className="navbar-brand">
          Ticket<span>Book</span>
        </Link>

        <nav className="navbar-links">
          <NavLink to="/" end>
            Events
          </NavLink>

          {user && (
  <>
    {["ORGANIZER", "ADMIN"].includes(user.role) && (
      <NavLink to="/events/new">Create Event</NavLink>
    )}

    <NavLink to="/my-bookings">My Bookings</NavLink>
    <NavLink to="/my-waitlist">My Waitlist</NavLink>
  </>
)}
        </nav>

        <div className="navbar-user">
          {user ? (
            <>
              <span className="navbar-greeting">Hi, {user.name}</span>
              <button type="button" className="btn btn-outline btn-small" onClick={handleLogout}>
                Logout
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn btn-outline btn-small">
                Login
              </Link>
              <Link to="/register" className="btn btn-primary btn-small">
                Sign up
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}

export default Navbar;
