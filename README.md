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

## Evolution Loop

```
Domain input (Python QA)
        ↓
StemAgent proposes 3 candidate configurations (A / B / C)
        ↓
Evaluator tests each on 5 Python bug-detection tasks
        ↓
VersionManager selects the candidate with best score / cost ratio
        ↓
SpecializedAgent is frozen with the winning configuration
        ↓
UI shows: logs · metrics · selected tools · before/after comparison
        ↓
Export → reports/report-<runId>.md
```

---

## Candidate Strategies

| Agent | Tools | Expected score (mock) |
|-------|-------|-----------------------|
| Baseline | (none) | ~0.31 |
| Candidate A | code\_reader, static\_analyzer | ~0.54 |
| **Candidate B** ✓ | code\_reader, test\_generator, python\_runner, failure\_analyzer | **~0.86** |
| Candidate C | all above + patch\_suggester | ~0.77 (higher cost → lower ratio) |

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
├── storage/                   ← JsonStorage, RunHistoryStore
└── report/                    ← MarkdownReportExporter

src/main/resources/
├── datasets/python_qa_tasks.json
├── registries/tools.json + skills.json
└── demo/mock_evolution_run.json

runs/        ← JSON run history (auto-created)
reports/     ← Exported markdown reports (auto-created)
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
