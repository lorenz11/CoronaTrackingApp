/* inspired by https://www.youtube.com/watch?v=cRfsUrU3RjE*/

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * entry point for the server
 */
public class ServerEntry {
    public static ArrayList<String> infectedList;

    public static void main(String[] args) {
        infectedList = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader("infectedlist.txt"));

            String line = null;
            while ((line = br.readLine()) != null) {
                infectedList.add(line);
            }
        } catch (IOException i) {
            System.out.println(i);
        }

        for(int i = 0; i < infectedList.size(); i++) {
            System.out.println(infectedList.get(i));
        }

        int port = 8000;        // specified port the server is supposed to be bound to
        Server server = new Server(port);       // actual server (thread)
        server.start();
    }
}
