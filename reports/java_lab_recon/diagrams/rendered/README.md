# Rendered Diagram Assets Status

- **Status:** **BLOCKED (Local CLI Renderers Unavailable)**
- **Checked Tools:** `mmdc` (Mermaid CLI): not installed; `dot` (Graphviz binary): not installed; `plantuml`: not installed.
- **Editable Verified Sources:** All 16 validated Mermaid (`.mmd`) diagram sources are located in `../` (`01_system_architecture.mmd` through `16_deployment_two_peers.mmd`).
- **How to Render on Any Machine with Node.js / Mermaid CLI:**
  ```powershell
  npx -y @mermaid-js/mermaid-cli -i 01_system_architecture.mmd -o rendered/01_system_architecture.svg -b white
  npx -y @mermaid-js/mermaid-cli -i 01_system_architecture.mmd -o rendered/01_system_architecture.png -b white -s 3
  ```
