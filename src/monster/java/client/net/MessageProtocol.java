package monster.java.client.net;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import monster.java.client.MonsterGame;
import monster.java.client.gui.MainMenu;
import monster.java.client.world.Entity;

public class MessageProtocol {
	
	private static Pattern movePattern = Pattern
			.compile("([a-z]*):(\\d*),(\\d*),(\\d*)");

	public static void sendReady() {
		MonsterGame.instance.client.send("ready");
	}
	
	public static void sendMove(int x, int y) {
		String msg = "mv:" + x + "," + y;
		MonsterGame.instance.client.send(msg);
	}
	
	/**
	 * Send the server the local time survived.
	 * Usually not accurate between all players, but it looks cool
	 * when it ticks up
	 */
	public static void sendTime() {
		String msg = String.format("time:%.02f;", MonsterGame.instance.game.go.time);
		MonsterGame.instance.client.send(msg);
	}
	
	/**
	 * Process a line from the server
	 * 
	 * @param line
	 * @throws IOException 
	 */
	public static void processLine(String line) throws IOException {
		
		System.out.println(line);
		
		for (String msg : line.split(";")) {
			// player or monster move
			if (msg.startsWith("mv:")) {
				processMove(msg);
			
			// player id from server
			} else if (msg.startsWith("player:")) {
				int id = Integer.parseInt(msg.split(":")[1]);
				MonsterGame.instance.game.addLocalPlayer(id + 1);
				// if no other players, send the requested numPlayers to
				// the server
				if (id == 0) {
					int numPlayers = 0;
					while (numPlayers == 0) {
						try {
							numPlayers = Integer.parseInt(MainMenu.GameMenu.playerNumbers);
						} catch (NumberFormatException e) {
							numPlayers = 0;
						}
					}
					MonsterGame.instance.client.send("num:" + numPlayers);
				}
			
			// start the game after all clients are connected
			} else if (msg.equals("begin")) {
				MonsterGame.instance.game.start();
				
			// the world as a string
			} else if (msg.startsWith("world:")) {
				processWorld(msg);
			
			// player death
			} else if (msg.startsWith("kill:")) {
				processDeath(msg);
				sendTime();
				
			// game over
			} else if (msg.startsWith("end:")) {
				processEnd(msg);
			}
		}
	}
	
	/**
	 * Set winData in GameOverlay, which will be displayed in the
	 * main render loop in Game
	 * @param endMsg
	 */
	private static void processEnd(String endMsg) {
		MonsterGame.instance.game.go.setWinData(endMsg.replace("end:", ""));
	}
	
	/**
	 * Process deaths of each player
	 * @param deathMsg
	 */
	public static void processDeath(String deathMsg) {
		int player = Integer.parseInt(deathMsg.replace("kill:", ""));
		MonsterGame.instance.game.killPlayer(player);
	}
	
	public static void processWorld(String worldMsg) {
		
		String[] worldStrings = worldMsg.replace("world:", "").split(",");
		MonsterGame.instance.game.loadWorld(worldStrings);
		
	}
	
	/**
	 * Process moves of the players
	 * @param moveMsg
	 * @throws IOException 
	 */
	public static void processMove(String moveMsg) throws IOException {
		/* Message looks like
		 * mv:pID,x,y
		 * where pID is playerID or 0 [MonsterID] and x & y are integers
		 */
		
		Matcher matcher = movePattern.matcher(moveMsg);
		
		if (!matcher.matches()) {
			System.out.println("Invalid mv message: " + moveMsg);
			throw new IOException();
		}
		
		int player = Integer.parseInt(matcher.group(2));
		int x = Integer.parseInt(matcher.group(3));
		int y = Integer.parseInt(matcher.group(4));
		
		Entity p = MonsterGame.instance.game.getEntity(player);
		p.setPos(x, y);
	}
}
