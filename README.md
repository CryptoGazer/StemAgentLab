# Stem Agent Lab

Stem Agent Lab is a Kotlin Compose Desktop prototype for a controlled **stem-agent specialisation loop**.

The application is not a chatbot. It is a small lab UI where you create evaluation projects, run a baseline agent, ask OpenAI to propose specialised candidate agents, evaluate those candidates on generated benchmark tasks, and freeze the best configuration by measurable score/cost trade-off.

In practical terms:

1. You create or select a project, such as `Python QA`, `SQL Optimizer`, or `TypeScript Review`.
2. The app asks OpenAI to generate benchmark tasks for that project domain.
3. A baseline agent is evaluated first.
4. A StemAgent asks OpenAI for three candidate configurations.
5. Candidate agents are evaluated in parallel with a concurrency limit.
6. `VersionManager` selects the best candidate by `score² / cost`.
7. The UI shows logs, metrics, selected tools, candidates, and exported reports.

The current version is **OpenAI-backed only**. There is no no-key mock runtime mode.

---

## Requirements

| Tool | Required |
|------|----------|
| JDK | 21+ recommended |
| Gradle | Provided by `gradlew` / `gradlew.bat` |
| OpenAI API key | Required |
| Python 3 | Optional; only relevant for future live tool execution |

The project uses:

- Kotlin `2.3.10`
- Compose Multiplatform Desktop `1.11.0`
- JVM toolchain `21`
- Ktor CIO client for OpenAI HTTP calls
- kotlinx.serialization for local JSON storage

---

## OpenAI Key Setup

The app always requires `OPENAI_API_KEY`.

It loads the key in this order:

1. Environment variable `OPENAI_API_KEY`
2. Local `.env` file in the project root

`.env` is gitignored and should never be committed.

### macOS / Linux

One terminal session:

```bash
export OPENAI_API_KEY=sk-...
./gradlew run
```

Persistent local dev setup:

```bash
printf '%s\n' 'OPENAI_API_KEY=sk-...' > .env
chmod 600 .env
./gradlew run
```

### Windows PowerShell

One PowerShell session:

```powershell
$env:OPENAI_API_KEY="sk-..."
.\gradlew.bat run
```

Persistent local dev setup:

```powershell
'OPENAI_API_KEY=sk-...' | Out-File -Encoding ascii .env
.\gradlew.bat run
```

### GitHub Actions

CI expects a repository secret named:

```text
OPENAI_API_KEY
```

Set it in GitHub:

```text
Repository Settings -> Secrets and variables -> Actions -> New repository secret
```

The workflow passes this secret to macOS and Windows jobs.

---

## Install After Downloading From GitHub

There are two supported ways to use the app on another machine.

### Option A: Run From Source

Use this when the other person is a developer or is comfortable with a terminal.

1. Install JDK 21.
2. Clone or download the repository.
3. Open a terminal in the repository root.
4. Configure `OPENAI_API_KEY`.
5. Run the app with the Gradle wrapper.

macOS / Linux:

```bash
git clone <repo-url>
cd StemAgentLab
printf '%s\n' 'OPENAI_API_KEY=sk-...' > .env
chmod 600 .env
./gradlew run
```

Windows PowerShell:

```powershell
git clone <repo-url>
cd StemAgentLab
'OPENAI_API_KEY=sk-...' | Out-File -Encoding ascii .env
.\gradlew.bat run
```

For this source-based workflow, `.env` is the easiest local setup because the working directory is the repository root.

### Option B: Install A Native Package

Use this when you want a normal desktop app install.

Build the package first, or download it from GitHub Actions artifacts if CI has produced one.

macOS DMG:

```bash
./gradlew packageDmg
```

Windows MSI:

```powershell
.\gradlew.bat packageMsi
```

Package outputs:

```text
build/compose/binaries/main/dmg/
build\compose\binaries\main\msi\
```

For installed DMG/MSI apps, prefer a user-level environment variable instead of `.env`. A double-clicked desktop app may not start with the repository root as its working directory, so a project-root `.env` is mainly for `./gradlew run`.

macOS:

```bash
launchctl setenv OPENAI_API_KEY "sk-..."
```

Then restart the app. If the variable is not visible to GUI apps yet, log out and log back in.

Windows PowerShell:

```powershell
[Environment]::SetEnvironmentVariable("OPENAI_API_KEY", "sk-...", "User")
```

Then close and reopen the app or terminal.

---

## Run The App

### macOS / Linux

```bash
./gradlew run
```

### Windows

```powershell
.\gradlew.bat run
```

If the key is missing, startup fails with a clear `OPENAI_API_KEY is required` error.

---

## Basic Usage

### 1. Create or select a project

Use either:

- the project dropdown in the top-right corner;
- the `Projects` panel on the left.

The default project is `Python QA`.

Click `+ New Project` to create another project. A project has:

- `Project name`: display name in the UI;
- `Domain`: what OpenAI uses to generate tasks and candidate strategies;
- `Description`: local notes.

### 2. Edit project details

Use the `Project Details` panel.

Click `Apply` to save name/domain/description. This updates `projects/index.json` and resets old metrics/candidates for that project, because a changed domain means a new benchmark context.

`Apply` does not call OpenAI. OpenAI is called only when you run the evolution loop.

### 3. Run evolution

Click `Run Evolution`.

The app will:

1. Generate evaluation tasks for the active project domain.
2. Run the baseline agent.
3. Ask StemAgent to propose three candidates.
4. Evaluate candidates in parallel.
5. Select the best candidate.
6. Save a run JSON file.

