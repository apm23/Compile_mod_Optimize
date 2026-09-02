# Consolidation migration status

This ledger prevents accidental behavior drift while the proven-working mods are consolidated by responsibility.

## Server-only bundle

| Module | Golden reference | Source reference | Final location | Status |
|---|---|---|---|---|
| XP / Global Mending + optional TACZ kill XP multiplier | `exp-system-0.1.0 (2).jar` supplied by user | `apm23/Exp_System` | `apm23.compilemod.server.xp` | Verified baseline: build + GameTest + dedicated-server smoke green on run #5 |
| Custom Pickaxe | `custom-pickaxe-0.1.0 (7).jar` supplied by user | `apm23/Custom_Pickaxe` | `src/main/java/apm23/compilemod/server/mining` (legacy Java package retained during golden migration) | Verified baseline: build + JUnit + GameTest + dedicated-server smoke green on run #6 |

Custom Pickaxe recipe JSONs are preserved byte-for-byte under their original data namespace so recipe IDs and hidden custom-data signatures do not drift during consolidation. Java source is physically grouped under the final `server/mining` responsibility directory while retaining the proven class package for the first regression gate.

`create-lava-source` is **not** included in the consolidated server-only bundle because its current source directly targets Create classes and contains client-side Create mixins. Create itself is explicitly excluded from consolidation.

## Dual bundle

| Module | Golden reference | Source reference | Final location | Status |
|---|---|---|---|---|
| Linked Shulker | `linked-shulker-0.1.3-animated+mc26.2(1).jar` | `apm23/linked-shulker` | `dual/src/main/java/apm23/compilemod/dual/storage/linkedshulker` | Golden core + assets/models migrated; build + dual artifact green on run #10 |
| Fuel / Recipe | `fuel-recipe-0.1.0+mc26.2(1).jar` | `apm23/fuel-recipe` | `dual/src/main/java/apm23/compilemod/dual/recipe/fuel` + `data/fuelrecipe/recipe` | Golden JAR baseline migrated; build + dual artifact green on run #8. Lighting subsystem from newer source HEAD intentionally excluded because it is absent from golden JAR. |
| Force Anvil | `force-anvil-0.1.0-alpha.9+mc26.2(1).jar` | `apm23/force-anvil` | `dual/src/main/java/apm23/compilemod/dual/functional/forceanvil` + `data/forceanvil/recipe` | Golden core/mixins/recipes/assets migrated; build + regression + smoke + dual artifact green on run #11 |
| Custom Hotbar Inventory | `custom-hotbar-inventory-1.0.0-tacz-workbench-multipage-fix-mc26.2(1).jar` | `apm23/custom-hotbar-inventory` | TBD by responsibility | Pending |
| God Villager | `god-villagers-0.1.0-alpha.85+mc26.2 (4).jar` | `apm23/god-villager` | `dual/src/main/resources/data/godvillagers/recipe` for data-driven recipes; Java core TBD | Golden data-driven recipes migrated byte-for-byte; Java core pending because source HEAD is an optimizer overlay rather than the complete golden source, and golden JAR also carries optional TACZ compatibility which must not bundle TACZ |

The root project produces `compile-mod-server`; the `dual` subproject produces `compile-mod-dual`. Root `build` depends on `:dual:build` so CI cannot report green unless both distributions compile. CI uploads both artifacts separately.

## Explicitly excluded from consolidation

- TACZ itself and TACZ AmmoBox Tweak
- Create
- Create–VS2 compatibility system

Optional compatibility logic may exist only when it does not bundle the excluded mod and is required to preserve proven behavior.

## Migration rule

Golden JAR behavior wins over a newer source HEAD when they disagree. Source repositories are implementation references. Every migration must pass compile/regression/smoke evidence before its status can become verified.
