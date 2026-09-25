package chess;

import java.util.Arrays;
import java.util.Objects;

import static chess.ChessGame.TeamColor.BLACK;
import static chess.ChessGame.TeamColor.WHITE;

/**
 * A chessboard that can hold and rearrange chess pieces.
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessBoard {
    private ChessPiece[][] board = new ChessPiece[8][8];

    public ChessBoard() {

    }

    /**
     * Adds a chess piece to the chessboard
     *
     * @param position where to add the piece to
     * @param piece    the piece to add
     */
    public void addPiece(ChessPosition position, ChessPiece piece) {
        board[position.getRow() - 1][position.getColumn() - 1] = piece;
    }

    /**
     * Gets a chess piece on the chessboard
     *
     * @param position The position to get the piece from
     * @return Either the piece at the position, or null if no piece is at that
     * position
     */
    public ChessPiece getPiece(ChessPosition position) {
        return board[position.getRow() - 1][position.getColumn() - 1];
    }

    /**
     * Sets the board to the default starting board
     * (How the game of chess normally starts)
     */
    public void resetBoard() {
        board = new ChessPiece[8][8];

        for (var c = 0; c < 8; c++) {
            board[1][c] = new ChessPiece(WHITE, ChessPiece.PieceType.PAWN);
            board[6][c] = new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.PAWN);
        }

        for (var r : new int[]{0, 7}) {
            var color = (r == 0 ? WHITE : BLACK);
            board[r][0] = new ChessPiece(color, ChessPiece.PieceType.ROOK);
            board[r][1] = new ChessPiece(color, ChessPiece.PieceType.KNIGHT);
            board[r][2] = new ChessPiece(color, ChessPiece.PieceType.BISHOP);
            board[r][3] = new ChessPiece(color, ChessPiece.PieceType.QUEEN);
            board[r][4] = new ChessPiece(color, ChessPiece.PieceType.KING);
            board[r][5] = new ChessPiece(color, ChessPiece.PieceType.BISHOP);
            board[r][6] = new ChessPiece(color, ChessPiece.PieceType.KNIGHT);
            board[r][7] = new ChessPiece(color, ChessPiece.PieceType.ROOK);
        }

    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessBoard that = (ChessBoard) o;
        return Objects.deepEquals(board, that.board);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(board);
    }

    public String toString() {
        var b = new StringBuilder();
        for (var r = 0; r < 8; r++) {
            b.append('|');
            for (var c = 0; c < 8; c++) {
                b.append(board[r][c]);
                b.append('|');
            }
            b.append("\n");
        }
        return b.toString();
    }
}
