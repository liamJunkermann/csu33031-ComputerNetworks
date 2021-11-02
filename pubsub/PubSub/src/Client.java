import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;

public class Client extends Node {
  static final int DEFAULT_SRC_PORT = 50002;
  static final int DEFAULT_DST_PORT = 50001;
  static final String DEFAULT_DST_NODE = "localhost";

  static final int HEADER_LENGTH = 2;
  static final int TYPE_POS = 0;

  static final int LENGTH_POS = 1;
  static final byte TYPE_ACK = 4;
  static final byte CONNECT_ACK = 5;
  static final int ACKCODE_POS = 1;
  static final byte ACK_ALLOK = 10;

  static final byte CANDC = 1;
  static final byte BROKER = 2;
  static final byte CLIENT = 3;

  public static final int NO_OF_CLIENTS = 3;

  Terminal terminal;
  InetSocketAddress dstAddr;
  static ArrayList<Client> clients = new ArrayList<Client>();

  Client(Terminal terminal, String dstHost, int dstPort, int srcPort) {
    try {
      this.terminal = terminal;
      dstAddr = new InetSocketAddress(dstHost, dstPort);
      socket = new DatagramSocket(srcPort);
      listener.go();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void onReceipt(DatagramPacket packet) {
    String content;
    byte[] data;
    byte[] buffer;

    data = packet.getData();
    switch (data[TYPE_POS]) {
      case TYPE_ACK:
        terminal.println("Received by broker");
        break;
      case BROKER:
        buffer = new byte[data[LENGTH_POS]];
        System.arraycopy(data, HEADER_LENGTH, buffer, 0, buffer.length);
        content = new String(buffer);
        terminal.println("Broker said: " + content + "\nPlease 'accept', 'decline' or 'withdraw'");
        try {
          sendResponse();
        } catch (Exception e) {
          e.printStackTrace();
        }
        break;
      default:
        terminal.println("Unexpected packet" + packet.toString());
    }
  }

  public void makeConnection() throws Exception {
    byte[] data = null;
    String input = "Connect me";
    byte[] message = input.getBytes();
    DatagramPacket packet = null;

    data = new byte[HEADER_LENGTH + message.length];
    data[TYPE_POS] = CLIENT;
    data[LENGTH_POS] = (byte) message.length;
    System.arraycopy(message, 0, data, HEADER_LENGTH, message.length);

    terminal.println("Connecting to broker...");
    packet = new DatagramPacket(data, data.length);
    packet.setSocketAddress(dstAddr);
    socket.send(packet);
  }

  public void sendResponse() throws Exception {
    byte[] data = null;
    byte[] buffer = null;
    DatagramPacket packet = null;
    String input;

    input = terminal.read("Payload: ");
    buffer = input.getBytes();
    data = new byte[HEADER_LENGTH + buffer.length];
    data[TYPE_POS] = CLIENT;
    data[LENGTH_POS] = (byte) buffer.length;
    System.arraycopy(buffer, 0, data, HEADER_LENGTH, buffer.length);
    if (!new String(buffer).equals("")) {
      terminal.println("Sending response...");
      packet = new DatagramPacket(data, data.length);
      packet.setSocketAddress(dstAddr);
      socket.send(packet);
      if (new String(buffer).equalsIgnoreCase("withdraw")) {
        terminal.println("Withdrawn for mailing list");
      } else {
        terminal.println("Response sent: " + new String(buffer));
      }
    }
  }

  public static void main(String[] args) {
    try {
      for (int i = 0; i < NO_OF_CLIENTS; i++) {
        Terminal terminal = new Terminal("Client " + (i + 1));
        Client client = new Client(terminal, DEFAULT_DST_NODE, DEFAULT_DST_PORT, (DEFAULT_SRC_PORT + i));
        client.makeConnection();
        clients.add(client);
      }
    } catch (java.lang.Exception e) {
      e.printStackTrace();
    }
  }
}
