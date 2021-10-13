# Demo Android application which uses HAAPI

[![Quality](https://img.shields.io/badge/quality-demo-red)](https://curity.io/resources/code-examples/status/)
[![Availability](https://img.shields.io/badge/availability-source-blue)](https://curity.io/resources/code-examples/status/)

This is an example Android app which uses the Curity Identity Server's Hypermedia API to perform an
OAuth2 flow with authentication done completely from the app, without the need of an external browser.

Note: The app needs at least Android 8.0 (*Oreo*, API level 26) to properly use the attestation features.
You will need the Curity Identity Server at least in version 5.4. to work with this app.

## Getting started

### Android Studio

Android Studio version >= 4.1.2.

### Setting up the identity server

Install and run Curity Identity Server: https://curity.io/resources/getting-started/ 

### Configure the identity server

The demo is configured to run with `curity-android-config.xml`. 

❗️When applying this configuration to your identity server, you will be able to run directly the demo application on the emulator. 

1. Login to your identity server to access the configuration page
2. Upload `curity-android-config.xml` to your identity server and commit the changes

## Testing the demo app against your identity server

__Prerequisite__: "Setting up the identity server" and "Configure the identity server" are required

### Emulator

1. The identity server is running
2. Start the demo application on an emulator that has API level bigger than 26.
3. Tap the button `Start Authentication`

### Physical device (API level >= 26)

1. The identity server is running
2. In the identity server, update the identity server URL to be reachable from your physical device.
3. Start the demo application on your physical device
4. Tap Settings in the tab navigation bar of the app.
5. Edit a configuration to target your identity server. If you are using `curity-android-config.xml`, you need to replace https://10.0.2.2:8443 with the identity server URL.
6. Tap `Home` in the tab navigation
7. Tap the button `Start Authentication`

## How to get the API Signature ?

#### Running a device with an API version >= API 28 (Android 9.0, Pie)

When starting the demo application, in Logcat, you should the APK Signature printed in DEBUG mode.

`2021-07-14 12:22:37.952 9631-9631/io.curity.haapidemo D/AppInfo: APK signatures $RESULT$`

#### Running a device with an API version < API 28 (Android 9.0, Pie)

Follow the instructions in this page: https://curity.io/docs/idsvr/latest/developer-guide/haapi/index.html#android-client-attestation-configuration

## Resources

- [Introduction](https://curity.io/resources/architect/haapi/what-is-hypermedia-authentication-api/)
  to the Hypermedia Authentication API.

- [An article](https://curity.io/resources/tutorials/howtos/haapi/authentication-api-android-sdk)
  showing how to properly configure the Curity Identity Server and a client to use the Hypermedia
  API from an Android app.
