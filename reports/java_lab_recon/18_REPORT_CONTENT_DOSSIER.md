# 18 — Academic Report Content Dossier (Chapter-Wise)

**Audited Commit:** `230bb5fb750812f7d1f19a3fc5586932f99e58ed`

| Chapter | Key Factual Content & Evidence | Recommended Diagrams / Tables / Excerpts |
|---|---|---|
| **1. Abstract** | 230-word abstract from `02_PROJECT_OVERVIEW.md` §8 | — |
| **2. Introduction** | Symmetric P2P LAN file sharing in Java 17 + JavaFX 21 + SQLite | `02_PROJECT_OVERVIEW.md` |
| **3. Problem Statement** | Cloud internet/account dependency & USB inconvenience | Comparison table (`02_PROJECT_OVERVIEW.md` §3) |
| **4. Objectives** | Primary & 4 secondary objectives (`02_PROJECT_OVERVIEW.md` §5) | Bulleted objective list |
| **5. Existing & Proposed System** | Cloud/USB/SMB vs. PeerShare symmetric TCP nodes | Architecture diagram (`01_system_architecture.mmd`) |
| **6. Requirement Analysis** | FR1–FR6, NFRs, JDK 17+, Windows/POSIX hardware & software reqs | Requirements table (`02_PROJECT_OVERVIEW.md` §7) |
| **7. Technology Stack** | Java 17 (`--release 17`), JavaFX `21.0.12+2`, `sqlite-jdbc 3.54.1.0`, NIO, JDBC, TCP | Dependency table (`03_TECHNOLOGY_STACK.md`) |
| **8. System Architecture** | Layered desktop architecture + constructor DI (`MainApp`) | `01_system_architecture.mmd`, `05_package_dependencies.mmd` |
| **9. System Design & UML** | 16 UML/DFD/deployment diagrams in `diagrams/` | All `.mmd` files in `diagrams/` |
| **10. Database Design** | 3 SQLite tables (`peer`, `shared_file`, `download_history`), 3NF analysis | `schema.sql`, `13_er_diagram.mmd`, `data_dictionary.md` |
| **11. Module Description** | 9 packages, 25 production classes/records (`11_CLASS_AND_METHOD_INVENTORY.md`) | Class inventory table |
| **12. Java Concepts Applied** | 23 verified concepts + 9 not-present (`05_JAVA_CONCEPT_EVIDENCE_MATRIX.md`) | Concept matrix + `code_excerpts/` |
| **13. Algorithms & Implementation** | 7 core algorithms + custom TCP protocol (`06` & `07`) | Protocol table + pseudocode |
| **14. Graphical User Interface** | `MainView` + 4 tabs + `peershare.css` (`09_UI_AND_USER_WORKFLOWS.md`) | 4 screenshots (`screenshots/`) |
| **15. Testing & Validation** | `AllTests` (36/37 pass; Windows RST note) + 10 functional demos | Test matrix (`13_TESTING_AND_RESULTS.md`) |
| **16. Results & Discussion** | Verified SHA-256 transfers, duplicate protection, DB persistence | `recon_audit.log` |
| **17. Security & Reliability** | Path traversal, symlinks, TOCTOU, SHA-256, `Semaphore(10)`, no TLS/auth | Security table (`10_SECURITY_AND_RELIABILITY.md`) |
| **18. Limitations** | 9 verified limitations (`16_LIMITATIONS_AND_FUTURE_SCOPE.md` §1) | Limitations table |
| **19. Future Scope** | TLS, auth, mDNS, transfer queue, byte-range resume, Maven/Gradle | Roadmap table |
| **20. Learning Outcomes** | 7 verified academic outcomes (`16_LIMITATIONS_AND_FUTURE_SCOPE.md` §2) | Outcome summary |
| **21. Conclusion** | Summary of achievements and fitness for LAN academic use | — |
| **22. References** | Official Oracle Java 17 docs, OpenJFX 21 docs, SQLite & Xerial JDBC docs, RFC 9293 (TCP) | Authoritative URLs below |
| **23. Appendices** | `schema.sql`, `code_excerpts/`, `logs/` | — |

## Authoritative References (Verified Official Sources)
- Oracle Java SE 17 Documentation: https://docs.oracle.com/en/java/javase/17/docs/api/
- OpenJFX / JavaFX 21 API Documentation: https://openjfx.io/javadoc/21/
- SQLite Documentation: https://www.sqlite.org/docs.html
- Xerial SQLite JDBC Driver: https://github.com/xerial/sqlite-jdbc
- RFC 9293 — Transmission Control Protocol (TCP): https://www.rfc-editor.org/rfc/rfc9293

## Claims Requiring Manual Confirmation
- Student/author full names, roll numbers, faculty guide, institution, and semester.
- Interactive desktop screenshots for active download progress bar and error alerts.
