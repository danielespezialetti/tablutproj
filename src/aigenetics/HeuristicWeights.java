package aigenetics;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;

public class HeuristicWeights implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;

    // Deve corrispondere al numero di feature estratte in MyAIPlayerLogic
    public static final int NUM_WEIGHTS = 19; 
    
    private static final double MIN_WEIGHT = -10.0;
    private static final double MAX_WEIGHT = 10.0;
    private static final Random rand = new Random();

    public double[] weights;
    
    // 'transient' perché non ci serve salvare il punteggio nel file .dat, 
    // ci servono solo i pesi per giocare.
    private double fitness; 

    public HeuristicWeights() {
        this.weights = new double[NUM_WEIGHTS];
        for (int i = 0; i < NUM_WEIGHTS; i++) {
            this.weights[i] = MIN_WEIGHT + (MAX_WEIGHT - MIN_WEIGHT) * rand.nextDouble();
        }
        this.fitness = 0.0;
    }

    // Costruttore privato per figli vuoti
    private HeuristicWeights(boolean blank) {
        this.weights = new double[NUM_WEIGHTS];
        this.fitness = 0.0;
    }

    public static HeuristicWeights createBlankChild() {
        return new HeuristicWeights(true);
    }
    
    // --- Metodi per la Fitness (aggiunti per chiarezza) ---
    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public double getFitness() {
        return this.fitness;
    }

    @Override
    public String toString() {
        return "Fit: " + String.format("%.3f", fitness) + " W: " + Arrays.toString(weights);
    }

    @Override
    public HeuristicWeights clone() {
        try {
            HeuristicWeights clone = (HeuristicWeights) super.clone();
            clone.weights = this.weights.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}