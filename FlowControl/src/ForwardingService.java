import java.net.*;

public class ForwardingService extends Node {
  static final int DEFAULT_PORT = 51510;

  static final int TYPE = 0;
  static final int LENGTH = 1;
  static final int HEADER_LENGTH = 2;

  static final byte ENDPOINT_ONE = 0;
  static final byte ENDPOINT_TWO = 1;
  static final byte ROUTER_FIVE = 5;
  static final byte ROUTER_SIX = 6;
  static final byte ERROR = 7;
  static final byte ACK = 9;

  static final int ACKCODE = 1;
  static final byte ACKPACKET = 10;

  private InetSocketAddress router_add;
  private InetSocketAddress application_add;

  Terminal terminal;

  ForwardingService(Terminal terminal, int port) {
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
      byte[] data;
      String content;
      data = packet.getData();
      terminal.println("Type " + String.valueOf(data[TYPE]));
      switch (data[TYPE]) {
      case ACK:
        terminal.println("Packet Received");
        break;
      case ENDPOINT_ONE:
        content = sendAck(packet, data);
        router_add = new InetSocketAddress("RouterOne", DEFAULT_PORT);
        application_add = new InetSocketAddress("EndpointTwo", DEFAULT_PORT);
        terminal.println("From EndpointOne");
        packet.setSocketAddress(router_add);
        socket.send(packet);
        break;
      case ENDPOINT_TWO:
        content = sendAck(packet, data);
        router_add = new InetSocketAddress("RouterTwo", DEFAULT_PORT);
        application_add = new InetSocketAddress("EndpointOne", DEFAULT_PORT);
        terminal.println("From EndpointTwo");
        packet.setSocketAddress(router_add);
        socket.send(packet);
        break;
      case ROUTER_FIVE:
        content = sendAck(packet, data);
        sendPacket(ENDPOINT_ONE, content, application_add);
        break;
      case ROUTER_SIX:
        content = sendAck(packet, data);
        sendPacket(ENDPOINT_TWO, content, application_add);
        break;
      case ERROR:
        content = sendAck(packet, data);
        terminal.println("Unknown destination in topology, packet dropped");
        break;
      default:
        System.err.println("ERROR: Unexpected packet received");
        break;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public synchronized void start() {
    try {
      terminal.println("Forwarding Service starting");
      while (true)
        this.wait();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String sendAck(DatagramPacket packet, byte[] data) {
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

  private void sendPacket(byte type, String content, InetSocketAddress dstAddress) {
    try {
      byte[] data;
      DatagramPacket packet;
      data = new byte[HEADER_LENGTH + content.length()];
      data[TYPE] = type;
      data[LENGTH] = (byte) content.length();
      System.arraycopy(content.getBytes(), 0, data, HEADER_LENGTH, content.length());
      packet = new DatagramPacket(data, data.length);
      packet.setSocketAddress(dstAddress);
      socket.send(packet);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    try {
      Terminal terminal = new Terminal("Forwarding Service");
      ForwardingService fs = new ForwardingService(terminal, DEFAULT_PORT);
      fs.start();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
