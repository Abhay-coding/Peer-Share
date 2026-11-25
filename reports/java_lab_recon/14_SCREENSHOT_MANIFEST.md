# 14 — Screenshot and Demonstration Evidence Manifest

**Audited Commit:** `230bb5fb750812f7d1f19a3fc5586932f99e58ed`

## 1. Environment Note on Live GUI Capture
Launching `com.peershare.Launcher` from the automated shell in an isolated temporary directory succeeded at the process level (`HAS_EXITED=False`), but `MainWindowHandle` remained `0` because this shell session does not have an interactive desktop window station. Therefore, live GUI screenshot capture is **BLOCKED** in this automated environment.

However, the repository contains **4 genuine, up-to-date screenshots** in `docs/screenshots/` (committed in `296da1b` on `2026-11-23`), which have been visually inspected and verified to match the current `MainView`, `*Tab`, and `peershare.css` implementation. Copies are placed in `reports/java_lab_recon/screenshots/`.

## 2. Verified Screenshot Inventory

| Fig # | Filename | Source Status | What It Demonstrates | Verified UI Elements & Code Match |
|---|---|---|---|---|
| Fig 1 | `shared-files.png` | Existing repository screenshot | **Shared Files** screen populated with 4 sample files | Dark navy sidebar, `"Sharing on port 63747 / This computer only"`, `Sharing from shared_files`, `Search files`, `Refresh`, `Open shared folder`, 3-column table (`File name`, `Size`, `Last changed`), `"4 files in the shared folder"`. Matches `SharedFilesTab.java` & `MainView.java`. |
| Fig 2 | `peers.png` | Existing repository screenshot | **Peers** screen with saved peers & edit form | `Edit peer` form (`Name`, `IP address or hostname`, `Port`), `Save changes`, `Clear`, `Find a peer`, `Browse files`, `Remove`, 2 saved rows. Matches `PeersTab.java`. |
| Fig 3 | `download.png` | Existing repository screenshot | **Download** screen with remote file list | `Download from` ComboBox (`Studio desktop (127.0.0.1:63748)`), `Manage peers`, `Refresh files`, 4 remote files, `Download file`, `Open downloads`, `"4 files available"`. Matches `DownloadTab.java`. |
| Fig 4 | `history.png` | Existing repository screenshot | **History** screen with `Saved` and `Failed` rows | `Search downloads`, `Refresh`, `Open downloads`, 4-column table (`File name`, `From`, `Date`, `Result` with green `Saved` and red `Failed`), `Remove entry`. Matches `HistoryTab.java` & `peershare.css`. |

## 3. Reproduction Instructions for Additional Live Screenshots (Interactive Desktop)
1. Copy the repo to two temporary folders (`peerA` on `server.port=5000`, `peerB` on `server.port=5001`).
2. Run `.\scripts\run.ps1` in both folders on an interactive Windows desktop.
3. Capture screenshots of: (a) invalid peer port validation error, (b) unreachable peer error message, (c) active download progress bar + `Cancel download` button, (d) two side-by-side PeerShare windows, and (e) PowerShell terminal running `.\scripts\test.ps1`.
