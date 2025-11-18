package it.unibo.ai.didattica.competition.tablut.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

import it.unibo.ai.didattica.competition.tablut.domain.Action;
import it.unibo.ai.didattica.competition.tablut.domain.State;
import it.unibo.ai.didattica.competition.tablut.domain.StateTablut;

public class TablutAiClient extends TablutClient {

	public TablutAiClient(String player) throws UnknownHostException, IOException {
		super(player, "AI");
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub

	}
	


}
