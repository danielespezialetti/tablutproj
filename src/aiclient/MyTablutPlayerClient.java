package aiclient;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import aigenetics.*;
import ailogicbusiness.*;
import it.unibo.ai.didattica.competition.tablut.client.TablutClient;
import it.unibo.ai.didattica.competition.tablut.domain.*;
import it.unibo.ai.didattica.competition.tablut.domain.State.Turn;

//public class MyTablutPlayerClient extends TablutClient { // TODO: Scommenta
public class MyTablutPlayerClient extends TablutClient{

 private MyAIPlayerLogic aiLogic;
 private int timeoutSeconds;
 private State.Turn myRole; 
 private Set<String> history;
 
 public MyTablutPlayerClient(String role, int timeout, String serverIP) throws UnknownHostException, IOException{
	 super(role, "N-CODERS", timeout, serverIP);
     this.timeoutSeconds = timeout;   
     this.myRole = State.Turn.valueOf(role.toUpperCase());
     String weightsFile = (this.myRole == State.Turn.WHITE) ? "white_weights.dat" : "black_weights.dat";
     HeuristicWeights weights = loadWeights(weightsFile);
     this.history = new HashSet<String>();

     this.aiLogic = new MyAIPlayerLogic(weights, this.myRole);
     
     System.out.println("Player " + role + " inizializzato con pesi da " + weightsFile);
 }

 public void run() {

		try {
			this.declareName();
		} catch (Exception e) {
			e.printStackTrace();
		}
		State state;
		System.out.println("You are player " + this.getPlayer().toString() + "!");

		while (true) {
			try {
				this.read();
			} catch (ClassNotFoundException | IOException e1) {
				e1.printStackTrace();
				System.exit(1);
			}
			State current = this.getCurrentState();
			if (this.getPlayer().equals(Turn.WHITE)) {
				// Mio turno
				if (current.getTurn().equals(StateTablut.Turn.WHITE)) {
					history.add(current.boardString());
					Action action=null;
					try {
						action = aiLogic.findBestMove(current, timeoutSeconds, -1, history);
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println("Mossa scelta: " + action.toString());
					try {
						this.write(action);
					} catch (ClassNotFoundException | IOException e) {
						e.printStackTrace();
					}
				}
				// Turno dell'avversario
				else if (current.getTurn().equals(StateTablut.Turn.BLACK)) {
					System.out.println("Waiting for your opponent move... ");
				}
				// ho vinto
				else if (current.getTurn().equals(StateTablut.Turn.WHITEWIN)) {
					System.out.println("YOU WIN!");
					System.exit(0);
				}
				// ho perso
				else if (current.getTurn().equals(StateTablut.Turn.BLACKWIN)) {
					System.out.println("YOU LOSE!");
					System.exit(0);
				}
				// pareggio
				else if (current.getTurn().equals(StateTablut.Turn.DRAW)) {
					System.out.println("DRAW!");
					System.exit(0);
				}

			} else {

				// Mio turno
				if (current.getTurn().equals(StateTablut.Turn.BLACK)) {
					history.add(current.boardString());
					Action action = null;
					try {
						action = aiLogic.findBestMove(current, timeoutSeconds, -1, history);
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println("Mossa scelta: " + action.toString());
					try {
						this.write(action);
					} catch (ClassNotFoundException | IOException e) {
						e.printStackTrace();
					}

				}

				else if (current.getTurn().equals(StateTablut.Turn.WHITE)) {
					System.out.println("Waiting for your opponent move... ");
				} else if (current.getTurn().equals(StateTablut.Turn.WHITEWIN)) {
					System.out.println("YOU LOSE!");
					System.exit(0);
				} else if (current.getTurn().equals(StateTablut.Turn.BLACKWIN)) {
					System.out.println("YOU WIN!");
					System.exit(0);
				} else if (current.getTurn().equals(StateTablut.Turn.DRAW)) {
					System.out.println("DRAW!");
					System.exit(0);
				}
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
 * @throws IOException 
 * @throws UnknownHostException 
  */
 public static void main(String[] args) throws UnknownHostException, IOException {
     String role = args[0]; // "WHITE" o "BLACK"
     int timeout = Integer.parseInt(args[1]); // Timeout in secondi
     String serverIP = args[2];
     
     MyTablutPlayerClient client = new MyTablutPlayerClient(role, timeout, serverIP);
     client.run();
 }
}