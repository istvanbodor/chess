package vik.onlab.backend.dto;

import lombok.Data;

@Data
public class MoveRequestDto {
    private String[][] board;
    private String playerColor;
    private boolean wKingMoved;
    private boolean wRookLMoved;
    private boolean wRookRMoved;
    private boolean bKingMoved;
    private boolean bRookLMoved;
    private boolean bRookRMoved;
}
