import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.*;

public class Router {
    private final RouterManager routerManager;
    private final int index;
    private final String address;
    private Map<String, List<Link>> neighbors;
    private Map<String, Map<String, List<Link>>> neighborsNeighbors;
    private Map<String, String> forwardingTable;

    public Router(RouterManager routerManager, int index, List<String> adjacent) throws UnknownHostException {
        this.routerManager = routerManager;
        this.index = index;
        neighbors = new HashMap<>();
        address = routerManager.getHostAddress() + ":" + index;
        neighbors.put(address, new ArrayList<>());
        if (adjacent != null) {
            for (String router : adjacent) {
                int random = (int) (10 * Math.random() + 1);
                neighbors.get(address).add(new Link(router, random));
                neighbors.put(router, new ArrayList<>());
                neighbors.get(router).add(new Link(address, random));
            }
        }
    }

    public Map<String, List<Link>> getNeighbors() {
        return neighbors;
    }

    private void updateForwardingTable() {
        forwardingTable = generateForwardingTable(address);
    }

    private String bestPath(String source, String goal) {
        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        Set<String> q = new HashSet<>();
        for (String vertex : neighbors.keySet()) {
            dist.put(vertex, Integer.MAX_VALUE);
            q.add(vertex);
        }
        dist.put(source, 0);
        while (!q.isEmpty()) {
            String u = null;
            for (String vertex : q) {
                if (u == null) {
                    u = vertex;
                } else {
                    if (dist.get(u) > dist.get(vertex)) {
                        u = vertex;
                    }
                }
            }
            if (u.equals(goal)) {
                break;
            }
            q.remove(u);
            for (Link neighbor : neighbors.get(u)) {
                if (q.contains(neighbor.getAddress())) {
                    int distance = dist.get(u) + neighbor.getWeight();
                    if (distance < dist.get(neighbor.getAddress())) {
                        dist.put(neighbor.getAddress(), distance);
                        prev.put(neighbor.getAddress(), u);
                    }
                }
            }
        }
        Stack<String> path = new Stack<>();
        path.push(goal);
        while (!path.peek().equals(source)) {
            path.push(prev.get(path.peek()));
        }
        path.pop();
        return path.pop();
    }

    private Map<String, String> generateForwardingTable(String router) {
        Map<String, String> forwardingTable = new HashMap<>();

        for (String destinationRouter : neighbors.keySet()) {
            if (!destinationRouter.equals(router)) {
                forwardingTable.put(destinationRouter, bestPath(router, destinationRouter));
            }
        }
        return forwardingTable;
    }

    public void receive(String content) throws IOException {
        System.out.printf("Router %d Received %s\n", index, content);
        String[] splitContent = content.split("\n");
        if (splitContent[0].equals("request")) {
            routerManager.send(splitContent[1], "reply\n" + address + "\n" + neighborsToString());
        } else if (splitContent[0].equals("reply")) {
            StringBuilder neighborString = new StringBuilder();
            for (int i = 2; i < splitContent.length; i++) {
                neighborString.append(splitContent[i]);
                neighborString.append("\n");
            }
            neighborsNeighbors.put(splitContent[1], stringToNeighbors(neighborString.toString()));
            boolean finished = true;
            for (Link link : neighbors.get(address)) {
                if (!neighborsNeighbors.containsKey(link.getAddress())) {
                    finished = false;
                    break;
                }
            }
            if (finished) {
                updateNetworkGraph();
                updateForwardingTable();
                for (String router : neighbors.keySet()) {
                    if (!router.equals(address)) {
                        routerManager.send(forwardingTable.get(router), "update\n" + router + "\n" + neighborsToString());
                    }
                }
            }
        } else if (splitContent[0].equals("update")) {
            if (splitContent[1].equals(address)) {
                StringBuilder neighborString = new StringBuilder();
                for (int i = 2; i < splitContent.length; i++) {
                    neighborString.append(splitContent[i]);
                    neighborString.append("\n");
                }
                neighbors = stringToNeighbors(neighborString.toString());
                updateForwardingTable();
            } else {
                routerManager.send(forwardingTable.get(splitContent[1]), content);
            }
        } else if (splitContent[0].equals("send")) {
            int first = content.indexOf("\n");
            int second = content.indexOf("\n", first + 1);
            int third = content.indexOf("\n", second + 1);
            String target = content.substring(first + 1, second);
            String path = content.substring(second + 1, third);
            String info = content.substring(third + 1);
            if (target.equals(address)) {
                System.out.println("Received file: ");
                System.out.println(path + "->" + address);
                File received = new File(address.replace(":", "-") + ".txt");
                if (received.exists()) {
                    received.delete();
                }
                received.createNewFile();
                PrintWriter writer = new PrintWriter(received);
                writer.write(info);
                writer.close();
            } else {
                path += "->" + address;
                routerManager.send(forwardingTable.get(target), "send\n" + target + "\n" + path + "\n" + info);
            }
        }
    }


