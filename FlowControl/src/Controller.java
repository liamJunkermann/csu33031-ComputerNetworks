import java.net.*;

public class Controller extends Node {
    static final int DEFAULT_PORT = 51510;

    static final int TYPE = 0;
    static final int LENGTH = 1;
    static final int HEADER_LENGTH = 2;

    static final byte ENDPOINT_ONE = 0;
    static final byte ENDPOINT_TWO = 1;
    static final byte ERROR = 7;
    static final byte CONTROLLER = 8;
    static final byte ACK = 9;

    static final int ROUTER_COUNT = 6;
    static final int ENDPOINT_COUNT = 2;
    static final int INFO_TO_BE_STORED = 3;
    static final int DEST = 0;
    static final int IN = 1;
    static final int OUT = 2;
    static final int ROUTER_ONE = 0;
    static final int ROUTER_TWO = 1;
    static final int ROUTER_THREE = 2;
    static final int ROUTER_FOUR = 3;
    static final int ROUTER_FIVE = 4;
    static final int ROUTER_SIX = 5;

    static final int ACKCODE = 1;
    static final byte ACKPACKET = 10;

    private static InetSocketAddress[] addressTable;
    private static Object[][][] forwardingTable;
    Terminal terminal;

    private String content;
    private String address;

    Controller(Terminal terminal) {
        try {
            this.terminal = terminal;
            initAddrTable();
            initForwardTable();
            socket = new DatagramSocket(DEFAULT_PORT);
            listener.go();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void initForwardTable() {
        forwardingTable = new Object[ROUTER_COUNT][ENDPOINT_COUNT][INFO_TO_BE_STORED];
        forwardingTable[ROUTER_ONE][ENDPOINT_ONE][DEST] = ENDPOINT_TWO;
        forwardingTable[ROUTER_ONE][ENDPOINT_TWO][DEST] = ENDPOINT_ONE;
        forwardingTable[ROUTER_TWO][ENDPOINT_ONE][DEST] = ENDPOINT_TWO;
        forwardingTable[ROUTER_TWO][ENDPOINT_TWO][DEST] = ENDPOINT_ONE;
        forwardingTable[ROUTER_THREE][ENDPOINT_ONE][DEST] = ENDPOINT_TWO;
        forwardingTable[ROUTER_THREE][ENDPOINT_ONE][DEST] = ENDPOINT_TWO;
        forwardingTable[ROUTER_FOUR][ENDPOINT_TWO][DEST] = ENDPOINT_ONE;
        forwardingTable[ROUTER_FOUR][ENDPOINT_TWO][DEST] = ENDPOINT_ONE;
        forwardingTable[ROUTER_FIVE][ENDPOINT_TWO][DEST] = ENDPOINT_ONE;
        forwardingTable[ROUTER_FIVE][ENDPOINT_TWO][DEST] = ENDPOINT_ONE;
        forwardingTable[ROUTER_SIX][ENDPOINT_TWO][DEST] = ENDPOINT_ONE;
        forwardingTable[ROUTER_SIX][ENDPOINT_TWO][DEST] = ENDPOINT_ONE;
        forwardingTable[ROUTER_ONE][ENDPOINT_ONE][IN] = "ForwardingService";
        forwardingTable[ROUTER_ONE][ENDPOINT_ONE][OUT] = "RouterThree";
        forwardingTable[ROUTER_TWO][ENDPOINT_TWO][IN] = "ForwardingService";
        forwardingTable[ROUTER_TWO][ENDPOINT_TWO][OUT] = "RouterFour";
        forwardingTable[ROUTER_THREE][ENDPOINT_ONE][IN] = "RouterOne";
        forwardingTable[ROUTER_THREE][ENDPOINT_ONE][OUT] = "RouterFive";
        forwardingTable[ROUTER_FOUR][ENDPOINT_TWO][IN] = "RouterTwo";
        forwardingTable[ROUTER_FOUR][ENDPOINT_TWO][OUT] = "RouterSix";
        forwardingTable[ROUTER_FIVE][ENDPOINT_ONE][IN] = "RouterThree";
        forwardingTable[ROUTER_FIVE][ENDPOINT_ONE][OUT] = "ForwardingService";
        forwardingTable[ROUTER_SIX][ENDPOINT_TWO][IN] = "RouterFour";
        forwardingTable[ROUTER_SIX][ENDPOINT_TWO][OUT] = "ForwardingService";
    }

    private static void initAddrTable() {
        addressTable = new InetSocketAddress[ROUTER_COUNT];
        addressTable[ROUTER_ONE] = new InetSocketAddress("RouterOne", DEFAULT_PORT);
        addressTable[ROUTER_TWO] = new InetSocketAddress("RouterTwo", DEFAULT_PORT);
        addressTable[ROUTER_THREE] = new InetSocketAddress("RouterThree", DEFAULT_PORT);
        addressTable[ROUTER_FOUR] = new InetSocketAddress("RouterFour", DEFAULT_PORT);
        addressTable[ROUTER_FIVE] = new InetSocketAddress("RouterFive", DEFAULT_PORT);
        addressTable[ROUTER_SIX] = new InetSocketAddress("RouterSix", DEFAULT_PORT);
    }

    public synchronized void onReceipt(DatagramPacket packet) {
        try {
            byte[] data;
            data = packet.getData();
            switch (data[TYPE]) {
                case ACK:
                    terminal.println("Packet Recieved");
                    break;
                case ROUTER_ONE:
                    content = sendAck(packet, data);
                    address = (String) forwardingTable[ROUTER_ONE][Integer.parseInt(content)][OUT];
                    sendPacket(CONTROLLER, address, addressTable[ROUTER_ONE]);
                    break;
                case ROUTER_TWO:
                    content = sendAck(packet, data);
                    address = (String) forwardingTable[ROUTER_TWO][Integer.parseInt(content)][OUT];
                    sendPacket(CONTROLLER, address, addressTable[ROUTER_TWO]);
                    break;
                case ROUTER_THREE:
                    content = sendAck(packet, data);
                    address = (String) forwardingTable[ROUTER_THREE][Integer.parseInt(content)][OUT];
                    sendPacket(CONTROLLER, address, addressTable[ROUTER_THREE]);
                    break;
                case ROUTER_FOUR:
                    content = sendAck(packet, data);
                    address = (String) forwardingTable[ROUTER_FOUR][Integer.parseInt(content)][OUT];
                    sendPacket(CONTROLLER, address, addressTable[ROUTER_FOUR]);
                    break;
                case ROUTER_FIVE:
                    content = sendAck(packet, data);
                    address = (String) forwardingTable[ROUTER_FIVE][Integer.parseInt(content)][OUT];
                    sendPacket(CONTROLLER, address, addressTable[ROUTER_FIVE]);
                    break;
                case ROUTER_SIX:
                    content = sendAck(packet, data);
                    address = (String) forwardingTable[ROUTER_SIX][Integer.parseInt(content)][OUT];
                    sendPacket(CONTROLLER, address, addressTable[ROUTER_SIX]);
                    break;
                case ERROR:
                    sendAck(packet, data);
                    System.err
                            .println(
                                    "Error: packet should have been dropped at forwarding service \ndropping packet now.");
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
            while (true)
                this.wait();
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

    public static void main(String[] args) {
        try {
            Terminal terminal = new Terminal("Controller");
            terminal.println("Controller");
            Controller r = new Controller(terminal);
            r.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
