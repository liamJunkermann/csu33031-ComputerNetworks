import java.net.*;

public class Application extends Node {
  static final int DEFAULT_PORT = 51510;

  static final int TYPE = 0;
  static final int LENGTH = 1;
  static final int HEADER_LENGTH = 2;

  static final byte ENDPOINT_ONE = 0;
  static final byte ENDPOINT_TWO = 1;
  static final byte ERROR = 7;
  static final byte ACK = 9;

  static final int ACKCODE = 1;
  static final byte ACKPACKET = 10;

  private InetSocketAddress forwardingService = new InetSocketAddress("ForwardingService", DEFAULT_PORT);
  private String destination;
  private String message;

  private boolean init = true;

  Terminal terminal;

  Application(Terminal terminal, int srcPort) {
    try {
      this.terminal = terminal;
      socket = new DatagramSocket(srcPort);
      listener.go();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public synchronized void onReceipt(DatagramPacket packet) {
    try {
      init = false;
      byte[] data;
      data = packet.getData();
      switch (data[TYPE]) {
      case ACK:
        terminal.println("Packet Received by Forwarding Service");
        break;
      case ENDPOINT_ONE:
        message = sendAck(packet, data);
        terminal.println("Endpoint One said: " + message);
        start();
        break;
      case ENDPOINT_TWO:
        message = sendAck(packet, data);
        terminal.println("Endpoint Two said: " + message);
        start();
        break;
      default:
        message = sendAck(packet, data);
        System.err.println("ERROR: Unexpected Packet Received");
        start();
        break;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public synchronized void start() {
    try {
      byte[] data;
      DatagramPacket packet;
      destination = terminal.read("Packet Destination (EndpointOne or EndpointTwo)");
      message = terminal.read("Message to send");

      data = new byte[HEADER_LENGTH + message.length()];
      if (destination.equalsIgnoreCase("endpoint1") || destination.equalsIgnoreCase("endpointone")
          || destination.equalsIgnoreCase("1")) {
        data[TYPE] = ENDPOINT_TWO;
      } else if (destination.equalsIgnoreCase("endpoint2") || destination.equalsIgnoreCase("endpointtwo")
          || destination.equalsIgnoreCase("2")) {
        data[TYPE] = ENDPOINT_ONE;
      } else {
        data[TYPE] = ERROR;
      }
      data[LENGTH] = (byte) message.length();
      System.out.println(data[TYPE]);
      System.arraycopy(message.getBytes(), 0, data, HEADER_LENGTH, message.length());
      packet = new DatagramPacket(data, data.length);
      packet.setSocketAddress(forwardingService);
      socket.send(packet);
      if (init) {
        while (true) {
          this.wait();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String sendAck(DatagramPacket packet, byte[] data) {
    try {
      String content;
      DatagramPacket response;
      byte[] buffer = new byte[data[LENGTH]];
      System.arraycopy(data, HEADER_LENGTH, buffer, 0, data[LENGTH]);
      content = new String(buffer);
      data = new byte[HEADER_LENGTH];
      data[TYPE] = ACK;
      data[ACKCODE] = ACKPACKET;
      response = new DatagramPacket(data, data.length);
      response.setSocketAddress(packet.getSocketAddress());
      socket.send(response);
      return content;
    } catch (Exception e) {
      e.printStackTrace();
      return "";
    }
  }

  public static void main(String[] args) {
    try {
      String terminalTitle = "App";
      if (args.length != 0) {
        terminalTitle += " " + args[0];
      }
      Terminal terminal = new Terminal(terminalTitle);
      Application app = new Application(terminal, DEFAULT_PORT);
      app.start();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}