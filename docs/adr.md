# Architecture Decision Records

```{toctree}
:glob:
:hidden:
adr/*
```

## Introduction

This section records the decisions the project has made between serious options: what was decided, why, and what follows from it. Each record (ADR) is a short, permanent note; it is not updated to reflect later changes. When a decision is changed, a new ADR is created and the previous one is marked `Superseded`.

An ADR is distinct from the rest of the documentation: the user and algorithm docs describe **what** the software does, an ADR explains **why this approach and not the alternative**.

## When to write an ADR

Write an ADR in the **same pull request as the decision it documents**, when the choice is:

- **costly or hard to reverse**: a public API, an MIP model change, a module separation, or a convention that will recur (a pattern later code must follow);
- **or business-visible and contestable**: the choice is visible in the results (costs, statuses, margins, performance, convergence guarantees) and at least one serious alternative existed or was considered.

If the rationale of a choice is one sentence that the PR description already carries, there is no ADR to write. When in doubt, ask in the pull request review.

## What is not an ADR

- **Rules imposed from outside** (TSO or regulator requirements) are requirements, documented in the user docs. They belong in an ADR only as the *context* of the decision that interprets them in code or parameters.
- **Fine-grained mapping** of a requirement to several small technical choices (which parameter, which class, which test): that is traceability, not a decision.
- **Routine implementation details** invisible from the results, with no recurring-convention value.

## Conventions

- **Location and numbering**: one file per ADR in `docs/adr/`, named `NNNN-slug.md` where `NNNN` is the next free four-digit number and the slug names the decision in kebab-case, at most five words (e.g. `0001-json-roundtrip-parameters-copy.md`).
- **Index**: the toctree of this page lists the files of `docs/adr/` automatically (Sphinx `:glob:` option), so a new ADR needs no toctree edit; add one line to the Records list at the bottom of this page in the same pull request.
- **Status**: `Accepted` from the moment the ADR is drafted (the ADR is opened in the same pull request as the decision, so it lands with the merge and no status change is needed afterwards), `Superseded by #NNNN` when a later ADR replaces it, `Deprecated` when the decision is no longer recommended but has no direct successor, `Rejected` when a real proposal was explicitly ruled out (see below).

### The `Rejected` status (rare)

A `Rejected` ADR is published only when a real proposal, with identifiable defenders among the maintainers or the TSOs, was explicitly ruled out as a decision, and the record exists to stop the re-proposal cycle. Three conditions, together:

- the reason of the rejection is non-obvious (a business or political trade-off, not "technically less clean");
- without the record, someone would credibly re-propose it;
- the rejection was ruled collectively (maintainer review, TSO meeting), not merely a closed pull request.

A defeated alternative inside an accepted decision is **not** a separate `Rejected` ADR: it belongs to the Context of the accepted ADR, as ADR 0001 does with its rejected copy constructors. A `Rejected` ADR always states, in its Consequences, the condition under which the topic may be reopened: it dates the lock, it does not make it permanent.
- **Superseding**: when an ADR replaces a previous one, set the old ADR's status to `Superseded by #NNNN` in the same pull request, and cross-reference the two.
- **No duplicates**: if an existing ADR already covers the decision, extend it rather than creating a new file.
- **English, 30-60 lines**: the ADR restructures the rationale already produced during the review, it does not re-essay it.

## Template

```markdown
# NNNN. <The decision, stated in the present tense>

| | |
|---|---|
| **Status** | Accepted |
| **Date** | YYYY-MM-DD |
| **Domain** | algo / archi / api / process |
| **Related** | #N issue, #N PR, #N ADR |

## Context

The requirement or problem (2-5 sentences), the constraints, and the serious alternatives that were considered. An externally imposed rule is stated here as a rule, not as the decision.

## Decision

What the project chose, stated plainly, with the exact class / parameter / module names (2-6 sentences). No re-justification of the Context.

## Consequences

- Positive: what the decision gains or enables.
- Negative / risks: known limitations and unverified cases, maintenance cost.
- Impact: on code, API, team, infrastructure; what must follow the decision.
```

The `Domain` tag is a single label: `algo` (algorithm and requirement-interpretation decisions), `archi` (module and structure decisions), `api` (public API and data format decisions), `process` (project process decisions).
