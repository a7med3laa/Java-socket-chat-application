import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

/*
 * The server that can be run both as a console application
 */
public class Server {
	private static int uniqueId;
	private ArrayList<ClientThread> clientList;
	private SimpleDateFormat CurrentDate;
	private int port;
	private boolean keepGoing;

	public Server(int port) {
		this.port = port;
		CurrentDate = new SimpleDateFormat("HH:mm:ss");
		clientList = new ArrayList<ClientThread>();
	}

	public void start() {
		keepGoing = true;
		try {
			ServerSocket serverSocket = new ServerSocket(port);

			while (keepGoing) {
				display("Server waiting for Clients on port " + port + ".");
				// accept connection
				Socket socket = serverSocket.accept();
				// if I was asked to stop
				if (!keepGoing)
					break;
				ClientThread t = new ClientThread(socket);
				clientList.add(t); // save it in the ArrayList
				t.start();
			}
			// I was asked to stop
			try {
				serverSocket.close();
				for (int i = 0; i < clientList.size(); ++i) {
					ClientThread tc = clientList.get(i);
					try {
						tc.sInput.close();
						tc.sOutput.close();
						tc.socket.close();
					} catch (IOException ioE) {
						// not much I can do
					}
				}
			} catch (Exception e) {
				display("Exception closing the server and clients: " + e);
			}
		} catch (IOException e) {
			String msg = CurrentDate.format(new Date())
					+ " Exception on new ServerSocket: " + e + "\n";
			display(msg);
		}
	}

	private void display(String msg) {
		String time = CurrentDate.format(new Date()) + " " + msg;
		System.out.println(time);
	}

	private synchronized void broadcast(String message) {
		String time = CurrentDate.format(new Date());
		String messageLf = time + " " + message + "\n";
		System.out.print(messageLf);

		// we loop in reverse order in case we would have to remove a Client
		// because it has disconnected
		for (int i = clientList.size(); --i >= 0;) {
			ClientThread ct = clientList.get(i);
			if (!ct.writeMsg(messageLf)) {
				clientList.remove(i);
				display("Disconnected Client " + ct.username
						+ " removed from list.");
			}
		}
	}

	synchronized void remove(int id) {
		for (int i = 0; i < clientList.size(); ++i) {
			ClientThread ct = clientList.get(i);
			if (ct.id == id) {
				clientList.remove(i);
				return;
			}
		}
	}

	public static void main(String[] args) {
		int portNumber;
		Scanner scan = new Scanner(System.in);
		System.out.println("Enter your server port");
		portNumber = scan.nextInt();
		Server server = new Server(portNumber);
		server.start();
	}

	class ClientThread extends Thread {
		Socket socket;
		ObjectInputStream sInput;
		ObjectOutputStream sOutput;
		int id;
		// the User name of the Client
		String username;
		// the date I connect
		String date;

		ClientThread(Socket socket) {
			// a unique id
			id = ++uniqueId;
			this.socket = socket;
			System.out
					.println("Thread trying to create Object Input/Output Streams");
			try {

				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput = new ObjectInputStream(socket.getInputStream());

				username = (String) sInput.readObject();
				display(username + " just connected.");
			} catch (IOException e) {
				display("Exception creating new Input/output Streams: " + e);
				return;
			} catch (ClassNotFoundException e) {
			}
			date = new Date().toString() + "\n";
		}

		// what will run forever
		public void run() {
			// to loop until LOGOUT
			String message;
			boolean keepGoing = true;
			while (keepGoing) {
				try {
					message = (String) sInput.readObject();
				} catch (IOException e) {
					display(username + " Exception reading Streams: " + e);
					break;
				} catch (ClassNotFoundException e2) {
					break;
				}
				String s = message.substring(0, message.indexOf("-"));
				String msg = message.substring(message.indexOf("-") + 1);
				int type = Integer.parseInt(s.substring(0, s.indexOf(" ")));
				// Switch on the type of message receive
				switch (type) {

				case 0: {
					if (!msg.equals(" "))
						broadcast(username + ": " + msg);
					break;
				}
				case 1:
					display(username + " disconnected with a LOGOUT message.");
					keepGoing = false;
					break;
				case 2:
					writeMsg("List of the users connected at "
							+ CurrentDate.format(new Date()) + "\n");
					for (int i = 0; i < clientList.size(); ++i) {
						ClientThread ct = clientList.get(i);
						writeMsg((i + 1) + ") " + ct.username + " since "
								+ ct.date);
					}
					break;
				}
			}
			remove(id);
			close();
		}

		// try to close everything
		private void close() {
			// try to close the connection
			try {
				if (sOutput != null)
					sOutput.close();
			} catch (Exception e) {
			}
			try {
				if (sInput != null)
					sInput.close();
			} catch (Exception e) {
			}
			;
			try {
				if (socket != null)
					socket.close();
			} catch (Exception e) {
			}
		}

		private boolean writeMsg(String msg) {
			// if Client is still connected send the message to it
			if (!socket.isConnected()) {
				close();
				return false;
			}
			// write the message to the stream
			try {
				sOutput.writeObject(msg);
			}
			// if an error occurs, do not abort just inform the user
			catch (IOException e) {
				display("Error sending message to " + username);
				display(e.toString());
			}
			return true;
		}
	}
}