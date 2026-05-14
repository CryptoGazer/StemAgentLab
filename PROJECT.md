# Stem Agent Lab — Project Reference

> Comprehensive technical reference for developers, agents, and reviewers.  
> Read this before making changes or handing the project to another agent.

---

## 1. What This Project Is

**Stem Agent Lab** is a Kotlin Compose Multiplatform Desktop application that demonstrates a controlled _stem agent specialisation loop_:

```
Create/select project
    ↓
Project domain input
    ↓
Baseline agent evaluated on benchmark tasks
    ↓
StemAgent proposes 3 candidate configurations (A / B / C)
    ↓
Each candidate evaluated on same benchmark tasks
    ↓
VersionManager selects winner by score² / cost ratio
    ↓
SpecializedAgent frozen with winning configuration
    ↓
UI shows before/after metrics + logs
    ↓
Export → projects/<projectId>/reports/report-<runId>.md
```

The agent does **not** rewrite source code. It generates and evaluates `AgentConfig` objects — structured data specifying tools, skills, prompt strategy, and token budget.

---

## 2. Technology Stack

| Component | Version |
|-----------|---------|
| Kotlin | 2.3.10 |
| Compose Multiplatform Desktop | 1.11.0 |
| Gradle (Kotlin DSL) | 9.2.0 |
| JVM toolchain | 21 (compatible with JDK 24) |
| kotlinx.serialization | 1.8.1 |
| kotlinx.coroutines | 1.10.2 |
| ktor (CIO engine) | 3.1.3 |
| kotlin.test + coroutines-test | bundled |

**Important:** The IDE shows false-positive "Unresolved reference" errors across the project. This is a known Kotlin plugin 2.1.x vs stdlib 2.3.10 version mismatch in IntelliJ IDEA. Trust Gradle output, not IDE squiggles. `./gradlew compileKotlin` always succeeds.

---

## 3. How to Run

```bash
# OpenAI key is required — Option A: environment variable
export OPENAI_API_KEY=sk-...
./gradlew run

# OpenAI key is required — Option B: .env file (gitignored, persists across restarts)
echo 'OPENAI_API_KEY=sk-...' > .env
./gradlew run

# Run all tests
./gradlew test

# Build (compile only)
./gradlew build

# Package macOS DMG
./gradlew packageDmg

# Package Windows MSI (must run on Windows or Windows CI runner)
./gradlew packageMsi
```

---

## 4. Complete File Tree

