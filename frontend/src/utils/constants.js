// keep in sync with the EventCategory enum in the backend
export const CATEGORIES = [
  { value: "MUSIC", label: "Music" },
  { value: "SPORTS", label: "Sports" },
  { value: "TECH", label: "Tech" },
  { value: "COMEDY", label: "Comedy" },
  { value: "THEATRE", label: "Theatre" },
  { value: "CONFERENCE", label: "Conference" },
  { value: "OTHER", label: "Other" },
];

// the backend also enforces this limit
export const MAX_TICKETS_PER_BOOKING = 10;

export const PAGE_SIZE = 6;
