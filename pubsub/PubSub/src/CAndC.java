import java.net.*;

public class CAndC extends Node {
  static final int COMMAND_PORT = 50000; // Port of the command and control
  static final int BROKER_PORT = 50001; // Port of the broker
  static final String DEFAULT_DST_NODE = "localhost"; // Name of the host for the server

  static final int HEADER_LENGTH = 2; // Fixed length of the header
  static final int TYPE_POS = 0; // Position of the type within the header

  static final int LENGTH_POS = 1;

  static final byte TYPE_ACK = 4; // Indicating an acknowledgement
  static final int ACKCODE_POS = 1; // Position of the acknowledgement type in the header
  static final byte ACK_ALLOK = 10; // Inidcating that everything is ok

  static final byte CANDC = 1;
  static final byte BROKER = 2;
  static final byte WORKER = 3;

  Terminal terminal;
  InetSocketAddress dstAddress;

  CAndC(Terminal terminal, String dstHost, int dstPort, int srcPort) {
    try {
      this.terminal = terminal;
      dstAddress = new InetSocketAddress(dstHost, dstPort);
      socket = new DatagramSocket(srcPort);
      listener.go();
    } catch (java.lang.Exception e) {
      e.printStackTrace();
    }
  }

  public synchronized void onReceipt(DatagramPacket packet) {
    String content;
    byte[] data;
    byte[] buffer;

    data = packet.getData();
    switch (data[TYPE_POS]) {
      case TYPE_ACK:
        terminal.println("Broker has received packet");
        break;
      case BROKER:
        buffer = new byte[data[LENGTH_POS]];
        System.arraycopy(data, HEADER_LENGTH, buffer, 0, buffer.length);
        content = new String(buffer);
        terminal.println(content);
        this.notify();
        break;
      default:
        terminal.println("Unexpected packet" + packet.toString());
    }
  }

  public void sendMessage() throws Exception {
    byte[] data = null;
    byte[] buffer = null;
    DatagramPacket packet = null;
    String input;

    input = terminal.read("Payload: ");
    buffer = input.getBytes();
    if (!new String(buffer).equals("")) {
      data = new byte[HEADER_LENGTH + buffer.length];
      data[TYPE_POS] = CANDC;
      data[LENGTH_POS] = (byte) buffer.length;
      System.arraycopy(buffer, 0, data, HEADER_LENGTH, buffer.length);

      terminal.println("Contacting broker...");
      packet = new DatagramPacket(data, data.length);
      packet.setSocketAddress(dstAddress);
      socket.send(packet);
      terminal.println("Packet sent");
    }
  }

  public static void main(String[] args) {
    try {
      Terminal terminal = new Terminal("Command and Control");
      CAndC command = new CAndC(terminal, DEFAULT_DST_NODE, BROKER_PORT, COMMAND_PORT);
      while (true) {
        command.sendMessage();
      }
    } catch (java.lang.Exception e) {
      e.printStackTrace();
    }
  }
}
