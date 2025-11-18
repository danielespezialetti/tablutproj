package ailogicbusiness;

import it.unibo.ai.didattica.competition.tablut.domain.State;
import it.unibo.ai.didattica.competition.tablut.domain.State.*;

import java.util.List;
import java.util.ArrayList;

public class FeaturesExtractor {
	private State state;
	private Pawn[][] board;
	
	private int[] kingPos;
	private List<int[]> whiteSoldiers;
	private List<int[]> blackPawns;

	static final int[][] LOOKUPTABLE = {
			{1,0,0,1,2},
			{0,1,1,2,3},
			{0,1,2,3,4},
			{1,2,3,4,5},
			{2,3,4,5,6}
	};

	static final int[][] CITADELS = { // 1 = Accampamento, 2 = Trono
			{0,0,0,1,1,1,0,0,0},
			{0,0,0,0,1,0,0,0,0},
			{0,0,0,0,0,0,0,0,0},
			{1,0,0,0,0,0,0,0,1},
			{1,1,0,0,2,0,0,1,1},
			{1,0,0,0,0,0,0,0,1},
			{0,0,0,0,0,0,0,0,0},
			{0,0,0,0,1,0,0,0,0},
			{0,0,0,1,1,1,0,0,0}
	};
	
	public FeaturesExtractor() {
		this.whiteSoldiers = new ArrayList<>();
		this.blackPawns = new ArrayList<>();
	}

	/**
	 * Metodo principale. Calcola tutte le feature.
	 */
	public double[] extractFeatures(State state) {
		this.setState(state);
		this.setBoard(this.getState().getBoard());
		
		this.scanBoard(); 
		
		if (this.kingPos == null) {
			// Re catturato o stato non valido
			return new double[15]; // Ritorna array vuoto
		}
		
		int kr = this.kingPos[0];
		int kc = this.kingPos[1];
		
		int[] kingThreats = this.feature_kingThreats(kr, kc);
		int[] totalMobility = this.feature_totalMobility();

		double[] features = {
			(double) this.feature_whiteCount()/8.0, 
			(double) -this.feature_blackCount()/16.0,
			(double) -kingThreats[0]/4.0, // Minacce adiacenti (0-4)
			(double) -kingThreats[1]/2.0, // Minacce a tenaglia (0-4)
			(double) this.feature_kingGuards(kr, kc)/4.0,
			(double) this.feature_supportSoldiers(kr, kc)/2.0,
			(double) -this.feature_whiteInDanger()/8.0,
			(double) this.feature_blackInDanger()/16.0,
			(double) this.feature_multipleEscapeRoutes(kr, kc, kingThreats[1])/2.0,
			(double) totalMobility[0]/40.0,
			(double) -totalMobility[1]/48.0,
			//dynamic features
			(double) -this.feature_kingManhattanDist(kr, kc)/6.0,
			(double)  this.feature_kingFreedom(kr, kc),
			(double) -this.feature_blackProximity(kr, kc)/16.0,
			(double) this.feature_whiteProximity(kr, kc)/8.0,
		};
		
		return features;
	}

	/**
	 * OTTIMIZZAZIONE: Scansiona la scacchiera UNA VOLTA
	 */
	private void scanBoard() {
		this.kingPos = null;
		this.whiteSoldiers.clear();
		this.blackPawns.clear();
		
		for (int r = 0; r < 9; r++) {
			for (int c = 0; c < 9; c++) {
				Pawn p = this.board[r][c];
				if (p == Pawn.KING) {
					this.kingPos = new int[]{r, c};
				} else if (p == Pawn.WHITE) {
					this.whiteSoldiers.add(new int[]{r, c});
				} else if (p == Pawn.BLACK) {
					this.blackPawns.add(new int[]{r, c});
				}
			}
		}
	}

	// --- 1. FEATURE CONTEGGIO ---
	
	private int feature_whiteCount() {
		return this.whiteSoldiers.size();
	}
	
	private int feature_blackCount() {
		return this.blackPawns.size();
	}

	// --- 2. FEATURE POSIZIONE RE ---
	
