# Stem Agent Lab — Writeup

## 1. The Problem and My Framing

The challenge is to build an agent that starts generic and grows into something specific through its own process. The hardest part is not deciding what it should become, but designing _how_ it figures that out.

My framing: treat specialisation as a search over agent configurations. A configuration is a concrete, serialisable struct — tools, skills, prompt strategy, token budget — not source code. The stem agent searches this space by generating candidates, evaluating each on a benchmark, and selecting the best by a score/cost ratio. The result is a frozen artifact that can be re-evaluated at any time without repeating the search.

I chose **Python code quality analysis** as the domain because it is narrow enough to allow deterministic-enough evaluation (keyword matching on known bugs), and familiar enough that a baseline LLM already knows the domain — which makes the before/after gap meaningful. The evaluation method is intentionally simple: if a task's expected diagnostic keywords appear in the agent's response, the task is scored as matched.

---

## 2. Approach

### The Loop

```
Baseline evaluation
      ↓
StemAgent: propose 3 candidate AgentConfigs via OpenAI
      ↓
Parallel candidate evaluation (max 2 concurrent)
      ↓
VersionManager: select by score² / cost
      ↓
FrozenAgent artifact saved to projects/<id>/agent.json
```

The stem agent never edits source code. It outputs `AgentConfig` objects. The app decides whether to accept the output.

### Candidate Design

Three candidates of increasing capability are proposed per run:

| Candidate | Tools | Expected trade-off |
|-----------|-------|--------------------|
| A | code_reader, static_analyzer | Low cost, moderate improvement |
| B | code_reader, test_generator, python_runner, failure_analyzer | Better coverage, moderate cost |
| C | B's tools + patch_suggester | Highest cost, often diminishing returns |

The StemAgent asks OpenAI to generate these via a structured JSON prompt. It receives back a `{"candidates": [...]}` object and validates that exactly 3 configs are returned. If parsing fails, the run aborts with a clear error — no silent mock fallback.

### Scoring and Selection

Each candidate is evaluated on the same set of LLM-generated benchmark tasks. Tasks are generated fresh per run by asking OpenAI for `EvalTask` objects in a given domain, so the benchmark adapts to the project domain rather than being fixed.

Score per task:

```
score = matched_keywords / total_expected_keywords
```

Aggregate score: mean across all tasks.

`VersionManager` selects by `score² / cost`. The quadratic numerator amplifies strong candidates — an agent with twice the score is four times more attractive, even at higher cost. A candidate must also beat the baseline score outright; cost efficiency alone does not win.

---

## 3. What Worked

The score²/cost formula behaved as intended: Candidate B beat Candidate C in both test runs even though C occasionally scored slightly higher in raw terms. The extra `patch_suggester` in C's config cost enough tokens that the ratio came out lower, so C kept getting rejected. That said, "behaved as intended" is doing a lot of work here — I designed the formula to prefer B, so finding that it prefers B is not a surprising validation.

More interesting was how much the simulated tool invocations change what the LLM actually produces. When the prompt includes:

```
[PYTHON_RUNNER] Invoking python_runner on task_001...
[TEST_GENERATOR] Invoking test_generator on task_001...
```

the model responds as though tests were actually run — it discusses specific failure conditions, edge cases, and concrete fixes rather than giving a generic overview. The tool names are a framing device, not a mechanism. This turned out to be practically useful: the gap between baseline and specialized agents does not require real tool execution.

Freezing the winning config to `projects/<id>/agent.json` was more useful than expected. The "Eval Frozen" button reruns the same config on a fresh task set without starting a new evolution cycle, giving a quick read on whether the improvement generalises beyond the original benchmark.

---

## 4. What Surprised Me

I expected the candidate proposal to vary more between runs. In practice, the three configs — lightweight, full-pipeline, maximal — are nearly identical every time. The LLM is essentially filling in a template, because the example JSON in the prompt anchors it strongly. This is not a bad property (it makes the pipeline predictable), but it means the "stem agent proposing strategies" story is softer than it sounds. The StemAgent is selecting from an implicit menu, not exploring the space.

Switching from a bundled `python_qa_tasks.json` to LLM-generated tasks worked better than I expected. The system ran correctly on an SQL Optimizer project without any code changes — OpenAI produced SQL-specific diagnostics (N+1 patterns, index hints, non-sargable predicates) and the scoring still worked. The downside is reproducibility: the same domain can produce different task sets on different runs, which makes it harder to know whether a score change reflects genuine improvement or just task variation.

---

## 5. What Failed

The keyword scoring is the weakest part. A response can mention every keyword without explaining anything useful. If a task expects `["case", "spaces", "normalize"]`, a response that says "check case, spaces, and normalize" gets full marks even if it offers no actual fix. This was a deliberate choice — LLM-as-judge adds cost and its own failure modes — but it means the improvement numbers measure vocabulary overlap, not reasoning quality.

Single-cycle evolution can't learn from its own result. If Candidate B wins with a marginal improvement, there is no way to ask "what would make B better?" — the run ends. A second round using the winning config as the new baseline and proposing refinements would help significantly, especially for domains where first-round gains are small.

