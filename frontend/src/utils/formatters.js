import { CATEGORIES } from "./constants";

export function formatDateTime(value) {
  if (!value) {
    return "";
  }
  const date = new Date(value);
  return date.toLocaleString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function formatPrice(value) {
  const amount = Number(value);
  if (amount === 0) {
    return "Free";
  }
  return amount.toLocaleString("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
  });
}

export function categoryLabel(value) {
  const match = CATEGORIES.find((category) => category.value === value);
  return match ? match.label : value;
}

// value for <input type="datetime-local" min=...>, in local time
export function nowForDateTimeInput() {
  const now = new Date();
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
  return now.toISOString().slice(0, 16);
}
