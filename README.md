# Stem Agent Lab

A Kotlin Compose Multiplatform Desktop prototype demonstrating a controlled **stem agent specialisation loop** — where a generic baseline agent evolves into a specialised Python QA agent by selecting tools, skills, and workflow steps, then self-evaluating on a small benchmark.

---

## Quick Start (macOS)

### Prerequisites

| Tool | Version |
|------|---------|
| JDK | 21+ (temurin recommended) |
| Gradle | Provided via wrapper (`./gradlew`) |
| Python 3 | Optional — used only by `PythonRunner` in live mode |

### Run the application

```bash
export OPENAI_API_KEY=sk-...
./gradlew run
```

Or create a local `.env` file (recommended for local dev, persists across restarts):
```bash
# Create once in the project root — gitignored, never committed
echo 'OPENAI_API_KEY=sk-...' > .env
./gradlew run
```

The app requires an OpenAI key. It reads `OPENAI_API_KEY` from the environment first, then falls back to `.env`. If no key is present, startup fails with a clear configuration error.

### Run tests

```bash
./gradlew test
```

### Build native packages

```bash
# macOS DMG
./gradlew packageDmg

# Windows MSI (run on Windows or in CI)
./gradlew packageMsi

# Linux DEB
./gradlew packageDeb
```

Packages are output to `build/compose/binaries/main/`.

---

## Projects and Evolution Loop

```
Create or select a project
        ↓
Project domain input (Python QA, SQL Optimizer, ...)
        ↓
StemAgent proposes 3 candidate configurations (A / B / C)
        ↓
Evaluator tests each on OpenAI-generated benchmark tasks
        ↓
VersionManager selects the candidate with best score / cost ratio
        ↓
SpecializedAgent is frozen with the winning configuration
        ↓
UI shows: logs · metrics · selected tools · before/after comparison
        ↓
Export → projects/<projectId>/reports/report-<runId>.md
```

The left sidebar contains the current project list and a `New Project` button. Each project has its own domain, logs, latest result, selected tools, report path, and running job. You can start one project, switch to another, and start a second run while the first is still running.

---

## Candidate Strategies

Candidate configurations are proposed by OpenAI for the active project domain. The default prompt still asks for three capability levels: lightweight analysis, fuller test/execution workflow, and maximal tooling with patch suggestion.

---

## Project Structure

```
src/main/kotlin/com/stemlab/
├── Main.kt                    ← entry point
├── app/                       ← AppController, AppState, DemoScenario
├── ui/                        ← Compose UI
│   ├── StemAgentLabApp.kt
│   ├── components/            ← MetricsPanel, CandidateListPanel, ...
│   └── theme/AppTheme.kt
├── core/
│   ├── model/                 ← AgentConfig, CandidateAgent, EvalTask, ...
│   ├── agent/                 ← BaselineAgent, StemAgent, SpecializedAgent
│   ├── evolution/             ← EvolutionEngine, VersionManager, StopCriteria
│   ├── eval/                  ← PythonQaEvaluator, ScoreCalculator
│   └── registry/              ← ToolRegistry, SkillRegistry
├── llm/                       ← LlmClient, OpenAiLlmClient
├── tools/                     ← PythonRunner, FileReaderTool, ...
├── storage/                   ← JsonStorage, ProjectStore, RunHistoryStore
└── report/                    ← MarkdownReportExporter

src/main/resources/
├── datasets/python_qa_tasks.json
├── registries/tools.json + skills.json
└── demo/mock_evolution_run.json

projects/    ← local project index, per-project runs, exported reports (auto-created, gitignored)
```

---

## Troubleshooting

**Gradle downloads JDK on first run** — this is expected; `foojay-resolver-convention` provisions a compatible JDK automatically.

**Version mismatch errors** — if you see plugin resolution failures, update the version strings in `build.gradle.kts`:
```kotlin
kotlin("jvm") version "2.3.10"
id("org.jetbrains.compose") version "1.11.0"
```

**`packageDmg` requires Xcode CLI tools** on macOS:
```bash
xcode-select --install
```

**`packageMsi` requires WiX Toolset** on Windows (installed automatically by the Compose plugin on Windows CI runners).
