import { Link } from "react-router-dom";

function NotFoundPage() {
  return (
    <div className="empty-state">
      <h1>404</h1>
      <p>The page you are looking for does not exist.</p>
      <Link to="/" className="btn btn-primary">
        Go to home
      </Link>
    </div>
  );
}

export default NotFoundPage;