Logs appear in the `Evolution Log` panel. The log panel auto-scrolls and has a visible scrollbar.

### 4. Run projects in parallel

Different projects can run at the same time.

Example:

1. Start `Python QA`.
2. Switch to `SQL Review`.
3. Start `SQL Review`.
4. Switch between projects and verify that logs, candidates, metrics, and selected tools stay separate.

The same project cannot be started twice at the same time.

### 5. Export a report

After a run finishes, click `Export Report`.

Reports are saved under:

```text
projects/<projectId>/reports/report-<runId>.md
```

---

## Current Pipeline

```text
ProjectSpec(name, domain, description)
        ↓
LlmTaskGenerator -> OpenAI task JSON
        ↓
BaselineAgent -> OpenAI analysis
        ↓
StemAgent -> OpenAI candidate AgentConfig JSON
        ↓
Candidate A/B/C evaluation in parallel
        ↓
ScoreCalculator keyword matching
        ↓
VersionManager selects best by score² / cost
        ↓
ProjectStore saves run
        ↓
MarkdownReportExporter writes report
```

Candidate evaluation is bounded by:

```kotlin
Budget(maxParallelCandidates = 2)
```

OpenAI requests retry on HTTP `429` and transient `5xx` responses. If OpenAI sends `Retry-After`, the client waits accordingly.

---

## Local Storage

This project does **not** use SQLite or a database.

It uses local JSON files:

```text
projects/
├── index.json
├── python-qa/
│   ├── runs/
│   │   └── <runId>.json
│   └── reports/
│       └── report-<runId>.md
└── sql-review/
    ├── runs/
    └── reports/
```

`projects/` is gitignored.

Storage hardening currently includes:

- safe project id validation;
- safe run id validation;
- canonical path checks against path traversal;
- synchronized project store operations inside one JVM process;
- input length limits for project name/domain/description.

Note: two separate app processes writing the same `projects/index.json` at the same time are not fully protected. That would require file locking or SQLite.

---

## Build And Test

### macOS / Linux

```bash
./gradlew test
./gradlew build
```

### Windows PowerShell

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

Tests include:

- scoring and selection logic;
- parser behavior;
- project storage;
- project creation/update flows;
- bounded parallel candidate evaluation;
- prompt-domain hardening;
- live OpenAI smoke tests.

Because live OpenAI tests call the API, `OPENAI_API_KEY` must be configured before running the full test suite.

---

## Native Packages

### macOS DMG

```bash
./gradlew packageDmg
```

The DMG appears under:

```text
build/compose/binaries/main/dmg/
```

If packaging fails because macOS tooling is missing, install Xcode command-line tools:

```bash
xcode-select --install
```

### Windows MSI

Run on Windows or in GitHub Actions:

```powershell
.\gradlew.bat packageMsi
```

The MSI appears under:

```text
build\compose\binaries\main\msi\
```

The repository includes a GitHub Actions Windows job that runs tests and builds the MSI on `windows-latest`.

### Linux DEB

```bash
./gradlew packageDeb
```

---

## CI/CD

GitHub Actions workflow:

```text
.github/workflows/build.yml
```

Current jobs:

- macOS:
  - verify `OPENAI_API_KEY`;
  - run `./gradlew test`;
  - build DMG;
  - upload DMG artifact.
- Windows:
  - verify `OPENAI_API_KEY`;
  - run `.\gradlew.bat test`;
  - build MSI;
  - upload MSI artifact.

---

## Project Structure

```text
src/main/kotlin/com/stemlab/
├── Main.kt
├── app/
│   ├── AppController.kt       # project orchestration and running jobs
│   └── AppState.kt            # UI state, project view state, metrics
├── core/
│   ├── agent/                 # BaselineAgent, StemAgent, SpecializedAgent
│   ├── eval/                  # Evaluator, PythonQaEvaluator, ScoreCalculator
│   ├── evolution/             # EvolutionEngine, VersionManager, StopCriteria
│   ├── model/                 # ProjectSpec, AgentConfig, CandidateAgent, EvalTask, ...
│   └── registry/              # ToolRegistry, SkillRegistry
├── llm/
│   ├── LlmClient.kt
│   ├── OpenAiLlmClient.kt
│   └── PromptTemplates.kt
├── report/
│   └── MarkdownReportExporter.kt
├── storage/
│   ├── JsonStorage.kt
│   ├── ProjectStore.kt
│   └── RunHistoryStore.kt     # legacy global run store
├── tools/
└── ui/
    ├── StemAgentLabApp.kt
    ├── components/
    └── theme/
```

---

## Troubleshooting

### `OPENAI_API_KEY is required`

Set the key through an environment variable or `.env`.

### The IDE shows unresolved Kotlin references

Trust Gradle first:

```bash
./gradlew compileKotlin
```

Some IntelliJ/Kotlin plugin combinations show false-positive unresolved references when the IDE plugin version lags behind the Gradle Kotlin version.

### JVM or SLF4J warnings appear on startup

Current macOS runs can print warnings about restricted native access from Gradle/Skiko and a missing SLF4J provider. These warnings are non-fatal; the app can still start normally.

### The app closes to tray instead of quitting

Closing the window hides it to the system tray. Use the tray menu `Quit` or stop the Gradle process from the terminal.

### Project data looks stale

Project data is local. To reset all app data from the UI, use `Reset All`.

Manual cleanup:

```bash
rm -rf projects
```

On Windows PowerShell:

```powershell
Remove-Item -Recurse -Force projects
```
