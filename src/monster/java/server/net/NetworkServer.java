package monster.java.server.net;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Scanner;

import monster.java.server.MonsterServer;
import monster.java.server.world.Entity;
import monster.java.server.world.Monster;

public class NetworkServer extends Thread {

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
	
	public void destroy() {
		try {
			this.serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
			while (!(this.numPlayers == 1 && this.players.size() == 1) && (this.readyPlayers == 0
					|| (this.readyPlayers < this.numPlayers 
					&& i < this.numPlayers))) {
				// add new NetworkPlayer object to list
				this.players.add(new NetworkPlayer(this.serverSocket.accept(),
						i));
				
				// Send an initial message to the client
				MessageProtocol.sendWorld(this.players.get(i), loadWorld());
				this.players.get(i).send("player:" + i);
				this.players.get(i).setName("Player " + (i + 1));
				
				// sleep until the num players is set by p1
				if (i == 0) {
					//System.out.println("Waiting for player count...");
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
		this.init();
		
		boolean exit = false;
		
		// Allow players to move around before monster
		this.sleepn(3);
		
		while(!exit) {
			// Begin the AI movement
			monster.moveToPlayer(players);
			
			// Increase the speed by 1%
			MonsterServer.MON_TICK = (int) Math.ceil(MonsterServer.MON_TICK*0.99);
			
			for (NetworkPlayer player : this.players) {
				Entity playerObj = player.getPlayer();
				if (playerObj.isAlive()) {
					if (playerObj.X() == monster.X() && playerObj.Y() == monster.Y()) {
						// if player at monster's position, broadcast the
						// kill message and remove the player from the array
						MessageProtocol.sendKill(player);
						playerObj.kill();
						playerObj.setRank(numAlivePlayers());
						System.out.println("Player " + player.getID() + " died.");
					}
				}
			}
			
			// Implement this after finding out all players are dead
			if (numAlivePlayers() == 0)
				exit = true;
			
			if (numConnectedPlayers() == 0)
				return;
			
		}
		
		// create and send win message
		String winMsg = "end:";
		for (int i = 0; i < this.players.size(); i++) {
			winMsg = winMsg + getRankedPlayer(i).getName() + ",";
		}
		// replace last comma with semi
		winMsg = winMsg.substring(0, winMsg.length() - 1) + ";";
		this.broadcast(winMsg);
		
		// close connections
		for (NetworkPlayer player : this.players) {
			player.close();
		}
	}
	
	public NetworkPlayer getRankedPlayer(int i) {
		for (NetworkPlayer player : this.players) {
			if (player.getPlayer().getRank() == i)
				return player;
		}
		return null;
	}
	
	public int numAlivePlayers() {
		int i = 0;
		for (NetworkPlayer player : this.players)
			if (player.getPlayer().isAlive())
				i++;
		return i;
	}
	
	public int numConnectedPlayers() {
		int i = 0;
		for (NetworkPlayer player : this.players)
			if (player.connected)
				i++;
		return i;
		
	}
	
	/* * * Getters and Setters * * */
	
	public String[] getWorld() {
		return this.world;
	}
	
	public int getWorldSize() {
		return this.world.length;
	}
	
	/* * * DEBUGGING CODE BELOW * * */
	
	private void sleepn(float n) {
		try {
			Thread.sleep((int) (n * 1000));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
