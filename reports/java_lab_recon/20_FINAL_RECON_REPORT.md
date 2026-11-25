# 20 — Final Reconnaissance Report (PeerShare Milestone R0)

**Repository:** `https://github.com/Zylus08/Peer-Share.git`  
**Branch / Commit:** `main` @ `230bb5fb750812f7d1f19a3fc5586932f99e58ed` (`2026-11-23 11:37:16 +0530`)  
**Audit Environment:** Windows 11 (`10.0.26300.0`), OpenJDK `17.0.17` (`javac 17.0.17`), 12th Gen Intel i7-12650H, 15.68 GB RAM  

## 1. Consolidated Findings
PeerShare is a complete, well-structured **Java 17 + JavaFX 21.0.12 + SQLite JDBC 3.54.1.0** desktop application implementing **symmetric peer-to-peer local-network file sharing** over raw TCP sockets. Every node embeds both a multithreaded `PeerServer` (bounded by `Semaphore(10)`, `MAX_FILE_BYTES = 100 MiB`, `MAX_LIST_FILES = 500`) and a `FileTransferClient` with a custom `\n`-framed UTF-8 + binary protocol, SHA-256 integrity verification, `SocketDeadline` timeouts/cancellation, and atomic `.part` → `Files.createLink` file publication.

## 2. Key Metrics (`metrics.json`)
- **Production:** 25 `.java` files, 9 packages, 22 classes + 3 `record`s, 135 declared methods, `2,131` SLOC (+ `109` CSS lines).
- **Tests:** 9 `.java` files, 1 package, 9 classes, 52 declared methods, `877` SLOC, 37 audited test methods.
- **Database:** 3 SQLite tables (`peer`, `shared_file`, `download_history`) in `peershare.db`.

## 3. Verification Summary
- **Build (`.\scripts\compile.ps1`):** **PASS** (exit code `0`).
- **Automated Tests (`.\scripts\test.ps1`):** **PARTIAL FAIL** — `NetworkTransferTest.testStopClosesConnectedClient` throws `java.net.SocketException: Connection reset` on Windows JDK 17 when `PeerServer.stop()` closes an idle accepted socket; **36/37** individual test methods pass.
- **Functional Demo:** **PASS** — Verified 2-instance loopback `LIST`, text & binary `DOWNLOAD` with 100% SHA-256 match, duplicate rejection, unreachable peer handling, and SQLite persistence across DB re-open.
- **Screenshots:** 4 genuine repository screenshots verified; live GUI capture in background shell blocked (`MainWindowHandle=0`).

## 4. Index of Generated Deliverables (`reports/java_lab_recon/`)
- Reports: `00_EXECUTIVE_SUMMARY.md` through `20_FINAL_RECON_REPORT.md`
- Diagrams: `diagrams/` (16 Mermaid `.mmd` sources + `README.md`)
- Database: `database/schema.sql`, `database/data_dictionary.md`
- Code Excerpts: `code_excerpts/` (12 verified `.java` excerpts)
- Logs: `logs/compile.log`, `logs/test_execution.log`, `logs/test_execution_2.log`, `logs/recon_audit.log`
- Screenshots: `screenshots/` (4 verified `.png` files)
- Metrics: `metrics.json`

**Final Verdict:** **REPORT READY WITH DOCUMENTED LIMITATIONS**