```
StemAgentLab/
│
├── build.gradle.kts                   ← Kotlin DSL build file
├── settings.gradle.kts                ← project name + plugin repos
├── CLAUDE.md                          ← project instructions for Claude Code
├── PROJECT.md                         ← this file
├── README.md                          ← user-facing quick start
├── writeup.md                         ← design decisions + results summary
├── .env                               ← LOCAL ONLY, gitignored — OPENAI_API_KEY=sk-...
├── .gitignore
│
├── .github/
│   └── workflows/
│       └── build.yml                  ← CI: test + packageDmg (macOS) + packageMsi (Windows)
│
├── src/
│   ├── main/
│   │   ├── kotlin/com/stemlab/
│   │   │   │
│   │   │   ├── Main.kt                ← entry point; system tray + Window
│   │   │   │
│   │   │   ├── app/
│   │   │   │   ├── AppController.kt   ← project orchestration: create/select projects, run/stop/export per project
│   │   │   │   ├── AppState.kt        ← AppState + ProjectViewState + Phase enum + Metrics
│   │   │   │   └── DemoScenario.kt    ← loads bundled python_qa_tasks.json
│   │   │   │
│   │   │   ├── ui/
│   │   │   │   ├── StemAgentLabApp.kt ← root Composable; header, status bar, action buttons, layout
│   │   │   │   ├── components/
│   │   │   │   │   ├── MetricsPanel.kt        ← baseline / best / improvement / tokens / cost
│   │   │   │   │   ├── CandidateListPanel.kt  ← list of candidates with score bars and status badges
│   │   │   │   │   ├── EvolutionLogPanel.kt   ← scrollable monospace log with colour coding
│   │   │   │   │   ├── ToolRegistryPanel.kt   ← selected tools with token/cost estimates
│   │   │   │   │   └── SettingsPanel.kt       ← domain TextField + Apply button
│   │   │   │   └── theme/
│   │   │   │       └── AppTheme.kt    ← dark colour scheme; all colour constants
│   │   │   │
│   │   │   ├── core/
│   │   │   │   ├── model/
│   │   │   │   │   ├── AgentConfig.kt         ← id, name, description, tools, skills, promptStrategy, maxTokens
│   │   │   │   │   ├── CandidateAgent.kt      ← AgentConfig + score + cost + status + scoreCostRatio
│   │   │   │   │   ├── ToolSpec.kt            ← id, name, description, estimatedTokensPerCall, estimatedCostPerCall
│   │   │   │   │   ├── SkillSpec.kt           ← id, name, description
│   │   │   │   │   ├── EvalTask.kt            ← id, description, code, expectedIssueKeywords
│   │   │   │   │   ├── EvalResult.kt          ← taskId, agentId, agentResponse, matchedKeywords, score, tokensUsed, costEstimate
│   │   │   │   │   ├── Budget.kt              ← maxTokens, maxCost, maxCandidates, maxRounds
│   │   │   │   │   ├── EvolutionResult.kt     ← runId, domain, candidates, selectedCandidateId, improvement, logs, timestamp
│   │   │   │   │   └── ProjectSpec.kt         ← project id/name/domain/description and latest run metadata
│   │   │   │   │
│   │   │   │   ├── agent/
│   │   │   │   │   ├── BaselineAgent.kt       ← no tools, direct reasoning only
│   │   │   │   │   ├── StemAgent.kt           ← proposeCandidates(); parses OpenAI JSON and fails loudly on invalid output
│   │   │   │   │   ├── SpecializedAgent.kt    ← runs tool-augmented prompt for a given AgentConfig
│   │   │   │   │   ├── CandidateAgentBuilder.kt ← builds (CandidateAgent, SpecializedAgent) pairs from configs
│   │   │   │   │   └── CandidateConfigParser.kt ← parses {"candidates":[...]} JSON from LLM response; returns null on failure
│   │   │   │   │
│   │   │   │   ├── evolution/
│   │   │   │   │   ├── EvolutionEngine.kt     ← main loop: baseline → propose → evaluate → select; uses onLog callback
│   │   │   │   │   ├── VersionManager.kt      ← selectBest() by score²/cost ratio above baseline
│   │   │   │   │   ├── StopCriteria.kt        ← shouldStop() checks rounds / candidates / tokens / cost
│   │   │   │   │   ├── ToolSelector.kt        ← tier-based tool ID lists (utility, not called by engine directly)
│   │   │   │   │   └── SkillSelector.kt       ← tier-based skill ID lists (utility, not called by engine directly)
│   │   │   │   │
│   │   │   │   ├── eval/
│   │   │   │   │   ├── Evaluator.kt           ← interface: evaluate(agentId, tasks, runTask) → List<EvalResult>
│   │   │   │   │   ├── PythonQaEvaluator.kt   ← implements Evaluator; calls ScoreCalculator per task
│   │   │   │   │   └── ScoreCalculator.kt     ← score() = matched keywords / total keywords; aggregateScore() = mean
│   │   │   │   │
│   │   │   │   ├── registry/
│   │   │   │   │   ├── ToolRegistry.kt        ← loads tools.json; resolve() returns GenericToolSpec for unknown IDs
│   │   │   │   │   └── SkillRegistry.kt       ← loads skills.json; resolve() returns GenericSkillSpec for unknown IDs
│   │   │   │   │
│   │   │   │   └── tasks/
│   │   │   │       ├── TaskGenerator.kt       ← interface: generate(domain, count) → List<EvalTask>
│   │   │   │       └── LlmTaskGenerator.kt    ← calls OpenAI for real tasks; invalid output is a visible error
│   │   │   │
│   │   │   ├── llm/
│   │   │   │   ├── LlmClient.kt              ← interface: complete(prompt) → LlmResponse
│   │   │   │   ├── OpenAiLlmClient.kt        ← ktor POST to gpt-4o-mini; domain-agnostic system prompt
│   │   │   │   └── PromptTemplates.kt        ← baseline(), toolAugmented(), candidateProposal(), generateTasks()
│   │   │   │
│   │   │   ├── tools/
│   │   │   │   ├── PythonRunner.kt           ← subprocess Python execution (simulated in MVP)
│   │   │   │   ├── FileReaderTool.kt         ← reads source files
│   │   │   │   ├── TestGeneratorTool.kt      ← generates pytest stubs
│   │   │   │   └── StaticAnalyzerTool.kt     ← static analysis simulation
│   │   │   │
│   │   │   ├── storage/
│   │   │   │   ├── JsonStorage.kt            ← save/load<T> using kotlinx.serialization; @PublishedApi internal json
│   │   │   │   ├── ProjectStore.kt           ← projects/index.json + projects/<projectId>/runs + reports
│   │   │   │   └── RunHistoryStore.kt        ← legacy global run store
│   │   │   │
│   │   │   ├── report/
│   │   │   │   └── MarkdownReportExporter.kt ← export() writes reports; buildReport() is pure
│   │   │   │
│   │   │   └── util/
│   │   │       └── DotEnvLoader.kt           ← reads OPENAI_API_KEY from env var, then .env file
│   │   │
│   │   └── resources/
│   │       ├── datasets/
│   │       │   └── python_qa_tasks.json       ← 5 Python bug-detection tasks with expectedIssueKeywords
│   │       ├── registries/
│   │       │   ├── tools.json                 ← 6 tool specs (code_reader, test_generator, python_runner, failure_analyzer, static_analyzer, patch_suggester)
│   │       │   └── skills.json                ← skill specs (direct_reasoning, edge_case_reasoning, test_driven_analysis, ...)
│   │       └── demo/
│   │           └── mock_evolution_run.json    ← sample completed run for UI preview
│   │
│   └── test/
│       └── kotlin/com/stemlab/
│           ├── ScoreCalculatorTest.kt         ← 6 tests: perfect match, empty, partial, case-insensitive, aggregate, empty keywords
│           ├── VersionManagerTest.kt          ← 4 tests: best ratio, rejects below baseline, SELECTED status, empty list
│           ├── EvolutionEngineTest.kt         ← 6 tests: baseline scored, B selected, all scored, improvement positive, log milestones, ordering
│           ├── StopCriteriaTest.kt            ← 8 tests: shouldStop on rounds/candidates/tokens/cost, isAcceptable
│           ├── MarkdownReportExporterTest.kt  ← 11 tests: domain, baseline/best score, selected/rejected names, safeguards, file created, run id
│           ├── TaskGeneratorTest.kt           ← 7 tests: python domain, count limit, generic domain, domain in description, keywords non-empty
│           ├── CandidateConfigParserTest.kt   ← 7 tests: valid JSON, IDs, tools, invalid JSON, wrong count, prose surrounding, empty string
│           └── ToolRegistryTest.kt            ← 7 tests: known tool, unknown → generic, mixed, cost > 0, readable name, empty, all 6 known tools
│
└── projects/                          ← auto-created local project data; gitignored
```

