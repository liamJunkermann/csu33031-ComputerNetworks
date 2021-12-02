import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class Router extends Node {
  static final int DEFAULT_PORT = 51510;

  static final int TYPE = 0;
  static final int LENGTH = 1;
  static final int HEADER_LENGTH = 2;

  static final byte ENDPOINT_ONE = 0;
  static final byte ENDPOINT_TWO = 1;
  static final byte ERROR = 7;
  static final byte ACK = 9;
  static final byte CONTROLLER = 8;

  static final int ENDPOINT_COUNT = 2;
  static final int DEST = 0;
  static final int IN = 1;
  static final int OUT = 2;
  static final int ROUTER_ONE = 1;
  static final int ROUTER_TWO = 2;
  static final int ROUTER_THREE = 3;
  static final int ROUTER_FOUR = 4;
  static final int ROUTER_FIVE = 5;
  static final int ROUTER_SIX = 6;

  static final int ACKCODE = 1;
  static final byte ACKPACKET = 10;

  private int routerNumber;
  private byte[] temp;
  private String tempContent;
  private String[] addressTable;
  private InetSocketAddress controllerAddress = new InetSocketAddress("Controller", DEFAULT_PORT);
  private String content;
  Terminal terminal;

  Router(Terminal terminal, int port, int designation) {
    try {
      initAddrTable();
      this.routerNumber = designation;
      socket = new DatagramSocket(port);
      this.terminal = terminal;
      listener.go();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public synchronized void onReceipt(DatagramPacket packet) {
    try {
      byte[] data;
      data = packet.getData();
      switch (data[TYPE]) {
        case ACK:
          terminal.println("Packet Received");
          break;
        case ENDPOINT_ONE:
          content = sendAck(packet, data);
          if (addressTable[ENDPOINT_ONE].equals("")) {
            terminal.println("Unknown address calling on the Controller");
            tempContent = content;
            temp = data;
            String message = "0";
            sendPacket((byte) (routerNumber - 1), message, controllerAddress);
          } else {
            if (routerNumber == ROUTER_FIVE) {
              sendPacket((byte) ROUTER_FIVE, content,
                  (new InetSocketAddress(addressTable[ENDPOINT_ONE], DEFAULT_PORT)));
            } else {
              sendPacket(ENDPOINT_ONE, content, (new InetSocketAddress(addressTable[ENDPOINT_ONE], DEFAULT_PORT)));
            }
          }
          break;
        case ENDPOINT_TWO:
          content = sendAck(packet, data);
          if (addressTable[ENDPOINT_TWO].equals("")) {
            terminal.println("Unknown address calling on the Controller");
            tempContent = content;
            temp = data;
            String message = "1";
            sendPacket((byte) (routerNumber - 1), message, controllerAddress);
          } else {
            if (routerNumber == ROUTER_SIX) {
              sendPacket((byte) ROUTER_SIX, content, (new InetSocketAddress(addressTable[ENDPOINT_TWO], DEFAULT_PORT)));
            } else {
              sendPacket(ENDPOINT_ONE, content, (new InetSocketAddress(addressTable[ENDPOINT_TWO], DEFAULT_PORT)));
            }
          }
          break;
        case ERROR:
          sendAck(packet, data);
          System.err.println("Error: packet should have been dropped at forwarding service \ndropping packet now.");
          break;
        case CONTROLLER:
          terminal.println("packet from controller");
          if (temp[TYPE] == ENDPOINT_ONE) {
            if (routerNumber == ROUTER_FIVE) {
              content = sendAck(packet, data);
              updateAddrTable(content, ENDPOINT_ONE);
              sendPacket((byte) ROUTER_FIVE, tempContent,
                  (new InetSocketAddress(addressTable[ENDPOINT_ONE], DEFAULT_PORT)));
            } else {
              content = sendAck(packet, data);
              updateAddrTable(content, ENDPOINT_ONE);
              sendPacket(ENDPOINT_ONE, tempContent, (new InetSocketAddress(addressTable[ENDPOINT_ONE], DEFAULT_PORT)));
            }
          } else if (temp[TYPE] == ENDPOINT_TWO) {
            if (routerNumber == ROUTER_SIX) {
              content = sendAck(packet, data);
              updateAddrTable(content, ENDPOINT_TWO);
              sendPacket((byte) ROUTER_SIX, tempContent,
                  (new InetSocketAddress(addressTable[ENDPOINT_TWO], DEFAULT_PORT)));
            } else {
              content = sendAck(packet, data);
              updateAddrTable(content, ENDPOINT_TWO);
              sendPacket(ENDPOINT_TWO, tempContent, (new InetSocketAddress(addressTable[ENDPOINT_TWO], DEFAULT_PORT)));
            }
          }
          break;
        default:
          System.err.println("Error: Unexpected packet received");

      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public synchronized void start() {
    try {
      while (true) {
        this.wait();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String sendAck(DatagramPacket packet, byte[] data) {
    try {
      String message;
      DatagramPacket response;
      byte[] buffer = new byte[data[LENGTH]];
      System.arraycopy(data, HEADER_LENGTH, buffer, 0, data[LENGTH]);
      message = new String(buffer);
      data = new byte[HEADER_LENGTH];
      data[TYPE] = ACK;
      data[ACKCODE] = ACKPACKET;
      response = new DatagramPacket(data, data.length);
      response.setSocketAddress(packet.getSocketAddress());
      socket.send(response);
      return message;
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

  private void initAddrTable() {
    addressTable = new String[ENDPOINT_COUNT];
    addressTable[ENDPOINT_ONE] = "";
    addressTable[ENDPOINT_TWO] = "";
  }

  private void updateAddrTable(String content, int index) {
    addressTable[index] = content;
  }

  public static void main(String[] args) {
    try {
      Terminal terminal = new Terminal("Router");
      int designation;
      if (args.length == 0) {
        String input = terminal.read("Router Designation Number");
        designation = Integer.parseInt(input);
      } else {
        System.out.println("argument " + args[0]);
        designation = Integer.parseInt(args[0]);
        terminal.println("Router " + designation);
      }
      Router r = new Router(terminal, DEFAULT_PORT, designation);
      r.start();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
