// Display sign-in options on landing page
function loadSignIn() {
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
    signInSuccessUrl: "/userDash.html",
    signInFlow: 'popup',
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

// If user is logged in, redirect to user dashboard
firebase.auth().onAuthStateChanged(function(user) {
  if (user) {
    window.location.href = "/userDash.html";
  }
});

// Homepage checks for sign in onload
function start() {
  loadSignIn();
}