	private int feature_kingManhattanDist(int kr, int kc) {
		int rr = kr <= 4 ? kr : 8 - kr;
		int cc = kc <= 4 ? kc : 8 - kc;
		return LOOKUPTABLE[rr][cc];
	}

	// --- 3. FEATURE LIBERTÀ RE ---
	
	/**
	 * CORRETTO: Usa la nuova logica di pathClearBetween
	 * che sa che il Re non può passare sul Trono (Regola 2).
	 */
	private int feature_kingFreedom(int kr, int kc) {
		int freedom=0;
		
		// UP
		for (int i = kr - 1; i >= 0; i--) {
			if (!pathClearBetween(kingPos, new int[]{i, kc}, Pawn.KING)) break;
			freedom++;
		}
		// RIGHT
		for (int j = kc + 1; j < 9; j++) {
			if (!pathClearBetween(kingPos, new int[]{kr, j}, Pawn.KING)) break;
			freedom++;
		}
		// DOWN
		for (int i = kr + 1; i < 9; i++) {
			if (!pathClearBetween(kingPos, new int[]{i, kc}, Pawn.KING)) break;
			freedom++;
		}
		// LEFT
		for (int j = kc - 1; j >= 0; j--) {
			if (!pathClearBetween(kingPos, new int[]{kr, j}, Pawn.KING)) break;
			freedom++;
		}
		return freedom;
	}

	// --- 4. FEATURE PERICOLO RE (RISCRITTA) ---
	
	/**
	 * RISCRITTA: Implementa le 3 regole di cattura del Re.
	 * [0] = Minacce adiacenti (lati coperti)
	 * [1] = Minacce a tenaglia (lati vuoti che un nero può raggiungere
	 * per chiudere una cattura)
	 */
	private int[] feature_kingThreats(int kr, int kc) {
		int[] threats = {0, 0};
		
		// REGOLA 6c: Re SUL TRONO (4 neri)
		if (CITADELS[kr][kc] == 2) {
			int[][] adjacent = {{kr-1, kc}, {kr, kc+1}, {kr+1, kc}, {kr, kc-1}};
			for (int[] pos : adjacent) {
				if (!isOutOfBounds(pos[0], pos[1]) && board[pos[0]][pos[1]] == Pawn.BLACK) {
					threats[0]++;
				}
			}
			return threats; // Nessuna minaccia a tenaglia possibile
		}
		
		// REGOLA 6b: Re ADIACENTE AL TRONO (3 neri)
		if (isKingAdjacentToThrone(kr, kc)) {
			int[][] adjacent = {{kr-1, kc}, {kr, kc+1}, {kr+1, kc}, {kr, kc-1}};
			boolean thr = false;
			for (int[] pos : adjacent) {
				int ar = pos[0];
				int ac = pos[1];
				if (isOutOfBounds(ar, ac)) continue; 
				
				// Ignora il lato del Trono
				if (CITADELS[ar][ac] == 2) continue; 
				
				
				if (board[ar][ac] == Pawn.BLACK || CITADELS[ar][ac] == 1) {
					threats[0]++;
				} else if (board[ar][ac] == Pawn.EMPTY && canBlackReach(ar, ac)) {
					thr=true; // Minaccia a tenaglia
				}
			}
			if (threats[0]==2 && thr)
				threats[1]++;
			return threats;
		}

		// REGOLA 6a: Re in campo aperto (2 lati opposti)
		int[][] oppositePairs = {
			{kr-1, kc, kr+1, kc}, // UP vs DOWN
			{kr, kc-1, kr, kc+1}  // LEFT vs RIGHT
		};
		
		for (int[] pair : oppositePairs) {
			int r1 = pair[0], c1 = pair[1];
			int r2 = pair[2], c2 = pair[3];
			
			boolean threat1 = isHostileForKing(r1, c1);
			boolean threat2 = isHostileForKing(r2, c2);
			boolean empty1 = isEmptyAndReachable(r1, c1);
			boolean empty2 = isEmptyAndReachable(r2, c2);
			
			if (threat1) threats[0]++;
			if (threat2) threats[0]++;
			
			// Minaccia a tenaglia
			if (threat1 && empty2) threats[1]++;
			if (threat2 && empty1) threats[1]++;
		}
		// Normalizza minacce adiacenti (non può essere > 4)
		threats[0] = Math.min(4, threats[0]);
		
		return threats;
	}
	
