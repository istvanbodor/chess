package vik.onlab.backend.logic;

import vik.onlab.backend.dto.MoveRequestDto;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MoveGenerator {

    private static final int[][] KNIGHT_OFFSETS = {{-2, -1}, {-2, 1}, {-1, -2}, {-1, 2}, {1, -2}, {1, 2}, {2, -1}, {2, 1}};
    private static final int[][] KING_OFFSETS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final int[][] ROOK_DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[][] BISHOP_DIRS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final int[][] QUEEN_DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    public static List<Move> getTrueLegalMoves(MoveRequestDto request, String color) {
        List<Move> pseudoMoves = getPseudoLegalMovesWithCastling(request, color);
        List<Move> legalMoves = new ArrayList<>();

        for (Move m : pseudoMoves) {
            String[][] tempBoard = simulateMove(request.getBoard(), m);
            if (!isKingInCheck(tempBoard, color)) {
                legalMoves.add(m);
            }
        }
        return legalMoves;
    }

    public static List<Move> getPseudoLegalMovesWithCastling(MoveRequestDto request, String color) {
        List<Move> moves = getBasePseudoLegalMoves(request.getBoard(), color);
        String[][] board = request.getBoard();

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                String piece = board[r][c];
                if (piece != null && piece.equals(color + "k")) {
                    addCastlingMoves(r, c, board, moves, color, request);
                }
            }
        }
        return moves;
    }

    public static List<Move> getBasePseudoLegalMoves(String[][] board, String color) {
        List<Move> moves = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                String piece = board[r][c];
                if (piece != null && piece.startsWith(color)) {
                    char type = piece.charAt(1);
                    switch (type) {
                        case 'p' -> addPawnMoves(r, c, board, moves, color);
                        case 'n' -> addLeaperMoves(r, c, board, moves, color, KNIGHT_OFFSETS);
                        case 'r' -> addSlidingMoves(r, c, board, moves, color, ROOK_DIRS);
                        case 'b' -> addSlidingMoves(r, c, board, moves, color, BISHOP_DIRS);
                        case 'q' -> addSlidingMoves(r, c, board, moves, color, QUEEN_DIRS);
                        case 'k' -> addLeaperMoves(r, c, board, moves, color, KING_OFFSETS);
                    }
                }
            }
        }
        return moves;
    }

    public static boolean isKingInCheck(String[][] board, String color) {
        int kingR = -1, kingC = -1;
        String targetKing = color + "k";

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (targetKing.equals(board[r][c])) {
                    kingR = r;
                    kingC = c;
                    break;
                }
            }
        }
        if (kingR == -1) return true;

        return isSquareAttacked(board, kingR, kingC, color);
    }

    public static boolean isSquareAttacked(String[][] board, int r, int c, String myColor) {
        String oppColor = myColor.equals("w") ? "b" : "w";
        List<Move> oppMoves = getBasePseudoLegalMoves(board, oppColor);
        for (Move m : oppMoves) {
            if (m.getToRow() == r && m.getToCol() == c) return true;
        }
        return false;
    }

    public static String[][] simulateMove(String[][] board, Move m) {
        String[][] next = Arrays.stream(board).map(String[]::clone).toArray(String[][]::new);
        String piece = next[m.getFromRow()][m.getFromCol()];

        next[m.getToRow()][m.getToCol()] = piece;
        next[m.getFromRow()][m.getFromCol()] = null;

        if (piece != null && piece.endsWith("p")) {
            if (m.getToRow() == 0 || m.getToRow() == 7) {
                next[m.getToRow()][m.getToCol()] = piece.charAt(0) + "q";
            }
        }

        if (piece != null && piece.endsWith("k") && Math.abs(m.getFromCol() - m.getToCol()) == 2) {
            int row = m.getToRow();
            if (m.getToCol() == 6) {
                next[row][5] = next[row][7];
                next[row][7] = null;
            } else if (m.getToCol() == 2) {
                next[row][3] = next[row][0];
                next[row][0] = null;
            }
        }
        return next;
    }

    private static void addLeaperMoves(int r, int c, String[][] board, List<Move> moves, String color, int[][] offsets) {
        for (int[] o : offsets) {
            int nr = r + o[0], nc = c + o[1];
            if (isValid(nr, nc) && (board[nr][nc] == null || !board[nr][nc].startsWith(color))) {
                moves.add(new Move(r, c, nr, nc));
            }
        }
    }

    private static void addSlidingMoves(int r, int c, String[][] board, List<Move> moves, String color, int[][] dirs) {
        for (int[] d : dirs) {
            for (int i = 1; i < 8; i++) {
                int nr = r + d[0] * i, nc = c + d[1] * i;
                if (!isValid(nr, nc)) break;
                if (board[nr][nc] == null) {
                    moves.add(new Move(r, c, nr, nc));
                } else {
                    if (!board[nr][nc].startsWith(color)) {
                        moves.add(new Move(r, c, nr, nc));
                    }
                    break;
                }
            }
        }
    }

    private static void addCastlingMoves(int r, int c, String[][] board, List<Move> moves, String color, MoveRequestDto flags) {
        if (c != 4) return;

        boolean kingMoved = color.equals("w") ? flags.isWKingMoved() : flags.isBKingMoved();
        if (kingMoved || isKingInCheck(board, color)) return;

        boolean rookRMoved = color.equals("w") ? flags.isWRookRMoved() : flags.isBRookRMoved();
        if (!rookRMoved && board[r][c + 1] == null && board[r][c + 2] == null) {
            if (!isSquareAttacked(board, r, c + 1, color) && !isSquareAttacked(board, r, c + 2, color)) {
                moves.add(new Move(r, c, r, c + 2));
            }
        }

        boolean rookLMoved = color.equals("w") ? flags.isWRookLMoved() : flags.isBRookLMoved();
        if (!rookLMoved && board[r][c - 1] == null && board[r][c - 2] == null && board[r][c - 3] == null) {
            if (!isSquareAttacked(board, r, c - 1, color) && !isSquareAttacked(board, r, c - 2, color)) {
                moves.add(new Move(r, c, r, c - 2));
            }
        }
    }

    private static void addPawnMoves(int r, int c, String[][] board, List<Move> moves, String color) {
        int dir = color.equals("w") ? -1 : 1;
        int startRow = color.equals("w") ? 6 : 1;

        if (isValid(r + dir, c) && board[r + dir][c] == null) {
            moves.add(new Move(r, c, r + dir, c));
            if (r == startRow && isValid(r + 2 * dir, c) && board[r + 2 * dir][c] == null) {
                moves.add(new Move(r, c, r + 2 * dir, c));
            }
        }

        for (int side : new int[]{-1, 1}) {
            int nc = c + side;
            if (isValid(r + dir, nc) && board[r + dir][nc] != null && !board[r + dir][nc].startsWith(color)) {
                moves.add(new Move(r, c, r + dir, nc));
            }
        }
    }

    private static boolean isValid(int r, int c) {
        return r >= 0 && r < 8 && c >= 0 && c < 8;
    }
}