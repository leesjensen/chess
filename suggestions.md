# Code Review: `pieceMoves` Implementation

**Prompt:**
> Review the code in this project. Look for how pieceMoves is    
implemented and give suggestions on how industry software      
engineering principles could be applied to improve the code.   
Do not make any changes to the code. Instead create a document
named suggestions.md.

This review covers how move calculation is implemented in `shared/src/main/java/chess/ChessPiece.java`
(`pieceMoves`, `calcMoves`, `calcPawnMoves`, and helpers), plus the closely related classes
`ChessPosition`, `ChessBoard`, and `Bishop`. No code was changed; everything below is a suggestion.

## Summary

The implementation is compact and gets a lot right. The data-driven `MoveRule` table means king, queen,
rook, bishop, and knight share a single algorithm, and that is the most important design decision in
this code. The suggestions below are mostly about making that idea cleaner, safer, and easier to extend.

| # | Suggestion | Principle | Priority |
|---|------------|-----------|----------|
| 1 | Use `this` instead of re-reading the piece from the board | Single source of truth, fail fast | High |
| 2 | Make the move-rule table a `static final` constant | Avoid needless work, immutability | Medium |
| 3 | Replace recursion with a simple loop for sliding pieces | KISS | Medium |
| 4 | Remove duplication in the direction tables and pawn captures | DRY | Medium |
| 5 | Move `inBounds` / offset logic into `ChessPosition` | Information Expert, cohesion | Medium |
| 6 | Replace `int[][]` with a small value type | Avoid primitive obsession | Low |
| 7 | Name the magic numbers | Readability | Low |
| 8 | Decide on one polymorphism strategy (`Bishop` subclass vs. type enum) | Open/Closed, Strategy pattern | Medium |
| 9 | Clarify naming | Self-documenting code | Low |
| 10 | Add your own focused unit tests | Test-driven development | Medium |
| 11 | Related bug in `ChessBoard.resetBoard` | Correctness | High |

---

## 1. Use `this` instead of re-reading the piece from the board

```java
public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
    var piece = board.getPiece(myPosition);
    var moves = new ArrayList<ChessMove>();
    if (piece.type.equals(this.type) && piece.pieceColor.equals(this.pieceColor)) {
```

`pieceMoves` is an instance method, so the piece being moved is already `this`. Reading it back off the
board creates two sources of truth, and that causes two problems:

- **Hidden failure mode:** If `myPosition` is empty, `piece` is `null` and the method throws a
  `NullPointerException`.
- **Silent failure:** If the square holds a different piece, the method quietly returns an empty list.
  That makes a caller's bug look like "this piece has no moves", which is hard to debug.

**Suggestion:** Remove the lookup and the `if` and use `this.type` / `this.pieceColor` directly. If you
want a sanity check, *fail fast* with a clear message (for example,
`throw new IllegalArgumentException(...)`). Don't silently return nothing.

## 2. Make the move-rule table a `static final` constant

```java
var moveMap = Map.of(
        PieceType.KING, new MoveRule(true, new int[][]{...}),
        ...
);
```

Every call to `pieceMoves` allocates a new map, five records, and every direction array. Later,
`ChessGame.validMoves` / `isInCheck` / `isInCheckmate` will call `pieceMoves` many times per turn, so
this repeated allocation will add up.

**Suggestion:** Lift the table to a `private static final Map<PieceType, MoveRule> MOVE_RULES`. An
`EnumMap` fits well when the keys are enum values. This also documents that the rules are fixed
configuration, not per-call state.

## 3. Replace recursion with a loop for sliding pieces

```java
if (!rule.single) {
    calcMoves(board, startPos, newPos, new MoveRule(false, new int[][]{{direction[0], direction[1]}}), moves);
}
```

This recursion works, but it is harder to follow than it needs to be. Each step builds a new
`MoveRule` with a single-element direction array just to continue in the same direction, and the
`startPos` / `pos` parameter pair exists only to support the recursion.

**Suggestion (KISS):** An iterative "walk until blocked" loop states the intent directly:

```java
for (var dir : rule.directions()) {
    var pos = start.offset(dir);
    while (pos.isOnBoard()) {
        var occupant = board.getPiece(pos);
        if (occupant == null) {
            moves.add(new ChessMove(start, pos, null));
        } else {
            if (occupant.getTeamColor() != pieceColor) {
                moves.add(new ChessMove(start, pos, null));
            }
            break;
        }
        if (!rule.slides()) break;
        pos = pos.offset(dir);
    }
}
```

## 4. Remove duplication (DRY)

**Direction tables.** The queen's directions are copied from the king's, and they are also the
rook's and bishop's directions combined. If you define each set once and compose the others from them,
a typo can only happen in one place:

```java
private static final Direction[] ORTHOGONAL = {...};
private static final Direction[] DIAGONAL   = {...};
private static final Direction[] ALL_DIRS   = concat(ORTHOGONAL, DIAGONAL);
```

**Pawn captures.** The left and right diagonal capture blocks in `calcPawnMoves` are identical except
for `-1` / `+1`. Loop over the column offsets instead:

```java
for (int dCol : new int[]{-1, 1}) { ... }
```

**Promotion pieces.** `addPromotions` hard-codes four `moves.add(...)` calls. A constant such as
`List.of(QUEEN, ROOK, BISHOP, KNIGHT)` with a loop keeps the rule in one place.

## 5. Put position logic in `ChessPosition` (Information Expert / cohesion)

`inBounds` and the "add a row/column offset" arithmetic live in `ChessPiece`, but they describe
positions, not pieces. This is a mild case of *feature envy*: `ChessPiece` keeps reaching into
`ChessPosition` to do position math. Move that logic to the class that owns the data:

