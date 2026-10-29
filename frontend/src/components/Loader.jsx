function Loader({ text = "Loading..." }) {
  return (
    <div className="loader">
      <div className="loader-spinner" />
      <p>{text}</p>
    </div>
  );
}

export default Loader;
