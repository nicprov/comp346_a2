package com.concordia;

import java.util.Scanner;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.InputMismatchException;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/** Client class
 *
 * @author Kerly Titus
 */

public class Client extends Thread{

    private static int numberOfTransactions;   		/* Number of transactions to process */
    private static int maxNbTransactions;      		/* Maximum number of transactions */
    private static Transactions[] transaction; 	/* Transactions to be processed */
    private String clientOperation;    				/* sending or receiving */
    private String serverThreadId;				 /* Identification of the two server threads - Receiving, Sending */
    private static String serverThreadRunningStatusSending;	 /* Running status of thread sending - idle, running, terminated */
    private static String serverThreadRunningStatusReceiving;	 /* Running status of thread receiving - idle, running, terminated */

    /** Constructor method of Client class
     *
     * @return
     * @param
     */
    Client(String operation)
    {
        if (operation.equals("sending"))
        {
            serverThreadId = "sending";
            System.out.println("\n Initializing client sending application ...");
            numberOfTransactions = 0;
            maxNbTransactions = 100;
            transaction = new Transactions[maxNbTransactions];
            clientOperation = operation;
            System.out.println("\n Initializing the transactions ... ");
            readTransactions();
            System.out.println("\n Connecting client to network ...");
            String cip = Network.getClientIP();
            if (!(Network.connect(cip)))
            {   System.out.println("\n Terminating client application, network unavailable");
                System.exit(0);
            }
        }
        else
        if (operation.equals("receiving"))
        {
            serverThreadId = "receiving";
            System.out.println("\n Initializing client receiving application ...");
            clientOperation = operation;
        }
    }

    /**
     * Accessor method of Client class
     *
     * @return numberOfTransactions
     * @param
     */
    public int getNumberOfTransactions() {
        return numberOfTransactions;
    }

    /**
     * Mutator method of Client class
     *
     * @return
     * @param nbOfTrans
     */
    public void setNumberOfTransactions(int nbOfTrans) {
        numberOfTransactions = nbOfTrans;
    }

    /**
     * Accessor method of Client class
     *
     * @return clientOperation
     * @param
     */
    public String getClientOperation() {
        return clientOperation;
    }

    /**
     * Mutator method of Client class
     *
     * @return
     * @param operation
     */
    public void setClientOperation(String operation) {
        clientOperation = operation;
    }

    /**
     * Set the server thread status
     * @param status
     * @param serverThreadId
     */
    public void setClientThreadStatus(String status, String serverThreadId){
        if (serverThreadId.equals("sending"))
            serverThreadRunningStatusSending = status;
        else
            serverThreadRunningStatusReceiving  = status;
    }

    /**
     * Get the server thread status
     * @param serverThreadId
     * @return
     */
    public String getClientThreadStatus(String serverThreadId){
        if (serverThreadId.equals("sending"))
            return serverThreadRunningStatusSending;
        else
            return serverThreadRunningStatusReceiving;
    }

    /**
     * Reading of the transactions from an input file
     *
     * @return
     * @param
     */
    public void readTransactions() {
        Scanner inputStream = null;     /* Transactions input file stream */
        int i = 0;                      /* Index of transactions array */
        try {
            inputStream = new Scanner(new FileInputStream("transaction.txt"));
        } catch(FileNotFoundException e) {
            System.out.println("File transaction.txt was not found");
            System.out.println("or could not be opened.");
            System.exit(0);
        }
        while (inputStream.hasNextLine()) {
            try {
                transaction[i] = new Transactions();
                transaction[i].setAccountNumber(inputStream.next());            /* Read account number */
                transaction[i].setOperationType(inputStream.next());            /* Read transaction type */
                transaction[i].setTransactionAmount(inputStream.nextDouble());  /* Read transaction amount */
                transaction[i].setTransactionStatus("pending");                 /* Set current transaction status */
                i++;
            } catch(InputMismatchException e) {
                System.out.println("Line " + i + "file transactions.txt invalid input");
                System.exit(0);
            }
        }
        setNumberOfTransactions(i);		/* Record the number of transactions processed */
        System.out.println("\n DEBUG : Client.readTransactions() - " + getNumberOfTransactions() + " transactions processed");
        inputStream.close( );
    }

    /**
     * Sending the transactions to the server
     *
     * @return
     * @param
     */
    public void sendTransactions() throws InterruptedException {
        int i = 0;     /* index of transaction array */
        while (i < getNumberOfTransactions()) {
            while (Network.getInBufferStatus().equals("full"))
                Thread.yield();
            transaction[i].setTransactionStatus("sent");   /* Set current transaction status */
            System.out.println("\n DEBUG : Client.sendTransactions() - sending transaction on account " + transaction[i].getAccountNumber());
            Network.send(transaction[i]);                            /* Transmit current transaction */
            i++;
        }
    }

    /**
     * Receiving the completed transactions from the server
     *
     * @return
     * @param transact
     */
    public void receiveTransactions(Transactions transact) throws InterruptedException {
        int i = 0;     /* Index of transaction array */
        boolean test = false;
        while (i < getNumberOfTransactions()) {
            while (Network.getOutBufferStatus().equals("empty"))
                Thread.yield();
            Network.receive(transact);                               	/* Receive updated transaction from the network buffer */
            System.out.println("\n DEBUG : Client.receiveTransactions() - receiving updated transaction on account " + transact.getAccountNumber());
            System.out.println(transact + " | Out: " + Network.getOutBufferStatus() + " | In: " + Network.getInBufferStatus() + " | #" + i + " | Total:" + getNumberOfTransactions());                               	/* Display updated transaction */
            i++;
        }
    }

    /**
     * Create a String representation based on the Client Object
     *
     * @return String representation
     * @param
     */
    public String toString() {
        return ("\n client IP " + Network.getClientIP() + " Connection status" + Network.getClientConnectionStatus() + "Number of transactions " + getNumberOfTransactions());
    }

    /** Code for the run method
     *
     * @return
     * @param
     */
    public void run() {
        long sendClientStartTime, receiveClientStartTime;
        if (clientOperation.equals("sending")) {
            sendClientStartTime = System.currentTimeMillis();
            setClientThreadStatus("running", "sending");
            try {
                sendTransactions();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            setClientThreadStatus("terminated", "sending");
            System.out.println("\n Terminating client send thread - " + " Running time " + (System.currentTimeMillis() - sendClientStartTime) + " milliseconds");
        }
        else {
            receiveClientStartTime = System.currentTimeMillis();
            setClientThreadStatus("running", "receiving");
            try {
                receiveTransactions(new Transactions());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            setClientThreadStatus("terminated", "receiving");
            System.out.println("\n Terminating client receive thread - " + " Running time " + (System.currentTimeMillis() - receiveClientStartTime) + " milliseconds");
        }
        if (getClientThreadStatus("receiving").equals("terminated") && getClientThreadStatus("sending").equals("terminated"))
            Network.disconnect(Network.getClientIP());
    }
}