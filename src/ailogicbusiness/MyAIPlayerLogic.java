package ailogicbusiness;
import java.util.List;
import java.util.Set;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import aigenetics.HeuristicWeights;
import it.unibo.ai.didattica.competition.tablut.domain.State;
import it.unibo.ai.didattica.competition.tablut.domain.State.Pawn;
import it.unibo.ai.didattica.competition.tablut.domain.State.Turn;
import it.unibo.ai.didattica.competition.tablut.domain.Action;

/**
 * Questa classe è il "cervello" standalone.
 * Contiene Minimax, Alpha-Beta, e la funzione euristica.
 * NON estende TablutClient, è pura logica di gioco.
 * Sarà usata sia dal GA Trainer sia dal Client del Torneo.
 */
public class MyAIPlayerLogic {

    private static final String CASTLE = "E5";
    private static final List<String> CAMPS = List.of(
    	"A4", "A5", "A6", "B5", "D1", "E1", "F1", "E2", "I4", "I5", "I6",
    	"H5", "D9", "E9", "F9", "E8"
    );
    private HeuristicWeights weights;
    private State.Turn myRole;
    
    // Campi per la gestione del tempo e dell'Iterative Deepening
    private long startTime;
    private long timeoutMillis;
    private boolean stopSearch;
    private Action bestMoveFromPreviousIteration; // Per il Move Ordering
    private FeaturesExtractor features;
    private SimulationEngine gameRules;


    public MyAIPlayerLogic(HeuristicWeights weights, State.Turn myRole) {
        this.weights = weights;
        this.myRole = myRole;
        this.features = new FeaturesExtractor();
        this.gameRules = new SimulationEngine();
    }

    /**
     * Questo è il "Launcher" (il "Root" del Minimax).
     * Gestisce sia la ricerca a profondità fissa (per il GA) 
     * sia l'approfondimento iterativo (per la gara).
     *
     * @param state Lo stato attuale.
     * @param fixedDepth Profondità fissa per il training. Se 0, usa il timeout.
     * @param timeoutSeconds Timeout in secondi. Se 0, cerca solo a fixedDepth.
     * @return La mossa migliore trovata.
     */
    public Action findBestMove(State state, int fixedDepth, long timeoutSeconds, Set<String> realGameHistory) throws IOException{
        this.startTime = System.currentTimeMillis();
        this.stopSearch = false;
        this.bestMoveFromPreviousIteration = null;
        Action bestMoveOverall = null;
        Set<String> pathHistory = new HashSet<>(realGameHistory);
        pathHistory.add(state.toString());

        // Imposta il timeout con un buffer di sicurezza (es. 500ms)
        this.timeoutMillis = (timeoutSeconds > 0) ? (timeoutSeconds * 1000L) - 500L : Long.MAX_VALUE;

        int startDepth = (fixedDepth > 0) ? fixedDepth : 1;
        int endDepth = (fixedDepth > 0) ? fixedDepth : 99; // 99 = "infinito"
        int currentDepth;
        for (currentDepth = startDepth; currentDepth <= endDepth; currentDepth++) {
            
            Action bestMoveAtThisDepth = null;
            double maxScore = Double.NEGATIVE_INFINITY;
            double alpha = Double.NEGATIVE_INFINITY;
            double beta = Double.POSITIVE_INFINITY;

            // --- OTTIMIZZAZIONE: MOVE ORDERING ---
            List<Action> possibleMoves = getAllPossibleMoves(state);
            orderMoves(possibleMoves, this.bestMoveFromPreviousIteration);

            for (Action move : possibleMoves) {
                
                State newState = simulateMove(state, move, pathHistory); // Simula la mossa

                // Chiama la ricorsione per il MIN player
                double score = alphaBetaSearch(newState, currentDepth - 1, alpha, beta, false, pathHistory);
                
                if (this.stopSearch) {
                    break; // Il tempo è scaduto
                }

                if (score > maxScore) {
                    maxScore = score;
                    bestMoveAtThisDepth = move;
                }
                
                alpha = Math.max(alpha, maxScore);
            }

            if (this.stopSearch) {
                // Tempo scaduto DURANTE questa iterazione.
                // Usa il risultato dell'iterazione precedente (bestMoveOverall).
                break;
            } else {
                // Iterazione completata con successo. Salva il risultato.
                bestMoveOverall = bestMoveAtThisDepth;
                this.bestMoveFromPreviousIteration = bestMoveOverall;
                //System.out.println("Profondità " + currentDepth + " completata. Mossa: " + bestMoveOverall);
            }

            if (fixedDepth > 0) {
                break; // Eravamo in modalità "fixedDepth"
            }
        }

        // Fallback: se il tempo scade prima di completare depth=1
        if (bestMoveOverall == null) {
            List<Action> moves = getAllPossibleMoves(state);
            bestMoveOverall = moves.isEmpty() ? null : moves.get(0);
        }
        return bestMoveOverall;
    }


