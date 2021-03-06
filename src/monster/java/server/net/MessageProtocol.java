package monster.java.server.net;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import monster.java.server.MonsterServer;

public class MessageProtocol {

	// regex pattern to match move msg
	private static Pattern movePattern = Pattern
			.compile("([a-z]*):(\\d*),(\\d*)");

	// OUTGOING MESSAGES //
	
	/**
	 * Send the world as a string to a client
	 * 
	 * @param client
	 * @param worldString
	 */
	public static void sendWorld(NetworkPlayer client, String worldString) {
		client.send("world:" + worldString);
	}
	
	/**
	 * Broadcast a player's movement to all clients
	 * 
	 * @param client
	 * @param x
	 * @param y
	 */
	public static void sendMove(NetworkPlayer client, int x, int y) {
		String msg = "mv:" + client.getID() + "," + x + "," + y + ";";		
		MonsterServer.server.broadcast(msg);
	}
	
	/**
	 * Broadcast after a player disconnects
	 * 
	 * @param client
	 */
	public static void sendDisconnect(NetworkPlayer client) {
		String msg = "dc:" + client.getID() + ";";
		MonsterServer.server.broadcast(msg);
	}
	
	/**
	 * Broadcast a player's death
	 * 
	 * @param client
	 */
	public static void sendKill(NetworkPlayer client) {
		String msg = "kill:" + client.getID();
		MonsterServer.server.broadcast(msg);
	}
	
	/**
	 * Broadcast monster movement to all clients
	 * 
	 * @param x
	 * @param y
	 */
	public static void sendMonsterMove(int x, int y) {
		String msg = "mv:0," + x + "," + y + ";";
		MonsterServer.server.broadcast(msg);
	}
	
	public static void sendBegin() {
		MonsterServer.server.broadcast("begin");
	}
	
	// INCOMING MESSAGES //
	
	/**
	 * Process a message from a client and perform appropriate actions
	 * 
	 * @param client
	 * @param line
	 * @throws IOException
	 */
	public static void process(NetworkPlayer client, String line)
			throws IOException {

		for (String msg : line.split(";")) {

			// player move
			if (msg.startsWith("mv:"))
				processMove(client, msg);
			// num players
			else if (msg.startsWith("num:"))
				processNumPlayers(client, msg);
			// player time
			else if (msg.startsWith("time:"))
				processPlayerTime(client, msg);
			
		}

	}
	
	/**
	 * Process a player sending their play time
	 * 
	 * @param client
	 * @param timeMsg
	 */
	private static void processPlayerTime(NetworkPlayer client, String timeMsg) {
		float time = Float.parseFloat(timeMsg.replace("time:", ""));
		client.setTime(time);
	}
	
	/**
	 * Process the first player setting numPlayers
	 * 
	 * @param client
	 * @param npMsg
	 */
	private static void processNumPlayers(NetworkPlayer client, String npMsg) {
		int numPlayers = Integer.parseInt(npMsg.split(":")[1]);
		MonsterServer.server.setNumPlayers(numPlayers);		
	}

	/**
	 * Process a player move
	 * 
	 * @param client
	 * @param mvMsg
	 * @throws IOException
	 */
	private static void processMove(NetworkPlayer client, String mvMsg)
			throws IOException {

		// Message should look like
		// mv:10,12
		// (where 10 is x and 12 is y)
		
		// prevents glitching on scores being displayed
		if (!client.getPlayer().isAlive())
			return;

		Matcher matcher = movePattern.matcher(mvMsg);

		if (!matcher.matches()) {
			System.out.println("Invalid mv message: " + mvMsg);
			throw new IOException();
		}

		int x = Integer.parseInt(matcher.group(2));
		int y = Integer.parseInt(matcher.group(3));

		// update player, broadcast move
		client.getPlayer().setPos(x, y);
		sendMove(client, x, y);
		
	}

}
