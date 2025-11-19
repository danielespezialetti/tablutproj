package aigenetics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class MergeEngine {

    private static final double MUTATION_RATE = 0.15;
    private static final Random rand = new Random();
    
    // --- Parametri di Fusione per la Selezione ---
    private static final int TARGET_SIZE = 60;        // Dimensione finale della popolazione Elite
    private static final double ELITE_PERCENT = 0.10; // Quota di individui migliori (Top 10%)
    private static final double BREED_PERCENT = 0.40; // Quota di individui mutati/incrociati (40%)

    public static Population mergeAndSeed(Population pop1, Population pop2) {
        
        // 1. Unione e Ordinamento
        List<HeuristicWeights> combinedPool = new ArrayList<>();
        combinedPool.addAll(pop1.individuals);
        combinedPool.addAll(pop2.individuals);
        
        // Ordina per fitness decrescente (il più forte in cima)
        combinedPool.sort(Comparator.comparingDouble(HeuristicWeights::getFitness).reversed());
        
        Population newElitePop = new Population(TARGET_SIZE, false);
        int currentSize = 0;
        
        // Calcolo delle quote
        int eliteCount = (int) Math.round(TARGET_SIZE * ELITE_PERCENT);
        int breedCount = (int) Math.round(TARGET_SIZE * BREED_PERCENT);

        
        // --- 2. ELITE ASSOLUTA (10% dei migliori) ---
        // Questi individui passano direttamente, preservando la miglior fitness trovata.
        for (int i = 0; i < Math.min(eliteCount, combinedPool.size()); i++) {
            newElitePop.add(combinedPool.get(i).clone());
            currentSize++;
        }
        
        // --- 3. SEEDING E MUTAZIONE (40% di esplorazione locale) ---
        // Prendiamo i campioni che sono al di sotto della quota elite
        int startingBreedingIndex = eliteCount;
        
        while (currentSize < eliteCount + breedCount && currentSize < TARGET_SIZE) {
            
            // Se non ci sono più individui nel combinedPool, usciamo
            if (startingBreedingIndex >= combinedPool.size()) break; 

            // Seleziona un individuo dal pool rimanente per la mutazione (o un campione)
            HeuristicWeights parent = combinedPool.get(rand.nextInt(startingBreedingIndex)).clone();
            
            // Applichiamo una mutazione per creare nuove varianti
            mutate(parent); 
            
            newElitePop.add(parent);
            currentSize++;
        }
        
        // --- 4. DIVERSITÀ CASUALE (Riempimento del restante 50%) ---
        // Riempiamo lo spazio rimanente con individui totalmente casuali
        // per iniettare nuova diversità genetica.
        while (currentSize < TARGET_SIZE) {
            newElitePop.add(new HeuristicWeights());
            currentSize++;
        }

        System.out.println("Merge Completato. Nuova popolazione Elite creata con " + newElitePop.size() + " individui.");
        return newElitePop;
    }
    private static void mutate(HeuristicWeights ind) {
        for (int i = 0; i < HeuristicWeights.NUM_WEIGHTS; i++) {
            if (rand.nextDouble() <= MUTATION_RATE) {
                // Aggiunge un valore distribuito in maniera gaussiana (68% di mutazioni tra +0.3 e -0.3)
                ind.weights[i] += rand.nextGaussian()*0.3; 
            }
        }
    }
}