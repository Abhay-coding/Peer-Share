# 01 — Repository Baseline and Provenance

## 1. Git Repository State
| Attribute | Verified Value | Evidence Source |
|---|---|---|
| Repository Root | `E:\Peer-Share` | `git rev-parse --show-toplevel` / workspace |
| Remote `origin` | `https://github.com/Zylus08/Peer-Share.git` (fetch & push) | `git remote -v` |
| Remote `upstream` | `https://github.com/Abhay-coding/Peer-Share.git` (fetch & push) | `git remote -v` |
| Active Branch | `main` | `git branch --show-current` |
| HEAD Commit SHA | `230bb5fb750812f7d1f19a3fc5586932f99e58ed` | `git rev-parse HEAD` |
| Latest Commit Date | `2026-11-23 11:37:16 +0530` | `git show -s --format=%ai HEAD` |
| Latest Commit Message | `fix: clarify peer errors and exclude sharing placeholders` | `git show -s --format=%s HEAD` |
| Working Tree Status | Clean before recon (no modified or untracked files) | `git status --short` |
| Local vs `origin/main` | `0` ahead, `0` behind (in sync) | `git rev-list --left-right --count HEAD...origin/main` |
| Local vs `upstream/main` | `0` ahead, `0` behind (in sync) | `git rev-list --left-right --count HEAD...upstream/main` |

## 2. Contributors
| Author Name | Email | Commit Count | Scope Note |
|---|---|---|---|
| `Abhay-coding` | `abhay.kumar.8669@gmail.com` | 8 of 8 | Single recorded Git author across all 8 commits; student/team academic metadata requires manual confirmation. |

## 3. Commit History Summary
1. `aacf33d` (2026-10-18) — `feat: add TCP file download, .part file atomic cleanup, and transfer history`
2. `78ea757` (2026-10-18) — `Finalize UI, docs, scripts & .gitignore updates`
3. `5e9b0bd` (2026-11-23) — `fix: make PeerShare transfers reliable and document the project`
4. `db01d55` (2026-11-23) — `fix: default PeerShare to localhost and limit peer resource use`
5. `14340c9` (2026-11-23) — `fix: harden PeerShare P2P file-sharing (socket leak, TOCTOU, integer overflow, UI thread DB ops, SO_REUSEADDR, symlink defense)`
6. `296da1b` (2026-11-23) — `fix: prevent download overwrites and untrack local artifacts`
7. `d2f1cac` (2026-11-23) — `fix: bound peer requests, support cancellation, and validate transfer folders`
8. `230bb5f` (2026-11-23) — `fix: clarify peer errors and exclude sharing placeholders`

*Note: No merged branches or pull requests exist in local Git refs; all history is linear on `main`.*

## 4. Development & Verification Environment
| Component | Verified Value |
|---|---|
| Operating System | Microsoft Windows NT 10.0.26300.0 (Windows 11) |
| Processor | 12th Gen Intel(R) Core(TM) i7-12650H |
| System Memory | 15.68 GB RAM |
| JDK (`java -version`) | OpenJDK 17.0.17 (Temurin-17.0.17+10, 64-Bit Server VM) |
| Compiler (`javac -version`) | `javac 17.0.17` |
| Target Bytecode (`--release`) | Java 17 (`--release 17` in `compile.ps1` and `compile.sh`) |
