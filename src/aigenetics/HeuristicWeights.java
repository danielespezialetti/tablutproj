package aigenetics;

import java.util.Arrays;
import java.util.Random;
import java.io.Serializable; // Utile se vuoi salvarli su file

/**
 * Rappresenta un "Individuo" (Cromosoma) nel GA.
 * Contiene il suo "DNA" (i pesi) e la sua fitness.
 */
public class HeuristicWeights implements Cloneable, Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// !!! MODIFICA QUESTO !!!
    // Deve corrispondere al numero di euristiche in MyAIPlayerLogic.evaluate()
    public static final int NUM_WEIGHTS = 4;
    
    private static final double MIN_WEIGHT = -10.0;
    private static final double MAX_WEIGHT = 10.0;
    private static final Random rand = new Random();

    public double[] weights;
    public double fitness;

    public HeuristicWeights() {
        this.weights = new double[NUM_WEIGHTS];
        for (int i = 0; i < NUM_WEIGHTS; i++) {
            this.weights[i] = MIN_WEIGHT + (MAX_WEIGHT - MIN_WEIGHT) * rand.nextDouble();
        }
        this.fitness = 0.0;
    }

    private HeuristicWeights(boolean blank) {
        this.weights = new double[NUM_WEIGHTS];
        this.fitness = 0.0;
    }

    public static HeuristicWeights createBlankChild() {
        return new HeuristicWeights(true);
    }

    @Override
    public String toString() {
        return "Fitness: " + String.format("%.4f", fitness) + 
               ", Pesi: " + Arrays.toString(weights);
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