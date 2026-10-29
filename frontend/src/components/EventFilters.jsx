import { CATEGORIES } from "../utils/constants";

function EventFilters({ filters, onChange, onClear }) {
  const hasActiveFilters = filters.category || filters.from || filters.to;

  function handleInputChange(event) {
    onChange(event.target.name, event.target.value);
  }

  return (
    <section className="filters">
      <div className="filter-field">
        <label htmlFor="category">Category</label>
        <select id="category" name="category" value={filters.category} onChange={handleInputChange}>
          <option value="">All categories</option>
          {CATEGORIES.map((category) => (
            <option key={category.value} value={category.value}>
              {category.label}
            </option>
          ))}
        </select>
      </div>

      <div className="filter-field">
        <label htmlFor="from">From date</label>
        <input id="from" name="from" type="date" value={filters.from} max={filters.to || undefined} onChange={handleInputChange} />
      </div>

      <div className="filter-field">
        <label htmlFor="to">To date</label>
        <input id="to" name="to" type="date" value={filters.to} min={filters.from || undefined} onChange={handleInputChange} />
      </div>

      <div className="filter-field filter-actions">
        <button type="button" className="btn btn-outline" onClick={onClear} disabled={!hasActiveFilters}>
          Clear filters
        </button>
      </div>
    </section>
  );
}

export default EventFilters;