	private int feature_multipleEscapeRoutes(int kr, int kc, int kingThreats) {
	    int viablePaths = 0;
	    
	    // Controlla ogni direzione cardinale
	    int[][] directions = {{-1,0}, {0,1}, {1,0}, {0,-1}};
	    
	    for (int[] dir : directions) {
	        int r = kr + dir[0];
	        int c = kc + dir[1];
	        
	        // Verifica se esiste un percorso chiaro verso un bordo di fuga
	        while (!isOutOfBounds(r, c)) {
	            if (isObstacle(r, c, Pawn.KING, kr, kc)) break;
	            if (isEscapeTile(r, c)) {
	                viablePaths++;
	                break;
	            }
	            r += dir[0];
	            c += dir[1];
	        }
	    }
	    
	    if (viablePaths >= 2 && kingThreats==0) {
	        return 2;  // Tuicha rimane massimo
	    } else if (viablePaths == 1) {
	        if (kingThreats > 0) {
	            return -2;    // Grave pericolo: via non salva
	        } else if (kingThreats == 0) {
	            return 1;   // Moderato pericolo: via moderatamente preziosa
	        }
	    } else if (viablePaths>=2 && kingThreats>0){
	        return -2;
	    }
		return 0;
	}
	// Helper per kingThreats
	private boolean isHostileForKing(int r, int c) {
		if (isOutOfBounds(r, c)) return true; // Bordo è ostile
		if (board[r][c] == Pawn.BLACK) return true;
		if (CITADELS[r][c] == 1) return true; // Cittadella è ostile
		// Trono non è ostile (solo se adiacente)
		return false; 
	}
	
	// Helper per kingThreats
	private boolean isEmptyAndReachable(int r, int c) {
		if (isOutOfBounds(r, c)) return false;
		if (board[r][c] == Pawn.EMPTY && CITADELS[r][c] == 0) {
			return canBlackReach(r, c);
		}
		return false;
	}
	
	// Helper per kingThreats
	private boolean isKingAdjacentToThrone(int kr, int kc) {
		if (!isOutOfBounds(kr-1, kc) && CITADELS[kr-1][kc] == 2) return true;
		if (!isOutOfBounds(kr+1, kc) && CITADELS[kr+1][kc] == 2) return true;
		if (!isOutOfBounds(kr, kc-1) && CITADELS[kr][kc-1] == 2) return true;
		if (!isOutOfBounds(kr, kc+1) && CITADELS[kr][kc+1] == 2) return true;
		return false;
	}

	// --- 5. FEATURE PROSSIMITÀ NERI (CORRETTA) ---
	
	private int feature_blackProximity(int kr, int kc) {
		int prxm = 0;
		for (int i = kr - 2; i <= kr + 2; i++) {
			for (int j = kc - 2; j <= kc + 2; j++) {
				if (isOutOfBounds(i, j) || (i == kr && j == kc)) {
					continue;
				}
				if (this.board[i][j] == Pawn.BLACK) {
					prxm++;
				}
			}
		}
		return prxm;
	}
	
	private int feature_whiteProximity(int kr, int kc) {
		int prxm = 0;
		for (int i = kr - 2; i <= kr + 2; i++) {
			for (int j = kc - 2; j <= kc + 2; j++) {
				if (isOutOfBounds(i, j) || (i == kr && j == kc)) {
					continue;
				}
				if (this.board[i][j] == Pawn.WHITE) {
					prxm++;
				}
			}
		}
		return prxm;
	}

	// --- 6. FEATURE SUPPORT_SOLDATI ---
	
	private int feature_kingGuards(int kr, int kc) {
		int guards = 0;
		int[][] adjacent = {{kr-1, kc}, {kr, kc+1}, {kr+1, kc}, {kr, kc-1}};
		
		for(int[] pos : adjacent) {
			if (!isOutOfBounds(pos[0], pos[1]) && this.board[pos[0]][pos[1]] == Pawn.WHITE) {
				guards++;
			}
		}
		return guards;
	}
	
