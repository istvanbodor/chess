package vik.onlab.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vik.onlab.backend.dto.GameResponseDto;
import vik.onlab.backend.dto.MoveRequestDto;
import vik.onlab.backend.logic.Move;
import vik.onlab.backend.logic.MoveGenerator;
import vik.onlab.backend.model.GameHistory;
import vik.onlab.backend.repository.GameHistoryRepository;
import vik.onlab.backend.service.GameEngineService;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/chess")
public class ChessController {

    private final GameEngineService gameEngineService;
    private final GameHistoryRepository historyRepository;

    @PostMapping("/move")
    public GameResponseDto makeMove(@RequestBody MoveRequestDto request) {
        String[][] currentBoard = request.getBoard();

        List<Move> aiMoves = MoveGenerator.getTrueLegalMoves(request, "b");
        if (aiMoves.isEmpty()) {
            return MoveGenerator.isKingInCheck(currentBoard, "b")
                ? new GameResponseDto(null, true, false, "PLAYER_WON")
                : new GameResponseDto(null, false, true, "DRAW");
        }

        Move bestMove = gameEngineService.calculateBestMove(request);
        if (bestMove == null) {
            return new GameResponseDto(null, false, true, "DRAW");
        }

        String[][] boardAfterAi = MoveGenerator.simulateMove(currentBoard, bestMove);

        MoveRequestDto nextReq = new MoveRequestDto();
        nextReq.setBoard(boardAfterAi);

        List<Move> humanMoves = MoveGenerator.getTrueLegalMoves(nextReq, "w");
        boolean isHumanInCheck = MoveGenerator.isKingInCheck(boardAfterAi, "w");

        if (humanMoves.isEmpty()) {
            return isHumanInCheck
                ? new GameResponseDto(bestMove, true, false, "AI_WON")
                : new GameResponseDto(bestMove, false, true, "DRAW");
        }

        return new GameResponseDto(bestMove, false, false, "PLAYING");
    }

    @PostMapping("/legal-moves")
    public List<Move> getLegalMoves(@RequestBody MoveRequestDto request, @RequestParam int row, @RequestParam int col) {
        String[][] board = request.getBoard();
        String piece = board[row][col];

        if (piece == null) return new ArrayList<>();

        return MoveGenerator.getTrueLegalMoves(request, String.valueOf(piece.charAt(0)))
            .stream()
            .filter(m -> m.getFromRow() == row && m.getFromCol() == col)
            .toList();
    }

    @GetMapping("/history")
    public List<GameHistory> getHistory() {
        return historyRepository.findAll();
    }

    @PostMapping("/history")
    public GameHistory saveGame(@RequestBody GameHistory game) {
        return historyRepository.save(game);
    }
}