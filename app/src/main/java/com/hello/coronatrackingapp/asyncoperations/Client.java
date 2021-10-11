package com.hello.coronatrackingapp.asyncoperations;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

/**
 * This class represents one connection to the main server, which is needed
 * to report ones own infection and to download keys of infected people. It provides
 * the methods that are needed for that.
 */
public class Client {
    private static final String TAG = "at Client";
    private final String ipAddress;

    private final int port;
    private Socket socket;

    private InputStream inputStream;
    private OutputStream outputStream;
    private BufferedReader bufferedReader;

    /**
     * prepares connection with setting the server to connect to and its port.
     *
     * @param ipAddress name of the server, the client connects to (IP address)
     * @param port port the server is bound to
     */
    public Client(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    /**
     * connects client to server and sets up the necessary streams
     *
     * @return true, if connecting was successful, false otherwise
     */
    public void connect() {
        try {
            // establish connection to server, represented in a socket object
            this.socket = new Socket(ipAddress, port);
            // object, representing the stream to the server
            this.outputStream = socket.getOutputStream();
            // object, representing the stream from the server to this client
            this.inputStream = socket.getInputStream();
            this.bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            Log.i(TAG, "connected to main server.");
        } catch (IOException e) {
            Log.i(TAG, "connection to main server failed.");
            e.printStackTrace();
        }
    }

    /**
     * sends users ID to the server
     *
     * @param message  data to send
     * @throws IOException
     */
    public void sendData(String message) throws IOException {
        Log.i(TAG, "sending infection status with password to main server.");
        outputStream.write(message.getBytes());
        closeConnection();
    }

    /**
     * sends the String "requestlist" to the server, which returns the list of infected people.
     *
     * @throws IOException
     */
    public ArrayList<String> requestList() throws IOException{
        Log.i(TAG, "requesting infected keys from main server.");

        outputStream.write("requestlist\n".getBytes());
        ArrayList<String> list = new ArrayList<>();

        String line = null;
        int i = 0;
        while (!(line = bufferedReader.readLine()).equals("done")) {
            Log.i("key " + i + ": ", line);
            list.add(line);
            i++;
        }
        closeConnection();

        return list;
    }

    /**
     * close all streams and socket
     *
     * @throws IOException
     */
    private void closeConnection() throws IOException {
        Log.i(TAG, "closing connection to main server.");
        outputStream.close();
        bufferedReader.close();
        inputStream.close();
        socket.close();
    }
}
