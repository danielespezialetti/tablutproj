package aigenetics;

import java.util.Random;
import java.util.concurrent.Callable;

import ailogicbusiness.MyAIPlayerLogic;
import it.unibo.ai.didattica.competition.tablut.domain.State;

// TODO: Importa le classi REALI (State, Turn)

/**
 * Un task parallelo che calcola la fitness di UN individuo
 * facendolo giocare N partite contro avversari casuali.
 */
public class FitnessEvaluator implements Callable<Double> {

    private HeuristicWeights individualToEvaluate;
    private Population opponentPopulation;
    private State.Turn myRole;
    private int numGames;
    private int trainingDepth;
    private Random rand = new Random();

    public FitnessEvaluator(HeuristicWeights individual, Population opponents, 
                            State.Turn role, int numGames, int depth) {
        this.individualToEvaluate = individual;
        this.opponentPopulation = opponents;
        this.myRole = role;
        this.numGames = numGames;
        this.trainingDepth = depth;
    }

    @Override
    public Double call() throws Exception {
        int wins = 0;

        for (int i = 0; i < numGames; i++) {
            // Scegli un avversario a caso dalla popolazione opposta
            int oppIndex = rand.nextInt(opponentPopulation.size());
            HeuristicWeights opponentWeights = opponentPopulation.get(oppIndex);

            // Crea i giocatori per questa simulazione
            MyAIPlayerLogic myPlayer = new MyAIPlayerLogic(individualToEvaluate, myRole);
            MyAIPlayerLogic oppPlayer = new MyAIPlayerLogic(opponentWeights, 
                (myRole == State.Turn.WHITE) ? State.Turn.BLACK : State.Turn.WHITE);
            
            GameSimulator simulator;
            if (myRole == State.Turn.WHITE) {
                simulator = new GameSimulator(myPlayer, oppPlayer, trainingDepth);
            } else {
                simulator = new GameSimulator(oppPlayer, myPlayer, trainingDepth);
            }
            
            State.Turn winner = simulator.simulateGame();
            
            if ((winner == State.Turn.WHITEWIN && myRole == State.Turn.WHITE) ||
                (winner == State.Turn.BLACKWIN && myRole == State.Turn.BLACK)) {
                wins++;
            }
        }
        
        return (double) wins / (double) numGames;
    }
}