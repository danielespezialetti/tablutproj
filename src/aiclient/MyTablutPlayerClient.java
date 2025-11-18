package aiclient;

//TODO: Importa le classi REALI del tuo progetto GitHub
//import it.unibo.ai.didattica.competition.tablut.domain.State;
//import it.unibo.ai.didattica.competition.tablut.domain.Action;
//import it.unibo.ai.didattica.competition.tablut.domain.State.Turn;
//import it.unibo.ai.didattica.competition.tablut.client.TablutClient;
//... etc ...

import java.io.FileInputStream;
import java.io.ObjectInputStream;

import aigenetics.*;
import ailogicbusiness.*;
import it.unibo.ai.didattica.competition.tablut.domain.*;

/**
* Questa è la classe che estende TablutClient e gioca nel torneo.
* Carica i pesi evoluti e usa MyAIPlayerLogic per scegliere la mossa.
*/
//public class MyTablutPlayerClient extends TablutClient { // TODO: Scommenta
public class MyTablutPlayerClient { // Fittizio per compilare

 private MyAIPlayerLogic aiLogic;
 private long timeoutSeconds;
 
 // TODO: Sostituisci State.Turn con la classe reale
 private State.Turn myRole; 

 /**
  * Costruttore del Client.
  * Legge i parametri da riga di comando, carica i pesi,
  * e inizializza il cervello AI.
  */
 public State State() {
	// TODO Auto-generated method stub
	return null;
}
 public MyTablutPlayerClient(String role, long timeout, String serverIP) {
     // TODO: Chiama il costruttore della superclasse
     // super(role, "IlTuoNomePlayer", timeout, serverIP);
     
     this.timeoutSeconds = timeout;
     
     // TODO: Imposta il tuo ruolo (WHITE o BLACK)
     // this.myRole = (role.equalsIgnoreCase("WHITE")) ? State.Turn.WHITE : State.Turn.BLACK;
     this.myRole = State.Turn.WHITE; // Fittizio
     
     // Carica i pesi giusti dal file
     String weightsFile = (this.myRole == State.Turn.WHITE) ? "white_weights.dat" : "black_weights.dat";
     HeuristicWeights weights = loadWeights(weightsFile);
     
     // Inizializza il cervello AI
     this.aiLogic = new MyAIPlayerLogic(weights, this.myRole);
     
     System.out.println("Player " + role + " inizializzato con pesi da " + weightsFile);
 }

 /**
  * Metodo principale del client (come da esempio TablutRandomClient)
  */
 public void run() {
     
     //[cite_start]// TODO: Implementa il ciclo di gioco come da TablutClient.java [cite: 148-152]
     // Questo è lo pseudocodice basato sulle slide
     
     // 1. Connessione iniziale e invio nome (gestito da super())
     
     while(true) { // Il ciclo di gioco
         try {
             //[cite_start]// 1. Leggi lo stato dal server (bloccante) [cite: 152]
             // this.read(); 
             // State currentState = this.getState();
             State currentState = State(); // Fittizio
             
             // Se è il mio turno, calcolo la mossa
             if (currentState.getTurn().equals(this.myRole)) {
                 System.out.println("È il mio turno, calcolo la mossa...");
                 
                 // 2. Calcola la mossa usando il cervello AI
                 Action bestMove = this.aiLogic.findBestMove(
                     currentState, 
                     0, // No profondità fissa
                     this.timeoutSeconds // Usa il timeout
, null
                 );
                 
                 //[cite_start]// 3. Invia la mossa al server [cite: 150]
                 // this.write(bestMove);
                 
                 //[cite_start]// 4. Leggi lo stato aggiornato (la mia mossa) [cite: 151]
                 // this.read(); 
             } else {
                 // Turno avversario, rimani in attesa
                 System.out.println("Attendo la mossa avversaria...");
             }
             
             // 5. Torna a 1 (leggi lo stato aggiornato dall'avversario)
             
         } catch (Exception e) {
             // Gestisci disconnessioni, timeout, mosse illegali, etc.
             e.printStackTrace();
             break;
         }
     }
 }


/**
  * Carica un oggetto HeuristicWeights da un file.
  */
 private HeuristicWeights loadWeights(String filename) {
     try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
         return (HeuristicWeights) ois.readObject();
     } catch (Exception e) {
         System.err.println("ATTENZIONE: File dei pesi '" + filename + "' non trovato o corrotto.");
         System.err.println("Utilizzo pesi casuali di default! Le performance saranno basse.");
         e.printStackTrace();
         // Fallback: crea pesi casuali se il file non esiste
         return new HeuristicWeights();
     }
 }

 /**
  * Main per lanciare il client
  */
 public static void main(String[] args) {
     //[cite_start]// Leggi i parametri da riga di comando [cite: 178-181]
     String role = args[0]; // "WHITE" o "BLACK"
     long timeout = Long.parseLong(args[1]); // Timeout in secondi
     String serverIP = args[2];
     
     MyTablutPlayerClient client = new MyTablutPlayerClient(role, timeout, serverIP);
     client.run();
 }
}