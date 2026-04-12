import { useEffect, useState } from 'react';
import { BACKEND_URL } from "../utils/constants.js";

const HistoryPage = () => {
    const [ history, setHistory ] = useState([]);

    useEffect(() => {
        fetch(`${BACKEND_URL}/api/chess/history`)
            .then(res => res.json())
            .then(data => setHistory(data.reverse()))
            .catch(err => console.error(err));
    }, []);

    return (
        <div className="max-w-4xl mx-auto p-4">
            <h1 className="text-3xl font-bold mb-6 text-blue-400">Match History</h1>
            <div className="grid gap-4">
                {history.map((game) => (
                    <div key={game.id} className="bg-slate-800 p-4 rounded-xl border border-slate-700 flex justify-between items-center">
                        <div>
                            <p className="text-slate-400 text-xs">{new Date(game.playedAt).toLocaleString()}</p>
                            <p className="text-xl font-bold">{game.result}</p>
                        </div>
                        <div className="text-right">
                            <p className="text-slate-400 text-xs">Total Moves</p>
                            <p className="text-xl font-mono">{game.totalMoves}</p>
                        </div>
                    </div>
                ))}
                {history.length === 0 && <p className="text-slate-500 italic">No games played yet.</p>}
            </div>
        </div>
    );
};

export default HistoryPage;