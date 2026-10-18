# PeerShare — P2P File Sharing Desktop Application

PeerShare is a lightweight, portable Java desktop application designed for students and developers to demonstrate peer-to-peer file sharing over a local network. It requires **no external build tools** (no Maven, no Gradle) and uses core Java APIs alongside SQLite JDBC and JavaFX.

---

## Architecture Overview

```text
JavaFX UI (MainView, Tabs)
    │
    ▼
Service Layer (FileScanner, TransferService)
    ├──▶ Data Access Objects (PeerDao, SharedFileDao, DownloadHistoryDao) ──▶ SQLite (peershare.db)
    ├──▶ TCP Network Layer (PeerServer, ClientHandler, FileTransferClient) ──▶ Remote Peer
    └──▶ Local File System (shared_files/, downloads/)
```

* **No Central Server**: Every instance runs both a background TCP `PeerServer` and a `FileTransferClient`.
* **Local Database**: Each peer maintains an independent local SQLite database (`peershare.db`) for saved peers and download history.
* **Separation of Concerns**: UI components handle user events, services handle business logic and transfers, and DAOs encapsulate all SQL interactions.

---

## Requirements

* **Java Development Kit (JDK)**: Java 17 or later (tested with JDK 21+ / JDK 26).
* **JavaFX SDK & SQLite JDBC**: Pre-packaged in the `lib/` directory:
  * `lib/javafx-base.jar`
  * `lib/javafx-controls.jar`
  * `lib/javafx-graphics.jar`
  * `lib/sqlite-jdbc.jar`
* **Operating System**: Windows, macOS, or Linux.

---

## Quick Start Commands

### 1. Run Automated Unit & Integration Tests

On Windows PowerShell:
```powershell
.\scripts\test.ps1
```

On Linux / macOS:
```sh
chmod +x scripts/*.sh
./scripts/test.sh
```

### 2. Run the JavaFX Application

On Windows PowerShell:
```powershell
.\scripts\run.ps1
```

On Linux / macOS:
```sh
./scripts/run.sh
```

---

## Configuration (`config.properties`)

The application is configured through `config.properties` in the project root:

```properties
server.port=5000
shared.directory=shared_files
downloads.directory=downloads
```

* `server.port`: The TCP port your instance listens on.
* `shared.directory`: Local folder whose files you make available to other peers.
* `downloads.directory`: Local folder where files received from other peers are saved.

---

## Two-Peer Demonstration Guide

### Running Two Peers on the Same Computer

You can simulate two classmates sharing files on a single computer by setting up two instances:

1. Copy the `java_proj` directory to a second folder (e.g., `java_proj_peer2`).
2. In the second folder, open `config.properties` and change the port:
   ```properties
   server.port=5001
   ```
3. Open two separate terminal windows:
   * **Terminal 1 (Peer A, port 5000)**:
     ```powershell
     cd path\to\java_proj
     .\scripts\run.ps1
     ```
   * **Terminal 2 (Peer B, port 5001)**:
     ```powershell
     cd path\to\java_proj_peer2
     .\scripts\run.ps1
     ```
4. Place sample files into `shared_files/` of both folders.
5. In Peer A's **Peers** tab:
   * Name: `Peer B`
   * IP Address: `127.0.0.1`
   * Port: `5001`
   * Click **Add Peer**.
6. Switch to Peer A's **Download** tab, select `Peer B`, click **View Peer Files**, select a file, and click **Download Selected File**.
7. Check Peer A's `downloads/` folder and **History** tab to see the downloaded file and the `SUCCESS` record.

---

### Running Across Two Computers on the Same Wi-Fi

1. Ensure both computers are connected to the same Wi-Fi or local area network (LAN).
2. Find Computer B's private IPv4 address:
   * Windows: Open PowerShell and run `ipconfig` (e.g., `192.168.1.45`).
   * macOS / Linux: Run `ifconfig` or `ip addr`.
3. Ensure the Windows Firewall allows incoming connections on port `5000` (or your configured port).
4. On Computer A, open PeerShare, go to the **Peers** tab, and add:
   * Name: `Computer B`
   * IP Address: `192.168.1.45` (replace with actual IP)
   * Port: `5000`