---

## 5. Key Architectural Decisions

### Score formula: score² / cost
`CandidateAgent.scoreCostRatio = score * score / estimatedCost`

The quadratic amplification means a 2× better score is 4× more attractive. This makes Candidate B win over Candidate A even though A is cheaper: B's score is ~58% better, so its ratio is ~2.5× higher.

### OpenAI-backed scoring
All agent responses now come from `OpenAiLlmClient`. Scoring is still deterministic once text is returned: `ScoreCalculator` matches expected issue keywords in the OpenAI response. There is no runtime path that silently substitutes canned mock answers when the key is missing or OpenAI output is malformed.

### Single propose-evaluate cycle
`EvolutionEngine` always passes `round = 0` to `StopCriteria.shouldStop()`. `maxRounds = 2` in `Budget` is therefore never violated in the current implementation. This is intentional — the engine runs exactly one cycle: propose → evaluate → select.

### Project sessions
`AppController` manages a list of `ProjectViewState` objects and a `projectJobs` map keyed by `projectId`. Each project has its own phase, logs, metrics, selected tools, latest result, export path, and optional running job. One project cannot be started twice, but different projects can run concurrently.

### Parallel candidate evaluation
Within a single project run, `EvolutionEngine` evaluates candidate agents concurrently after baseline and candidate proposal complete. `Budget.maxParallelCandidates` limits in-run OpenAI pressure (default: 2), so extra candidates wait for a permit instead of launching unlimited requests. Candidate task execution remains sequential inside each candidate to keep logs, budget accounting, and rate-limit behavior predictable.

