# Stem Agent Lab — Project Writeup

> **Template** — fill in your own observations after running the demo.

---

## 1. Problem Statement

Large language models are general-purpose tools. The hypothesis behind this project is that a _specialisation loop_ — where an agent iteratively selects its own tools, skills, and evaluation strategy — can produce a dramatically more effective agent for a narrow domain (here: Python code quality analysis) than a bare prompted baseline.

---

## 2. Approach

### Evolution Loop

1. **Baseline** — a generic agent with no tools reasons directly about Python code.
2. **StemAgent proposes candidates** — three configurations of increasing tool complexity are generated.
3. **Evaluator scores candidates** — each candidate is run against 5 Python bug-detection tasks. Score = fraction of expected issue keywords present in the response.
4. **VersionManager selects the winner** — the candidate with the best score / estimated cost ratio that also beats the baseline is selected.
5. **SpecializedAgent is frozen** — the winning configuration is locked and the UI displays the before/after comparison.

### Scoring

Each `EvalTask` carries an `expectedIssueKeywords` list. A response earns `matched / total` keywords as its task score. The aggregate score is the mean across all tasks.

This approach is intentionally simple and deterministic — the goal is to demonstrate the _shape_ of the loop, not production-grade evaluation.

---

## 3. Results

Live OpenAI responses vary by model output, so fixed mock score tables are no longer part of the product behavior. Reports now capture the actual score, token, and cost values for each project run.

---

## 4. Architecture Decisions

- **Project sessions** — each project has its own phase, logs, latest result, report path, and running job, while `EvolutionEngine` remains stateless.
- **StateFlow + coroutines** — the UI reacts to a single `AppState` emitted by `AppController`, keeping the Compose UI free of business logic.
- **Required OpenAI client** — the `LlmClient` interface is backed by `OpenAiLlmClient`; startup fails clearly if `OPENAI_API_KEY` is absent.
- **Keyword scoring** — chosen over LLM-as-judge so returned model text can be scored consistently against benchmark expectations.
- **No database** — project metadata and run history are persisted as JSON files under `projects/`, keeping the project self-contained.

---

## 5. Limitations

- Live OpenAI responses can vary by model and prompt, so integration tests assert structural behavior instead of fixed candidate ordering.
- The evaluator only checks keyword presence, not semantic correctness.
- `PythonRunner` executes code in a subprocess without a true sandbox; do not run untrusted code.
- OpenAI mode uses `gpt-4o-mini` for cost reasons; swap `model` in `OpenAiLlmClient` for stronger models.

---

## 6. Possible Extensions

- [ ] Add a second evolution round (round 2 of refinement)
- [ ] Implement LLM-as-judge scoring for more nuanced evaluation
- [ ] Persist candidate configs and allow loading a frozen specialised agent
- [ ] Add a "live benchmark" tab showing per-task response diffs
- [ ] Support additional domains (e.g. SQL optimisation, TypeScript linting)

---

## 7. How to Run

See [README.md](README.md) for setup instructions.

```bash
./gradlew run      # launch the desktop app
./gradlew test     # run unit tests
./gradlew packageDmg   # build macOS installer
```
