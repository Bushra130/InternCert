# InternCert

An Android application built to generate, dispatch, and verify digital internship certificates with QR-code authentication.

## Key Features
- **Custom Certificate Generation:** Dynamically renders personalized internship certificates using Android Canvas.
- **Live Web Verification Portal:** Scanning the certificate's QR code opens the hosted web portal (`verify.html`), which fetches and displays the certificate's details (recipient name, internship domain, issue date, and verification status) directly from Cloud Firestore.
- **Direct Email Dispatch:** Allows users to share generated certificates through Gmail using Android email intents with built-in recipient validation.

## Tech Stack
- **Mobile App:** Kotlin, XML Layouts, Android SDK
- **Backend & Hosting:** Firebase Cloud Firestore, Firebase Hosting
- **Web Verification Portal:** HTML5, JavaScript (`verify.html`)

## Download & Testing
An installable debug APK (`InternCert-v1.0.apk`) is available under the [Releases](https://github.com/Bushra130/InternCert/releases) section of this repository for quick evaluation on Android devices.

## Author
- **Bushra Shaikh** ([@Bushra130](https://github.com/Bushra130)) — Developed as part of the Android Development Internship at Codec Technologies.