The candidate proposal is too anchored by the example JSON in the prompt. The tool names (`code_reader`, `test_generator`, etc.) end up in nearly every run's output. A more open approach — giving the model a registry and asking it to select — would produce more varied proposals, at the cost of more parse failures and potentially unusual configs.

---

## 6. What I Would Do With More Time

Multi-round evolution is the obvious next step: use the round-1 winner as the new baseline, run another propose-evaluate cycle, stop when the score improvement falls below a threshold. The infrastructure is mostly already there.

LLM-as-judge scoring would fix the main evaluation weakness. Instead of keyword matching, a second call evaluates whether the response actually identifies the bug and explains the fix. The tradeoff is cost and another failure surface — the judge prompt also needs to be correct.

Real tool execution is where the performance gap between configs would become large. Right now `python_runner` is a string in the prompt. A subprocess call with a timeout would let the agent observe actual test failures and include real execution traces in its analysis.

The StemAgent currently gets two inputs: domain name and baseline score. Richer signal — which keyword categories the baseline keeps missing, which tasks it fails consistently — would let it make more targeted proposals. "Baseline misses normalisation-related keywords in 80% of tasks" is more useful than "score is 0.314".

---

## 7. Observed Run Results

Two end-to-end runs against real OpenAI calls. Both produced a frozen `agent.json` and a markdown report.

### Run 1 — Python QA

| Metric | Value |
|--------|-------|
| Baseline score | 0.314 |
| Candidate B score | 0.571 |
| Improvement | +82% |
| Total tokens | ~18 000 |
| Estimated cost | ~$0.007 |

Candidate B (`code_reader + test_generator + python_runner + failure_analyzer`) won. Candidate C scored the same but was rejected because its score²/cost ratio was lower — `patch_suggester` did not pay for itself.

Frozen artifact: `projects/python-qa/agent.json`

---

### Run 2 — SQL Optimizer (run ID: `d9fdd6a8`)

No code changes between the two runs. OpenAI generated SQL-specific tasks from the domain name alone.

| Metric | Value |
|--------|-------|
| Baseline score | 0.350 |
| Candidate A score | 0.200 (below baseline — rejected) |
| Candidate B score | 0.400 |
| Candidate C score | 0.400 |
| Selected | Candidate B (higher score²/cost ratio than C) |
| Improvement | +14.3% |
| Total tokens | 12 785 |
| Estimated cost | $0.0052 |

Winning configuration: `code_reader, test_generator, python_runner, failure_analyzer` / score-cost ratio 131.36.

The smaller improvement (+14% vs +82%) makes sense: SQL diagnostic keywords are more precise than Python ones, so the keyword-matching ceiling is lower. The candidate ranking was stable even though the absolute scores were not comparable across domains.

Frozen artifact: `projects/sql-review/agent.json`  
Report: `reports/report-d9fdd6a8.md`

### Where to find the output files

After a run completes, two files are written to disk:

| File | Path |
|------|------|
| Frozen agent config | `projects/<project-id>/agent.json` |
| Markdown report | `reports/report-<run-id>.md` |

Both are shown in the app after the run: the **Frozen Agent** panel shows the `agent.json` path in cyan text (selectable), and the **Output Files** panel in the left column shows both paths once they exist.

### Re-evaluation bug on SQL Optimizer

Clicking "Eval Frozen" on the SQL project failed with:

```
Unexpected JSON token at offset 1368: Expected quotation mark '"',
but had '+' instead at path: $[4].code
```

OpenAI had generated a SQL injection example as one of the task's code fields:

```sql
SELECT * FROM users WHERE username = '" + userInput + "';
```

The double-quotes inside the SQL string were not escaped as `\"` in the JSON response, breaking the parser. The fix was threefold: the `generateTasks` prompt now explicitly instructs OpenAI to escape double quotes and encode newlines as `\n`; a `sanitizeCodeFields()` pre-processor handles literal newlines before parsing; and the generator retries once on parse failure, since the error is non-deterministic.

---

## 8. Architecture Notes

The stem agent's output is restricted to `AgentConfig` objects. The LLM never writes to the project's source files. This makes the loop safe to run without sandboxing and keeps it auditable: every run produces a JSON artifact that fully describes what was selected and why.

The UI is a control panel, not a chat interface. The evolution loop is triggered explicitly, runs as a coroutine, and streams log lines to a bounded panel. Project state is persisted as JSON under `projects/<id>/`. Multiple projects can run in parallel.

| Constraint | Value |
|------------|-------|
| Candidates per run | 3 |
| Max evolution rounds | 1 |
| Max parallel candidate evaluation | 2 |
| Max tokens per run | 100,000 |
| Max estimated cost | $2.00 |
| LLM write access to source | None |

---

*Run:* `./gradlew run` — requires `OPENAI_API_KEY` in environment or `.env` file.  
*Test:* `./gradlew test` — 82 unit tests, no network calls except `EvolutionEngineTest`.
