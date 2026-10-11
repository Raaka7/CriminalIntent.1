# CriminalIntent

Kotlin implementation of the Big Nerd Ranch CriminalIntent app: Fragments + Navigation,
RecyclerView, Room, ViewModels with StateFlow, camera photos, and implicit intents.

## Opening and running
1. Android Studio → **File › Open** → select this `CriminalIntent` folder.
2. Let Gradle sync (needs internet the first time; it downloads Gradle 8.7, AGP 8.5.2, Room, etc.).
3. Run on an emulator or device with API 24+.

## Where each requirement lives

| # | Requirement | Code |
|---|---|---|
| 1 | Scrollable list (title + date) | `CrimeListFragment`, `CrimeListAdapter`, `list_item_crime.xml` |
| 2 | Tap → detail screen | `nav_graph.xml` (`show_crime_detail`, Safe Args `crimeId`), `CrimeDetailFragment` |
| 3 | Add / delete | App-bar **+** and empty-state button (`CrimeListFragment.showNewCrime`); trash icon on detail (`CrimeDetailFragment.deleteCrime`) |
| 4 | Persists across restart | `database/` (Room entity, DAO, type converters), `CrimeRepository` |
| 5 | Solved + visual in list | Checkbox on detail; green check badge in `CrimeHolder.bind` |
| 6 | Date picker | `DatePickerFragment` (dialog destination), result via Fragment Result API |
| 7 | Camera photo | `TakePicture` contract + `FileProvider` (`res/xml/files.xml`), `PictureUtils.getScaledBitmap` |
| 8 | Implicit intents | Pick suspect from Contacts (`PickContact`), send report (`ACTION_SEND` chooser), camera (`IMAGE_CAPTURE`) |
| 9 | No crash on rotation | State held in ViewModels; pending photo name saved in `onSaveInstanceState`; dialog is a fragment; double-tap navigation guarded |

## Design notes
- **Single activity** (`MainActivity`) hosting fragments through a `NavHostFragment`.
- **Repository pattern**: fragments → ViewModels → `CrimeRepository` → Room DAO.
- The list observes a Room `Flow`, so it updates automatically when crimes change.
- Edits on the detail screen live in `CrimeDetailViewModel` and are saved once in `onCleared()`
  (when leaving the screen), so rotation never loses or double-writes data.
- Leaving a crime with a blank title is blocked with a toast (Back and Up both go through the same check).
- No `CAMERA` or `READ_CONTACTS` permissions are needed: the camera and contacts apps do the work
  and grant temporary access. `<queries>` in the manifest lets Android 11+ check those apps exist.
- Photos are downsampled to the view size and rotated upright using EXIF data.

## Testing checklist
- Add a crime, give it a title, rotate the device mid-edit, go back — title is kept.
- Mark solved → check badge appears in the list.
- Change the date, rotate while the picker is open — picker stays open.
- Take a photo (emulator camera works), rotate — photo stays.
- Choose a suspect, send report — suspect name appears in the report text.
- Delete a crime; force-stop and relaunch the app — remaining crimes are still there.
