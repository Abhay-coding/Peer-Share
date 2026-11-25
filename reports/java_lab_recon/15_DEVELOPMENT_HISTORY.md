# 15 — Development History and Team Contributions

**Audited Commit:** `230bb5fb750812f7d1f19a3fc5586932f99e58ed`

## 1. Chronological Development Timeline (All 8 Commits on `main`)

| # | Commit | Date (IST) | Author | Milestone / Summary of Changes |
|---|---|---|---|---|
| 1 | `aacf33d` | `2026-10-18 09:20:44` | `Abhay-coding` | **Core Backend & Tests:** Added `AppConfig`, `DatabaseManager`, 3 DAOs, 3 models, `PeerServer`, `ClientHandler`, `FileTransferClient`, `Protocol`, `FileScanner`, `TransferService`, `AppLogger`, `InputValidator`, `sqlite-jdbc.jar`, build/test scripts, and 9 test files. |
| 2 | `78ea757` | `2026-10-18 09:27:49` | `Abhay-coding` | **JavaFX GUI & Scripts:** Added `Launcher`, `MainApp`, `MainView`, `PeersTab`, `SharedFilesTab`, `DownloadTab`, `HistoryTab`, `run.ps1`/`run.sh`, bundled JavaFX JARs, and `EXPLANATION.md`. |
| 3 | `5e9b0bd` | `2026-11-23 11:04:00` | `Abhay-coding` | **Transfer Reliability & Docs:** Improved transfer reliability, added `peershare.css` / `ViewParts` UI polish, updated documentation. |
| 4 | `db01d55` | `2026-11-23 11:10:00` | `Abhay-coding` | **Security Defaults & Resource Limits:** Defaulted `server.bindAddress` to `127.0.0.1` and added peer resource bounds. |
| 5 | `14340c9` | `2026-11-23 11:16:00` | `Abhay-coding` | **Hardening Pass:** Fixed socket leaks, TOCTOU file-size/mtime checks, integer overflow, UI-thread DB ops, `SO_REUSEADDR`, and symlink defenses. |
| 6 | `296da1b` | `2026-11-23 11:22:07` | `Abhay-coding` | **Overwrite Protection & Repo Cleanup:** Prevented download overwrites via `Files.createLink`, added 4 screenshots in `docs/screenshots/`, untracked `out/` `.class` files. |
| 7 | `d2f1cac` | `2026-11-23 11:31:20` | `Abhay-coding` | **Deadlines & Cancellation:** Added `SocketDeadline`, `AtomicBoolean` cancellation, `AppConfig` folder overlap/symlink validation, updated `lib/*.jar` (`javafx 21.0.12`, `sqlite-jdbc 3.54.1.0`). |
| 8 | `230bb5f` | `2026-11-23 11:37:16` | `Abhay-coding` | **UX Polish & Placeholder Exclusion:** Excluded `.gitkeep` from `FileScanner` & `ClientHandler`, added `ListRejectedException`, `HistoryWriteException`, and user-friendly peer error messages. |

## 2. Contributor Attribution & Confirmation Flags
- Only one Git author (`Abhay-coding <abhay.kumar.8669@gmail.com>`) appears in `git log`.
- **Requires Manual Confirmation:** Student full name(s), university roll number(s), team member split (if a group lab project), guide/faculty name, and department/semester metadata.
