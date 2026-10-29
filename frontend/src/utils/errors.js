// The backend always answers errors as { success:false, message, errors? }.
// These two helpers pull the useful parts out of an axios error.

export function getErrorMessage(error) {
  if (error.response && error.response.data && error.response.data.message) {
    return error.response.data.message;
  }
  if (error.request) {
    return "Cannot reach the server. Please check that the backend is running.";
  }
  return "Something went wrong. Please try again.";
}

export function getFieldErrors(error) {
  if (error.response && error.response.data && error.response.data.errors) {
    return error.response.data.errors;
  }
  return {};
}
