import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import GamePage from "./pages/game-page.jsx";
import HistoryPage from "./pages/history-page.jsx";

function App() {
    return (
        <Router>
            <div className="min-h-screen bg-slate-900 text-white">
                <nav className="p-4 bg-slate-800 flex gap-6 shadow-xl">
                    <Link to="/" className="hover:text-blue-400 font-bold">Play Game</Link>
                    <Link to="/history" className="hover:text-blue-400 font-bold">Match History</Link>
                </nav>

                <div className="container mx-auto p-8">
                    <Routes>
                        <Route path="/" element={<GamePage/>}/>
                        <Route path="/history" element={<HistoryPage/>}/>
                    </Routes>
                </div>
            </div>
        </Router>
    );
}

export default App;