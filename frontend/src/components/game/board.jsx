import { useState } from 'react';
import { PIECES, INITIAL_BOARD, BACKEND_URL } from '../../utils/constants';

const StatusBanner = ({ status, isAiThinking, onNewGame }) => {
    const isGameOver = status !== "PLAYING";

    return (
        <div className="w-full max-w-md h-20 flex items-center justify-center mb-4 px-4 bg-slate-800 rounded-xl border border-slate-700 shadow-lg">
            {isGameOver ? (
                <div className="flex items-center gap-6">
                    <div className="text-left">
                        <p className="text-red-400 font-bold text-sm uppercase tracking-widest">Game Over</p>
                        <h2 className="text-xl font-black text-white uppercase">
                            {status.replace("_", " ")}
                        </h2>
                    </div>
                    <button onClick={onNewGame}
                            className="bg-emerald-600 hover:bg-emerald-500 text-white px-6 py-2 rounded-lg font-bold transition-all transform hover:scale-105 active:scale-95">
                        New Game
                    </button>
                </div>
            ) : (
                <div className="flex flex-col items-center">
                    <p className="text-slate-400 text-xs uppercase mb-1">Status</p>
                    <p className={`${isAiThinking ? 'text-blue-400 animate-pulse' : 'text-emerald-400'} font-bold`}>
                        {isAiThinking ? "Robot is thinking..." : "Your Turn"}
                    </p>
                </div>
            )}
        </div>
    );
};

