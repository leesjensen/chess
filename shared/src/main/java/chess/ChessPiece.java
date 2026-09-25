package chess;

import java.util.*;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {

    private final ChessGame.TeamColor pieceColor;
    private final PieceType type;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.pieceColor = pieceColor;
        this.type = type;
    }

    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        return pieceColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return type;
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        var moves = new ArrayList<ChessMove>();
        if (equals(board.getPiece(myPosition))) {
            if (getPieceType().equals(PieceType.PAWN)) {
                calcPawnMoves(board, myPosition, moves);
            } else {

                calcMoves(board, myPosition, myPosition, moveRules.get(getPieceType()), moves);
            }
        }
        return moves;
    }

    private record MoveRule(boolean single, int[][] directions) {
    }

    private static final Map<PieceType, MoveRule> moveRules = Map.of(
            PieceType.KING, new MoveRule(true, new int[][]{{0, 1}, {1, 0}, {0, -1}, {-1, 0}, {1, -1}, {1, 1}, {-1, 1}, {-1, -1}}),
            PieceType.QUEEN, new MoveRule(false, new int[][]{{0, 1}, {1, 0}, {0, -1}, {-1, 0}, {1, -1}, {1, 1}, {-1, 1}, {-1, -1}}),
            PieceType.ROOK, new MoveRule(false, new int[][]{{0, 1}, {1, 0}, {0, -1}, {-1, 0}}),
            PieceType.BISHOP, new MoveRule(false, new int[][]{{1, -1}, {1, 1}, {-1, 1}, {-1, -1}}),
            PieceType.KNIGHT, new MoveRule(true, new int[][]{{2, -1}, {2, 1}, {-2, 1}, {-2, -1}, {1, 2}, {1, -2}, {-1, 2}, {-1, -2}})
    );

    private void calcMoves(ChessBoard board, ChessPosition startPos, ChessPosition pos, MoveRule rule, List<ChessMove> moves) {
        for (int[] direction : rule.directions) {
            var newPos = new ChessPosition(pos.getRow() + direction[0], pos.getColumn() + direction[1]);
            if (inBounds(newPos)) {
                var pieceAtPos = getPieceAt(board, newPos);
                if (pieceAtPos == null) {
                    moves.add(new ChessMove(startPos, newPos, null));
                    if (!rule.single) {
                        calcMoves(board, startPos, newPos, new MoveRule(false, new int[][]{{direction[0], direction[1]}}), moves);
                    }
                } else if (!pieceAtPos.pieceColor.equals(pieceColor)) {
                    moves.add(new ChessMove(startPos, newPos, null));
                }
            }
        }
    }

    private ChessPiece getPieceAt(ChessBoard board, ChessPosition pos) {
        if (inBounds(pos)) {
            return board.getPiece(pos);
        }
        return null;
    }

    private boolean inBounds(ChessPosition pos) {
        return (pos.getColumn() > 0 && pos.getColumn() <= 8 && pos.getRow() > 0 && pos.getRow() <= 8);
    }

    private void calcPawnMoves(ChessBoard board, ChessPosition startPos, List<ChessMove> moves) {
        var direction = (pieceColor == ChessGame.TeamColor.BLACK ? -1 : 1);

        // forward if not blocked
        var endPos = new ChessPosition(startPos.getRow() + direction, startPos.getColumn());
        var pieceAtPos = getPieceAt(board, endPos);
        if ((pieceAtPos == null) && (inBounds(endPos))) {
            addPromotions(direction, startPos, endPos, moves);
        }

        // Double jump
        if ((startPos.getRow() == 2 && pieceColor == ChessGame.TeamColor.WHITE) || (startPos.getRow() == 7 && pieceColor == ChessGame.TeamColor.BLACK)) {
            var endPosDoubleJump = new ChessPosition(startPos.getRow() + direction + direction, startPos.getColumn());
            if (pieceAtPos == null && getPieceAt(board, endPosDoubleJump) == null) {
                moves.add(new ChessMove(startPos, endPosDoubleJump, null));
            }
        }
        // attack left diagonal if available
        endPos = new ChessPosition(startPos.getRow() + direction, startPos.getColumn() - 1);
        pieceAtPos = getPieceAt(board, endPos);
        if ((pieceAtPos != null) && (!pieceAtPos.pieceColor.equals(pieceColor)) && (inBounds(endPos))) {
            addPromotions(direction, startPos, endPos, moves);
        }

        // attack right diagonal if available
        endPos = new ChessPosition(startPos.getRow() + direction, startPos.getColumn() + 1);
        pieceAtPos = getPieceAt(board, endPos);
        if ((pieceAtPos != null) && (!pieceAtPos.pieceColor.equals(pieceColor)) && (inBounds(endPos))) {
            addPromotions(direction, startPos, endPos, moves);
        }

    }

    private boolean isPromoting(int direction, ChessPosition pos) {
        return (direction == 1 && pos.getRow() == 8) || (direction == -1 && pos.getRow() == 1);
    }

    private void addPromotions(int direction, ChessPosition startPos, ChessPosition endPos, List<ChessMove> moves) {
        if (isPromoting(direction, endPos)) {
            moves.add(new ChessMove(startPos, endPos, PieceType.ROOK));
            moves.add(new ChessMove(startPos, endPos, PieceType.QUEEN));
            moves.add(new ChessMove(startPos, endPos, PieceType.KNIGHT));
            moves.add(new ChessMove(startPos, endPos, PieceType.BISHOP));
        } else {
            moves.add(new ChessMove(startPos, endPos, null));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessPiece that = (ChessPiece) o;
        return pieceColor == that.pieceColor && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pieceColor, type);
    }

    public String toString() {
        return String.format("%s:%s", pieceColor, type);
    }

}
