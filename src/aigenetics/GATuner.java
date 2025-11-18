package aigenetics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import it.unibo.ai.didattica.competition.tablut.domain.State;

import java.io.FileOutputStream; // Per salvare i pesi
import java.io.ObjectOutputStream; // Per salvare i pesi

// TODO: Importa le classi REALI (State, Turn)

/**
 * Classe principale (main) per avviare il training genetico.
 * Gestisce il pool di thread e il ciclo delle generazioni.
 */
public class GATuner {

    // --- PARAMETRI DI TRAINING (Regola questi!) ---
    private static final int POPULATION_SIZE = 50;
    private static final int MAX_GENERATIONS = 100;
    private static final int GAMES_PER_EVALUATION = 20;
    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();
    private static final int TRAINING_DEPTH = 3; // Profondità bassa e fissa!

    public static void main(String[] args) throws Exception {
        
        System.out.println("Avvio Training Genetico (Co-evoluzione)...");
        System.out.println("Usando " + NUM_THREADS + " thread in parallelo.");
        
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        Population whitePopulation = new Population(POPULATION_SIZE, true);
        Population blackPopulation = new Population(POPULATION_SIZE, true);

        for (int gen = 1; gen <= MAX_GENERATIONS; gen++) {
            long startTime = System.currentTimeMillis();
            System.out.println("\n--- Inizio Generazione " + gen + "/" + MAX_GENERATIONS + " ---");

            // Valuta la popolazione BIANCA (giocando contro la NERA)
            evaluatePopulation(executor, whitePopulation, blackPopulation, State.Turn.WHITE);
            
            // Valuta la popolazione NERA (giocando contro la BIANCA)
            evaluatePopulation(executor, blackPopulation, whitePopulation, State.Turn.BLACK);

            HeuristicWeights bestWhite = whitePopulation.getFittest();
            HeuristicWeights bestBlack = blackPopulation.getFittest();
            System.out.println("Miglior BIANCO: " + bestWhite);
            System.out.println("Miglior NERO: " + bestBlack);

            // Evolvi
            whitePopulation = GAEngine.evolvePopulation(whitePopulation);
            blackPopulation = GAEngine.evolvePopulation(blackPopulation);
            
            long endTime = System.currentTimeMillis();
            System.out.println("Generazione " + gen + " completata in " + (endTime - startTime) + "ms");
        }

        executor.shutdown();
        System.out.println("\n--- Training Terminato ---");
        
        HeuristicWeights finalWhite = whitePopulation.getFittest();
        HeuristicWeights finalBlack = blackPopulation.getFittest();
        System.out.println("Campione finale BIANCO: " + finalWhite);
        System.out.println("Campione finale NERO: " + finalBlack);
        
        // Salva i pesi migliori su file
        saveWeights(finalWhite, "white_weights.dat");
        saveWeights(finalBlack, "black_weights.dat");
    }

    private static void evaluatePopulation(ExecutorService executor, Population popToEvaluate, 
                                           Population opponentPop, State.Turn role) 
                                           throws Exception {
        List<Future<Double>> futures = new ArrayList<>();
        for (HeuristicWeights individual : popToEvaluate.individuals) {
            Callable<Double> task = new FitnessEvaluator(
                individual, opponentPop, role, GAMES_PER_EVALUATION, TRAINING_DEPTH
            );
            futures.add(executor.submit(task));
        }
        for (int i = 0; i < popToEvaluate.size(); i++) {
            popToEvaluate.get(i).fitness = futures.get(i).get();
        }
    }

    private static void saveWeights(HeuristicWeights weights, String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(weights);
            System.out.println("Pesi salvati in: " + filename);
        } catch (Exception e) {
            System.err.println("Errore nel salvataggio dei pesi: " + e.getMessage());
        }
    }
}