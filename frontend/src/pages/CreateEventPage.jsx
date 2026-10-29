import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { createEvent } from "../api/eventApi";
import Alert from "../components/Alert";
import { CATEGORIES } from "../utils/constants";
import { getErrorMessage, getFieldErrors } from "../utils/errors";
import { nowForDateTimeInput } from "../utils/formatters";

const INITIAL_FORM = {
  title: "",
  description: "",
  category: "",
  venue: "",
  eventDate: "",
  totalSeats: "",
  price: "",
};

function CreateEventPage() {
  const navigate = useNavigate();

  const [form, setForm] = useState(INITIAL_FORM);
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  function handleChange(event) {
    setForm({ ...form, [event.target.name]: event.target.value });
  }

  function validate() {
    const errors = {};

    if (!form.title.trim()) {
      errors.title = "Title is required";
    } else if (form.title.trim().length > 150) {
      errors.title = "Title must be at most 150 characters";
    }

    if (form.description.length > 2000) {
      errors.description = "Description must be at most 2000 characters";
    }

    if (!form.category) {
      errors.category = "Please choose a category";
    }

    if (!form.venue.trim()) {
      errors.venue = "Venue is required";
    }

    if (!form.eventDate) {
      errors.eventDate = "Event date is required";
    } else if (new Date(form.eventDate) <= new Date()) {
      errors.eventDate = "Event date must be in the future";
    }

    const seats = Number(form.totalSeats);
    if (!form.totalSeats || !Number.isInteger(seats) || seats < 1) {
      errors.totalSeats = "Enter a whole number of seats (at least 1)";
    } else if (seats > 100000) {
      errors.totalSeats = "Total seats cannot be more than 100000";
    }

    const price = Number(form.price);
    if (form.price === "" || Number.isNaN(price) || price < 0) {
      errors.price = "Enter a price of 0 or more";
    }

    return errors;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");

    const errors = validate();
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      return;
    }

    // datetime-local gives "2026-12-01T19:30", the backend is happy with that but we add seconds to be explicit
    const eventDate = form.eventDate.length === 16 ? `${form.eventDate}:00` : form.eventDate;

    setSubmitting(true);
    try {
      const created = await createEvent({
        title: form.title.trim(),
        description: form.description.trim(),
        category: form.category,
        venue: form.venue.trim(),
        eventDate,
        totalSeats: Number(form.totalSeats),
        price: Number(form.price),
      });
      navigate(`/events/${created.id}`);
    } catch (err) {
      setError(getErrorMessage(err));
      setFieldErrors(getFieldErrors(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="form-page">
      <h1>Create a new event</h1>
      <p className="muted">Fill in the details below. Tickets become available for booking right away.</p>

      <Alert type="error" message={error} />

      <form className="card-form" onSubmit={handleSubmit} noValidate>
        <div className="form-group">
          <label htmlFor="title">Title</label>
          <input id="title" name="title" type="text" value={form.title} onChange={handleChange} />
          {fieldErrors.title && <p className="field-error">{fieldErrors.title}</p>}
        </div>

        <div className="form-row">
          <div className="form-group">
            <label htmlFor="category">Category</label>
            <select id="category" name="category" value={form.category} onChange={handleChange}>
              <option value="">Select a category</option>
              {CATEGORIES.map((category) => (
                <option key={category.value} value={category.value}>
                  {category.label}
                </option>
              ))}
            </select>
            {fieldErrors.category && <p className="field-error">{fieldErrors.category}</p>}
          </div>

          <div className="form-group">
            <label htmlFor="eventDate">Date and time</label>
            <input
              id="eventDate"
              name="eventDate"
              type="datetime-local"
              min={nowForDateTimeInput()}
              value={form.eventDate}
              onChange={handleChange}
            />
            {fieldErrors.eventDate && <p className="field-error">{fieldErrors.eventDate}</p>}
          </div>
        </div>

        <div className="form-group">
          <label htmlFor="venue">Venue</label>
          <input id="venue" name="venue" type="text" value={form.venue} onChange={handleChange} />
          {fieldErrors.venue && <p className="field-error">{fieldErrors.venue}</p>}
        </div>

        <div className="form-row">
          <div className="form-group">
            <label htmlFor="totalSeats">Total seats</label>
            <input id="totalSeats" name="totalSeats" type="number" min="1" step="1" value={form.totalSeats} onChange={handleChange} />
            {fieldErrors.totalSeats && <p className="field-error">{fieldErrors.totalSeats}</p>}
          </div>

          <div className="form-group">
            <label htmlFor="price">Price per ticket (INR)</label>
            <input id="price" name="price" type="number" min="0" step="0.01" value={form.price} onChange={handleChange} />
            {fieldErrors.price && <p className="field-error">{fieldErrors.price}</p>}
          </div>
        </div>

        <div className="form-group">
          <label htmlFor="description">Description (optional)</label>
          <textarea id="description" name="description" rows="5" value={form.description} onChange={handleChange} />
          {fieldErrors.description && <p className="field-error">{fieldErrors.description}</p>}
        </div>

        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? "Creating..." : "Create event"}
        </button>
      </form>
    </div>
  );
}

export default CreateEventPage;
