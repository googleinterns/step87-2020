// Give the service worker access to Firebase Messaging.
importScripts('https://www.gstatic.com/firebasejs/7.15.0/firebase-app.js');
importScripts('https://www.gstatic.com/firebasejs/7.15.0/firebase-messaging.js');

// Initialize the Firebase app in the service worker
var firebaseConfig = {
  apiKey: "AIzaSyA1r_PfVDCXfTgoUNisci5Ag2MKEEwsZCE",
  authDomain: "fulfillment-deco-step-2020.firebaseapp.com",
  databaseURL: "https://fulfillment-deco-step-2020.firebaseio.com",
  projectId: "fulfillment-deco-step-2020",
  storageBucket: "fulfillment-deco-step-2020.appspot.com",
  messagingSenderId: "7165833112",
  appId: "1:7165833112:web:3b4af53c5de6aa73b7c5ed"
};

firebase.initializeApp(firebaseConfig);

// Retrieve an instance of Firebase Messaging to handle background messages
const messaging = firebase.messaging();

//handles background messaging
messaging.setBackgroundMessageHandler(function(payload) {
  console.log('[firebase-messaging-sw.js] Received background message ', payload);
  // Customize notification here
  const notificationTitle = 'Background Message Title';
  const notificationOptions = {
    body: 'Background Message body.'
  };

  return self.registration.showNotification(notificationTitle,
    notificationOptions);
});

