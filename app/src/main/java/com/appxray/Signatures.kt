package com.appxray

/** A third-party library or service, recognised by a class-name prefix inside the app's code. */
data class Sdk(val prefix: String, val name: String, val category: String)

object Signatures {

    val sdks = listOf(
        // Backend and database
        Sdk("Lcom/google/firebase/auth/", "Firebase Authentication", "Backend & login"),
        Sdk("Lcom/google/firebase/firestore/", "Cloud Firestore (database)", "Backend & login"),
        Sdk("Lcom/google/firebase/database/", "Firebase Realtime Database", "Backend & login"),
        Sdk("Lcom/google/firebase/storage/", "Firebase Storage", "Backend & login"),
        Sdk("Lcom/google/firebase/functions/", "Firebase Cloud Functions", "Backend & login"),
        Sdk("Lcom/google/firebase/remoteconfig/", "Firebase Remote Config", "Backend & login"),
        Sdk("Lio/github/jan/supabase/", "Supabase", "Backend & login"),
        Sdk("Lcom/amazonaws/", "Amazon Web Services (AWS)", "Backend & login"),
        Sdk("Lcom/amplifyframework/", "AWS Amplify", "Backend & login"),
        Sdk("Lcom/parse/", "Parse", "Backend & login"),
        Sdk("Lio/appwrite/", "Appwrite", "Backend & login"),
        Sdk("Lio/realm/", "Realm / MongoDB", "Backend & login"),
        Sdk("Lcom/apollographql/", "Apollo GraphQL", "Backend & login"),
        Sdk("Lcom/auth0/", "Auth0 login", "Backend & login"),
        Sdk("Lcom/google/android/gms/auth/api/signin/", "Google Sign-In", "Backend & login"),
        Sdk("Lcom/facebook/login/", "Facebook Login", "Backend & login"),

        // Networking
        Sdk("Lokhttp3/", "OkHttp", "Networking"),
        Sdk("Lretrofit2/", "Retrofit (REST API client)", "Networking"),
        Sdk("Lio/ktor/", "Ktor", "Networking"),
        Sdk("Lcom/android/volley/", "Volley", "Networking"),
        Sdk("Lio/grpc/", "gRPC", "Networking"),
        Sdk("Lio/socket/", "Socket.IO (live updates)", "Networking"),
        Sdk("Lcom/pusher/", "Pusher (live updates)", "Networking"),

        // Payments
        Sdk("Lcom/stripe/", "Stripe", "Payments"),
        Sdk("Lcom/paypal/", "PayPal", "Payments"),
        Sdk("Lcom/braintreepayments/", "Braintree", "Payments"),
        Sdk("Lcom/razorpay/", "Razorpay", "Payments"),
        Sdk("Lcom/revenuecat/", "RevenueCat (subscriptions)", "Payments"),
        Sdk("Lcom/android/billingclient/", "Google Play Billing", "Payments"),
        Sdk("Lcom/adyen/", "Adyen", "Payments"),
        Sdk("Lcom/squareup/sdk/", "Square", "Payments"),

        // Analytics
        Sdk("Lcom/google/firebase/analytics/", "Firebase Analytics", "Analytics & tracking"),
        Sdk("Lcom/google/firebase/crashlytics/", "Firebase Crashlytics", "Analytics & tracking"),
        Sdk("Lcom/mixpanel/", "Mixpanel", "Analytics & tracking"),
        Sdk("Lcom/amplitude/", "Amplitude", "Analytics & tracking"),
        Sdk("Lcom/segment/", "Segment", "Analytics & tracking"),
        Sdk("Lcom/appsflyer/", "AppsFlyer", "Analytics & tracking"),
        Sdk("Lcom/adjust/sdk/", "Adjust", "Analytics & tracking"),
        Sdk("Lio/branch/", "Branch (deep links)", "Analytics & tracking"),
        Sdk("Lio/sentry/", "Sentry (crash reports)", "Analytics & tracking"),
        Sdk("Lcom/bugsnag/", "Bugsnag (crash reports)", "Analytics & tracking"),
        Sdk("Lcom/posthog/", "PostHog", "Analytics & tracking"),
        Sdk("Lcom/facebook/appevents/", "Facebook App Events", "Analytics & tracking"),
        Sdk("Lcom/microsoft/clarity/", "Microsoft Clarity", "Analytics & tracking"),
        Sdk("Lcom/smartlook/", "Smartlook (screen recording)", "Analytics & tracking"),

        // Ads
        Sdk("Lcom/google/android/gms/ads/", "Google AdMob", "Ads"),
        Sdk("Lcom/applovin/", "AppLovin", "Ads"),
        Sdk("Lcom/unity3d/ads/", "Unity Ads", "Ads"),
        Sdk("Lcom/ironsource/", "ironSource", "Ads"),
        Sdk("Lcom/facebook/ads/", "Meta Audience Network", "Ads"),
        Sdk("Lcom/vungle/", "Vungle / Liftoff", "Ads"),
        Sdk("Lcom/chartboost/", "Chartboost", "Ads"),

        // Messaging and notifications
        Sdk("Lcom/google/firebase/messaging/", "Firebase Cloud Messaging (push)", "Messaging & push"),
        Sdk("Lcom/onesignal/", "OneSignal (push)", "Messaging & push"),
        Sdk("Lcom/braze/", "Braze", "Messaging & push"),
        Sdk("Lcom/appboy/", "Braze (Appboy)", "Messaging & push"),
        Sdk("Lio/intercom/", "Intercom (support chat)", "Messaging & push"),
        Sdk("Lcom/zendesk/", "Zendesk (support)", "Messaging & push"),
        Sdk("Lzendesk/", "Zendesk (support)", "Messaging & push"),
        Sdk("Lcom/twilio/", "Twilio", "Messaging & push"),
        Sdk("Lio/getstream/", "Stream (chat/feeds)", "Messaging & push"),
        Sdk("Lcom/sendbird/", "Sendbird (chat)", "Messaging & push"),
        Sdk("Lio/agora/", "Agora (voice/video calls)", "Messaging & push"),
        Sdk("Lorg/webrtc/", "WebRTC (voice/video calls)", "Messaging & push"),

        // Maps and location
        Sdk("Lcom/google/android/gms/maps/", "Google Maps", "Maps & location"),
        Sdk("Lcom/google/android/gms/location/", "Google Location services", "Maps & location"),
        Sdk("Lcom/mapbox/", "Mapbox", "Maps & location"),
        Sdk("Lorg/osmdroid/", "OpenStreetMap (osmdroid)", "Maps & location"),
        Sdk("Lcom/here/", "HERE Maps", "Maps & location"),

        // Media and AI
        Sdk("Lcom/bumptech/glide/", "Glide (images)", "Media & AI"),
        Sdk("Lcom/squareup/picasso/", "Picasso (images)", "Media & AI"),
        Sdk("Lcoil/", "Coil (images)", "Media & AI"),
        Sdk("Lcom/facebook/fresco/", "Fresco (images)", "Media & AI"),
        Sdk("Lcom/google/android/exoplayer2/", "ExoPlayer (video/audio)", "Media & AI"),
        Sdk("Landroidx/media3/", "Media3 (video/audio)", "Media & AI"),
        Sdk("Lcom/airbnb/lottie/", "Lottie (animations)", "Media & AI"),
        Sdk("Lcom/google/mlkit/", "Google ML Kit", "Media & AI"),
        Sdk("Lorg/tensorflow/", "TensorFlow Lite", "Media & AI"),
        Sdk("Landroidx/camera/", "CameraX", "Media & AI"),
        Sdk("Lcom/google/ai/client/", "Google Gemini AI", "Media & AI"),
        Sdk("Lcom/aallam/openai/", "OpenAI client", "Media & AI"),

        // App building blocks
        Sdk("Landroidx/compose/", "Jetpack Compose (modern UI)", "App building blocks"),
        Sdk("Landroidx/room/", "Room (local database)", "App building blocks"),
        Sdk("Landroidx/work/", "WorkManager (background jobs)", "App building blocks"),
        Sdk("Landroidx/navigation/", "Jetpack Navigation", "App building blocks"),
        Sdk("Lcom/google/dagger/", "Dagger / Hilt", "App building blocks"),
        Sdk("Ldagger/hilt/", "Hilt", "App building blocks"),
        Sdk("Lorg/koin/", "Koin", "App building blocks"),
        Sdk("Lio/reactivex/", "RxJava", "App building blocks"),
        Sdk("Lkotlinx/coroutines/", "Kotlin Coroutines", "App building blocks"),
        Sdk("Lcom/google/gson/", "Gson (JSON)", "App building blocks"),
        Sdk("Lcom/squareup/moshi/", "Moshi (JSON)", "App building blocks"),
        Sdk("Lkotlinx/serialization/", "Kotlin Serialization", "App building blocks"),
        Sdk("Lnet/sqlcipher/", "SQLCipher (encrypted database)", "App building blocks"),
        Sdk("Landroidx/biometric/", "Fingerprint / face unlock", "App building blocks"),
    )

