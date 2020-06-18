function getParam(name) {
  const query = window.location.search;
  const urlParams = new URLSearchParams(query);
  return urlParams.get(name);
}