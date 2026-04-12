package vik.onlab.backend.logic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Move {
    private int fromRow;
    private int fromCol;
    private int toRow;
    private int toCol;
}