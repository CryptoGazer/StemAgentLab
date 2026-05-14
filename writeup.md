# Stem Agent Lab — Writeup

## 1. The Problem and My Framing

The challenge is to build an agent that starts generic and grows into something specific through its own process. The hardest question is not _what_ the agent becomes, but _how_ it figures out what to become.

My framing: treat specialisation as a **search problem over agent configurations**. A configuration is a concrete, serialisable struct — tools, skills, prompt strategy, token budget — not source code. The stem agent searches this space by generating candidate configurations, evaluating each on a benchmark, and selecting the best by a score/cost ratio. The result is a frozen artifact that can be re-evaluated at any time without repeating the search.

I chose **Python code quality analysis** as the domain because it is narrow enough to produce deterministic-enough evaluation (keyword matching on known bugs), and familiar enough that a baseline LLM already knows the domain — which makes the before/after gap meaningful. The evaluation method is intentionally simple: if a task's expected diagnostic keywords appear in the agent's response, the task is scored as matched.

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

Each candidate is evaluated on the same set of LLM-generated benchmark tasks. Tasks are generated fresh per run by asking OpenAI for `EvalTask` objects in a given domain. This means the benchmark itself is not fixed — it adapts to the project domain.

Score per task:

```
score = matched_keywords / total_expected_keywords
```

Aggregate score: mean across all tasks.

`VersionManager` selects by `score² / cost`. The quadratic numerator amplifies strong candidates — an agent with twice the score is four times more attractive, even at higher cost. A candidate must also beat the baseline score outright; cost efficiency alone does not win.

---

## 3. What Worked

**The score²/cost formula produces the right result.** In practice, Candidate B consistently wins over Candidate C. Candidate C adds one more tool (`patch_suggester`) but the marginal score gain from patch suggestion does not justify the token cost — the ratio is lower even when C scores slightly higher than B in raw terms.

**Tool context in the prompt changes LLM behaviour, even when tools are not really executed.** This is the most practically useful observation. When the prompt says:

```
[PYTHON_RUNNER] Invoking python_runner on task_001...
[TEST_GENERATOR] Invoking test_generator on task_001...
```

the LLM produces more structured, step-by-step analysis. It treats the "tool invocations" as evidence that tests were run and code was executed, and responds accordingly — covering edge cases and failure modes it would skip in baseline mode. The tool names in the prompt act as a framing device that activates a more analytical response pattern.

**FrozenAgent as a persistent artifact.** After each run, the winning config is saved to `projects/<id>/agent.json`. This means the specialisation is tangible: you can close the app, reopen it, and the frozen agent is still there. The "Eval Frozen" action reruns the frozen config on fresh tasks without starting a new evolution cycle, which gives a meaningful stability check.

---

## 4. What Surprised Me

**The simulated-tools trick works.** I expected the evaluation gap between baseline and candidates to be modest without real tool execution. Instead, the improvement is measurable — typically 20–60% depending on the domain and the specific benchmark tasks generated. The LLM is not actually running the code; it is pattern-matching on tool invocation strings. This raises a question I did not fully resolve: how much of the "specialisation" is the agent genuinely reasoning differently, versus being primed by the prompt structure?

**The candidate proposal is more stable than expected.** I assumed that asking OpenAI to propose candidate configs would produce different configs every run, making evaluation noisy. In practice, the structure of the three candidates — lightweight, full-pipeline, maximal — is stable across runs. The tool names vary slightly, but the general shape does not. This is partly because the example JSON in the prompt anchors the LLM's output format.

**Generating benchmark tasks with OpenAI works better than a fixed dataset.** I initially used a bundled `python_qa_tasks.json`. Switching to LLM-generated tasks per project domain made the system genuinely domain-agnostic: creating a project called "SQL Optimizer" produces SQL-specific tasks, not Python tasks. The trade-off is that evaluation tasks are less reproducible across runs.

---

## 5. What Failed

**Keyword scoring does not capture reasoning quality.** A response can match all expected keywords without demonstrating understanding. For example, if a task expects `["case", "spaces", "normalize"]`, a response that lists "you should check case, spaces, and normalize" trivially passes — even if it offers no actual fix. The score measures vocabulary overlap, not correctness.

This was a deliberate trade-off for the prototype: LLM-as-judge scoring would be more accurate but adds another LLM call per task and makes the evaluation itself dependent on the quality of the judge prompt. I chose reproducibility over accuracy.

**Single-cycle evolution cannot refine.** The current loop runs one propose-evaluate cycle. If Candidate B wins but scores only marginally above baseline, a second round that uses B's config as the new baseline — and asks the StemAgent to propose improvements — would likely produce a significantly better final agent. Without iteration, the system can select the best of three first-attempt candidates but cannot learn from what worked.

**The candidate proposal is too guided.** The `candidateProposal` prompt includes a full example JSON with specific tool names and skill names. This means the LLM is completing a template more than inventing a specialisation strategy. The "figuring out" part is largely done by the prompt author, not the stem agent. A less guided approach — giving the LLM a tool registry and asking it to select from it — would produce more varied candidates, at the cost of more parse failures.

---

## 6. What I Would Do With More Time

**Multi-round evolution.** Use the winning candidate's config as the new baseline in a second round. After round 1, the StemAgent knows what worked and can propose more targeted improvements. Stop when the score improvement between rounds falls below a threshold — this is a natural convergence criterion.

**LLM-as-judge scoring.** Replace keyword matching with a second LLM call that evaluates whether the agent's response actually identifies the bug and explains the fix correctly. This would make scoring domain-agnostic without requiring hand-crafted keyword lists, at the cost of evaluation reproducibility and cost.

**Real tool execution.** The current tools are simulated — their "output" is just a string in the prompt. Connecting `python_runner` to an actual Python subprocess (with a timeout and sandboxing) would let the agent actually run test cases, observe failures, and include real execution traces in its analysis. This is where the performance gap between candidates would become genuinely large.

**Environment signal reading.** The current StemAgent's only inputs are domain name and baseline score. A richer signal — error categories from baseline failures, task difficulty distribution, keyword hit/miss breakdown — would let the stem agent make more targeted proposals. "Your baseline misses normalization-related keywords in 80% of tasks" is much more informative than "your baseline score is 0.314".

**Persistent agent reuse.** The frozen config is saved, but the agent is not yet deployed as a live endpoint. The natural next step is to wrap the frozen `AgentConfig` in a persistent service that accepts new tasks without requiring a new evolution cycle.

---

## 7. Architecture Notes

The key architectural decision was to keep the stem agent's output to `AgentConfig` objects only. The LLM never writes to the project's source files. This constraint makes the system safe to run without sandboxing and keeps the evolution loop auditable: every run produces a JSON artifact that fully describes what was selected and why.

The UI is a control panel, not a chat interface. The evolution loop is triggered explicitly, runs as a coroutine, and streams log lines to a bounded panel. Project state is persisted as JSON under `projects/<id>/`. Multiple projects can run in parallel.

Safeguards built into the current loop:

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
*Test:* `./gradlew test` — 82 unit tests, no network calls except `EvolutionEngineTest` which requires the key.