5. Click **View Peer Files** in the **Download** tab and initiate transfers directly over Wi-Fi.

---

## Manual UI Acceptance Checklist

| Feature | Action to Perform | Expected Result |
|---|---|---|
| **Add Peer** | Fill Name, IP, Port, and click **Add Peer**. | Peer appears in the table; input fields clear. |
| **Input Validation** | Enter invalid port (`70000` or `abc`) or leave fields blank. | Warning alert appears with clear explanation; nothing added. |
| **Update Peer** | Select a peer, change the name or port, click **Update Selected**. | Table row reflects updated information in SQLite. |
| **Delete Peer** | Select a peer, click **Delete Selected**. | Peer is removed from the table and database. |
| **Scan Shared Files** | Place files in `shared_files/`, click **Refresh Files**. | Table lists all files with sizes and modification times; nested subfolders are ignored. |
| **List Remote Files** | Select a running peer in **Download** tab, click **View Peer Files**. | Remote files table populates with files from that peer's `shared_files/`. |
| **Download File** | Select a remote file, click **Download Selected File**. | Progress indicator runs; file appears in `downloads/`; success alert displays. |
| **Download History** | Open **History** tab. | Completed download shows green `SUCCESS` badge with timestamp. |
| **Unavailable Peer** | Add peer with an offline port and click **View Peer Files**. | Error alert surfaces ("Could not retrieve files from peer..."); UI remains completely responsive. |
| **Failed History** | Check **History** tab after an unavailable/aborted download. | Failed attempt shows red `FAILED` badge. |

---

## Academic Syllabus & Concepts Mapping

This table maps academic Java syllabus concepts directly to the corresponding classes and implementations in PeerShare:

| Concept / Syllabus Topic | Class / Implementation in PeerShare | Demonstration Details |
|---|---|---|
| **Object-Oriented Programming (OOP)** | `Peer`, `SharedFile`, `DownloadHistory` | Immutable models (records), encapsulation, clear separation between UI, Services, and DAOs. |
| **Collections Framework** | `PeersTab`, `FileScanner`, `SharedFileDao` | `List<T>`, `ArrayList`, `ObservableList`, sorting via `Comparator.comparing()`. |
| **Exception Handling** | `TransferService`, `InputValidator`, `ClientHandler` | Try-with-resources, checked exceptions (`IOException`, `SQLException`), custom validation, atomic cleanup. |
| **Java NIO File I/O** | `FileScanner`, `TransferService`, `AppConfig` | `Path`, `Files.list()`, `Files.size()`, `Files.move()` with `ATOMIC_MOVE`, `.part` temp file management. |
| **Properties & Configuration** | `AppConfig` | `java.util.Properties` loading from `config.properties` for cross-platform portability. |
| **Logging** | `AppLogger`, `PeerServer`, `TransferService` | Built-in `java.util.logging.Logger` logging technical events with console formatting. |
| **JDBC & SQLite Database** | `DatabaseManager`, `PeerDao`, `DownloadHistoryDao` | `DriverManager`, `PreparedStatement` preventing SQL injection, transactions (`setAutoCommit(false)`), auto-generated keys. |
| **DAO Pattern** | `dao` package | Full CRUD abstraction isolating SQL queries from application logic. |
| **TCP Sockets & Networking** | `PeerServer`, `FileTransferClient`, `Protocol` | `ServerSocket`, `Socket`, connect/read timeouts, custom line-based protocol (`LIST`, `DOWNLOAD`, `OK`, `ERROR`). |
| **Multithreading & Concurrency** | `PeerServer`, `ClientHandler`, JavaFX `Task` | Server accept loop thread, client handler per socket, background `javafx.concurrent.Task` keeping GUI responsive. |
| **JavaFX Desktop GUI** | `MainApp`, `MainView`, UI Tabs | `Stage`, `Scene`, `TabPane`, `TableView`, `ComboBox`, `Alert`, dynamic binding, custom cell rendering. |
