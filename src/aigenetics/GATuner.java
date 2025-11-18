package aigenetics;

import java.io.File;              // <--- AGGIUNTO
import java.io.FileInputStream;   // <--- AGGIUNTO
import java.io.FileOutputStream;
import java.io.ObjectInputStream; // <--- AGGIUNTO
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import it.unibo.ai.didattica.competition.tablut.domain.State;

public class GATuner {

    // --- CONFIGURAZIONE ---
    private static final int POP_SIZE = 80;
    private static final int GENERATIONS = 100; // Quante generazioni AGGIUNTIVE fare
    private static final int GAMES_PER_INDIVIDUAL = 10; 
    private static final int TRAINING_DEPTH = 3; 
    private static final int THREADS = 4; 

    // Nomi dei file per il checkpoint completo
    private static final String WHITE_POP_FILE = "white_population.ser";
    private static final String BLACK_POP_FILE = "black_population.ser";

    public static void main(String[] args) throws Exception {
        System.out.println("=== STARTING GENETIC TRAINING ===");
        System.out.println("Threads: " + THREADS + " | Depth: " + TRAINING_DEPTH);

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        
        Population whitePop;
        Population blackPop;

        // --- MODIFICA 1: CARICAMENTO INTELLIGENTE ---
        System.out.println("Checking for existing checkpoints...");
        
        // Prova a caricare la popolazione BIANCA
        whitePop = loadPopulation(WHITE_POP_FILE);
        if (whitePop == null) {
            System.out.println("Checkpoint not found. Creating NEW White Population.");
            whitePop = new Population(POP_SIZE, true);
        } else {
            System.out.println("Resume: Loaded White Population from file!");
        }

        // Prova a caricare la popolazione NERA
        blackPop = loadPopulation(BLACK_POP_FILE);
        if (blackPop == null) {
            System.out.println("Checkpoint not found. Creating NEW Black Population.");
            blackPop = new Population(POP_SIZE, true);
        } else {
            System.out.println("Resume: Loaded Black Population from file!");
        }
        // ---------------------------------------------

        for (int gen = 1; gen <= GENERATIONS; gen++) {
            long start = System.currentTimeMillis();
            System.out.println("\nGeneration " + gen + " running...");

            // 1. Valuta i BIANCHI
            evaluate(executor, whitePop, blackPop, State.Turn.WHITE);
            
            // 2. Valuta i NERI
            evaluate(executor, blackPop, whitePop, State.Turn.BLACK);

            // 3. Stampa Migliori e Salva il Campione (Backup pesi singoli)
            HeuristicWeights bestW = whitePop.getFittest();
            HeuristicWeights bestB = blackPop.getFittest();
            System.out.println("Best WHITE: " + bestW);
            System.out.println("Best BLACK: " + bestB);
            
            saveWeights(bestW, "white_best_weights.dat");
            saveWeights(bestB, "black_best_weights.dat");

            // 4. Evoluzione
            whitePop = GAEngine.evolvePopulation(whitePop);
            blackPop = GAEngine.evolvePopulation(blackPop);
            
            // --- MODIFICA 2: SALVATAGGIO CHECKPOINT ---
            // Salviamo l'intera popolazione evoluta per poter riprendere dopo
            savePopulation(whitePop, WHITE_POP_FILE);
            savePopulation(blackPop, BLACK_POP_FILE);
            // ------------------------------------------
            
            System.out.println("Time: " + (System.currentTimeMillis() - start) + "ms");
        }

        executor.shutdown();
        System.out.println("Training Complete.");
    }

    // --- METODI PER POPOLAZIONE (AGGIUNTI) ---

    private static void savePopulation(Population pop, String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(pop);
        } catch (Exception e) {
            System.err.println("Error saving population checkpoint: " + e.getMessage());
        }
    }

    private static Population loadPopulation(String filename) {
        File f = new File(filename);
        if (!f.exists()) return null; // Se il file non c'è, ritorna null
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            return (Population) ois.readObject();
        } catch (Exception e) {
            System.err.println("Found file but failed to load (corrupted?): " + e.getMessage());
            return null; // Se fallisce (es. versione vecchia), ritorna null e ricomincia
        }
    }

    // --- METODI VECCHI (RESTANO UGUALI) ---

    private static void evaluate(ExecutorService exec, Population evalPop, Population oppPop, State.Turn role) throws Exception {
    	int count = 0;
    	List<Future<Double>> futures = new ArrayList<>();
        for (HeuristicWeights ind : evalPop.individuals) {
            futures.add(exec.submit(new FitnessEvaluator(ind, oppPop, role, GAMES_PER_INDIVIDUAL, TRAINING_DEPTH)));
            count++;
            System.out.println("Partite: "+count*10);
        }
        for (int i = 0; i < evalPop.size(); i++) {
            Double fitnessValue = futures.get(i).get();
            evalPop.get(i).setFitness(fitnessValue); 
        }
    }

    private static void saveWeights(HeuristicWeights w, String name) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(name))) {
            oos.writeObject(w);
        } catch (Exception e) { e.printStackTrace(); }
    }
}