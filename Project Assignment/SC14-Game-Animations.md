# Story Card #14 — Game Animations

> **Replaces** the original SC#14 "AI Attacking" (which duplicated SC#11 and has been absorbed into SC#11/SC#15 implementation).

---

## User Story

As a player, I want actions on the board to be accompanied by appropriate animations, so that I can clearly follow what is happening during play.

**Priority:** 6
**SP:** 3

---

## Scope

This story card covers all missing animation calls identified in the codebase:

| Gap | Location | Fix |
|-----|----------|-----|
| Walk animation not played on move | `BoardLogic.moveSelectedUnit` | Call `playUnitAnimation(move)` before `moveUnitToTile` |
| Idle not restored after actions | `UnitStopped`, `CombatLogic` | Call `playUnitAnimation(idle)` after move/attack completes |
| No summon effect on spawn | `GameState.placeUnit` / summon path | Call `playEffectAnimation(f1_summon)` on the destination tile |
| No spell effect animations | All `Spell.cast()` implementations | Call `playEffectAnimation` with the appropriate effect per spell type |
| No channel animation on spellcaster | All `Spell.cast()` implementations | Call `playUnitAnimation(channel)` on the casting unit before effect |

### Effect animation mapping

| Spell | Effect file | Reason |
|-------|-------------|--------|
| Truestrike | `f1_inmolation` | Direct damage |
| Beamshock | `f1_inmolation` | Stun / damage |
| Sundrop Elixir | `f1_buff` | Heal / buff |
| Dark Terminus | `f1_martyrdom` | Destroy unit |
| Horn of the Forsaken | `f1_buff` | Equip / buff |
| Wraithling Swarm | `f1_summon` | Summons units |

---

## Acceptance Tests

- When a unit moves to a new tile, the walk (`move`) animation plays on that unit for the duration of the movement.
- When a unit finishes any action (move, attack, or spell cast), it returns to the `idle` animation.
- When a unit is summoned to the board, the summon effect animation (`f1_summon`) plays on the tile where it appears.
- When a damage spell (Truestrike, Beamshock) is cast on a target, a damage effect animation (`f1_inmolation`) plays on the target tile.
- When a heal spell (Sundrop Elixir) is cast, a buff effect animation (`f1_buff`) plays on the target tile.
- When a destroy spell (Dark Terminus) is cast, a martyrdom effect animation (`f1_martyrdom`) plays on the target tile.
- When a unit card spell (Wraithling Swarm, Horn of the Forsaken) is cast, a buff/summon effect animation plays on the target tile.
- When a unit casts a spell, the `channel` animation plays on the casting unit before the effect resolves.

---

## Implementation Notes

- `BasicCommands.playUnitAnimation(out, unit, type)` returns the animation duration in ms; use `Thread.sleep(duration)` where sequencing matters (e.g. channel before effect).
- `BasicCommands.playEffectAnimation(out, effect, tile)` likewise returns duration in ms.
- Load effects via `BasicObjectBuilders.loadEffect(StaticConfFiles.f1_xxx)`.
- The `idle` reset after movement should be placed in `UnitStopped.processEvent`, after the unit's board position is updated.
- The `idle` reset after attack should be placed in `CombatLogic.resolveCombat`, after all damage and counterattack logic completes.
