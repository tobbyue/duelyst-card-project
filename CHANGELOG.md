# Changelog

All notable changes to this project will be documented in this file.

---

## [Unreleased] — 2026-03-04

### Added

#### `app/events/OtherClicked.java` — Story Card #12 (Cancel Selection)
- Fully implemented from empty stub: clicking a non-interactive area clears `selectedUnit`, `selectedHandPosition`, and sends mode-0 resets for every board tile.
- Author: Minghao

#### `app/events/CardClicked.java` — Story Card #12 (Card Toggle Deselect)
- Added toggle behaviour: if the player clicks a card that is already selected, it is immediately deselected (`selectedHandPosition` → −1) and all highlights are cleared — without re-applying new ones.
- Author: Minghao

#### `test/OtherClickedTest.java` *(new file)* — 5 tests for Story Card #12
- `backgroundClickDeselectsUnit` — `selectedUnit` is null after `otherClicked`.
- `backgroundClickDeselectsCard` — `selectedHandPosition` is −1 after `otherClicked`.
- `backgroundClickClearsAllHighlights` — mode-0 drawTile sent for all 45 board tiles.
- `reclickingSameCardDeselectsIt` — clicking the same card position twice sets `selectedHandPosition` to −1.
- `reclickingSameCardClearsHighlights` — no green (mode 2) highlights remain after card is deselected.
- Author: Minghao

#### `test/ManaIncreaseTest.java` *(new file)* — 5 tests for Story Card #19
- `player1StartsWithMana2` — P1 mana equals 2 at game start.
- `manaIncreasesEachTurn` — mana grows by 1 each turn for both players.
- `manaFilledToMaxAtTurnStart` — mana is always replenished to the full turn maximum, regardless of spending.
- `manaIsCappedAt9` — mana never exceeds 9 even after many turns.
- `manaDisplayUpdatedAtTurnStart` — front-end receives correct `setPlayer1Mana` / `setPlayer2Mana` values each turn.
- Author: Minghao

---

### Story Cards Addressed

| # | Title | Changes |
|---|-------|---------|
| #12 | Cancel Selection | Full implementation in `OtherClicked`; toggle deselect added to `CardClicked` |
| #19 | Mana Increase | Covered by SC#6 implementation; explicit test suite added |

---

## [Unreleased] — 2026-03-03

### Added

#### `app/events/EndTurnClicked.java` — Story Card #6 (Mana Replenishment)
- Implemented turn-end mana logic: when a player ends their turn, the opposing player's mana is replenished to `min(N + 1, 9)`, where N is that player's turn count (capped at 9 to match standard Duelyst rules).
- Clears any active unit/card selection (`selectedUnit`, `selectedHandPosition`) at the start of each new turn.
- Clears all board highlights (mode 0) left over from the previous turn.
- Front-end mana display updated immediately via `BasicCommands.setPlayer1Mana` / `setPlayer2Mana`.
- Author: Minghao

#### `app/events/CardClicked.java` — Story Card #22 (Summon Tile Highlighting)
- Fully implemented: selecting a creature card from Player 1's hand highlights all empty board tiles adjacent (8-directional, 1 step) to any friendly unit already on the board in green (mode 2).
- Occupied adjacent tiles are never highlighted as valid summon destinations.
- Clears all previous highlights before applying new ones, preventing stale green tiles from prior selections.
- Spell (non-creature) cards do not trigger any summon highlights.
- Author: Minghao

#### `test/EndTurnClickedTest.java` *(new file)* — 5 tests for Story Card #6
- `endingPlayer1TurnReplenishesPlayer2Mana` — P2 receives mana = 2 on their first turn.
- `endingPlayer2TurnReplenishesPlayer1Mana` — P1 receives mana = 3 on their second turn.
- `manaIsNeverMoreThanNine` — mana is capped at 9 regardless of turn count.
- `endTurnClearsSelectedUnit` — `selectedUnit` is null after end turn.
- `endTurnClearsSelectedHandPosition` — `selectedHandPosition` is −1 after end turn.
- Author: Minghao

#### `test/CardClickedTest.java` *(new file)* — 5 tests for Story Card #22
- `selectingCreatureCardStoresHandPosition` — GameState records the selected hand position.
- `selectingCreatureCardHighlightsAllAdjacentEmptyTiles` — all 8 neighbours of the P1 avatar at [2,3] are highlighted in green.
- `occupiedAdjacentTileIsNotHighlighted` — a tile occupied by any unit is excluded from highlights.
- `spellCardDoesNotHighlightSummonTiles` — spell cards produce no green (mode 2) highlights.
- `clickingNewCardClearsPreviousHighlights` — switching card selection sends mode-0 resets before new highlights.
- Author: Minghao

---

### Changed

#### `app/structures/GameState.java`
- Added `boolean player1Turn = true` to track whose turn it is.
- Added `int player1TurnCount = 1` and `int player2TurnCount = 0` to calculate per-turn mana.
- Added `int selectedHandPosition = -1` to track the card currently selected in the hand (−1 = none).
- Added matching getters (`isPlayer1Turn`, `getPlayer1TurnCount`, `getPlayer2TurnCount`, `getSelectedHandPosition`), setters, and `incrementPlayer1TurnCount` / `incrementPlayer2TurnCount` helpers.
- Author: Minghao

