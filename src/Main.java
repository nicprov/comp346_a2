import java.util.concurrent.Semaphore;

/**
 * @author Kerly Titus
 */
public class Main {
  public static void main(String[] args) throws InterruptedException {
    // Activate the network
    Network network = new Network();
    network.start();

    // Activate the servers
    Server server1 = new Server("#1");
    Server server2 = new Server("#2");
    server1.start();
    server2.start();

    // Activate the clients
    Client sending = new Client("sending");
    Client receiving = new Client("receiving");
    sending.start();
    receiving.start();

    // Disconnect the server when both are done processing
    server1.join();
    server2.join();
    Network.disconnect(Network.getServerIP());
  }
}