	/**
	 * CORRETTO: Usa la nuova logica pathClearBetween
	 */

	private int feature_supportSoldiers(int kr, int kc) {
		int count = 0;

		for (int[] soldierPos : this.whiteSoldiers) {
			int br = soldierPos[0]; // Riga del Bianco
			int bc = soldierPos[1]; // Colonna del Bianco

            // 1. Trova le vie di fuga (ora ritorna un int[4])
			// Indici: 0=UP, 1=RIGHT, 2=DOWN, 3=LEFT
            // Valore: -1 se bloccato, altrimenti l'indice della riga/colonna
			int[] escapePaths = getSoldierEscapePaths(soldierPos);

			boolean kingCanSupport = false;

			// Controlla UP (paths[0] contiene la colonna 'bc' se libera)
			if (escapePaths[0] != -1) {
				if (kc != bc && kr < br && pathClearBetween(kingPos, new int[]{kr, bc}, Pawn.KING)) {
					kingCanSupport = true;
				}
			}

			// Controlla RIGHT (paths[1] contiene la riga 'br' se libera)
			if (!kingCanSupport && escapePaths[1] != -1) {
				if (kr != br && kc > bc && pathClearBetween(kingPos, new int[]{br, kc}, Pawn.KING)) {
					kingCanSupport = true;
				}
			}

			// Controlla DOWN (paths[2] contiene la colonna 'bc' se libera)
			if (!kingCanSupport && escapePaths[2] != -1) {
				if (kc != bc && kr > br && pathClearBetween(kingPos, new int[]{kr, bc}, Pawn.KING)) {
					kingCanSupport = true;
				}
			}

			// Controlla LEFT (paths[3] contiene la riga 'br' se libera)
			if (!kingCanSupport && escapePaths[3] != -1) {
				if (kr != br && kc < bc && pathClearBetween(kingPos, new int[]{br, kc}, Pawn.KING)) {
					kingCanSupport = true;
				}
			}
			
			if (kingCanSupport) {
				count++;
			}
		}
		return count;
	}

	// --- 7. FEATURE SCAMBIO PEZZI (RISCRITTA) ---

	private int feature_whiteInDanger() {
		int inDanger = 0;
		for (int[] sPos : this.whiteSoldiers) {
			if (isPawnInDanger(sPos, Pawn.BLACK)) {
				inDanger++;
			}
		}
		return inDanger;
	}
	
	private int feature_blackInDanger() {
		int inDanger = 0;
		for (int[] bPos : this.blackPawns) {
			if (isPawnInDanger(bPos, Pawn.WHITE)) {
				inDanger++;
			}
		}
		return inDanger;
	}
	
	/**
	 * RISCRITTO: Implementa la Regola 5 (cattura soldati).
	 * Controlla se 'pawnPos' (di tipo 'defenderType') può essere
	 * catturato da 'attackerType'.
	 */
	private boolean isPawnInDanger(int[] pawnPos, Pawn attackerType) {
		int r = pawnPos[0];
		int c = pawnPos[1];

		// Controlla minacce di tenaglia (pericolo futuro)
		int[][] pairs = {
			{r-1, c, r+1, c}, // U/D
			{r, c-1, r, c+1}  // L/R
		};
		
		for (int[] pair : pairs) {
			int r1 = pair[0], c1 = pair[1];
			int r2 = pair[2], c2 = pair[3];

			// Controlla se un lato è già una minaccia...
			boolean threat1 = isAttacker(r1, c1, attackerType) || isHostileWall(r1, c1);
			// ...e l'altro lato è vuoto e raggiungibile
			boolean empty2 = isEmptyAndReachable(r2, c2, attackerType);
			
			if (threat1 && empty2) return true;
			
			// Controlla viceversa
			boolean threat2 = isAttacker(r2, c2, attackerType) || isHostileWall(r2, c2);
			boolean empty1 = isEmptyAndReachable(r1, c1, attackerType);
			
			if (threat2 && empty1) return true;
		}
		return false;
	}
	
