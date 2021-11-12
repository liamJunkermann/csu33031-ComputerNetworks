import java.net.*;

public class Controller extends Node {
    static final int SERVICE_PORT = 51510;
    static final int CONTROLLER_PORT = 51510;

    static final int TYPE = 0;
    static final int LENGTH = 1;
    static final int HEADER_LENGTH = 2;

    static final byte ENDPOINT_ONE = 0;
    static final byte ENDPOINT_TWO = 1;
    static final byte ERROR = 5;
    static final byte ACK = 6;

    static final int NUMBER_OF_ENDPOINTS = 2;
    static final int INFO_TO_BE_STORED = 3;
    static final int DEST = 0;
    static final int IN = 1;
    static final int OUT = 2;
    static final int ROUTER_ONE = 1;
    static final int ROUTER_TWO = 2;
    static final int ROUTER_THREE = 3;

    static final int ACKCODE = 1;
    static final byte ACKPACKET = 10;

    private int controllerNumber;
    private Object[][] forwardingTable;
    Terminal terminal;

    Controller(Terminal terminal, int port, int designation) {
        try {
            this.terminal = terminal;
            this.controllerNumber = designation;
            socket = new DatagramSocket(port);
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
                if (controllerNumber == ROUTER_THREE) {
                    String content = sendAck(packet, data);
                    sendPacket((byte) ROUTER_TWO, content, (InetSocketAddress) forwardingTable[ENDPOINT_ONE][OUT]);
                } else {
                    sendAck(packet, data);
                    packet.setSocketAddress((InetSocketAddress) forwardingTable[ENDPOINT_ONE][OUT]);
                    socket.send(packet);
                }
                break;
            case ENDPOINT_TWO:
                if (controllerNumber == ROUTER_ONE) {
                    String content = sendAck(packet, data);
                    sendPacket((byte) ROUTER_THREE, content, (InetSocketAddress) forwardingTable[ENDPOINT_TWO][OUT]);
                } else {
                    sendAck(packet, data);
                    packet.setSocketAddress((InetSocketAddress) forwardingTable[ENDPOINT_TWO][OUT]);
                    socket.send(packet);
                }
                break;
            case ERROR:
                sendAck(packet, data);
                System.err
                        .println("Error: packet should have been dropped at forwarding service \ndropping packet now.");
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
            initialiseForwardingTable();
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

    private void initialiseForwardingTable() {
        forwardingTable = new Object[NUMBER_OF_ENDPOINTS][INFO_TO_BE_STORED];
        forwardingTable[ENDPOINT_ONE][DEST] = ENDPOINT_TWO;
        forwardingTable[ENDPOINT_TWO][DEST] = ENDPOINT_ONE;
        switch (controllerNumber) {
        case ROUTER_ONE:
            forwardingTable[ENDPOINT_ONE][IN] = new InetSocketAddress("ForwardingService", SERVICE_PORT);
            forwardingTable[ENDPOINT_ONE][OUT] = new InetSocketAddress("RouterTwo", CONTROLLER_PORT);
            forwardingTable[ENDPOINT_TWO][IN] = new InetSocketAddress("RouterTwo", CONTROLLER_PORT);
            forwardingTable[ENDPOINT_TWO][OUT] = new InetSocketAddress("ForwardingService", SERVICE_PORT);
            break;
        case ROUTER_TWO:
            forwardingTable[ENDPOINT_ONE][IN] = new InetSocketAddress("RouterOne", CONTROLLER_PORT);
            forwardingTable[ENDPOINT_ONE][OUT] = new InetSocketAddress("RouterThree", CONTROLLER_PORT);
            forwardingTable[ENDPOINT_TWO][IN] = new InetSocketAddress("RouterThree", CONTROLLER_PORT);
            forwardingTable[ENDPOINT_TWO][OUT] = new InetSocketAddress("RouterOne", SERVICE_PORT);
            break;
        case ROUTER_THREE:
            forwardingTable[ENDPOINT_ONE][IN] = new InetSocketAddress("RouterTwo", CONTROLLER_PORT);
            forwardingTable[ENDPOINT_ONE][OUT] = new InetSocketAddress("ForwardingService", SERVICE_PORT);
            forwardingTable[ENDPOINT_TWO][IN] = new InetSocketAddress("ForwardingService", SERVICE_PORT);
            forwardingTable[ENDPOINT_TWO][OUT] = new InetSocketAddress("RouterTwo", CONTROLLER_PORT);
            break;
        default:
            break;
        }
    }

    public static void main(String[] args) {
        try {
            Terminal terminal = new Terminal("Controller");
            String input = terminal.read("Controller Designation number");
            int designation = Integer.parseInt(input);
            Controller r = new Controller(terminal, CONTROLLER_PORT, designation);
            r.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
