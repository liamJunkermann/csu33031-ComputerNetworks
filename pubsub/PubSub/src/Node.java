import java.net.*;
import java.util.concurrent.CountDownLatch;

public abstract class Node {
  static final int PACKETSIZE = 65536;

  DatagramSocket socket;
  Listener listener;
  CountDownLatch latch;

  Node() {
    latch = new CountDownLatch(1);
    listener = new Listener();
    listener.setDaemon(true);
    listener.start();
  }

  public abstract void onReceipt(DatagramPacket packet);

  class Listener extends Thread {
    public void go() {
      latch.countDown();
    }

    public void run() {
      try {
        latch.await();

        while(true) {
          System.out.println("Packet Recieved");
          DatagramPacket packet = new DatagramPacket(new byte[PACKETSIZE], PACKETSIZE);
          socket.receive(packet);
          onReceipt(packet);
        }
      } catch (Exception e ) {
        if(!(e instanceof SocketException)) e.printStackTrace();
      }
    }
  }
}
