package aigenetics;

import java.util.Random;
import java.util.Comparator;

/**
 * Contiene la logica "biologica" statica del GA
 * (Selezione, Crossover, Mutazione).
 */
public class GAEngine {

    private static final double MUTATION_RATE = 0.05; 
    private static final int TOURNAMENT_SIZE = 5; 
    private static final double ELITISM_RATE = 0.10; 
    private static final Random rand = new Random();

    public static Population evolvePopulation(Population oldPop) {
        int popSize = oldPop.size();
        Population newPopulation = new Population(popSize, false);

        // 1. Elitismo
        int elitismCount = (int) (popSize * ELITISM_RATE);
        oldPop.individuals.sort(Comparator.comparingDouble(ind -> -ind.fitness));
        for (int i = 0; i < elitismCount; i++) {
            newPopulation.add(oldPop.get(i).clone());
        }

        // 2. Genera il resto della popolazione
        while (newPopulation.size() < popSize) {
            HeuristicWeights parent1 = tournamentSelection(oldPop);
            HeuristicWeights parent2 = tournamentSelection(oldPop);
            HeuristicWeights child = crossover(parent1, parent2);
            mutate(child);
            newPopulation.add(child);
        }
        
        return newPopulation;
    }

    private static HeuristicWeights tournamentSelection(Population pop) {
        Population tournament = new Population(TOURNAMENT_SIZE, false);
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            int randomIndex = rand.nextInt(pop.size());
            tournament.add(pop.get(randomIndex));
        }
        return tournament.getFittest();
    }

    private static HeuristicWeights crossover(HeuristicWeights parent1, HeuristicWeights parent2) {
        HeuristicWeights child = HeuristicWeights.createBlankChild();
        for (int i = 0; i < HeuristicWeights.NUM_WEIGHTS; i++) {
            if (rand.nextDouble() <= 0.5) {
                child.weights[i] = parent1.weights[i];
            } else {
                child.weights[i] = parent2.weights[i];
            }
        }
        return child;
    }

    private static void mutate(HeuristicWeights individual) {
        for (int i = 0; i < HeuristicWeights.NUM_WEIGHTS; i++) {
            if (rand.nextDouble() <= MUTATION_RATE) {
                double mutation = (rand.nextDouble() - 0.5) * 2.0; // Valore tra -1.0 e +1.0
                individual.weights[i] += mutation;
            }
        }
    }
}