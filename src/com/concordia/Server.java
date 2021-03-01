package com.concordia;

import java.io.File;
import java.util.Scanner;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.InputMismatchException;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/** Server class
 *
 * @author Kerly Titus
 */

public class Server extends Thread {

    private static int numberOfTransactions;         	/* Number of transactions handled by the server */
    private static int numberOfAccounts;             	/* Number of accounts stored in the server */
    private static int maxNbAccounts;                		/* maximum number of transactions */
    private static Accounts [] account;              		/* Accounts to be accessed or updated */
    /* NEW : member variabes to be used in PA2 with appropriate accessor and mutator methods */
    private String serverThreadId;				 /* Identification of the two server threads - Thread1, Thread2 */
    private static String serverThreadRunningStatus1;	 /* Running status of thread 1 - idle, running, terminated */
    private static String serverThreadRunningStatus2;	 /* Running status of thread 2 - idle, running, terminated */


    /**
     * Constructor method of Client class
     *
     * @return
     * @param
     */
    Server(String stid)
    {
        if ( !(Network.getServerConnectionStatus().equals("connected")))
        {
            System.out.println("\n Initializing the server ...");
            numberOfTransactions = 0;
            numberOfAccounts = 0;
            maxNbAccounts = 100;
            serverThreadId = stid;							/* unshared variable so each thread has its own copy */
            serverThreadRunningStatus1 = "idle";
            account = new Accounts[maxNbAccounts];
            System.out.println("\n Inializing the Accounts database ...");
            initializeAccounts( );
            System.out.println("\n Connecting server to network ...");
            if (!(Network.connect(Network.getServerIP())))
            {
                System.out.println("\n Terminating server application, network unavailable");
                System.exit(0);
            }
        }
        else
        {
            serverThreadId = stid;							/* unshared variable so each thread has its own copy */
            serverThreadRunningStatus2 = "idle";
        }
    }

    /**
     * Accessor method of Server class
     *
     * @return numberOfTransactions
     * @param
     */
    public int getNumberOfTransactions() {
        return numberOfTransactions;
    }

    /**
     * Mutator method of Server class
     *
     * @return
     * @param nbOfTrans
     */
    public void setNumberOfTransactions(int nbOfTrans) {
        numberOfTransactions = nbOfTrans;
    }

    /**
     * Accessor method of Server class
     *
     * @return numberOfAccounts
     * @param
     */
    public int getNumberOfAccounts() {
        return numberOfAccounts;
    }

    /**
     * Mutator method of Server class
     *
     * @return
     * @param nbOfAcc
     */
    public void setNumberOfAccounts(int nbOfAcc) {
        numberOfAccounts = nbOfAcc;
    }

    /**
     * Accessor method of Server class
     *
     * @return maxNbAccounts
     * @param
     */
    public int getmMxNbAccounts() {
        return maxNbAccounts;
    }

    /**
     * Mutator method of Server class
     *
     * @return
     * @param nbOfAcc
     */
    public void setMaxNbAccounts(int nbOfAcc) {
        maxNbAccounts = nbOfAcc;
    }

    /**
     * Initialization of the accounts from an input file
     *
     * @return
     * @param
     */
    public void initializeAccounts() {
        Scanner inputStream = null; /* accounts input file stream */
        int i = 0;                  /* index of accounts array */

        try {
            inputStream = new Scanner(new FileInputStream("account.txt"));
        } catch(FileNotFoundException e) {
            System.out.println("File account.txt was not found");
            System.out.println("or could not be opened.");
            System.exit(0);
        }
        while (inputStream.hasNextLine()) {
            try {
                account[i] = new Accounts();
                account[i].setAccountNumber(inputStream.next());    /* Read account number */
                account[i].setAccountType(inputStream.next());      /* Read account type */
                account[i].setFirstName(inputStream.next());        /* Read first name */
                account[i].setLastName(inputStream.next());         /* Read last name */
                account[i].setBalance(inputStream.nextDouble());    /* Read account balance */
            }
            catch(InputMismatchException e) {
                System.out.println("Line " + i + "file account.txt invalid input");
                System.exit(0);
            }
            i++;
        }
        setNumberOfAccounts(i);			/* Record the number of accounts processed */
        //System.out.println("\n DEBUG : Server.initializeAccounts() " + getNumberOfAccounts() + " accounts processed");
        inputStream.close( );
    }

