import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RouterManager implements Runnable {
    public static final int PORT = 1242;
    private List<Router> routers;
    private DatagramSocket socket;
    private Thread receivingThread;
    private volatile boolean running;
    private int count;

    public RouterManager() throws SocketException, UnknownHostException {
        routers = new ArrayList<>();
        socket = new DatagramSocket(PORT);
        receivingThread = new Thread(this);
        receivingThread.start();
        running = true;
        count = 0;
    }

    public void close() throws InterruptedException {
        socket.close();
        running = false;
        receivingThread.join();
    }

    public Map<String, List<Link>> getGraph() {
        if (routers.isEmpty()) {
            return null;
        } else {
            return routers.get(0).getNeighbors();
        }
    }

    public String getHostAddress() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostAddress();
    }

    // address = ip:router
    // message = content
    public void send(String address, String message) throws IOException {
        String[] array = address.split(":");
        String content = array[1] + "\n" + message;
        DatagramPacket packet = new DatagramPacket(content.getBytes(), content.getBytes().length, InetAddress.getByName(array[0]), PORT);
        socket.send(packet);
    }

    public void receive(String message) throws IOException, InterruptedException {
        int index = message.indexOf("\n");
        String routerNumber = message.substring(0, index);
        String content = message.substring(index + 1);
        for (int i = 0; i < routers.size(); i++) {
            if (routers.get(i).getAddress().contains(":" + routerNumber)) {
                routers.get(i).receive(content);
            }
        }
    }

    public String routerToString(String address) {
        return routers.get(0).forwardingTableToString(address);
    }

    public void addRouter(List<String> neighbors) throws IOException {
        Router something = new Router(this, count++, neighbors);
        routers.add(something);
        something.neighborRequest();
    }

    public void removeRouter(String address) throws IOException {
        for (int i = 0; i < routers.size(); i++) {
            if (routers.get(i).getAddress().equals(address)) {
                routers.get(i).removeSelf();
                routers.remove(i);
                break;
            }
        }
    }

    public String toString() {
        StringBuilder output = new StringBuilder();
        for (Router router : routers) {
            output.append(router.toString());
            output.append("\n");
        }
        return output.toString();
    }


    @Override
    public void run() {
        while (running) {
            byte[] buffer = new byte[65000];
            DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(receivedPacket);
                String message = new String(buffer, 0, receivedPacket.getLength());
                receive(message);
            } catch (IOException e) {
                //e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendFile(String s, String s1, String s2) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(new File(s)));
        StringBuilder content = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            content.append(line);
            content.append("\n");
            line = reader.readLine();
        }
        send(s1, "send\n" + s2 + "\nfile\n" + content.toString());
    }
}
