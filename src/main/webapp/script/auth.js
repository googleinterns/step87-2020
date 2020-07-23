const config = {
  apiKey: 'AIzaSyA1r_PfVDCXfTgoUNisci5Ag2MKEEwsZCE',
  databaseURL: "https://fulfillment-deco-step-2020.firebaseio.com",
  projectId: "fulfillment-deco-step-2020",
  

authDomain: "fulfillment-deco-step-2020.firebaseapp.com",
storageBucket: "fulfillment-deco-step-2020.appspot.com",
messagingSenderId: "7165833112",
appId: "1:7165833112:web:3b4af53c5de6aa73b7c5ed"
};
firebase.initializeApp(config);

// If user is not logged in, redirect to the home page
firebase.auth().onAuthStateChanged(function(user) {
  if (!user && (window.location.pathname !== "/")) {
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

function signOut() {
  firebase.auth().signOut().then(function() {
    console.log('Signed Out');
  }, function(error) {
    console.error('Sign Out Error', error);
  });
}