    /**
     * Find and return the index position of an account
     *
     * @return account index position or -1
     * @param accNumber
     */
    public int findAccount(String accNumber) {
        int i = 0;

        /* Find account */
        while ( !(account[i].getAccountNumber().equals(accNumber)))
            i++;
        if (i == getNumberOfAccounts())
            return -1;
        else
            return i;
    }

    /**
     * Processing of the transactions
     *
     * @return
     * @param trans
     */
    public boolean processTransactions(Transactions trans) {
        int accIndex;             	/* Index position of account to update */
        double newBalance; 		/* Updated account balance */

        /* Process the accounts until the client disconnects */
        while ((!Network.getClientConnectionStatus().equals("disconnected"))) {
            if (Network.getInBufferStatus().equals("empty"))
                Thread.yield();
            else {
                //System.out.println("\n DEBUG : Server.processTransactions() - transferring in account " + trans.getAccountNumber());
                Network.transferIn(trans);                              /* Transfer a transaction from the network input buffer */
                accIndex = findAccount(trans.getAccountNumber());

                /* Process deposit operation */
                switch (trans.getOperationType()) {
                    case "DEPOSIT":
                        newBalance = deposit(accIndex, trans.getTransactionAmount());
                        trans.setTransactionBalance(newBalance);
                        trans.setTransactionStatus("done");
                        //System.out.println("\n DEBUG : Server.processTransactions() - Deposit of " + trans.getTransactionAmount() + " in account " + trans.getAccountNumber());
                        break;
                    case "WITHDRAW":
                        newBalance = withdraw(accIndex, trans.getTransactionAmount());
                        trans.setTransactionBalance(newBalance);
                        trans.setTransactionStatus("done");
                        //System.out.println("\n DEBUG : Server.processTransactions() - Withdrawal of " + trans.getTransactionAmount() + " from account " + trans.getAccountNumber());
                    case "QUERY":
                        newBalance = query(accIndex);
                        trans.setTransactionBalance(newBalance);
                        trans.setTransactionStatus("done");
                        //System.out.println("\n DEBUG : Server.processTransactions() - Obtaining balance from account" + trans.getAccountNumber());
                    default:
                        break;
                }
                while((Network.getOutBufferStatus().equals("full")))
                    Thread.yield();

                //System.out.println("\n DEBUG : Server.processTransactions() - transferring out account " + trans.getAccountNumber());
                Network.transferOut(trans);                            		/* Transfer a completed transaction from the server to the network output buffer */
                setNumberOfTransactions( (getNumberOfTransactions() +  1) ); 	/* Count the number of transactions processed */
            }
        }
        //System.out.println("\n DEBUG : Server.processTransactions() - " + getNumberOfTransactions() + " accounts updated");
        return true;
    }

    /**
     * Processing of a deposit operation in an account
     *
     * @return balance
     * @param i, amount
     */
    public double deposit(int i, double amount) {
        double curBalance;      /* Current account balance */
        curBalance = account[i].getBalance( );          /* Get current account balance */
        account[i].setBalance(curBalance + amount);     /* Deposit amount in the account */
        return account[i].getBalance ();                /* Return updated account balance */
    }

    /**
     *  Processing of a withdrawal operation in an account
     *
     * @return balance
     * @param i, amount
     */
    public double withdraw(int i, double amount)
    {  double curBalance;      /* Current account balance */

        curBalance = account[i].getBalance( );          /* Get current account balance */
        account[i].setBalance(curBalance - amount);     /* Withdraw amount in the account */
        return account[i].getBalance ();                /* Return updated account balance */
    }

    /**
     *  Processing of a query operation in an account
     *
     * @return balance
     * @param i
     */
    public double query(int i) {
        double curBalance;      /* Current account balance */
        curBalance = account[i].getBalance( );          /* Get current account balance */
        return curBalance;                              /* Return current account balance */
    }

    /**
     *  Create a String representation based on the Server Object
     *
     * @return String representation
     */
    public String toString() {
        return ("\n server IP " + Network.getServerIP() + "connection status " + Network.getServerConnectionStatus() + "Number of accounts " + getNumberOfAccounts());
    }

    /**
     * Code for the run method
     *
     * @return
     * @param
     */
    public void run() {
        long serverStartTime = System.currentTimeMillis();
        //System.out.println("\n DEBUG : Server.run() - starting server thread " + objNetwork.getServerConnectionStatus());
        processTransactions(new Transactions());
        Network.disconnect(Network.getServerIP());
        System.out.println("\n Terminating server thread - " + " Running time " + (System.currentTimeMillis() - serverStartTime) + " milliseconds");
    }
}