#### `app/events/Initalize.java`
- Added `player1.setMana(2)` and `BasicCommands.setPlayer1Mana(out, player1)` to set Player 1's starting mana for turn 1 (Story Card #6).
- Author: Minghao

#### `build.sbt`
- Added `javaOptions` with four `--add-opens` flags and `fork := true` so the forked test JVM can run Guice/CGLIB under Java 21 without `InaccessibleObjectException`.

#### `.gitignore`
- Changed `public/**` to `public/images/**` so that `public/js/` and `public/css/` can be tracked by git.

---

### Build / Infrastructure

#### `.sbtopts` *(new file)*
- Passes four `-J--add-opens` flags directly to the sbt launcher JVM, fixing the `CodeGenerationException: InaccessibleObjectException` (`ClassLoader.defineClass`) crash that occurred when running `sbt run` under Java 21 with Play 2.8's bundled Guice/CGLIB.

---

### Story Cards Addressed

| # | Title | Changes |
|---|-------|---------|
| #6 | Mana Display & Replenishment | Full implementation in `EndTurnClicked`; initial mana set in `Initalize` |
| #22 | Summon Tile Highlighting | Full implementation in `CardClicked`; green highlights for adjacent empty tiles |

---

## [Unreleased] — 2026-02-27

### Added

#### `app/structures/Board.java` *(new file)*
- Introduced `Board` class representing the 9×5 game grid.
- Holds a 2D array of `Tile` objects, initialised via `BasicObjectBuilders.loadTile(x, y)`.
- Exposes `getTile(int x, int y)` for safe tile access and `getX()` / `getY()` for board dimensions.
- Author: Minghao

#### `app/events/TileClicked.java` — `clearAllHighlights()`
- Added helper method that resets every tile on the board to mode 0 (no highlight).
- Called before applying any new highlights to prevent stale white/red tiles remaining from a previous selection.
- Author: Minghao

---

### Changed

#### `app/structures/GameState.java`
- Added `Board board` field and initialised it in the constructor.
- Added `Player player1` and `Player player2` fields; both are constructed and given their avatars and decks on startup via `BasicObjectBuilders` and `OrderedCardLoader`.
- Added `Unit selectedUnit` field with `getSelectedUnit()` / `setSelectedUnit()` accessors to track the currently selected unit across events.
- Author: Minghao

#### `app/structures/basic/Player.java`
- Extended base class with `Unit avatar`, `List<Card> deck`, and `List<Card> hand` fields.
- Added `getAvatar()` / `setAvatar()`, `getDeck()` / `setDeck()`, `getHand()` accessors.
- Added `drawCard()`: moves the top card from the deck into the hand; no-ops if the deck is empty.
- Author: Minghao

#### `app/structures/basic/Unit.java`
- Extended base class with `int health`, `int maxHealth`, `int attack`, and `Player owner` fields.
- Added full getters and setters for all new fields.
- `owner` is annotated `@JsonIgnore` to avoid circular serialisation.
- Author: Minghao

#### `app/structures/basic/Tile.java`
- Extended base class with `Unit unit` field (annotated `@JsonIgnore`) to track occupancy.
- Added `getUnit()` / `setUnit()` to place or remove a unit on the tile.
- Author: Minghao

#### `app/events/Initalize.java`
- Added `avatar1.setOwner(player1)` and `avatar2.setOwner(player2)` immediately after avatar retrieval.
  - **Bug fix**: without ownership being set, `TileClicked` could never match a clicked unit to the human player, making unit selection non-functional (Story Card #3).
- Replaced commented-out `BasicCommands.drawHand()` (which does not exist) with a loop over `player1.getHand()` calling `BasicCommands.drawCard(out, card, position, 0)` for each card.
  - Ensures Player 1's 3 starting cards are rendered in the hand on game load (Story Card #18).
- Added imports: `structures.basic.Card`, `java.util.List`.
- Updated Javadoc to reference Story Card #18.
- Author: Minghao

#### `app/events/TileClicked.java`
- **Bug fix — movement range calculation**: replaced incorrect Chebyshev distance formula (`Math.max(|dx|, |dy|) <= 2`) with the correct two-part logic:
  - *Cardinal*: up to 2 steps along each of the four axis-aligned directions.
  - *Diagonal*: exactly 1 step in each of the four diagonal directions.
  - The old formula incorrectly highlighted tiles such as `(±2, ±1)`, `(±1, ±2)`, and `(±2, ±2)` which are not reachable under the game rules.
- **Bug fix — path blocking**: cardinal movement now stops (via `break`) as soon as an occupied tile is encountered; units can no longer highlight tiles behind a blocking unit.
- **Bug fix — stale highlights**: `clearAllHighlights()` is now called at the start of each new selection, preventing highlight build-up across multiple clicks.
- Updated Javadoc on `highlightMovementRange()` to document movement rules and path-blocking behaviour.
- Author: Minghao

#### `README.md`
- Replaced placeholder title with full project documentation.
- Added: team member table, requirements, run instructions, project structure, game rules summary, implemented story cards table, deck descriptions, architecture notes, and license section.
- Author: Minghao

---

### Story Cards Addressed

| # | Title | Changes |
|---|-------|---------|
| #3 | Unit Selection & Highlighting | Fixed owner assignment, movement range formula, path blocking, and highlight clearing |
| #18 | Game Initialization & Setup | Fixed missing avatar ownership; replaced broken hand-render call with correct `drawCard` loop |
