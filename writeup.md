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

## 3. Results (Mock Mode)

| Agent | Score | Tokens | Cost | Status |
|-------|-------|--------|------|--------|
| Baseline | 0.314 | ~1600 | $0.0032 | Rejected |
| Candidate A | 0.543 | ~3400 | $0.0068 | Rejected |
| **Candidate B** | **0.857** | ~4700 | **$0.0094** | ✅ Selected |
| Candidate C | 0.771 | ~7600 | $0.0152 | Rejected |

**Improvement: +172% over baseline.**

Candidate C scores lower than B despite having more tools, demonstrating that adding tools without improving accuracy still loses on score/cost ratio.

---

## 4. Architecture Decisions

- **StateFlow + coroutines** — the UI reacts to a single `AppState` emitted by `AppController`, keeping the Compose UI free of business logic.
- **Mock / Live switch** — the `LlmClient` interface is swapped at startup based on `OPENAI_API_KEY`. All downstream code is identical.
- **Keyword scoring** — chosen over LLM-as-judge to keep the prototype fully deterministic and runnable without an API key.
- **No database** — run history is persisted as JSON files in `runs/`, keeping the project self-contained.

---

## 5. Limitations

- The mock responses are handcrafted — real keyword coverage would vary by model and prompt.
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
