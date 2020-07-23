// Display sign-in options on landing page
function loadSignIn() {
  // web app's Firebase configuration
  var firebaseConfig = {
    apiKey: "AIzaSyA1r_PfVDCXfTgoUNisci5Ag2MKEEwsZCE",
    authDomain: "fulfillment-deco-step-2020.firebaseapp.com",
    databaseURL: "https://fulfillment-deco-step-2020.firebaseio.com",
    projectId: "fulfillment-deco-step-2020",
    storageBucket: "fulfillment-deco-step-2020.appspot.com",
    messagingSenderId: "7165833112",
    appId: "1:7165833112:web:3b4af53c5de6aa73b7c5ed"
  };
  // Initialize Firebase
  firebase.initializeApp(firebaseConfig);

  // Initialize the FirebaseUI Widget using Firebase
  var ui = new firebaseui.auth.AuthUI(firebase.auth());

  ui.start('#firebaseui-auth-container', {
    // Provide sign in options for user to select from
    signInOptions : [
      {
        provider: firebase.auth.GoogleAuthProvider.PROVIDER_ID, // Sign in w/ Google Account
        customParameters: {
          prompt: 'select_account'
        }
      },
    ],
    signInFlow: 'popup',
    signInSuccessUrl: "/enterClass.html",
  });
}

// Log out and provide indication of user status
function logout() {
  firebase.auth().signOut().then(function() {
  console.log('Signed Out');
  }, function(error) {
  console.error('Sign Out Error', error);
  });
}

// Homepage checks for sign in onload
function start() {
  loadSignIn();
}