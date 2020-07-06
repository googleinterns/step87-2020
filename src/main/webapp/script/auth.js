const config = {
  apiKey: 'AIzaSyA1r_PfVDCXfTgoUNisci5Ag2MKEEwsZCE',
  databaseURL: "https://fulfillment-deco-step-2020.firebaseio.com",
  projectId: "fulfillment-deco-step-2020",
};
firebase.initializeApp(config);

firebase.auth().onAuthStateChanged(function(user) {
  if (!user) {
    window.location.href = "/";
  }
});

function getToken() {
  return firebase.auth().currentUser.getIdToken();
}

function getUid() {
  return firebase.auth().currentUser.uid;
}