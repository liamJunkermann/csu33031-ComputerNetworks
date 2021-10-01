import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class Broker extends Node {
  static final int BROKER_PORT = 50001;
  static final int HEADER_LENGTH = 2;
  static final int TYPE_POS = 0;
  static final byte TYPE_UNKOWN = 0;

  static final int LENGTH_POS = 1;

  static final byte TYPE_ACK = 4;
  static final byte CONNECT_ACK = 5;
  static final int ACKCODE_POS = 1;
  static final byte ACK_ALLOK = 10;

  static final byte CANDC = 1;
  static final byte BROKER = 2;
  static final byte CLIENT = 3;

  private ArrayList<SocketAddress> clientAddresses = new ArrayList<SocketAddress>();

  Terminal terminal;
  InetSocketAddress dstAddress;
  SocketAddress commandAddress;
  public int declines = 0;
  public boolean beenDeclined = false;
  public boolean beenAccepted = false;

  Broker(Terminal terminal, int port) {
    try {
      this.terminal = terminal;
      socket = new DatagramSocket(port);
      listener.go();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public synchronized void onReceipt(DatagramPacket packet) {
    try {
      String content;
      byte[] data;
      byte[] buffer;

      data = packet.getData();
      DatagramPacket response;
      switch (data[TYPE_POS]) {
        case TYPE_ACK:
          terminal.println("Packet recieved by Client " + (packet.getPort() - BROKER_PORT));
          break;
        case CANDC:
          beenAccepted = false;
          beenDeclined = false;
          buffer = new byte[data[LENGTH_POS]];
          System.arraycopy(data, HEADER_LENGTH, buffer, 0, buffer.length);
          content = new String(buffer);

          terminal.println("Packet received from C&C");

          data = new byte[HEADER_LENGTH];
          data[TYPE_POS] = TYPE_ACK;
          data[ACKCODE_POS] = ACK_ALLOK;

          response = new DatagramPacket(data, data.length);
          response.setSocketAddress(packet.getSocketAddress());
          commandAddress = packet.getSocketAddress();

          socket.send(response);
          declines = 0;
          this.notify();

          publishMessage(content, "clients");
          break;
        case CLIENT:
          buffer = new byte[data[LENGTH_POS]];
          System.arraycopy(data, HEADER_LENGTH, buffer, 0, buffer.length);
          content = new String(buffer);
          if (!content.contentEquals("")) {
            terminal.println("Client " + (packet.getPort() - BROKER_PORT) + " said: " + content);
          }
          data = new byte[HEADER_LENGTH];
          data[TYPE_POS] = TYPE_ACK;
          data[ACKCODE_POS] = ACK_ALLOK;
          response = new DatagramPacket(data, data.length);
          response.setSocketAddress(packet.getSocketAddress());
          socket.send(response);
          SocketAddress address = packet.getSocketAddress();
          if (!checkClientList(address)) {
            clientAddresses.add(address);
            System.out.println("Current Client addresses:\n" + clientAddresses.toString());
          }
          if (content.equalsIgnoreCase("Accept") && !beenDeclined && !beenAccepted) {
            declines = clientAddresses.size() + 1;
            beenAccepted = true;
            publishMessage("Order has been accepted by client " + (packet.getPort() - BROKER_PORT), "command");
          } else if (content.equalsIgnoreCase("Decline")) {
            declines++;
            if (declines == clientAddresses.size()) {
              publishMessage("Order declined by all clients", "command");
              beenDeclined = true;
            }
          } else if (content.equalsIgnoreCase("Withdraw")) {
            clientAddresses.remove(packet.getSocketAddress());
            System.out.println("Current client addresses:\n" + clientAddresses.toString());
          }
          break;
        default:
          terminal.println("Unexpected packet" + packet.toString());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public boolean checkClientList(SocketAddress addr) {
    for (int i = 0; i < clientAddresses.size(); i++) {
      if (clientAddresses.get(i).equals(addr)) {
        return true;
      }
    }
    return false;
  }

  public synchronized void start() throws Exception {
    terminal.println("Waiting for contact");
    this.wait();
  }

  public void publishMessage(String contentString, String dest) throws IOException, InterruptedException {
    byte[] data = null;
    byte[] content = contentString.getBytes();
    DatagramPacket packet = null;

    data = new byte[HEADER_LENGTH + content.length];
    data[TYPE_POS] = BROKER;
    data[LENGTH_POS] = (byte) content.length;
    System.arraycopy(content, 0, data, HEADER_LENGTH, content.length);
    packet = new DatagramPacket(data, data.length);

    if (dest.contentEquals("clients")) {
      terminal.println("Publishing Packets...");
      for (int i = 0; i < clientAddresses.size(); i++) {
        packet.setSocketAddress(clientAddresses.get(i));
        socket.send(packet);
      }
      terminal.println("Packets published to clients");
    } else if (dest.contentEquals("command")) {
      terminal.println("Connecting with command...");
      packet.setSocketAddress(commandAddress);
      socket.send(packet);
      terminal.println("Command notified");
    }
  }

  public static void main(String[] args) {
    try {
      Terminal terminal = new Terminal("Broker");
      Broker broker = new Broker(terminal, BROKER_PORT);
      broker.start();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
