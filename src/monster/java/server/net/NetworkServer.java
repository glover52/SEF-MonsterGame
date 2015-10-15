package monster.java.server.net;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Scanner;

import org.junit.Test;
import static org.junit.Assert.*;

import monster.java.server.MonsterServer;
import monster.java.server.world.Monster;

public class NetworkServer {

	private int port;
	private ServerSocket serverSocket;
	private ArrayList<NetworkPlayer> players;
	private int readyPlayers = 0;
	private Monster monster;
	private String[] world;
	private int numPlayers = 5;

	public NetworkServer(int port) {
		this.port = port;
		this.players = new ArrayList<NetworkPlayer>();
	}
	
	/**
	 * Broadcast a message to all connected players
	 * 
	 * @param msg
	 */
	public void broadcast(String msg) {
		for (NetworkPlayer client : players) {
			client.send(msg);
		}
	}

	/**
	 * Increment the ready counter to break from the initialization loop.
	 */
	public void addReady() {
		this.readyPlayers++;
		System.out.println(this.readyPlayers + " player(s) ready.");
	}
	
	public void setNumPlayers(int numPlayers) {
		this.numPlayers = numPlayers;
	}
	
	private String loadWorld() throws FileNotFoundException {
		Scanner in = new Scanner(new FileReader("world.txt"));
		StringBuilder sb = new StringBuilder();
		
		while (in.hasNextLine()) {
			sb.append(in.nextLine() + ",");
		}
		
		world = sb.toString().split(",");
		
		in.close();
		return sb.toString();
	}

	/**
	 * Initialize connections with player clients, and create new threads for
	 * each ongoing conversation.
	 */
	public void init() {

		try {
			System.out.println("Working Directory = " +
		              System.getProperty("user.dir"));
			
			// Create the server socket
			this.serverSocket = new ServerSocket(this.port);

			System.out.println("Waiting for players on port " + this.port);
			
			int i = 0;
			// loop while less than 4 players and not all players are ready
			while (this.readyPlayers == 0
					|| (this.readyPlayers < this.numPlayers 
					&& i < this.numPlayers)) {
				// add new NetworkPlayer object to list
				this.players.add(new NetworkPlayer(this.serverSocket.accept(),
						i));
				
				// Send an initial message to the client
				MessageProtocol.sendWorld(this.players.get(i), loadWorld());
				this.players.get(i).send("player:" + i);
				
				// sleep until the num players is set by p1
				if (i == 0) {
					System.out.println("Waiting for player count...");
					while (this.numPlayers == 5) {
						Thread.sleep(1000);
					}
				}
				i++;
			}
			
			System.out.println(i + " players ready, starting game.");
			
			Thread.sleep(1000);
			
			MessageProtocol.sendBegin();
			
			// Create the Monster
			monster = new Monster(world);
			monster.setPos((int) Math.ceil(world.length/2), 
						   (int) Math.ceil(world.length/2));

		} catch (IOException e) {

			System.out.println("Error connecting to players.");
			e.printStackTrace();

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Server-side game loop
	 */
	public void run() {
		boolean exit = false;
		
		// Allow players to move around before monster
		try {
			Thread.sleep((int) (3000));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		while(!exit) {
			// Begin the AI movement
			monster.moveToPlayer(players);
			
			// Increase the speed by 1%
			MonsterServer.MON_TICK = (int) Math.ceil(MonsterServer.MON_TICK*0.99);
			
			// Implement this after finding out all players are dead
			if(exit != false)
				exit = true;
		}
	}
	
	/* * * Getters and Setters * * */
	public String[] getWorld() {
		return this.world;
	}
	
	public int getWorldSize() {
		return this.world.length;
	}
	
	/* * * JUnit Testing Below * * */
	@Test
	public void monsterInDomain() {
		// Verify that monster does not move outside the grid
		
		// Check left boundary
		assertTrue(monster.X() > 0);
		
		// Check top boundary
		assertTrue(monster.Y() > 0);
		
		// Check right boundary
		assertTrue(monster.X() < getWorldSize());
		
		// Check bottom boundary
		assertTrue(monster.Y() < getWorldSize());
	}
	
	@Test
	public void gameStartWithMaxPlayers() {
		// Verify that the game does not start until the specified number of players join
		
		// Check that maximum number of players is not exceeded
		assertTrue(this.numPlayers < 5);
		
		// Check that the number of ready players equals number of required players
		assertEquals(this.numPlayers, this.readyPlayers);
	}
}
