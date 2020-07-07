const config = {
  apiKey: 'AIzaSyA1r_PfVDCXfTgoUNisci5Ag2MKEEwsZCE',
  databaseURL: "https://fulfillment-deco-step-2020.firebaseio.com",
  projectId: "fulfillment-deco-step-2020",
};
firebase.initializeApp(config);

// If user is not logged in, redirect to the home page
firebase.auth().onAuthStateChanged(function(user) {
  if (!user) {
    window.location.href = "/";
  }
  else {
    console.log("User is signed in.");
  }
});

// Obtain the current user token for verification
function getToken() {
  return firebase.auth().currentUser.getIdToken();
}

function getUid() {
  return firebase.auth().currentUser.uid;
}