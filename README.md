# Spyglass ALPR Launcher Android SDK

## Overview

The **Spyglass ALPR Launcher SDK** is a lightweight solution designed to integrate Android
applications with the SENTINEL/LEGION ALPR app. With this SDK, your app can securely launch the
SENTINEL/LEGION app, pass necessary configuration data, and start the scanning process. The SDK also
provides responses such as success, failure, and cancellation for handling scan results.

This SDK allows your application to:

- Launch SENTINEL/LEGION ALPR app activities.
- Pass hotlist databases and configure scanning parameters.
- Handle scanner responses (success, failure, cancellation).
- Define and configure the camera or scanning device to be used for license plate recognition.

### Notes

- **Permissions:** This SDK does not require any `uses-permission`.
- **Libraries:** The SDK does not include any third-party libraries.
- **Minimum SDK Version:** 26 (Android 8.0)
- **Target SDK Version:** 34 (Android 14)
- **SENTINEL or LEGION app** must be installed on the target device.

### Sample App

You can download the APK of the sample application from the link below:

[Download the Latest Sample APK](https://github.com/Spyglass-Tech/alpr-launcher-sample/releases/tag/sample-app)

## Installation

### Implementation

1. **Adding the AAR File to Your Project:**

- Check the [Releases](https://github.com/Spyglass-Tech/alpr-launcher-sample/releases) to
  download the latest version of the SDK (Sentinel or Legion).
- Place the provided `sentinel-or-legion-launcher-vLATEST_VERSION.aar` file into your
  app's `app/libs` directory.

2. **Updating the build.gradle File:**

- In your project's `build.gradle (:app)` file, add the following line to the `dependencies`
  block:

```groovy
dependencies {
    implementation(files("libs/sentinel-launcher-v1.aar"))
}
```

or

```groovy
dependencies {
    implementation fileTree(include: ['*.aar'], dir: 'libs')
}
```

3. **Sync your project**.

---

### Sharing the URI with SENTINEL/LEGION App

To share the hotlist database file with the SENTINEL/LEGION application, users need to configure a
FileProvider in their application. This allows secure sharing of the file URI between applications.

**Step 1: Add the FileProvider in your AndroidManifest.xml**

```xml

<provider android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider" android:exported="false"
    android:grantUriPermissions="true">
    <meta-data android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

- authorities: This should be set to "${applicationId}.fileprovider", where ${applicationId} is your
  application’s package name.
- exported: Set to false to prevent external applications from directly accessing the FileProvider.
- grantUriPermissions: Ensures that the receiving application (SENTINEL/LEGION) has permission to
  read the file.

**Step 2: Define the File Path in res/xml/file_paths.xml**
Create the file_paths.xml file in your res/xml directory and define the paths where files will be
shared.

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <files-path name="hotlist" path="." />
</resources>
```

- The above configuration shares all files located in the internal storage directory (
  context.getFilesDir()).
- Modify the path attribute to match the directory where your hotlist database file is stored.

**Step 3: Get the URI for the Hotlist File**
You can retrieve the file’s URI using the FileProvider like this:

```kotlin
val hotlistFile = File(context.filesDir, "hotlist.db")
val hotlistUri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", hotlistFile)
```

This URI will then be passed to the SENTINEL/LEGION app for scanning operations.

## API Reference

### 1. **ALPRLauncherBuilder**

`ALPRLauncherBuilder` gathers and validates the data passed to the SENTINEL/LEGION application. This
class is used to configure hotlists, scanner type, and the expected response type.

#### `setHotlistFileAndUri`

This method sets the hotlist file and URI that will be passed to the SENTINEL/LEGION app for
scanning.

```kotlin
alprLauncherBuilder.setHotlistFileAndUri(hotlistFile, hotlistUri)
```

- **Parameters**:
    - `hotlistFile`: Local file path of the SQLite hotlist database (File).
    - `hotlistUri`: URI of the hotlist database.
- **Returns**: `ALPRLauncherBuilder` instance for method chaining.

---

#### `addHotlist`

This method specifies the table in the hotlist database that SENTINEL/LEGION will use for scanning.

```kotlin
alprLauncherBuilder.addHotlist("target_table_name")
```

- **Parameters**:
    - `table`: The name of the table in the hotlist database.
- **Returns**: `ALPRLauncherBuilder` instance for method chaining.

---

#### `setHotlistSourceType`

This method defines which hotlist source to use during scanning.

```kotlin
alprLauncherBuilder.setHotlistSourceType(HotlistSourceType.PASSED_ONLY)
```

- **Enum Values**:
    - `PASSED_ONLY`: Use only the hotlist passed via intent.
    - `LOADED_ONLY`: Use the hotlist loaded in the SENTINEL/LEGION app.
    - `BOTH`: Use both the passed and loaded hotlists.
- **Returns**: `ALPRLauncherBuilder` instance for method chaining.

---

#### `setCameraType`

This method defines the type of camera to be used for scanning.

```kotlin
alprLauncherBuilder.setCameraType(CameraType.EXTERNAL_CAMERA)
```

- **Enum Values**:
    - `DEVICE_CAMERA`: Uses the device’s front or rear camera.
    - `EXTERNAL_CAMERA`: Uses IP cameras defined within the SENTINEL/LEGION app.
    - `USB_CAMERA`: Uses connected USB/UVC cameras.
- **Returns**: `ALPRLauncherBuilder` instance for method chaining.

---

#### `setResponseType`

This method defines what kind of response the SENTINEL/LEGION app should return after scanning.

```kotlin
alprLauncherBuilder.setResponseType(ResponseType.ALL_ALERTS)
```

- **Enum Values**:
    - `ONLY_MANUAL`: Requires manual trigger.
    - `ONLY_WHITELIST`: Returns only whitelisted plates.
    - `ONLY_BLACKLIST`: Returns only blacklisted plates.
    - `ONLY_TIME_BASED`: Returns scan results that are based solely on time-based alert types.
    - `ALL_ALERTS`: Returns all alert types.
    - `ALL_READS`: Returns all scan results.
    - `NO_RESPONSE` Does not return any response data.
- **Returns**: `ALPRLauncherBuilder` instance for method chaining.

---

### 2. **ALPRLauncher**

`ALPRLauncher` is used to start the SENTINEL/LEGION app and receive scan responses. It manages the
launcher lifecycle and handles communication between your app and SENTINEL/LEGION.

#### `initLauncher`

Initializes the launcher and sets up the listener for receiving responses.

```kotlin
alprLauncher.initLauncher(context, launcherListener)
```

- **Parameters**:
    - `context`: The activity or fragment context.
    - `launcherListener`: An implementation of `LauncherListener` to handle responses.
- **Returns**: `ALPRLauncher` instance.

---

#### `start`

Starts the SENTINEL/LEGION application and passes the configured parameters to initiate the scan.

```kotlin
alprLauncher.start(context, alprLauncherBuilder)
```

- **Parameters**:
    - `context`: The activity or fragment context.
    - `launcherBuilder`: The `ALPRLauncherBuilder` instance containing all configurations.

---

#### `stop`

Stops the current scan in the SENTINEL/LEGION app.

```kotlin
alprLauncher.stop(context)
```

- **Parameters**:
    - `context`: The activity or fragment context.

---

#### `destroyLauncher`

Cleans up and destroys the launcher. This should be called in the `onDestroy()` method.

```kotlin
override fun onDestroy() {
    alprLauncher.destroyLauncher(this)
    super.onDestroy()
}
```

- **Parameters**:
    - `context`: The activity or fragment context.

---

## Example Usage

### Starting SENTINEL/LEGION and Receiving Responses

```kotlin
class MainActivity : AppCompatActivity(), LauncherListener {

    private val alprLauncherBuilder = ALPRLauncherBuilder()
    private val alprLauncher = ALPRLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        alprLauncher.initLauncher(this, this)

        val hotlistFile = File(context.filesDir, "hotlist.db")
        val hotlistUri =
            FileProvider.getUriForFile(context, "${this.packageName}.fileprovider", hotlistFile)

        alprLauncherBuilder
            .setCameraType(CameraType.DEVICE_CAMERA)
            .setHotlistSourceType(HotlistSourceType.PASSED_ONLY)
            .setResponseType(ResponseType.ALL_ALERTS)
            .setHotlistFileAndUri(hotlistFile, hotlistUri)
            .addHotlist("license_plate_table")

        alprLauncher.start(this, alprLauncherBuilder)
    }

    override fun onFailure(throwable: Throwable) {
        Log.e("Spyglass ALPR", "Error: ${throwable.message}")
    }

    override fun onSuccess(result: String) {
        Log.d("Spyglass ALPR", "Success: $result")
        alprLauncher.stop(this)
    }

    override fun onCancelled() {
        Log.d("Spyglass ALPR", "Cancelled by user.")
    }

    override fun onDestroy() {
        alprLauncher.destroyLauncher(this)
        super.onDestroy()
    }
}
```

# Hotlist Database Format

## Overview

The hotlist database must follow a specific format for proper integration with the SENTINEL/LEGION
application. Below are the required and optional columns that should be present in the database,
along with the format rules. You can find the **sample database** inside the `assets` folder of the
sample app.

## Required Columns

Your database must include **at least one** of the following columns to ensure correct
functionality:

- **`plate_text`**: This column contains the license plate text to be scanned. It is mandatory
  unless the vehicle attributes (listed below) are present.

  OR

- **`vehicle_color`**, **`vehicle_class`**, and **`vehicle_make`**: If the `plate_text` column is
  not available, all three of these columns must be present together. These fields allow the system
  to identify a vehicle based on its attributes:
    - **`vehicle_color`**: The color of the vehicle (e.g., Red, Blue).
    - **`vehicle_class`**: The class or type of the vehicle (e.g., Sedan, SUV).
    - **`vehicle_make`**: The make or manufacturer of the vehicle (e.g., Toyota, Ford).

## Optional Columns

In addition to the required columns, the database may include the following optional columns for
additional functionality:

- **`description`** (TEXT): A textual description of the vehicle or plate entry. This could be used
  for notes or additional information related to the hotlist item.

- **`hotlist_type`** (TEXT): A categorization or type for the hotlist entry. It can only contain the
  values:
    - `"whitelist"`
    - `"blacklist"`

  Any value other than `"whitelist"` or `"blacklist"` will be considered as `"blacklist"` by the
  system.

- **`hotlist_name`** (TEXT): The name or label for the hotlist. This can be used to differentiate
  between different lists (e.g., "VIP List", "Watchlist").

## Example of a Valid Hotlist Database

The following is an example of how a valid hotlist database structure might look:

| id | plate_text | vehicle_color | vehicle_class | vehicle_make | description       | hotlist_type | hotlist_name |
|----|------------|---------------|---------------|--------------|-------------------|--------------|--------------|
| 1  | ABC123     | Red           | Sedan         | Toyota       | VIP vehicle       | blacklist    | Watchlist    |
| 2  | DEF456     |               |               |              | Frequent visitor  | whitelist    | VIP List     |
| 3  |            | Blue          | SUV           | Ford         | Potential suspect | blacklist    | Watchlist    |

In this example:

- The first row includes both a `plate_text` and vehicle attributes, which is allowed.
- The second row includes only the `plate_text`, which is sufficient by itself.
- The third row omits the `plate_text` but includes all three vehicle
  attributes (`vehicle_color`, `vehicle_class`, and `vehicle_make`), making it valid.

## Important Note:

Ensure that either `plate_text` or the combination of `vehicle_color`, `vehicle_class`,
and `vehicle_make` is always present for each entry. The row will be considered invalid if neither
is provided.

## License

This SDK is proprietary to Spyglass Technologies. For more information, please
contact [support@spyglasstech.co](mailto:support@spyglasstech.co).