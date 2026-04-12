package vik.onlab.backend.service;

import org.springframework.stereotype.Service;
import vik.onlab.backend.dto.MoveRequestDto;
import vik.onlab.backend.logic.Move;
import vik.onlab.backend.logic.MoveGenerator;

import java.util.List;

@Service
public class GameEngineService {

    private static final int DEPTH = 3;
    private static final int CHECKMATE_SCORE = 10000;

    private static final int PAWN = 10;
    private static final int KNIGHT = 30;
    private static final int BISHOP = 30;
    private static final int ROOK = 50;
    private static final int QUEEN = 90;
    private static final int KING = 900;

    public Move calculateBestMove(MoveRequestDto request) {
        return minimax(request, DEPTH, Integer.MIN_VALUE, Integer.MAX_VALUE, true).move;
    }

    private Evaluation minimax(MoveRequestDto request, int depth, int alpha, int beta, boolean isMaximizing) {
        String color = isMaximizing ? "b" : "w";
        List<Move> moves = MoveGenerator.getTrueLegalMoves(request, color);

        if (moves.isEmpty()) {
            if (MoveGenerator.isKingInCheck(request.getBoard(), color)) {
                return new Evaluation(null, isMaximizing ? -CHECKMATE_SCORE : CHECKMATE_SCORE);
            }
            return new Evaluation(null, 0);
        }

        if (depth == 0) {
            return new Evaluation(null, evaluateBoard(request.getBoard()));
        }

        Move bestMove = null;
        int extremeEval = isMaximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (Move m : moves) {
            String[][] nextBoard = MoveGenerator.simulateMove(request.getBoard(), m);
            MoveRequestDto nextRequest = createNextRequest(request, m, nextBoard);
            int eval = minimax(nextRequest, depth - 1, alpha, beta, !isMaximizing).score;

            if (isMaximizing) {
                if (eval > extremeEval) {
                    extremeEval = eval;
                    bestMove = m;
                }
                alpha = Math.max(alpha, eval);
            } else {
                if (eval < extremeEval) {
                    extremeEval = eval;
                    bestMove = m;
                }
                beta = Math.min(beta, eval);
            }

            if (beta <= alpha) break;
        }

        return new Evaluation(bestMove, extremeEval);
    }

    private MoveRequestDto createNextRequest(MoveRequestDto current, Move m, String[][] nextBoard) {
        MoveRequestDto next = new MoveRequestDto();
        next.setBoard(nextBoard);
        next.setWKingMoved(current.isWKingMoved());
        next.setWRookLMoved(current.isWRookLMoved());
        next.setWRookRMoved(current.isWRookRMoved());
        next.setBKingMoved(current.isBKingMoved());
        next.setBRookLMoved(current.isBRookLMoved());
        next.setBRookRMoved(current.isBRookRMoved());

        String piece = current.getBoard()[m.getFromRow()][m.getFromCol()];
        if (piece == null) return next;

        if (piece.equals("wk")) next.setWKingMoved(true);
        else if (piece.equals("bk")) next.setBKingMoved(true);
        else if (piece.equals("wr")) {
            if (m.getFromRow() == 7 && m.getFromCol() == 0) next.setWRookLMoved(true);
            else if (m.getFromRow() == 7 && m.getFromCol() == 7) next.setWRookRMoved(true);
        } else if (piece.equals("br")) {
            if (m.getFromRow() == 0 && m.getFromCol() == 0) next.setBRookLMoved(true);
            else if (m.getFromRow() == 0 && m.getFromCol() == 7) next.setBRookRMoved(true);
        }

        return next;
    }

    private int evaluateBoard(String[][] board) {
        int score = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                String p = board[r][c];
                if (p != null) {
                    int val = getPieceValue(p.charAt(1));
                    score += (p.startsWith("b") ? val : -val);
                }
            }
        }
        return score;
    }

    private int getPieceValue(char type) {
        return switch (type) {
            case 'p' -> PAWN;
            case 'n' -> KNIGHT;
            case 'b' -> BISHOP;
            case 'r' -> ROOK;
            case 'q' -> QUEEN;
            case 'k' -> KING;
            default -> 0;
        };
    }

    private record Evaluation(Move move, int score) {}
}