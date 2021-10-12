/* inspired by https://www.youtube.com/watch?v=cRfsUrU3RjE*/

import java.io.*;
import java.net.Socket;

public class Connection extends Thread {
    private PrintWriter writer;
    /**
     * the client socket, that represents the connection to a single client
     */
    private final Socket socket;
    /**
     * instance of the server itself, passed to the constructor of Connection
     */
    private final Server server;
    /**
     * the data stream from the server to this particular client
     */
    private OutputStream outputStream;

    /**
     * instance gets copy of reference to server itself and the socket, representing the connection of a client to the server.
     *
     * @param server
     * @param clientSocket
     */
    public Connection(Server server, Socket clientSocket) {
        this.server = server;
        this.socket = clientSocket;
    }

    @Override
    public void run() {
        try {
            // handles commands from the client (passed in InputStream)
            connectClient();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * handles requests for list of infected people and "I'm infected" messages a client is passing to the server
     *
     * @throws IOException handled in the run method of the thread
     */
    private void connectClient() throws IOException {
        // establishes the input(stream) from the client to the server
        InputStream inputStream = socket.getInputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        outputStream = socket.getOutputStream();

        // contains messages from node
        String line;

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter("infectedlist.txt",true));
        } catch (IOException e) {
            e.printStackTrace();
        }
        // reads a line from the client node, distinguishing between request and ID transmission
        while ((line = reader.readLine()) != null) {
            if(line.equals("requestlist")) {
                for(int i = 0; i < ServerEntry.infectedList.size(); i++) {
                    send(ServerEntry.infectedList.get(i) + "\n");
                }
                //############################################################################
                send("done\n");             // changed!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                //############################################################################
            } else {
                System.out.println(line);
                if(line.substring(0, 3).equals("123")) {
                    line = line.substring(3);
                    ServerEntry.infectedList.add(line);
                    writer.println(line);
                }
                writer.close();
            }
        }
    }

    private void send(String msg) throws IOException {
        outputStream.write(msg.getBytes());
    }
}
