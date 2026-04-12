package vik.onlab.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import vik.onlab.backend.logic.Move;

@Data
@AllArgsConstructor
public class GameResponseDto {
    private Move move;
    private boolean isCheckmate;
    private boolean isDraw;
    private String status;
}