### Token and cost accounting
`PythonQaEvaluator` now preserves `LlmResponse.tokensUsed` and `LlmResponse.costEstimate` instead of recalculating cost from a fixed token multiplier. Run totals include task generation, baseline evaluation, candidate proposal, and all candidate evaluations.

### StateFlow + coroutines for UI reactivity
`AppController` holds a single `MutableStateFlow<AppState>`. The UI calls `collectAsState()` and recomposes on every update. No business logic lives in Compose composables.

### OpenAI key loading
`DotEnvLoader.requireApiKey()` checks `System.getenv("OPENAI_API_KEY")` first, then reads `.env` in the working directory. If no key is found, startup fails with a clear error. Secrets are never hardcoded in Kotlin, Gradle, docs, or GitHub Actions.

---

## 6. Evolution Log — What You See in the UI

The **Evolution Log** panel (bottom-right) shows entries for the active project. Each line is timestamped `[HH:MM]`. Colour coding in `EvolutionLogPanel`:
- **Cyan** — lines containing "score" / "Score"
- **Green** — lines containing "Selected" / "complete"
- **Red** — lines containing "Rejected" / "ERROR"
- **Grey** — all other lines

Typical log sequence for a full run:

```
[12:00] Evolution started: baseline → propose → evaluate → select
[12:00] Generating tasks for domain: "Python QA" via OpenAI
[12:00] Loaded 5 evaluation tasks
[12:00] [abc12345] Evolution started — domain: Python QA
[12:00] [abc12345] Loaded 5 evaluation tasks from dataset
[12:00] [abc12345] Running Baseline Agent on 5 tasks...
[12:00] [abc12345] Baseline score: 0.314 (avg keyword match)
[12:00] [abc12345] StemAgent proposing candidate configurations...
[12:00] [abc12345] Generated 3 candidates [OpenAI-parsed]: Candidate A, Candidate B, Candidate C
[12:00] [abc12345] Evaluating Candidate A...
[12:00] [abc12345] Candidate A score: 0.543 (Δ+0.229)
[12:00] [abc12345] Evaluating Candidate B...
[12:00] [abc12345] Candidate B score: 0.857 (Δ+0.543)
[12:00] [abc12345] Evaluating Candidate C...
[12:00] [abc12345] Candidate C score: 0.771 (Δ+0.457)
[12:00] [abc12345] VersionManager selecting best candidate...
[12:00] [abc12345] Selected: Candidate B (score=0.857, cost=$0.0094, ratio=78.12)
[12:00] [abc12345] Rejected: Candidate A
[12:00] [abc12345] Rejected: Candidate C
[12:00] [abc12345] SpecializedAgent frozen with Candidate B configuration
[12:00] [abc12345] Evolution complete — improvement: +172.3%
```

---

## 7. OpenAI Mode — How It Works and How to Extend It

```
AppController.runEvolution()
    └── LlmTaskGenerator.generate(domain)        ← asks OpenAI for EvalTask JSON
    └── EvolutionEngine.run(domain, tasks, onLog)
            └── OpenAiLlmClient.complete(prompt) ← called for baseline, candidate proposal, and candidate evaluation
                    └── ScoreCalculator          ← scores returned text against expectedIssueKeywords
```

### Adding a new domain

The task generator prompt in [PromptTemplates.kt](src/main/kotlin/com/stemlab/llm/PromptTemplates.kt) is domain-driven. For a new domain, update `generateTasks()` with any schema constraints or examples the model should follow. The runtime should still return `EvalTask` JSON:

```kotlin
EvalTask(
    id = "task_001",
    description = "Detect N+1 query in SQL data access code",
    code = "fun loadUsers() = users.map { loadOrders(it.id) }",
    expectedIssueKeywords = listOf("N+1", "batch loading", "JOIN")
)
```

### Adding new candidate behavior

Candidate configurations are generated by OpenAI through `PromptTemplates.candidateProposal()`, then validated by `CandidateConfigParser`. If the model returns malformed JSON or the wrong number of candidates, the run fails visibly instead of substituting hardcoded defaults.

