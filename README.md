# ITSD Card Game 2025-26

A simplified single-player browser-based tactical card game inspired by **Duelyst**, built as part of the IT+ Masters Team Project (COMPSCI5074M) at the University of Glasgow.

The human player (Player 1) competes against an AI opponent (Player 2) on a 9×5 grid board, using a deck of cards to summon units, cast spells, and defeat the enemy avatar.

---

## Team Members

| Name | GitLab Username |
|------|-----------------|
| Minghao | minghao |
| Scott Vint | ScottVint |
| *(add team member)* | |
| *(add team member)* | |

---

## Requirements

| Tool | Version |
|------|---------|
| Java JDK | 11 or higher |
| Scala / SBT | 2.13.1 / 1.x |
| Play Framework | 2.8.x (bundled via SBT) |
| A modern web browser | Chrome / Firefox recommended |

---

## How to Run

> **Java 21 users:** The project includes a `.sbtopts` file that automatically passes the required `--add-opens` flags to the JVM. No additional configuration is needed.

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd duelyst-card-project
   ```

2. **Place frontend assets** — copy `public/js/` and `public/css/` into the `public/` directory (gitignored; obtain from the project template assets package).

3. **Start the server**
   ```bash
   ./sbt run
   ```
   The first run will download all dependencies automatically. This may take a few minutes.

4. **Open the game in your browser**
   ```
   http://localhost:9000/game
   ```

5. **Run tests**
   ```bash
   ./sbt test
   ```

---

## Project Structure

```
duelyst-card-project/
├── app/
│   ├── actors/
│   │   └── GameActor.java          # Akka actor: receives events, dispatches to handlers
│   ├── commands/
│   │   └── BasicCommands.java      # Front-end API: send render commands over WebSocket
│   ├── controllers/
│   │   └── GameScreenController.java
│   ├── events/                     # Event handlers (one class per event type)
│   │   ├── Initalize.java          # Game setup: avatars, decks, initial hand (#18)
│   │   ├── TileClicked.java        # Tile click: unit selection & movement highlight (#3)
│   │   ├── CardClicked.java        # Card click: card selection
│   │   ├── EndTurnClicked.java     # End turn button
│   │   ├── Heartbeat.java          # Game loop heartbeat (~1s interval)
│   │   └── OtherClicked.java       # Click outside interactive areas
│   ├── structures/
│   │   ├── GameState.java          # Central game state (board, players, selected unit)
│   │   └── basic/
│   │       ├── Player.java         # Player: health, mana, deck, hand, avatar
│   │       ├── Unit.java           # Unit: id, HP, attack, position, owner
│   │       ├── Board.java              # 9x5 grid of Tile objects
│   │       ├── Tile.java           # Board tile: grid position, occupying unit
│   │       └── Card.java           # Card: id, name, mana cost
│   └── utils/
│       ├── BasicObjectBuilders.java  # Factory: load Unit/Card/Tile from config files
│       ├── OrderedCardLoader.java    # Load player decks from JSON configs
│       └── StaticConfFiles.java      # Constants for config file paths
├── conf/
│   ├── gameconfs/
│   │   ├── avatars/                # Avatar unit configs (avatar1.json, avatar2.json)
│   │   ├── cards/                  # Card configs for both decks (10 unique cards each)
│   │   ├── units/                  # Unit sprite and animation configs
│   │   └── effects/                # Visual effect animation configs
│   └── routes                      # Play Framework URL routing
├── build.sbt                       # SBT build configuration
└── README.md
```

---

## Game Rules Summary

- **Board**: 9 columns x 5 rows. Both players have an avatar unit placed on the board.
- **Avatars**: Player 1 starts at tile [2,3]; Player 2 starts at tile [8,3]. Each avatar has 20 HP.
- **Turn order**: Draw a card at end of turn -> spend mana to play cards / move / attack -> end turn.
- **Mana**: Starts at 2 on Player 1's first turn; increases by 1 each turn (max 9). All unspent mana is lost at end of turn.
- **Movement**: Up to 2 tiles in cardinal directions, or exactly 1 tile diagonally per turn. Cannot move through other units.
- **Attack**: Adjacent tiles only (including diagonals). Surviving defenders counter-attack.
- **Summoning**: Unit cards must be placed on a tile adjacent to a friendly unit already on the board.
- **Win condition**: Reduce the enemy avatar to 0 HP.

---

## Implemented Story Cards

| # | Story Card | Status | Author |
|---|------------|--------|--------|
| #3 | Unit Selection & Movement Highlighting | ✅ Done | Minghao |
| #6 | Mana Display & Replenishment | ✅ Done | Minghao |
| #12 | Cancel Selection | ✅ Done | Minghao |
| #18 | Game Initialization & Setup | ✅ Done | Minghao |
| #19 | Mana Increase | ✅ Done | Minghao |
| #22 | Summon Tile Highlighting | ✅ Done | Minghao |

## Test Coverage

| Test File | Story Card | Tests |
|-----------|------------|-------|
| `InitalizationTest` | #18 | 1 |
| `TileClickedTest` | #3 | 8 |
| `EndTurnClickedTest` | #6 | 5 |
| `CardClickedTest` | #22 | 5 |
| `OtherClickedTest` | #12 | 5 |
| `ManaIncreaseTest` | #19 | 5 |
| **Total** | | **29 — all passing** |

---

## Decks

### Player 1 — Abyssian Swarm
Theme: **Deathwatch** synergies with Wraithling (1/1) token generation.

Key cards: Bad Omen, Gloom Chaser, Shadow Watcher, Bloodmoon Priestess, Shadowdancer,
Wraithling Swarm, Dark Terminus.

### Player 2 (AI) — Lyonar Generalist
Theme: High-statted creatures with **Provoke**, **Rush**, **Flying**, and **Zeal**.

Key cards: Swamp Entangler, Silverguard Knight, Saberspine Tiger, Young Flamewing,
Ironcliffe Guardian, Beam Shock, True Strike.

---

## Architecture Notes

- Communication between the Java back-end and the browser front-end is **asynchronous** over
  WebSocket. Never block the event thread waiting for a front-end response.
- All game state is held in `GameState`, which is passed into every event handler.
- Use `BasicCommands` to send render commands to the front-end; use `BasicObjectBuilders`
  to construct game objects from their JSON config files.
- The card and unit configs in `conf/gameconfs/` are read-only template files and must not
  be modified.

---

## License

Duelyst assets used in this project entered the public domain in January 2023.
Template project provided by Dr. Richard McCreadie, University of Glasgow.