	// Helper per 'isPawnInDanger'
	private boolean isAttacker(int r, int c, Pawn attackerType) {
		if (isOutOfBounds(r, c)) return false;
		if (board[r][c] == attackerType) return true;
		// Il Re conta come un attaccante BIANCO
		if (attackerType == Pawn.WHITE && board[r][c] == Pawn.KING) return true;
		return false;
	}
	
	// Helper per 'isPawnInDanger'
	private boolean isHostileWall(int r, int c) {
		if (isOutOfBounds(r, c)) return false;
		// Il Trono (2) è sempre un muro ostile
		if (CITADELS[r][c] != 0) return true;
		return false;
	}

	// Helper per 'isPawnInDanger'
	private boolean isEmptyAndReachable(int r, int c, Pawn attackerType) {
		if (isOutOfBounds(r, c)) return false;
		// La casella deve essere vuota e non un ostacolo per l'attaccante
		if (board[r][c] == Pawn.EMPTY) {
			// Un bianco non può atterrare su una cittadella
			if (attackerType == Pawn.WHITE && CITADELS[r][c] == 1) return false;
			// Il re non può atterrare su una cittadella
			if (attackerType == Pawn.KING && CITADELS[r][c] == 1) return false;
			// Il trono è sempre bloccato
			if (CITADELS[r][c] == 2) return false;
			
			// Controlla se un attaccante può raggiungerlo
			return canPawnReach(attackerType, r, c);
		}
		return false;
	}
	
	// Helper per 'isPawnInDanger' e 'kingThreats'
	private boolean canPawnReach(Pawn attackerType, int r, int c) {
		List<int[]> attackers = (attackerType == Pawn.WHITE) ? this.whiteSoldiers : this.blackPawns;
		for (int[] aPos : attackers) {
			if (aPos[0] == r && pathClearBetween(aPos, new int[]{r,c}, attackerType)) return true;
			if (aPos[1] == c && pathClearBetween(aPos, new int[]{r,c}, attackerType)) return true;
		}
		// Aggiungi il Re se l'attaccante è Bianco
		if (attackerType == Pawn.WHITE) {
			if (kingPos[0] == r && pathClearBetween(kingPos, new int[]{r,c}, Pawn.KING)) return true;
			if (kingPos[1] == c && pathClearBetween(kingPos, new int[]{r,c}, Pawn.KING)) return true;
		}
		return false;
	}
	
	/** Versione specifica per i Neri di canPawnReach */
	private boolean canBlackReach(int r, int c) {
		for (int[] bPos : this.blackPawns) {
			if (bPos[0] == r && pathClearBetween(bPos, new int[]{r,c}, Pawn.BLACK)) return true;
			if (bPos[1] == c && pathClearBetween(bPos, new int[]{r,c}, Pawn.BLACK)) return true;
		}
		return false;
	}
	
	// --- METODI HELPER GENERALI (RISCRITTI) ---

	private boolean isOutOfBounds(int r, int c) {
		return r < 0 || r > 8 || c < 0 || c > 8;
	}

	/**
	 * RISCRITTO: Controlla se la casella (r,c) è un ostacolo per
	 * il 'pawnType' che parte da (start_r, start_c).
	 */
	private boolean isObstacle(int r, int c, Pawn pawnType, int start_r, int start_c) {
		if (isOutOfBounds(r, c)) return true;
		
		// REGOLA 2: Nessuno può passare sul Trono
		if (CITADELS[r][c] == 2) return true;
		
		// Ostacolo se c'è un'altra pedina
		if (this.board[r][c] != Pawn.EMPTY) return true;
		
		// Controlla Cittadelle (1)
		if (CITADELS[r][c] == 1) {
			// REGOLA 3: Bianchi e Re non possono entrare
			if (pawnType == Pawn.KING || pawnType == Pawn.WHITE) return true;
			
			// REGOLA 4: I Neri possono solo se sono già partiti da una cittadella
			if (pawnType == Pawn.BLACK && CITADELS[start_r][start_c] != 1) {
				return true; // Bloccato (partito da fuori)
			}
		}
		
		return false; // Casella libera
	}
	
