# UI Fixes & Google Sign-In Integration Plan

Address visibility issues on the main screen and integrate Google Sign-In using the modern Credential Manager API.

## User Review Required

> [!IMPORTANT]
> - **Google Sign-In Requirements**: For a real production app, a `web_client_id` from the Google Cloud Console is required. I will use a placeholder ID in the code, which you will need to replace for real authentication.
> - **Account Picker**: The implementation will trigger the system account picker to show all Google accounts on the device.

## Proposed Changes

### 1. UI Visibility Fixes
- **[MODIFY] [activity_main.xml](file:///C:/Users/pc/Downloads/New folder (9)/MigMigParking/app/src/main/res/layout/activity_main.xml)**:
    - Change "IM DRIVER" card to use a vibrant Primary color background with White text for maximum contrast against the dark gradient.
    - Adjust the gradient or card margins to ensure better depth.
- **[MODIFY] [colors.xml](file:///C:/Users/pc/Downloads/New folder (9)/MigMigParking/app/src/main/res/values/colors.xml)**: Refine the `primary` and `secondary` colors for better visual separation.

### 2. Google Sign-In Integration
- **[MODIFY] [libs.versions.toml](file:///C:/Users/pc/Downloads/New folder (9)/MigMigParking/gradle/libs.versions.toml)**: Add dependencies for `androidx.credentials` and `googleid`.
- **[MODIFY] [build.gradle.kts](file:///C:/Users/pc/Downloads/New folder (9)/MigMigParking/app/build.gradle.kts)**: Sync the new dependencies.
- **[MODIFY] [activity_main.xml](file:///C:/Users/pc/Downloads/New folder (9)/MigMigParking/app/src/main/res/layout/activity_main.xml)**: Add a "Continue with Google" button asset and UI element.
- **[MODIFY] [MainActivity.kt](file:///C:/Users/pc/Downloads/New folder (9)/MigMigParking/app/src/main/java/com/example/migmigparking/MainActivity.kt)**:
    - Implement `CredentialManager` flow.
    - Add logic to show the account picker and handle the selection.

### 3. Polish
- **[NEW] ic_google.xml**: Vector icon for the Google button.

## Verification Plan

### Automated Tests
- Build project (`:app:assembleDebug`).

### Manual Verification
- Confirm "IM DRIVER" is now clearly visible and bright.
- Click "Continue with Google" and verify that the system account selector appears.
