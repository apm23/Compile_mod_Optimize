# Compile Mod Optimize

Final consolidation workspace for custom Minecraft mods.

## Build targets

- Dual: client + server installation required.
- Server: server-side-only compatible modules.

## Consolidation rules

- Refactor by responsibility instead of preserving old mod boundaries.
- Centralize recipes, commands, detectors, events, networking, configuration, and other shared systems.
- Treat proven-working JARs as runtime behavior references and compare them against their source repositories before migration.
- Optimize only after preserving behavior and establishing regression coverage.
- Keep maintenance boundaries explicit so failures can be isolated by feature directory.

## Excluded from this consolidation

- TACZ-related project code.
- Create-related project code.
- Create / Valkyrien Skies 2 compatibility system.

## Source of truth

All consolidation development, CI, smoke/regression work, and final artifacts live in this repository.