	/**
	 * RISCRITTO: Usa la nuova logica 'isObstacle'
	 */
	private boolean pathClearBetween(int[] pos1, int[] pos2, Pawn pawnType) {
		int r1 = pos1[0], c1 = pos1[1];
		int r2 = pos2[0], c2 = pos2[1];
		
		if (r1 == r2 && c1 == c2) return true;

		if (r1 == r2) { // Stessa riga
			int start = Math.min(c1, c2) + 1;
			int end = Math.max(c1, c2);
			for (int c = start; c < end; c++) {
				// Controlla ostacoli sul percorso
				if (isObstacle(r1, c, pawnType, r1, c1)) return false;
			}
			 // Controlla la destinazione
			 if(isObstacle(r2, c2, pawnType, r1, c1)) return false;

		} else if (c1 == c2) { // Stessa colonna
			int start = Math.min(r1, r2) + 1;
			int end = Math.max(r1, r2);
			for (int r = start; r < end; r++) {
				// Controlla ostacoli sul percorso
				if (isObstacle(r, c1, pawnType, r1, c1)) return false;
			}
			// Controlla la destinazione
			if(isObstacle(r2, c2, pawnType, r1, c1)) return false;
		} else {
			 return false; // Non ortogonali
		}
		return true;
	}

	/**
	 * CORRETTO: Usa la nuova logica pathClearBetween
	 */
	private int[] getSoldierEscapePaths(int[] pos) {
		// Indici: 0=UP, 1=RIGHT, 2=DOWN, 3=LEFT
		int[] paths = new int[]{-1, -1, -1, -1};
		int r = pos[0];
		int c = pos[1];

		// Controlla SU
		if (pathClearBetween(pos, new int[]{0, c}, Pawn.WHITE) && isEscapeTile(0, c)) {
			paths[0] = c; // Salva la colonna
		}
		// Controlla GIU
		if (pathClearBetween(pos, new int[]{8, c}, Pawn.WHITE) && isEscapeTile(8, c)) {
			paths[2] = c; // Salva la colonna
		}
		// Controlla SINISTRA
		if (pathClearBetween(pos, new int[]{r, 0}, Pawn.WHITE) && isEscapeTile(r, 0)) {
			paths[3] = r; // Salva la riga
		}
		// Controlla DESTRA
		if (pathClearBetween(pos, new int[]{r, 8}, Pawn.WHITE) && isEscapeTile(r, 8)) {
			paths[1] = r; // Salva la riga
		}
		
		return paths;
	}
	
	private boolean isEscapeTile(int r, int c) {
		if (isOutOfBounds(r,c)) return false;
		if (CITADELS[r][c] == 2) return false;
		if (CITADELS[r][c] == 1) return false;
		return r == 0 || r == 8 || c == 0 || c == 8;
	}
	
	private int[] feature_totalMobility() {
	    int whiteMobility = 0;
	    int blackMobility = 0;
	    
	    for (int[] pos : this.whiteSoldiers) {
	        whiteMobility += countLegalMoves(pos, Pawn.WHITE);
	    }
	    whiteMobility += countLegalMoves(this.kingPos, Pawn.KING);
	    
	    for (int[] pos : this.blackPawns) {
	        blackMobility += countLegalMoves(pos, Pawn.BLACK);
	    }
	    int [] ris = {whiteMobility, blackMobility};
	    return ris;
	}

	private int countLegalMoves(int[] pos, Pawn type) {
	    int count = 0;
	    int[][] directions = {{-1,0}, {0,1}, {1,0}, {0,-1}};
	    
	    for (int[] dir : directions) {
	        int r = pos[0] + dir[0];
	        int c = pos[1] + dir[1];
	        while (!isOutOfBounds(r, c) && !isObstacle(r, c, type, pos[0], pos[1])) {
	            count++;
	            r += dir[0];
	            c += dir[1];
	        }
	    }
	    return count;
	}

	public State getState() { return state; }
	public void setState(State state) { this.state = state; }
	public Pawn[][] getBoard() { return board; }
	public void setBoard(Pawn[][] board) { this.board = board; }
}