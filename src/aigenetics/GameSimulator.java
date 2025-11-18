package aigenetics;

import java.io.IOException;

import ailogicbusiness.MyAIPlayerLogic;
import it.unibo.ai.didattica.competition.tablut.domain.Action;
import it.unibo.ai.didattica.competition.tablut.domain.State;

//TODO: Importa le classi REALI del tuo progetto GitHub (State, Action, Turn, etc.)

/**
* Simula una singola partita di Tablut "headless" (senza server).
* Usa due istanze di MyAIPlayerLogic.
*/
public class GameSimulator {

 private MyAIPlayerLogic whitePlayer;
 private MyAIPlayerLogic blackPlayer;
 private int trainingDepth;
 
 private static final int MAX_MOVES_PER_GAME = 100; // Evita stalli infiniti

 public GameSimulator(MyAIPlayerLogic whitePlayer, MyAIPlayerLogic blackPlayer, int trainingDepth) {
     this.whitePlayer = whitePlayer;
     this.blackPlayer = blackPlayer;
     this.trainingDepth = trainingDepth;
 }

 /**
  * Esegue l'intera partita e ritorna il vincitore.
  * @return Il ruolo del vincitore (WHITE_WIN o BLACK_WIN) o DRAW.
 * @throws IOException 
  */
 
 public State.Turn simulateGame() throws IOException {
     
     // TODO: Crea lo stato iniziale REALE
     // State state = new State();
     // state.setTurn(State.Turn.WHITE);
     State state = null; // Fittizio

     int moveCount = 0;

     while (!isTerminal(state) && moveCount < MAX_MOVES_PER_GAME) {
         
         Action move;
         
         if (state.getTurn() == State.Turn.WHITE) {
             // Chiama il launcher in modalità "fixedDepth"
             move = whitePlayer.findBestMove(state, trainingDepth, 0, null);
         } else {
             move = blackPlayer.findBestMove(state, trainingDepth, 0, null);
         }

         if (move == null) {
             //[cite_start]// Il giocatore non può muovere (sconfitta) [cite: 102]
             return (state.getTurn() == State.Turn.WHITE) ? State.Turn.BLACKWIN : State.Turn.WHITEWIN;
         }

         //
         // TODO: Applica la mossa REALE allo stato
         // 1. Clona lo stato: State newState = state.clone();
         // 2. Applica la mossa: gameRules.checkMove(newState, move);
         // 3. state = newState;
         //
         
         // Simula l'aggiornamento (sostituisci!)
         // state = ...
         
         moveCount++;
     }

     if (moveCount >= MAX_MOVES_PER_GAME) {
         return State.Turn.DRAW; // Stallo
     }
     
     return state.getTurn(); // Ritorna il vincitore (es. WHITE_WIN)
 }

 // TODO: Importa o duplica questo metodo
 private boolean isTerminal(State state) {
     State.Turn turn = state.getTurn();
     return turn.equals(State.Turn.WHITEWIN) ||
            turn.equals(State.Turn.BLACKWIN) ||
            turn.equals(State.Turn.DRAW);
 }
 
}