```java
// ChessPosition
public boolean isOnBoard() { return row >= 1 && row <= 8 && col >= 1 && col <= 8; }
public ChessPosition offset(int dRow, int dCol) { return new ChessPosition(row + dRow, col + dCol); }
```

`ChessBoard` could also expose a bounds-safe accessor. Then `getPieceAt` would no longer need to exist
in `ChessPiece`.

Because of this split, the pawn code checks bounds redundantly and in an awkward order:

```java
pieceAtPos = getPieceAt(board, endPos);          // already checks inBounds
if ((pieceAtPos != null) && ... && (inBounds(endPos))) {   // checks it again
```

Once bounds checking lives in one obvious place, you can drop these duplicate checks.

## 6. Avoid primitive obsession: replace `int[][]`

`int[][]{{0, 1}, {1, 0}, ...}` gives no hint which number is the row and which is the column, and
`direction[0]` / `direction[1]` are easy to swap by mistake. Records that hold arrays also have two
known pitfalls: they compare arrays by reference in `equals`, and they expose mutable internals.

**Suggestion:** Use a tiny value type:

```java
record Direction(int dRow, int dCol) {}
```

Then store directions as `List<Direction>`, which is immutable, or as a `Direction[]` that stays private.

## 7. Name the magic numbers

Several literals encode chess rules without saying so:

- `startPos.getRow() == 2` / `== 7` → the pawn's starting rank
- `pos.getRow() == 8` / `== 1` → the promotion rank
- `8` in `inBounds` → the board size
- `direction == 1` / `-1` → the forward direction for a color

**Suggestion:** Derive these from the team color in one place. For example, add small helpers such as
`forwardDirection()`, `startRow()`, and `promotionRow()`, or add fields to the `TeamColor` enum.
`isPromoting` would then no longer need the `direction` parameter. It only has to ask whether the end row
is this color's promotion row.

## 8. Choose one polymorphism strategy (Open/Closed, Strategy pattern)

The code currently mixes two approaches:

- `pieceMoves` branches on `PieceType` (the pawn `if`, then the map lookup).
- A `Bishop extends ChessPiece` subclass exists but adds no behavior. Its constructor still takes a
  `PieceType`, so `new Bishop(WHITE, PieceType.ROOK)` is legal, which is an inconsistent state.

Either approach can work, but pick one:

- **Option A: data-driven with the Strategy pattern (recommended here).** Keep `ChessPiece` as the
  single class. Define a `PieceMovesCalculator` interface with one implementation per movement style,
  for example `SlidingMoveCalculator(directions)`, `SteppingMoveCalculator(offsets)`, and
  `PawnMoveCalculator`. Keep them in an `EnumMap<PieceType, PieceMovesCalculator>`. `pieceMoves` then
  becomes a one-line delegation, and a new movement rule (such as castling) means adding a strategy
  instead of editing an `if` chain. Delete `Bishop`.
- **Option B: subclass per piece.** Make `pieceMoves` abstract or overridable and give each subclass
  its own implementation. If you choose this, remove the `type` parameter from subclass constructors
  (`super(color, PieceType.BISHOP)`). Note that the tests and `ChessBoard.resetBoard` create pieces
  with `new ChessPiece(color, type)`, so you would also need a factory to make this work.

Option A matches the passoff test harness, so it is the lower-friction choice. It also builds on the
data-driven idea you already have.

## 9. Naming

| Current | Suggested | Why |
|---------|-----------|-----|
| `MoveRule.single` | `slides` (inverted) or `repeats` | Says what the piece does, not what it doesn't |
| `calcMoves` | `addSlidingOrSteppingMoves` / `collectMoves` | Says it appends to a list |
| `endPos2` | `doubleStepPos` | Describes meaning |
| `pieceAtPos` | `occupant` | Shorter and clearer |

Also, the helpers use a *collecting parameter* (`List<ChessMove> moves`) that they mutate. That is a
legitimate pattern, but be consistent with it. Either use it everywhere and name the methods
`add...`/`collect...`, or have each helper return its own collection.

## 10. Add your own unit tests

The passoff tests check full move sets on a handful of boards. Small tests of your own would pin down
edge cases and let you refactor safely, which matters before you attempt items 1–8. Examples:

- Calling `pieceMoves` on an empty square, or with a mismatched piece (item 1)
- A pawn on the second-to-last rank that is blocked forward but can capture diagonally into promotion
- A pawn double step blocked on the *first* square vs. the *second* square
- A knight in a corner, and a king on an edge (bounds handling)
- Direct tests for `ChessPosition.isOnBoard` / `offset` once you add them

Refactor in small steps. Change one thing, run the tests, commit, and repeat.

## 11. Related bug: `ChessBoard.resetBoard`

This is outside `pieceMoves`, but you'll hit it soon:

```java
for (var r = 0; r < 8; r++) {
    for (var c = 0; r < 8; r++) {   // condition and increment use r, not c
        board[r][c] = null;
    }
}
```

The inner loop tests and increments `r` instead of `c`. As written, only column 0 is cleared, so a
board that is reused and then reset will keep leftover pieces in the middle ranks. The fix is
`c < 8; c++`. Simpler still, allocate a fresh array or use `Arrays.fill` on each row. Also,
`ChessGame.TeamColor.BLACK` on line 56 could just be `BLACK` because of the static import, which would
match the lines around it.

---

## Suggested order of work

1. Fix `resetBoard` (item 11) and the `this` vs. board lookup (item 1). Both are correctness issues.
2. Add targeted unit tests (item 10).
3. Move position logic into `ChessPosition` (item 5) and introduce `Direction` (item 6).
4. Make the rules table static and loop-based (items 2 and 3), and remove duplication (item 4).
5. Settle the design question (item 8), then clean up names and constants (items 7 and 9).