    /**
     * Il CUORE ricorsivo dell'algoritmo Minimax con Alpha-Beta Pruning.
     * @param pathHistory 
     * @throws IOException 
     */
    private double alphaBetaSearch(State state, int depth, double alpha, double beta, boolean isMaximizingPlayer, Set<String> pathHistory) throws IOException {
        
        //timeout
        if (System.currentTimeMillis() - this.startTime > this.timeoutMillis) {
            this.stopSearch = true;
            return 0; // Il valore è irrilevante
        }

        // --- 2. Casi Base (Foglia della Ricerca) ---
        if (depth == 0 || isTerminal(state)) {
            return evaluate(state);
        }

        String stateString = state.toString();
        if (pathHistory.contains(stateString)) {
            return 0.0; // PAREGGIO!
        }
        Set<String> newPath = new HashSet<>(pathHistory);
        newPath.add(stateString);
        // --- 3. Ricorsione ---
        List<Action> possibleMoves = getAllPossibleMoves(state);
        // TODO: Aggiungi un Move Ordering anche qui (es. basato su catture)

        if (isMaximizingPlayer) {
            double maxScore = Double.NEGATIVE_INFINITY;
            for (Action move : possibleMoves) {
                State newState = simulateMove(state, move, newPath);
                double score = alphaBetaSearch(newState, depth - 1, alpha, beta, false, newPath); 
                
                maxScore = Math.max(maxScore, score);
                alpha = Math.max(alpha, maxScore);
                
                if (beta <= alpha) {
                    break; // --- TAGLIO ALFA-BETA ---
                }
                if (this.stopSearch) return 0;
            }
            return maxScore;

        } else { // isMinimizingPlayer
            double minScore = Double.POSITIVE_INFINITY;
            for (Action move : possibleMoves) {
                State newState = simulateMove(state, move, newPath);
                double score = alphaBetaSearch(newState, depth - 1, alpha, beta, true, newPath); 
                
                minScore = Math.min(minScore, score);
                beta = Math.min(beta, minScore);
                
                if (beta <= alpha) {
                    break; // --- TAGLIO ALFA-BETA ---
                }
                if (this.stopSearch) return 0;
            }
            return minScore;
        }
    }

    /**
     * Valuta uno stato del gioco usando i pesi (DNA) di questo giocatore.
     * @param state Lo stato da valutare.
     * @return Un punteggio numerico.
     */
    private double evaluate(State state) {
        
        // 1. Controlli Fine Partita
        if (state.getTurn().equals(State.Turn.WHITEWIN)) {
            return (myRole == State.Turn.WHITE) ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }
        if (state.getTurn().equals(State.Turn.BLACKWIN)) {
            return (myRole == State.Turn.BLACK) ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }
        if (state.getTurn().equals(State.Turn.DRAW)) {
            return 0.0;
        }

        // 2. Estrazione Features (Array di 15 elementi)
        double[] feats = this.features.extractFeatures(state);

        // 3. CALCOLO FASE (Logoramento Proporzionale)
        double currentWhite = feats[0]*8.0+1;
        double currentBlack = feats[1]*16.0;
        
        final double START_WHITE = 9.0;  
        final double START_BLACK = 16.0;
        
        double whiteRatio = currentWhite / START_WHITE;
        double blackRatio = currentBlack / START_BLACK;
        
        // 0.0 = Inizio, 1.0 = Fine
        double boardRichness = (whiteRatio + blackRatio) / 2.0;
        double phase = 1.0 - boardRichness;
        
        // sicurezza
        phase = Math.max(0.0, Math.min(1.0, phase));


        // 4. APPLICAZIONE PESI (IBRIDA)
        double score = 0.0;
        int wIdx = 0; // Indice per scorrere l'array dei pesi

        try {
            // A. Pesi STATICI (Prime 11 Feature)
            // Le feature da 0 a 10 usano 1 peso ciascuna
            for (int f = 0; f < 11; f++) {
                score += this.weights.weights[wIdx] * feats[f];
                wIdx++; 
            }

            // B. Pesi DINAMICI (Ultime 4 Feature)
            // Le feature da 11 a 14 usano 2 pesi ciascuna (Start e End)
            for (int f = 11; f < 15; f++) {
                double wStart = this.weights.weights[wIdx];
                double wEnd = this.weights.weights[wIdx + 1];
                
                // Interpolazione Lineare
                double currentWeight = (wStart * (1.0 - phase)) + (wEnd * phase);
                
                score += currentWeight * feats[f];
                
                wIdx += 2; // 2 pesi
            }

        } catch (Exception e) {
            // Fallback sicuro
            score = 0;
            for(double val : feats) score += val;
        }

        return (myRole == State.Turn.WHITE) ? score : -score;
    }
    // -----------------------------------------------------------------
    // METODI HELPER DA IMPLEMENTARE (Usando le classi del progetto)
    // -----------------------------------------------------------------

