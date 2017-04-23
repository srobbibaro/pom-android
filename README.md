POM - Android
=====================

POM - Android is an application with the following functions:

  * link a user account to a device,
  * pull down location information from the server,
  * notify the server when the user has "arrived" at one of the specified locations.

## Getting Started

Follow Android development setup instructions here: http://developer.android.com/sdk/installing/index.html?pkg=tools

To compile the debug version, run: `$ ant debug`

To deploy to a USB-connected Android device, run: `$ tools/deploy.sh`

### Configuration

We do not store server addresses and secret keys in the source code. They should
be specified in `res/values/configuration.xml`. The application will not run
properly without these values set. Here's an example:

```
<?xml version="1.0" encoding="utf-8"?>
<resources>
  <string name="pom_server_url">http:/localhost:8000</string>
  <string name="pom_api_key">myapikey</string>
  <string name="pom_api_secret">myapisecret</string>
</resources>
```

This file is specified in the `.gitignore` file and should not be submitted to
the remote repository.

### Release Build

In order to properly build a release, you will need the appropriate keystore file
(not in source control), and the password.

Run: `$ tools/build_release.sh`

### Logging/Debug

With a device USB-connected, run: `$ tools/log.sh` for log messages specifically
for this application.
