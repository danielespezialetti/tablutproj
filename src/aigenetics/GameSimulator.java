package aigenetics;

import java.util.HashSet;
import java.util.Set;

import ailogicbusiness.MyAIPlayerLogic; // Assicurati che il package sia giusto
import it.unibo.ai.didattica.competition.tablut.domain.Action;
import ailogicbusiness.SimulationEngine;
import it.unibo.ai.didattica.competition.tablut.domain.State;
import it.unibo.ai.didattica.competition.tablut.domain.StateTablut;

public class GameSimulator {

    private MyAIPlayerLogic whitePlayer;
    private MyAIPlayerLogic blackPlayer;
    private int trainingDepth;
    
    // Limite di sicurezza per evitare partite infinite
    private static final int MAX_MOVES_PER_GAME = 150; 

    public GameSimulator(MyAIPlayerLogic whitePlayer, MyAIPlayerLogic blackPlayer, int trainingDepth) {
        this.whitePlayer = whitePlayer;
        this.blackPlayer = blackPlayer;
        this.trainingDepth = trainingDepth;
    }

    public State.Turn simulateGame() {
        // 1. Inizializza Stato
        State state = new StateTablut();
        state.setTurn(State.Turn.WHITE);
        
        // 2. Motore (Arbitro)
        SimulationEngine engine = new SimulationEngine();
        
        // 3. Cronologia (Per i pareggi)
        Set<String> realGameHistory = new HashSet<>();
        realGameHistory.add(state.toString());

        int moveCount = 0;

        while (moveCount < MAX_MOVES_PER_GAME) {
            
            // Verifica fine partita
            if (state.getTurn().equals(State.Turn.WHITEWIN) || 
                state.getTurn().equals(State.Turn.BLACKWIN) || 
                state.getTurn().equals(State.Turn.DRAW)) {
                return state.getTurn();
            }

            Action move = null;
            try {
                // Chiede la mossa al giocatore corretto
                if (state.getTurn().equals(State.Turn.WHITE)) {
                    move = whitePlayer.findBestMove(state, trainingDepth, 0, realGameHistory);
                } else {
                    move = blackPlayer.findBestMove(state, trainingDepth, 0, realGameHistory);
                }

                if (move == null) {
                    // Stallo -> Sconfitta del giocatore di turno
                    return (state.getTurn().equals(State.Turn.WHITE)) ? State.Turn.BLACKWIN : State.Turn.WHITEWIN;
                }

                // Applica la mossa, aggiorna lo stato e controlla i pareggi
                engine.checkMove(state, move, realGameHistory);
                
                // Aggiungi il NUOVO stato alla cronologia
                realGameHistory.add(state.toString());

            } catch (Exception e) {
                e.printStackTrace();
                return State.Turn.DRAW; // Errore inatteso = Pareggio tecnico
            }
            
            moveCount++;
        }

        // Limite mosse raggiunto
        return State.Turn.DRAW;
    }
}