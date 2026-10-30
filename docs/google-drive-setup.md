# Google Drive sync setup

Google Drive sync is optional and manual. It stores only the source list as OPML in Drive's private `appDataFolder`; cached articles remain on the device. The app requests the `drive.appdata` scope and does not store OAuth access tokens. Disconnecting revokes access but does not delete the Drive copy.

## Configure OAuth for local testing

1. In Google Cloud Console, create or select a project and enable the Google Drive API.
2. Configure the OAuth consent screen / Google Auth Platform. Add the Google account used for testing as a test user if the consent configuration is in testing mode.
3. Add an **Android** OAuth client for package `dev.spradlev.scholarlyfeed`.
4. Register the SHA-1 signing-certificate fingerprint used by the installed build. For the local debug build, get it from:

   ```sh
   ./gradlew :app:signingReport
   ```

   Use the SHA-1 under the `debug` variant. A release build signed with a different certificate needs its own matching Android OAuth client configuration.
5. Install the debug build on a device or emulator where the test account is available, open **Sources**, and choose **Connect Google Drive and sync**. Grant the requested app-data access.
6. To verify the private copy, sync from another installation using the same account. Files in `appDataFolder` are hidden from the normal Drive file browser; a successful re-read through the app is the expected check.

The app's Android OAuth configuration is associated with its package name and signing certificate. Do not add OAuth client secrets, access tokens, or personal account data to the repository. This document does not contain project-specific client identifiers.

## Sync and conflict behavior

- Sync is user-initiated; local-only use does not require Google sign-in.
- Only source title, HTTPS feed URL, and source level are synchronized. Cached articles and excerpts are not uploaded.
- New sources from Drive are verified before local storage. If any remote-only source fails verification, successful local imports may remain, but the Drive file is not overwritten.
- Matching feed URLs with different titles or levels require an explicit choice. Non-conflicting sources merge. An entry removed from one side is retained from the other side; deletions are not propagated.
- Disconnect revokes access for the app but leaves the private Drive file intact.
