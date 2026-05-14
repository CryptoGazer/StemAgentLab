# Stem Agent Lab

Kotlin Compose Desktop prototype of a controlled **stem-agent specialisation loop**.

This is not a chatbot. It is a small lab where you create evaluation projects, run a baseline agent, ask OpenAI to propose specialised candidates, evaluate those candidates on generated benchmark tasks, and freeze the best configuration by measurable score/cost trade-off.

How it works:

1. Create or select a project — `Python QA`, `SQL Optimizer`, `TypeScript Review`, anything.
2. The app asks OpenAI to generate benchmark tasks for that domain.
3. A baseline agent is evaluated first.
4. A StemAgent asks OpenAI for three candidate configurations.
5. Candidates are evaluated in parallel (concurrency limit: 2).
6. `VersionManager` selects the best by `score² / cost`.
7. The winning config is saved to `projects/<id>/agent.json`. A markdown report goes to `reports/`.

**OpenAI API key is required.** There is no offline mode.

---

## Requirements

| Tool | Version |
|------|---------|
| JDK | 21+ |
| Gradle | via `gradlew` / `gradlew.bat` |
| OpenAI API key | required |

Stack: Kotlin 2.3.10 · Compose Multiplatform Desktop 1.11.0 · JVM toolchain 21 · Ktor CIO · kotlinx.serialization

---

## OpenAI Key Setup

The app loads `OPENAI_API_KEY` from the environment first, then from a `.env` file in the project root. `.env` is gitignored and should never be committed.

**macOS / Linux — one session:**

```bash
export OPENAI_API_KEY=sk-...
./gradlew run
```

**macOS / Linux — persistent `.env`:**

```bash
printf '%s\n' 'OPENAI_API_KEY=sk-...' > .env
chmod 600 .env
./gradlew run
```

**Windows PowerShell — one session:**

```powershell
$env:OPENAI_API_KEY="sk-..."
.\gradlew.bat run
```

**Windows PowerShell — persistent `.env`:**

```powershell
'OPENAI_API_KEY=sk-...' | Out-File -Encoding ascii .env
.\gradlew.bat run
```

**GitHub Actions:** add a repository secret named `OPENAI_API_KEY` under Settings → Secrets and variables → Actions. The workflow passes it to both macOS and Windows jobs.

---

## Run

```bash
./gradlew run          # macOS / Linux
.\gradlew.bat run      # Windows
```

If the key is missing, startup fails immediately with `OPENAI_API_KEY is required`.

---

## Basic Usage

### Create or select a project

Use the project dropdown (top-right) or the **Projects** panel on the left. The default project is `Python QA`.

Click `+ New Project` to create one. Fill in:
- **Name** — display label in the UI
- **Domain** — what OpenAI uses to generate tasks and candidate strategies
- **Description** — local notes, optional

### Edit project details

The **Project Details** panel. Click **Apply** to save. Changing the domain resets metrics and candidates for that project, since the benchmark changes with the domain. Apply does not call OpenAI.

### Run evolution

Click **Run Evolution**. The app will:

1. Generate evaluation tasks for the domain via OpenAI.
2. Evaluate the baseline agent.
3. Ask StemAgent to propose three candidate configs.
4. Evaluate candidates in parallel.
5. Select the best candidate and save the run.

Progress streams to the **Evolution Log** panel.

### Run multiple projects in parallel

Different projects can run simultaneously. Switch between them; logs, metrics, and candidates stay separate per project.

### Inspect the frozen agent

After evolution, the winning config is locked as `projects/<projectId>/agent.json`. The **Frozen Agent** panel shows the score, improvement over baseline, tools, skills, strategy, and the file path. The path is selectable — you can copy it directly from the panel.

The **Output Files** panel (left column) shows both the frozen agent path and the report path once they exist.

### Re-evaluate the frozen agent

**⟳ Eval Frozen** reruns the frozen config on a fresh task set without starting a new evolution cycle. Useful for checking whether the selected configuration holds up beyond the original benchmark tasks. Results appear in the log.

### Export a report

Click **↓ Export Report** after a run. The report is a Markdown file saved to `reports/report-<runId>.md`. During export, one additional OpenAI call generates a short human-readable summary section. The rest of the report (metrics, candidate table, safeguards, log) is assembled from the saved run data without calling the API.