    private void orderMoves(List<Action> moves, Action bestPreviousMove) {
        if (bestPreviousMove == null) {
            return; 
        }
        // Metti la mossa migliore precedente in cima alla lista
        int index = -1;
        for (int i = 0; i < moves.size(); i++) {
            if (moves.get(i).equals(bestPreviousMove)) {
                index = i;
                break;
            }
        }
        if (index > 0) {
            Collections.swap(moves, 0, index);
        }
    }

    /**
     * Genera tutte le mosse legali per il giocatore di turno
     * nello stato corrente.
     * @param state Lo stato attuale.
     * @return Una lista di Azioni (Action).
     */
    private List<Action> getAllPossibleMoves(State state) throws IOException{
        List<Action> moves = new ArrayList<>();
        State.Turn player = state.getTurn();
        Pawn[][] board = state.getBoard();
        
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                Pawn pawn = board[r][c];
                
                if ((pawn.equals(Pawn.WHITE) && player.equals(Turn.WHITE)) ||
                    (pawn.equals(Pawn.KING) && player.equals(Turn.WHITE)) ||
                    (pawn.equals(Pawn.BLACK) && player.equals(Turn.BLACK))) {
                    
                    String from = coordToString(r, c);
                    
                    for (int i = r - 1; i >= 0; i--) { // SU
                        if (!isCellOpen(r, c, state, i, c, player)) break;
                        moves.add(new Action(from, coordToString(i, c), player));
                    }
                    for (int i = r + 1; i < 9; i++) { // GIÙ
                        if (!isCellOpen(r, c, state, i, c, player)) break;
                        moves.add(new Action(from, coordToString(i, c), player));
                    }
                    for (int j = c - 1; j >= 0; j--) { // SINISTRA
                        if (!isCellOpen(r, c, state, r, j, player)) break;
                        moves.add(new Action(from, coordToString(r, j), player));
                    }
                    for (int j = c + 1; j < 9; j++) { // DESTRA
                        if (!isCellOpen(r, c, state, r, j, player)) break;
                        moves.add(new Action(from, coordToString(r, j), player));
                    }
                }
            }
        }
        return moves;
    }
    
    private State simulateMove(State oldState, Action move, Set<String> pathHistory) {
    	State newState=null;
        try {
            newState = gameRules.checkMove(oldState.clone(), move, pathHistory);
        } catch (Exception e) {
            return oldState; 
        }
        
        return newState;
    }

    private boolean isTerminal(State state) {
        State.Turn turn = state.getTurn();
        return turn.equals(State.Turn.WHITEWIN) ||
               turn.equals(State.Turn.BLACKWIN) ||
               turn.equals(State.Turn.DRAW);
    }
    
    private String coordToString(int r, int c) {
        char col = (char) ('A' + c);
        String row = Integer.toString(r + 1); 
        return col + row;
    }
    
    private boolean isCellOpen(int r1, int c1, State state, int r2, int c2, State.Turn player) {
        if (!state.getBoard()[r2][c2].equals(Pawn.EMPTY)) {
            return false;
        }
        String to = coordToString(r2, c2);
        if (to.equals(CASTLE)) {
            return false;
        }
        if (CAMPS.contains(to)) {
        	String from = coordToString(r1, c1);
        	if (CAMPS.contains(from) && player==State.Turn.BLACK
        			&& (Math.abs(r1-r2)<3) && (Math.abs(c1-c2)<3)) {
        		return true;
        	} else {
        		return false;
        	}
        }
        return true;
    }
    
}