const Board = () => {
    const [ board, setBoard ] = useState(INITIAL_BOARD);
    const [ selected, setSelected ] = useState(null);
    const [ legalMoves, setLegalMoves ] = useState([]);
    const [ isAiThinking, setIsAiThinking ] = useState(false);
    const [ gameStatus, setGameStatus ] = useState("PLAYING");
    const [ moveCount, setMoveCount ] = useState(0);
    const [ castleRights, setCastleRights ] = useState({
        wKingMoved: false, wRookLMoved: false, wRookRMoved: false,
        bKingMoved: false, bRookLMoved: false, bRookRMoved: false
    });

    const saveToHistory = async (finalStatus, finalCount) => {
        try {
            await fetch(`${BACKEND_URL}/api/chess/history`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    result: finalStatus.includes("WON") ? finalStatus.replace("_", " ") : finalStatus,
                    totalMoves: finalCount,
                })
            });
        } catch (error) {
            console.error("History Save Error:", error);
        }
    };

    const updateCastleFlags = (piece, r, c) => {
        setCastleRights(prev => ({
            ...prev,
            wKingMoved: prev.wKingMoved || piece === 'wk',
            wRookLMoved: prev.wRookLMoved || (piece === 'wr' && r === 7 && c === 0),
            wRookRMoved: prev.wRookRMoved || (piece === 'wr' && r === 7 && c === 7),
            bKingMoved: prev.bKingMoved || piece === 'bk',
            bRookLMoved: prev.bRookLMoved || (piece === 'br' && r === 0 && c === 0),
            bRookRMoved: prev.bRookRMoved || (piece === 'br' && r === 0 && c === 7),
        }));
    };

    const applyMoveToBoard = (currentBoard, fR, fC, tR, tC) => {
        const newBoard = currentBoard.map(r => [ ...r ]);
        let piece = newBoard[fR][fC];

        if (piece.endsWith('p') && (tR === 0 || tR === 7)) piece = piece[0] + 'q';

        if (piece.endsWith('k') && Math.abs(fC - tC) === 2) {
            const rookCol = tC === 6 ? 7 : 0;
            const rookDest = tC === 6 ? 5 : 3;
            newBoard[fR][rookDest] = newBoard[fR][rookCol];
            newBoard[fR][rookCol] = null;
        }

        newBoard[tR][tC] = piece;
        newBoard[fR][fC] = null;
        updateCastleFlags(piece, fR, fC);
        return newBoard;
    };


    const handleSquareClick = async (row, col) => {
        if (isAiThinking || gameStatus !== "PLAYING") return;

        if (selected) {
            const move = legalMoves.find(m => m.toRow === row && m.toCol === col);
            if (move) {
                await executeTurn(selected.row, selected.col, row, col);
            } else {
                setSelected(null);
                setLegalMoves([]);
            }
            return;
        }

        if (board[row][col]?.startsWith('w')) {
            setSelected({ row, col });
            await fetchLegalMoves(row, col);
        }
    };

    const fetchLegalMoves = async (row, col) => {
        try {
            const res = await fetch(`${BACKEND_URL}/api/chess/legal-moves?row=${row}&col=${col}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ board, playerColor: 'w', ...castleRights })
            });
            setLegalMoves(await res.json());
        } catch (err) {
            console.error("Legal Move Fetch Error:", err);
        }
    };

    const executeTurn = async (fR, fC, tR, tC) => {
        const boardAfterHuman = applyMoveToBoard(board, fR, fC, tR, tC);
        const currentCount = moveCount + 1;

        setBoard(boardAfterHuman);
        setSelected(null);
        setLegalMoves([]);
        setMoveCount(currentCount);
        setIsAiThinking(true);

        try {
            const res = await fetch(`${BACKEND_URL}/api/chess/move`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ board: boardAfterHuman, playerColor: 'w', ...castleRights })
            });
            const data = await res.json();

            if (data.status === "PLAYER_WON") {
                setGameStatus("PLAYER_WON");
                await saveToHistory("PLAYER_WON", currentCount);
                return;
            }

            if (data.move) {
                const finalBoard = applyMoveToBoard(boardAfterHuman, data.move.fromRow, data.move.fromCol, data.move.toRow, data.move.toCol);
                setBoard(finalBoard);
                setMoveCount(currentCount + 1);
            }

            if (data.status !== "PLAYING") {
                setGameStatus(data.status);
                await saveToHistory(data.status, currentCount + 1);
            }
        } catch (err) {
            console.error("Turn Execution Error:", err);
        } finally {
            setIsAiThinking(false);
        }
    };

    return (
        <div className="flex flex-col items-center select-none">
            <StatusBanner
                status={gameStatus}
                isAiThinking={isAiThinking}
                onNewGame={() => {
                    setBoard(INITIAL_BOARD);
                    setGameStatus("PLAYING");
                    setMoveCount(0);
                    setCastleRights({
                        wKingMoved: false, wRookLMoved: false, wRookRMoved: false,
                        bKingMoved: false, bRookLMoved: false, bRookRMoved: false
                    });
                }}
            />

            <div
                className={`grid grid-cols-8 border-4 border-slate-800 shadow-2xl transition-all duration-500 ${gameStatus !== "PLAYING" ? 'ring-4 ring-red-500/50' : ''}`}>
                {board.map((row, rIdx) =>
                    row.map((piece, cIdx) => {
                        const isDark = (rIdx + cIdx) % 2 === 1;
                        const isSelected = selected?.row === rIdx && selected?.col === cIdx;
                        const isTarget = legalMoves.some(m => m.toRow === rIdx && m.toCol === cIdx);

                        return (
                            <div
                                key={`${rIdx}-${cIdx}`}
                                onClick={() => handleSquareClick(rIdx, cIdx)}
                                className={`w-12 h-12 sm:w-20 sm:h-20 flex items-center justify-center text-4xl cursor-pointer relative
                                    ${isDark ? 'bg-slate-600' : 'bg-slate-300'}
                                    ${isSelected ? 'bg-yellow-400' : ''}
                                    ${gameStatus === 'PLAYING' ? 'hover:brightness-110' : 'cursor-default'}`}
                            >
                                {isTarget && <div className="absolute w-4 h-4 bg-black/20 rounded-full"/>}
                                {piece && (
                                    <span className={`z-10 ${piece[0] === 'w' ? 'text-white drop-shadow-md' : 'text-black'}`}>
                                        {PIECES[piece[0]][piece[1]]}
                                    </span>
                                )}
                            </div>
                        );
                    })
                )}
            </div>

            <div className="mt-6 text-slate-400 font-mono text-sm uppercase tracking-widest">
                Moves: <span className="text-white font-bold">{moveCount}</span>
            </div>
        </div>
    );
};

export default Board;