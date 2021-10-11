package com.hello.coronatrackingapp.asyncoperations;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

/**
 * The code in
 * @see Client will be executed in this thread, depending
 * on what kind of exchange with the main server is needed.
 */
public class ClientThread extends Thread {
    private String msg;
    private volatile ArrayList<String> infectedList;

    public ClientThread(String msg) {
        this.msg = msg;
    }

    @Override
    public void run() {
        Client client = new Client("85.214.47.200", 8000);
        client.connect();
        try {
            // download list of 'infected' keys
            if (msg.equals("requestlist")) {
                infectedList = client.requestList();
                // upload key along with password upon infection
            } else {
                client.sendData(msg);
            }
        } catch (IOException i) {
            System.out.println(i);
        }
    }

    /**
     * getter for the fetched list of keys.
     *
     * @return that list
     */
    public ArrayList<String> getInfectedList() {
        return infectedList;
    }
}