---

## Pipeline

```text
ProjectSpec(name, domain, description)
        ↓
LlmTaskGenerator → OpenAI task JSON
        ↓
BaselineAgent → OpenAI analysis
        ↓
StemAgent → OpenAI candidate AgentConfig JSON
        ↓
Candidate A/B/C evaluation in parallel
        ↓
ScoreCalculator keyword matching
        ↓
VersionManager selects best by score² / cost
        ↓
ProjectStore saves run + agent.json
        ↓
MarkdownReportExporter writes report
```

---

## Local Storage

No database. Local JSON files only:

```text
projects/
├── index.json
├── python-qa/
│   ├── agent.json          ← frozen agent config
│   ├── runs/
│   │   └── <runId>.json
│   └── reports/
│       └── report-<runId>.md
└── sql-review/
    ├── agent.json
    ├── runs/
    └── reports/
```

`projects/` is gitignored.

The project store validates IDs, checks canonical paths against traversal, synchronises writes within one JVM process, and enforces input length limits. Two separate processes writing the same `projects/index.json` simultaneously are not protected — that would require file locking or a database.

---

## Build and Test

```bash
./gradlew test         # macOS / Linux
./gradlew build

.\gradlew.bat test     # Windows
.\gradlew.bat build
```

Tests cover scoring and selection logic, JSON parsing, project storage, candidate evaluation, and prompt hardening. Live OpenAI smoke tests also run when `OPENAI_API_KEY` is set — they hit the real API, so expect a small cost.

---

## Native Packages

**macOS DMG:**

```bash
./gradlew packageDmg
# output: build/compose/binaries/main/dmg/
```

If packaging fails, install Xcode command-line tools first: `xcode-select --install`

**Windows MSI** (run on Windows or in CI):

```powershell
.\gradlew.bat packageMsi
# output: build\compose\binaries\main\msi\
```

**Linux DEB:**

```bash
./gradlew packageDeb
```

For installed DMG/MSI builds, set `OPENAI_API_KEY` as a user-level environment variable rather than relying on `.env` — a double-clicked desktop app may not start in the repository root.

macOS: `launchctl setenv OPENAI_API_KEY "sk-..."` then restart the app.  
Windows: `[Environment]::SetEnvironmentVariable("OPENAI_API_KEY", "sk-...", "User")` then reopen the app.

---

## CI/CD

`.github/workflows/build.yml` runs two jobs:

- **macOS** — verify key, run tests, build DMG, upload artifact
- **Windows** — verify key, run tests, build MSI, upload artifact

---

## Project Structure

```text
src/main/kotlin/com/stemlab/
├── Main.kt
├── app/
│   ├── AppController.kt       orchestration and coroutine jobs
│   └── AppState.kt            UI state, metrics, project view state
├── core/
│   ├── agent/                 BaselineAgent, StemAgent, SpecializedAgent
│   ├── eval/                  PythonQaEvaluator, ScoreCalculator
│   ├── evolution/             EvolutionEngine, VersionManager, StopCriteria
│   ├── model/                 ProjectSpec, AgentConfig, CandidateAgent, EvalTask …
│   └── registry/              ToolRegistry, SkillRegistry
├── llm/
│   ├── LlmClient.kt
│   ├── OpenAiLlmClient.kt
│   └── PromptTemplates.kt
├── report/
│   └── MarkdownReportExporter.kt
├── storage/
│   ├── JsonStorage.kt
│   └── ProjectStore.kt
└── ui/
    ├── StemAgentLabApp.kt
    ├── components/
    └── theme/
```

---

## Troubleshooting

**`OPENAI_API_KEY is required`** — set the key via environment variable or `.env`.

**IDE shows unresolved Kotlin references** — trust Gradle first: `./gradlew compileKotlin`. The IntelliJ Kotlin plugin can lag behind the Gradle Kotlin version and show false-positive errors.

**JVM / SLF4J warnings on startup** — non-fatal. The app starts normally despite these.

**Window closes to tray instead of quitting** — use the tray menu Quit, or stop the Gradle process.

**Stale project data** — use **Reset All** in the UI, or manually: `rm -rf projects` (macOS/Linux) / `Remove-Item -Recurse -Force projects` (Windows PowerShell).
