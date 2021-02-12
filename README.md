# Demo Android application which uses HAAPI

[![Quality](https://img.shields.io/badge/quality-demo-red)](https://curity.io/resources/code-examples/status/)
[![Availability](https://img.shields.io/badge/availability-source-blue)](https://curity.io/resources/code-examples/status/)

This is an example Android app which uses the Curity Identity Server's Hypermedia API to perform an
OAuth2 flow with authentication done completely from the app, without the need of an external browser.

Note: The app needs at least Android 8.0 (*Oreo*, API level 26) to properly use the attestation features.
You will need the Curity Identity Server at least in version 5.4. to work with this app.

## Running the example

Before running the app, e.g. from Android Studio set properties which apply to your instance of the
Curity Identity Server. These properties can be found in the `Configuration.kt` file in the
`app/src/main/java/com/example/haapidemo` directory.

## Resources

- [Introduction](https://curity.io/resources/architect/haapi/what-is-hypermedia-authentication-api/)
  to the Hypermedia Authentication API.

- [An article](https://curity.io/resources/tutorials/howtos/haapi/authentication-api-android-sdk)
  showing how to properly configure the Curity Identity Server and a client to use the Hypermedia
  API from an Android app.
