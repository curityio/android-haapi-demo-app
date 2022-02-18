# Demo Android application which uses HAAPI

[![Quality](https://img.shields.io/badge/quality-demo-red)](https://curity.io/resources/code-examples/status/)
[![Availability](https://img.shields.io/badge/availability-source-blue)](https://curity.io/resources/code-examples/status/)

This is an example Android app which uses the Curity Identity Server's Hypermedia API to perform an
OAuth2 flow with authentication done completely from the app, without the need of an external browser.

Note: The app needs at least Android 8.0 (*Oreo*, API level 26) to properly use the attestation features.
You will need the Curity Identity Server at least in version 5.4. to work with this app.

## Getting started

Note that gradle tasks require at least Java 11 to run properly. Make sure to have the proper Java SDK
version set in `Preferences / Build,Execution,Deployment / Build Tools / Gradle / Gradle JDK` in Android Studio.

### Docker Automated Setup

The required Curity Identity Server setup and connectivity from devices can be automated via a bash script:

1. Copy a license.json file into the code example root folder.
2. Run the `./start-idsvr.sh` script to deploy a preconfigured Curity Identity Server via Docker. 
3. Build and run the mobile app from Android Studio using an emulator of your choice.
4. There is a preconfigured user account you can sign-in with: demouser / Password1. Feel free to create additional accounts.
5. Run the `./stop-idsvr.sh` script to free Docker resources.

By default the Curity Identity Server instance runs on the the Android emulator's default host IP. 
If you prefer to expose the Server on the Internet (e.g. to test with a real device), you can use the 
ngrok tool for that. Edit the `USE_NGROK` variable in `start-server.sh` and `stop-server.sh` scripts.
This [Mobile Setup](https://curity.io/resources/learn/mobile-setup-ngrok/) tutorial further describes
this option.

### Setting up with Your Own Instance of the Curity Identity Server

You can install and run your own instance of the Curity Identity Server by following this tutorial: https://curity.io/resources/getting-started/ 

Once installed you can easily configure the server by uploading the provided configuration file.
❗️When applying the provided configuration to your identity server, you will be able to run directly the demo application on the emulator. 

To upload the configuration, follow these steps:
1. Login to the admin UI (https://localhost:6749/admin if you're using defaults).
2. Upload `curity-android-config.xml` through the **Changes**->**Upload** menu at the top. (Make sure to use the `Merge` option)
3. Commit changes through the **Changes**->**Commit** menu.

## Testing the demo app against your identity server

### Emulator

1. Make sure that the Curity Identity Server is running and configured.
2. Start the demo application on an emulator that has Android API level equal to or larger than 26.
3. Tap the button `Start Authentication`.

### Physical device (API level >= 26)

1. Make sure that the Curity Identity Server is running and configured to be reachable on the Internet (e.g. by using [ngrok](https://curity.io/resources/learn/expose-local-curity-ngrok/)).
2. Start the demo application on your physical device.
4. Tap Settings in the tab navigation bar of the app.
5. Edit a configuration to target your instance of the Curity Identity Server. If you are using `curity-android-config.xml`,
   you need to replace https://10.0.2.2:8443 with the identity server URL.
6. Tap `Home` in the tab navigation.
7. Tap the button `Start Authentication`.

## How to get the API Signature ?

Use the signingReport task (`./gradlew SigningReport`) to get the app signature. You can copy the `SHA-256` signature and paste it in the signature field
in the admin UI (on your client's settings page). If you want to paste the signature into an XML file, or use the CLI or RestConf API to add the signature,
then you need to use a base64 version of the signature hash. You can run this command to obtain the encoded signature:

```shell
echo "<sha-256 signature from the signingReport>" | xxd -r -p | base64
```

If you're running the demo app on a device with an API version >= API 28 (Android 9.0, Pie), then when 
starting the application, in Logcat, you should the APK Signature printed in DEBUG mode.

`2021-07-14 12:22:37.952 9631-9631/io.curity.haapidemo D/AppInfo: APK signatures $RESULT$`

## Configuring the App

The application needs a few configuration options set to be able to call the instance of the Curity Identity Server.
Default configuration is set to work with the dockerized version of the Curity Identity Server which
is run with the `start-idsvr.sh` script. Should you need to make the app work with a different environment
(e.g. you have your instance of the Curity Identity Server already working online), then you can adjust
the configuration in two ways:

1. You can edit the default settings in the `src/main/java/io/curity/haapidemo/Configuration.kt` file.
   The default settings are returned by the static `newInstance` method, and this is the one that should
   be modified.

2. You can update settings directly in the running app. When on the home screen of the app you can tap
   the settings icon. There you will be able to create and edit configuration profiles. You can then
   switch the active profile to quickly test between different environments of the Curity Identity Server.

## Resources

- [Introduction](https://curity.io/resources/learn/what-is-hypermedia-authentication-api/)
  to the Hypermedia Authentication API.

- [An article](https://curity.io/resources/learn/authentication-api-android-sdk)
  showing how to properly configure the Curity Identity Server and a client to use the Hypermedia
  API from an Android app.
