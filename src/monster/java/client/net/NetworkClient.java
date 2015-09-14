package monster.java.client.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import monster.java.client.MonsterGame;

/**
 * Initializes and handles IO between game and server.
 * 
 * @author Alex
 *
 */
public class NetworkClient extends Thread {
	
	private PrintWriter out;
	private BufferedReader in;
	private Socket clientSocket;

	public NetworkClient(String host, int port) {

		try {
			this.clientSocket = new Socket(host, port);
			
			System.out.println("Connected to server.");
			
			this.out = new PrintWriter(clientSocket.getOutputStream(), true);
			
			this.in = new BufferedReader(new InputStreamReader(
					clientSocket.getInputStream()));

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public void run() {
		String fromServer;
		try {
			while ((fromServer = in.readLine()) != null) {

				MessageProtocol.processLine(fromServer);

			}
			
			this.clientSocket.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public PrintWriter getPrintWriterOut() {
		return out;
	}
}