    /** Packages that belong to Android, Kotlin or common libraries rather than the app itself. */
    val libraryPackages = listOf(
        "android.", "androidx.", "kotlin.", "kotlinx.", "java.", "javax.", "dalvik.", "sun.",
        "com.google.", "com.android.", "org.jetbrains.", "org.intellij.", "okhttp3", "okio",
        "retrofit2", "com.squareup.", "io.reactivex", "org.json", "org.apache.", "com.facebook.",
        "org.w3c.", "org.xml.", "org.chromium.", "io.flutter.", "com.fasterxml.", "org.reactivestreams",
        "org.bouncycastle.", "org.slf4j", "io.grpc.", "io.ktor.", "dagger.", "javax.inject",
        "org.koin.", "coil.", "com.bumptech.", "io.sentry.", "com.airbnb.", "org.checkerframework",
        "_COROUTINE", "j$.", "org.intellij", "io.netty.", "org.conscrypt", "com.revenuecat.",
    )

    /** Web addresses that are just standard references, not servers the app talks to. */
    val noiseHosts = listOf(
        "schemas.android.com", "www.w3.org", "w3.org", "apache.org", "www.apache.org",
        "xmlpull.org", "ns.adobe.com", "xml.org", "json-schema.org", "example.com",
        "www.example.com", "schemas.microsoft.com", "purl.org", "schemas.xmlsoap.org",
        "schemas.openxmlformats.org", "xml.apache.org", "java.sun.com", "javax.xml",
        "www.slf4j.org", "goo.gl", "github.com/square", "localhost", "127.0.0.1",
        "www.googleapis.com/auth", "issuetracker.google.com", "developer.android.com",
        "kotlinlang.org", "www.unicode.org", "www.iana.org", "tools.ietf.org", "ietf.org",
    )
}
