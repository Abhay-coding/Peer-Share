# 02 — Project Overview and Problem Definition

## 1. Project Identity
- **Official Project Name:** PeerShare (`SOURCE VERIFIED` — `README.md`, `MainApp.java`, `MainView.java`)
- **Suggested Academic Report Title:** *PeerShare: A Symmetric Peer-to-Peer Local Area Network File Sharing Desktop Application in Java*
- **Subtitle:** *Design and Implementation of a Multithreaded TCP Socket Transfer Engine with SQLite Metadata Persistence and JavaFX Graphical User Interface*

## 2. Problem Statement & Motivation
In classroom, laboratory, and small-team environments, sharing files between nearby computers commonly relies on either physical USB drives (prone to physical loss and cross-OS formatting issues) or cloud-based storage services (Google Drive, Dropbox, email attachments), which require internet connectivity, user accounts, and round-trip uploads/downloads through external servers.

PeerShare addresses this gap by enabling **direct, account-free, offline-capable file transfer** between two instances of the same desktop application over loopback (`127.0.0.1`) or a trusted local network (`0.0.0.0`).

## 3. Existing Approaches vs. PeerShare
| Approach | Mechanism | Limitations Addressed by PeerShare |
|---|---|---|
| Cloud Storage / Email | Upload to remote internet server, then download | Requires internet, accounts, and external server storage; unnecessary latency for LAN peers. |
| USB Flash Drives | Physical sneakernet copy | Requires physical proximity and manual media handling. |
| OS Network Shares (SMB) | OS-level folder sharing | Requires OS-specific permissions/credentials and varies across Windows/macOS/Linux. |
| **PeerShare** | Direct TCP socket transfer between symmetric desktop instances | Works offline on LAN/loopback, requires no central server or user accounts, uses a single cross-platform Java/JavaFX codebase. |

*Scope Note:* PeerShare does not claim superiority over cloud storage for internet-wide, encrypted, or multi-device-sync use cases; it specifically targets **trusted local-network ad-hoc file sharing**.

## 4. Architecture Classification: Symmetric Peer-to-Peer (Client-Server per Node)
- **SOURCE VERIFIED:** PeerShare is **NOT** a centralized server architecture, nor is it a fully decentralized DHT/mesh P2P network with automatic peer discovery.
- Instead, each PeerShare instance is a **symmetric peer** combining a local **TCP Server** (`PeerServer` + `ClientHandler`) and a **TCP Client** (`FileTransferClient` + `TransferService`). Peers are registered manually by IP/hostname and port, and every transfer is a direct point-to-point client-server TCP exchange between two peers.

## 5. Objectives
- **Primary Objective:** Build a reliable, user-friendly Java desktop application for direct file sharing over TCP sockets without a central server.
- **Secondary Objectives:**
  1. Sandbox shared and downloaded files to dedicated, non-overlapping project subfolders (`shared_files/`, `downloads/`).
  2. Persist peer contacts, local shared-file metadata, and download history in an embedded SQLite database (`peershare.db`).
  3. Keep the JavaFX UI responsive during network and database I/O via background `javafx.concurrent.Task` execution.
  4. Protect transfers against path traversal, partial-file corruption, destination overwrites, and resource exhaustion.

## 6. Target Users & Intended Usage Scenarios
- **Target Users:** Students, lab partners, instructors, and small teams on a shared Wi-Fi or wired LAN (or single-machine testing via loopback).
- **Usage Scenarios:** Sharing lab notes, datasets, PDFs, or project artifacts (up to 100 MB per file, up to 500 files) between two laptops in a classroom without internet access.

## 7. Functional & Non-Functional Requirements
**Functional Requirements (SOURCE VERIFIED):**
- FR1: Configure server port, bind address, and folder paths via `config.properties`.
- FR2: Scan and display local shareable regular files (`shared_files/`), excluding `.gitkeep`, subdirectories, and symlinks.
- FR3: Add, edit, search, and remove saved remote peers (`name`, `ip_address`, `port`).
- FR4: Query a remote peer's shared file list (`LIST`) and search/sort results.
- FR5: Download a selected remote file (`DOWNLOAD <filename>`) with SHA-256 verification, duplicate protection, and cancellation support.
- FR6: Record and view `SUCCESS` / `FAILED` download history and remove history entries without deleting downloaded files.

**Non-Functional Requirements (SOURCE VERIFIED):**
- **Reliability:** Atomic `.part` temp files, SHA-256 checksums, hard-link publication (`Files.createLink`), socket deadlines.
- **Usability:** 4-tab JavaFX UI, live search/sort, non-blocking background tasks, human-readable sizes/dates.
- **Portability:** Java 17 + NIO `Path` + relative config paths; Windows scripts included, macOS/Linux `.sh` scripts included (requiring OS-matching JavaFX JARs).
- **Resource Bounds:** Max 100 MB file size, max 500 listed files, max 10 concurrent server clients, 4096-byte protocol line limit.

## 8. Academic Abstract (approx. 230 words)
> **PeerShare** is a cross-platform desktop file-sharing application developed in Java 17 that enables direct, peer-to-peer file exchange over local area networks without requiring a central server, user accounts, or internet connectivity. Conventional file-sharing methods such as cloud storage platforms introduce external server dependencies and internet requirements, while physical media are inconvenient for rapid classroom or laboratory collaboration. PeerShare addresses these limitations by embedding both a multithreaded TCP server and a TCP client within every running instance, allowing any node to simultaneously serve local files and download files from registered peers.
>
> The application is structured into modular layers encompassing configuration management, JDBC/SQLite persistence via the Data Access Object (DAO) pattern, NIO-based filesystem services, a custom text-and-binary TCP application protocol, and an event-driven JavaFX graphical user interface. To ensure transfer reliability and defensive operation, PeerShare enforces strict filename and path-traversal validation, bounds concurrent client connections and file sizes, verifies downloaded payloads using SHA-256 checksums, and publishes completed downloads atomically from temporary `.part` files using filesystem hard links to prevent overwriting existing files. All network and database operations execute on background worker threads via `javafx.concurrent.Task`, preserving UI responsiveness. Verified through automated unit and integration tests as well as two-instance loopback demonstrations, PeerShare demonstrates practical application of Java networking, concurrency, file I/O, database connectivity, and object-oriented design.

## 9. Concise Project Synopsis
PeerShare is a Java 17 + JavaFX 21 desktop application for local-network peer-to-peer file sharing. Each instance runs a background TCP `PeerServer` serving files from `shared_files/` and a `FileTransferClient` for browsing and downloading files from manually registered peers into `downloads/`. Peer contacts, shared-file metadata, and transfer history are persisted locally in SQLite (`peershare.db`) via JDBC DAOs, with SHA-256 transfer verification and non-blocking background UI tasks.
