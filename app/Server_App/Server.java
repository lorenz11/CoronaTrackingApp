/* inspired by https://www.youtube.com/watch?v=cRfsUrU3RjE*/

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


public class Server extends Thread {
    private final int serverPort;

    /**
     * list of connections to users app handled at the moment
     */
    private ArrayList<Connection> connections = new ArrayList<>();

    /**
     * gets a server port (from ServerMain)
     *
     * @param serverPort the port, the server is supposed to be bound to
     */
    public Server(int serverPort) {
        this.serverPort = serverPort;
    }

    @Override
    public void run() {
        try {
            // creates the server bound to the specified port
            ServerSocket serverSocket = new ServerSocket(serverPort);
            while (true) {
                System.out.println("About to accept client connection...");

                // wait for a client, establish connection represented in clientSocket (thread stops until new client is accepted)
                Socket clientSocket = serverSocket.accept();

                System.out.println("Accepted connection from " + clientSocket);

                //  handles all tasks concerning communication with a user
                Connection connection = new Connection(this, clientSocket);

                // adds the connection to a list of already existing connections
                this.connections.add(connection);

                // starts the connection
                connection.start();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
