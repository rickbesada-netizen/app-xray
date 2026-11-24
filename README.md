# App X-Ray

An Android app that looks inside the other apps on your phone and shows how they were built.

## What it shows for each app

- **How it was built:** Flutter, React Native, Unity, native Kotlin/Java, and so on.
- **Services it uses:** Firebase, Stripe, Supabase, Google Maps, analytics and ads tools.
- **Servers it talks to:** the web addresses found in its code, plus likely API paths.
- **Screens, permissions and background services.**
- **Links that open the app**, and its Firebase project, if it has one.

## Sending an app to Claude

Tap **Send app + report to Claude**. This packs the app's APK file(s) and the report into one `.zip` file
and opens the share menu, so you can send it to Claude for a full breakdown with REA.

## Installing it

1. On your phone, open this repository's **Releases** page and tap `app-xray.apk`.
2. Allow your browser to install unknown apps when Android asks.
3. Open **App X-Ray**.

Each push to GitHub builds a new APK automatically; see `.github/workflows/build.yml`.