    public void removeSelf() throws IOException {
        List<Link> neighborList = neighbors.get(address);
        for (Link neighbor : neighborList) {    //for neighbor in the router-to-be-removed's neighbor list,
            List<Link> neighborsNeighborsList = neighbors.get(neighbor.getAddress());
            for (int i = 0; i < neighborsNeighborsList.size(); i++) {     //remove self from the neighbors' lists
                if (neighborsNeighborsList.get(i).getAddress().equals(address)) {
                    neighborsNeighborsList.remove(i);
                    break;
                }
            }
        }
        neighbors.remove(address);//remove self from graph
        for (String router : neighbors.keySet()) {      //send update packet to all routers
            routerManager.send(forwardingTable.get(router), "update\n" + router + "\n" + neighborsToString());
        }
    }

    public String toString() {
        StringBuilder output = new StringBuilder();
        output.append("Router ");
        output.append(address);
        output.append("\n");
        output.append("----------------------");
        output.append("\n");
        for (String router : forwardingTable.keySet()) {
            output.append(router);
            output.append(" -> ");
            output.append(forwardingTable.get(router));
            output.append("\n");
        }
        output.append("----------------------");
        return output.toString();
    }

    public String forwardingTableToString(String address) {
        Map<String, String> forwardingTable = generateForwardingTable(address);
        StringBuilder output = new StringBuilder();
        output.append("Router ");
        output.append(address);
        output.append("\n");
        output.append("----------------------");
        output.append("\n");
        for (String router : forwardingTable.keySet()) {
            output.append(router);
            output.append(" -> ");
            output.append(forwardingTable.get(router));
            output.append("\n");
        }
        output.append("----------------------");
        return output.toString();
    }


    private void updateNetworkGraph() {
        for (String neighbor : neighborsNeighbors.keySet()) {
            Map<String, List<Link>> neighborsNeighbor = neighborsNeighbors.get(neighbor);
            for (String router : neighborsNeighbor.keySet()) {
                if (!neighbors.containsKey(router)) {
                    neighbors.put(router, new ArrayList<>());
                }
                for (Link link : neighborsNeighbor.get(router)) {
                    if (!neighbors.get(router).contains(link)) {
                        neighbors.get(router).add(link);
                    }
                }
            }
        }
    }

    private String neighborsToString() {
        StringBuilder output = new StringBuilder();
        for (String router : neighbors.keySet()) {
            output.append(router);
            output.append("-");
            for (Link neighbor : neighbors.get(router)) {
                output.append(neighbor.getAddress());
                output.append("=");
                output.append(neighbor.getWeight());
                output.append(",");
            }
            output.append("\n");
        }
        return output.toString();
    }

    private Map<String, List<Link>> stringToNeighbors(String message) {
        Map<String, List<Link>> output = new HashMap<>();
        String[] lines = message.split("\n");
        for (String line : lines) {
            String[] lineContent = line.split("-");
            output.put(lineContent[0], new ArrayList<>());
            if (lineContent.length > 1) {
                String[] links = lineContent[1].split(",");
                for (String link : links) {
                    String[] linkContent = link.split("=");
                    output.get(lineContent[0]).add(new Link(linkContent[0], Integer.parseInt(linkContent[1])));
                }
            }
        }
        return output;
    }

    public void neighborRequest() throws IOException {
        neighborsNeighbors = new HashMap<>();
        for (Link neighbor : neighbors.get(address)) {
            routerManager.send(neighbor.getAddress(), "request\n" + address);
        }
    }

    public String getAddress() {
        return address;
    }
}