---

## 8. Testing Guide

### Run all tests

```bash
./gradlew test
```

### Test suite

| File | Tests | What it covers |
|------|-------|----------------|
| `ScoreCalculatorTest` | 6 | Keyword matching, scoring formula, edge cases |
| `VersionManagerTest` | 4 | Candidate selection, ratio logic, SELECTED status |
| `EvolutionEngineTest` | 1 | Live OpenAI evolution smoke test with structural assertions |
| `StopCriteriaTest` | 8 | shouldStop on all 4 axes, isAcceptable |
| `MarkdownReportExporterTest` | 11 | Report content, file creation, graceful no-candidate case |
| `TaskGeneratorTest` | 1 | Live OpenAI task generation structure |
| `CandidateConfigParserTest` | 7 | Valid JSON, malformed JSON, wrong count, prose-wrapped JSON |
| `ToolRegistryTest` | 7 | Known tools, unknown → generic fallback, empty list |
| `ProjectStoreTest` | 2 | Project index and per-project run paths |
| `AppControllerProjectTest` | 2 | Project creation/selection and independent project sessions with fake LLM |

### Watching test output

```bash
./gradlew test --info 2>&1 | grep -E "PASSED|FAILED|tests="
```

### What is NOT tested (known gaps)

- OpenAiLlmClient HTTP internals beyond live smoke coverage
- Compose UI components (no headless UI test harness configured)
- DotEnvLoader (trivial, no secret to test with)

---

## 9. What Still Needs Work

| Area | Gap | Effort |
|------|-----|--------|
| Multi-round evolution | Engine always runs round=0; maxRounds=2 is never used | Medium |
| LLM-as-judge scoring | Keyword matching only; no semantic correctness | High |
| Persistent frozen agent | Winning config not saved/reloaded across app restarts | Medium |
| Per-task response diff | No UI to compare what each candidate said per task | Medium |
| OpenAI task generation | LlmTaskGenerator calls LLM but EvalTask JSON must match exactly | Low |
| ToolSelector / SkillSelector | These classes exist but are not called by EvolutionEngine | Low (cleanup only) |
| UI tests | No automated headless Compose tests | High |
| `packageMsi` on macOS | Must be run on Windows or Windows CI runner | N/A |

---

## 10. Data Formats

### EvalTask (src/main/resources/datasets/python_qa_tasks.json)

```json
{
  "id": "task_001",
  "description": "Detect division by zero in divide function",
  "code": "def divide(a, b):\n    return a / b",
  "expectedIssueKeywords": ["ZeroDivisionError", "check b != 0", "guard clause"]
}
```

### AgentConfig (returned by StemAgent, stored in EvolutionResult)

```json
{
  "id": "candidate_b",
  "name": "Candidate B",
  "description": "Full evaluation pipeline for Python QA",
  "tools": ["code_reader", "test_generator", "python_runner", "failure_analyzer"],
  "skills": ["direct_reasoning", "edge_case_reasoning", "test_driven_analysis", "root_cause_analysis"],
  "promptStrategy": "tool_augmented",
  "maxTokens": 1200
}
```

### LLM candidate proposal format (OpenAI mode, parsed by CandidateConfigParser)

```json
{
  "candidates": [
    { "id": "candidate_a", "name": "Candidate A", "tools": [...], "skills": [...], "promptStrategy": "tool_augmented", "maxTokens": 800 },
    { "id": "candidate_b", "name": "Candidate B", "tools": [...], "skills": [...], "promptStrategy": "tool_augmented", "maxTokens": 1200 },
    { "id": "candidate_c", "name": "Candidate C", "tools": [...], "skills": [...], "promptStrategy": "tool_augmented", "maxTokens": 1500 }
  ]
}
```

---

## 11. Known IDE Issue

IntelliJ IDEA shows "Unresolved reference" errors for virtually everything in the project. This is because the IDE's bundled Kotlin plugin (2.1.x) is out of sync with the project's Kotlin stdlib (2.3.10). **These errors are false positives.** The project compiles and runs correctly via Gradle:

```bash
./gradlew compileKotlin   # always succeeds
./gradlew test            # all tests pass
./gradlew run             # app launches
```

Do not attempt to "fix" these IDE errors by downgrading dependencies.
