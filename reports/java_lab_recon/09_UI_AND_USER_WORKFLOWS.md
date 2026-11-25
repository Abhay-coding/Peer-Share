# 09 — Graphical User Interface and User Workflows

**Audited Commit:** `230bb5fb750812f7d1f19a3fc5586932f99e58ed`  
**UI Files:** `MainView.java`, `SharedFilesTab.java`, `PeersTab.java`, `DownloadTab.java`, `HistoryTab.java`, `ViewParts.java`, `peershare.css`

## 1. Shell & Navigation Structure (`MainView.java`)
- **Window Dimensions:** Initial `1040×680`, minimum `760×510`, title `"PeerShare"` (`MainApp.java` L76-L81).
- **Layout:** `BorderPane` with a dark navy left sidebar (`-sidebar: #172541`, `188px` wide) containing:
  - Brand header (`SVGPath` bidirectional arrows + `"PeerShare"` label).
  - 4 `ToggleButton`s in a `ToggleGroup`: **Shared Files**, **Peers**, **Download**, **History**.
  - Bottom network status card: `"Sharing on port <port>"` and `"This computer only"` (`127.0.0.1`) or `"Local network"` (`0.0.0.0`).
- **Workspace:** Right `BorderPane` with page title (`32px` bold), subtitle (`secondary-text`), and `StackPane` content area.

## 2. Screen-by-Screen Specification

| Screen | Class | Visible Components | User Actions & Validation | Backend Service / DAO |
|---|---|---|---|---|
| **Shared Files** | `SharedFilesTab` | Folder path card (`Sharing from`), `Search files` field, `Refresh` button, `Open shared folder` button, `TableView<SharedFile>` (`File name`, `Size`, `Last changed`), status label | `Refresh` scans folder; `Open shared folder` calls `Desktop.getDesktop().open(...)`; live search/sort via `ViewParts.searchable`. | `FileScanner.scanAndStore(sharedFileDao)` on `SharedFiles-ScannerTask` daemon thread |
| **Peers** | `PeersTab` | Form (`Add a peer` / `Edit peer`: `Name`, `IP address or hostname`, `Port`), `Add peer` / `Save changes`, `Clear`, `Find a peer` search, `Browse files`, `Remove`, `TableView<Peer>` | `InputValidator.validatePeer` & `parsePort`; row selection populates edit form; double-click or `Enter` triggers `Browse files`; `Remove` shows `ViewParts.confirm` modal `Alert`. | `PeerDao.create`, `update`, `deleteById`, `findAll` on `PeersTab-DatabaseTask` thread |
| **Download** | `DownloadTab` | `ComboBox<Peer>` (`Download from`), `Manage peers` button, `Search files` field, `Refresh files` button, `TableView<SharedFile>`, `Download file` / `Cancel download` (toggled visibility), `Open downloads` button, indeterminate `ProgressBar`, feedback label | Selecting peer or clicking `Refresh files` runs `LIST`; `Download file` (or `Enter` / double-click) downloads file; `Cancel download` sets `AtomicBoolean` to `true`. | `FileTransferClient.listRemoteFiles` (`DownloadTab-ListFiles`), `TransferService.download` (`DownloadTab-Transfer`, non-daemon so transfer finishes cleanly) |
| **History** | `HistoryTab` | `Search downloads` field, `Refresh`, `Open downloads`, `TableView<DownloadHistory>` (`File name`, `From`, `Date`, `Result` styled `Saved` green / `Failed` red), `Remove entry` | `Remove entry` shows `ViewParts.confirm` modal (`"The file stays in your downloads folder."`) then deletes DB row. | `DownloadHistoryDao.findAll`, `deleteById` on `HistoryTab-DatabaseTask` thread |

## 3. Event-Driven Threading Model
All network and SQLite operations run inside `javafx.concurrent.Task<T>` on dedicated worker threads; `setOnSucceeded` and `setOnFailed` callbacks execute automatically on the **JavaFX Application Thread** to update `ObservableList`s, labels, and button states without freezing the UI.
