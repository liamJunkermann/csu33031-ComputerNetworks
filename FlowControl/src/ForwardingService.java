import java.net.*;

public class ForwardingService extends Node {
  static final int APPLICATION_PORT = 50000;
  static final int SERVICE_PORT = 51510;
  static final int CONTROLLER_PORT = 51510;

  static final int TYPE = 0;
  static final int LENGTH = 1;
  static final int HEADER_LENGTH = 2;

  static final byte ENDPOINT_ONE = 0;
  static final byte ENDPOINT_TWO = 1;
  static final byte CONTROLLER_ONE = 1;
  static final byte CONTROLLER_TWO = 2;
  static final byte CONTROLLER_THREE = 3;
  static final byte ERROR = 5; // for now
  static final byte ACK = 6;

  static final int ACKCODE = 1;
  static final byte ACKPACKET = 10;

  private InetSocketAddress router;
  private InetSocketAddress application;

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
      // System.out.println(data.toString());
      terminal.println("Type " + String.valueOf(data[TYPE]));
      switch (data[TYPE]) {
      case ACK:
        terminal.println("Packet Received");
        break;
      case ENDPOINT_ONE:
        content = sendAck(packet, data);
        router = new InetSocketAddress("ControllerOne", CONTROLLER_PORT);
        application = new InetSocketAddress("EndpointTwo", packet.getPort());
        terminal.println("EndpointOne");
        packet.setSocketAddress(router);
        socket.send(packet);
        break;
      case ENDPOINT_TWO:
        content = sendAck(packet, data);
        router = new InetSocketAddress("ControllerThree", CONTROLLER_PORT);
        application = new InetSocketAddress("EndpointOne", packet.getPort());
        terminal.println("EndpointTwo");
        packet.setSocketAddress(router);
        socket.send(packet);
        break;
      case CONTROLLER_TWO:
        content = sendAck(packet, data);
        terminal.println("ControllerOne");
        sendPacket(ENDPOINT_TWO, content, application);
        socket.send(packet);
        break;
      case CONTROLLER_THREE:
        content = sendAck(packet, data);
        terminal.println("ControllerThree");
        sendPacket(ENDPOINT_ONE, content, application);
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
      ForwardingService fs = new ForwardingService(terminal, SERVICE_PORT);
      fs.start();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
