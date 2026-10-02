# WyCDN Sample App for Android

This sample application provides a demonstration of the capabilities and usage of the WyCDN SDK for Android developers.
It is designed to help you integrate the WyCDN service into your Android application effectively.
Below, you will find instructions on how to set up and run the sample app.

## Getting Started

### Prerequisites

- Android Studio with compatible AGP

### Installation

1. Clone this repository to your local machine using your preferred Git client.
2. Open the project in Android Studio.
3. Build the project by selecting 'Build -> Make Project'.

### Running the App

1. Connect your Android device via USB or use an Android emulator.
2. Run the app from Android Studio by selecting 'Run -> Run 'wycdn-sampleapp-android''.

### Selecting the ABIs

Since version 15.36.11, the WyCDN service has a Java part and one native part for each ABI (Application Binary Interface). By default, the app uses all the ABIs. The `wycdnAbis` Gradle property selects some ABIs only. Separate the values with commas.

| value    | ABI           |
|----------|---------------|
| `arm`    | `armeabi-v7a` |
| `arm64`  | `arm64-v8a`   |
| `x86`    | `x86`         |
| `x86_64` | `x86_64`      |

```shell
./gradlew assembleWithoutFirebaseCrashlyticsRelease -PwycdnAbis=arm64,arm
```

Versions of the WyCDN service older than 15.36.11 have no separate parts. With these versions, do not set `wycdnAbis`.
