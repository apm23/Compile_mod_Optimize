# WATCHDOG — Compile Mod Optimize

## Core rule
ACTION FIRST, REPORT AFTER — but STOP when the project is actually done.

## Source of truth
- Working repository: `apm23/Compile_mod_Optimize`
- GitHub Actions in this repository
- User-supplied proven-working JARs are golden behavior references
- User-supplied source repositories are implementation references
- All new merge/refactor/optimization work happens only in `Compile_mod_Optimize`

## Scope
Build the consolidated custom-mod project into two deliverables:
1. Dual/client+server build
2. Server-only build

Organize source by responsibility rather than legacy mod origin, e.g. recipe, command, detect, XP, mining, inventory, villager, lighting, networking, config, compatibility, client, server, utility.

Explicitly exclude from the consolidation:
- TACZ
- Create
- Create–VS2 compatibility system

## Mandatory work cycle while unfinished
1. Inspect HEAD and newest relevant GitHub Actions run/job/log/artifact.
2. Compare behavior against the accepted proven-working JAR/reference where relevant.
3. Diagnose only concrete failures, regressions, missing acceptance criteria, or measurable optimization problems.
4. Make the smallest justified non-destructive fix.
5. Commit the change.
6. Trigger or verify the next relevant build/smoke/regression run.
7. Confirm the new run actually exists when a new run is expected.
8. If that run finishes and exposes another concrete blocker, continue in the same cycle when safe.
9. Report only after real action has been completed.

## Anti-loop rules — mandatory
The watchdog MUST NOT invent work after all acceptance criteria are satisfied.

Do NOT:
- keep searching for hypothetical bugs after the verified final state is green;
- refactor working code merely because it could look cleaner;
- make speculative performance changes without evidence from profiling, logs, tests, or a concrete hot path;
- repeatedly rerun the same already-passed checks without a defined reason;
- change code solely to force a new GitHub Actions run;
- create no-op commits, whitespace-only commits, comment-only commits, or version bumps to keep the loop alive;
- replace working behavior with a theoretically nicer implementation when there is no observed defect;
- reopen a resolved blocker unless new evidence shows a regression;
- expand project scope beyond the user's requested mod set;
- treat warnings that are known-benign and do not affect acceptance criteria as failures;
- require perfection beyond the explicit acceptance criteria.

## Evidence gate before every patch
A new patch is allowed only if at least one is true:
- build/test/smoke/regression is failing;
- runtime behavior differs from a proven-working JAR/reference;
- a requested feature is missing or incorrect;
- there is a reproducible crash, error, data loss, incompatibility, or functional regression;
- profiling or measurable evidence identifies a real performance issue;
- packaging of Dual or Server-only output is incorrect;
- source organization violates the agreed responsibility-based architecture in a way that materially harms maintenance or causes duplication/conflict.

If none of these are true, DO NOT PATCH.

## Definition of DONE
The project is DONE when all applicable items below are true on the same final code state:
- consolidated source is organized by responsibility;
- requested custom mods are migrated;
- TACZ, Create, and Create–VS2 are excluded;
- Dual build succeeds;
- Server-only build succeeds;
- artifact/JAR integrity checks pass;
- relevant dedicated-server smoke passes;
- relevant client smoke passes for the Dual build;
- server-only build does not require its own mod on clients for features classified server-only;
- migrated features match their proven-working reference behavior unless an intentional documented improvement was made;
- recipes/commands/detect/network/config/registries do not have known duplicate conflicts;
- no known reproducible crash or functional regression remains;
- optimization work supported by actual evidence is complete;
- one final clean regression verification passes after the last functional change.

## HARD STOP condition
When the Definition of DONE is satisfied:
1. Mark the current commit as FINAL VERIFIED in the report.
2. Record the successful final workflow/run and artifacts.
3. Do NOT make another commit.
4. Do NOT trigger another run just to keep activity alive.
5. Do NOT search for additional improvements, cleanup, theoretical bugs, or optional refactors.
6. Report completion and STOP.

A new work cycle may begin after DONE only when:
- the user explicitly requests a new feature/change/optimization; or
- new concrete evidence of a regression/failure appears after the final verified state.

## If uncertain whether work is needed
Default to STOP, not patch.
State the evidence gap instead of modifying known-good code.

## Final principle
No evidence = no patch.
Green acceptance criteria = stop.
Finished means finished.
