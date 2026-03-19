import { initializeApp, getApps, getApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";
import { getDatabase } from "firebase/database";

const firebaseConfig = {
  apiKey: "AIzaSyAgWfMalS6pi-NcDFchKdP9n54vDyPd0ks",
  authDomain: "buslk-app.firebaseapp.com",
  databaseURL: "https://buslk-app-default-rtdb.asia-southeast1.firebasedatabase.app",
  projectId: "buslk-app",
  storageBucket: "buslk-app.firebasestorage.app",
  messagingSenderId: "876323453331",
  appId: "1:876323453331:web:e1946104dd4d58009fd066",
  measurementId: "G-X2EW199H9W"
};

// Initialize Firebase
const app = !getApps().length ? initializeApp(firebaseConfig) : getApp();
const auth = getAuth(app);
const db = getFirestore(app);
const rtdb = getDatabase(app);

export { app, auth, db, rtdb };
