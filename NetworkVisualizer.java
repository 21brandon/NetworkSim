import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Landen on 5/4/2016.
 */
public class NetworkVisualizer extends JFrame implements ActionListener, MouseListener {
    private RouterManager routerManager;
    private JPanel textButton;
    private JPanel drawingArea;
    private JTextField neighbors;
    private JButton addButton;
    private JTable table;
    private String current;

    public NetworkVisualizer(RouterManager routerManager) {
        super("Network Visualizer");
        drawingArea = new JPanel();
        drawingArea.setPreferredSize(new Dimension(800, 800));
        drawingArea.addMouseListener(this);
        textButton = new JPanel();
        textButton.setPreferredSize(new Dimension(800, 50));
        neighbors = new JTextField();
        neighbors.setPreferredSize(new Dimension(700, 50));
        addButton = new JButton("Add");
        addButton.setPreferredSize(new Dimension(100, 50));
        addButton.addActionListener(this);

        add(drawingArea, BorderLayout.CENTER);
        add(textButton, BorderLayout.PAGE_END);
        textButton.add(neighbors, BorderLayout.LINE_START);
        textButton.add(addButton, BorderLayout.LINE_END);
        pack();
        setVisible(true);
        setResizable(false);
        this.routerManager = routerManager;


        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                //super.windowClosing(e);
                try {
                    NetworkVisualizer.this.routerManager.close();
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                System.exit(0);
            }
        });
    }

    public void draw() {
        Image buffer = createImage(800, 800);
        Graphics graphics = buffer.getGraphics();
        List<String> routers = new ArrayList<>();
        Map<String, List<Link>> graph = routerManager.getGraph();
        for (String router : graph.keySet()) {
            routers.add(router);
        }
        for (int i = 0; i < routers.size(); i++) {
            int[] myPosition = getPosition(i, routers.size());
            for (Link link : graph.get(routers.get(i))) {
                for (int j = 0; j < routers.size(); j++) {
                    if (routers.get(j).equals(link.getAddress())) {
                        int[] theirPosition = getPosition(j, routers.size());
                        graphics.setColor(Color.GREEN);
                        graphics.drawLine(myPosition[0], myPosition[1], theirPosition[0], theirPosition[1]);
                        if (routers.get(i).equals(current)) {
                            graphics.setColor(Color.BLACK);
                            graphics.drawString(link.getWeight() + "", (myPosition[0] + theirPosition[0]) / 2, (myPosition[1] + theirPosition[1]) / 2);
                        }
                        break;
                    }
                }
            }
        }
        for (int i = 0; i < routers.size(); i++) {
            int[] myPosition = getPosition(i, routers.size());
            if (routers.get(i).equals(current)) {
                graphics.setColor(Color.BLACK);
                int height = graphics.getFontMetrics().getHeight();
                String[] table = routerManager.routerToString(current).split("\n");
                for (int j = 0; j < table.length; j++) {
                    graphics.drawString(table[j], 15, 15 + (j + 1) * height);
                }
                graphics.setColor(Color.YELLOW);
            } else {
                graphics.setColor(Color.CYAN);
            }
            graphics.fillOval(myPosition[0] - 35, myPosition[1] - 35, 70, 70);
            graphics.setColor(Color.BLACK);
            int width = graphics.getFontMetrics().stringWidth(routers.get(i));
            graphics.drawString(routers.get(i), myPosition[0] - width / 2, myPosition[1]);

        }

        drawingArea.getGraphics().drawImage(buffer, 0, 0, this);
    }

    public int[] getPosition(int index, int total) {
        double separation = Math.PI * 2 / total;
        int[] output = new int[2];
        output[0] = (int) (Math.cos(separation * index) * 300 + 400);
        output[1] = (int) (Math.sin(separation * index) * 300 + 400);
        return output;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String text = neighbors.getText();
        neighbors.setText("");
        if (!text.contains("send")) {
            String[] others = text.split(",");
            List<String> othersList = new ArrayList<>();
            for (String x : others) {
                othersList.add(x);
            }
            try {
                routerManager.addRouter(othersList);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } else {
            String[] stuff = text.split("-");
            try {
                routerManager.sendFile(stuff[1], stuff[2], stuff[3]);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            List<String> routers = new ArrayList<>();
            Map<String, List<Link>> graph = routerManager.getGraph();
            for (String router : graph.keySet()) {
                routers.add(router);
            }
            for (int i = 0; i < routers.size(); i++) {
                int[] myPosition = getPosition(i, routers.size());
                if (Math.pow(myPosition[0] - e.getX(), 2) + Math.pow(myPosition[1] - e.getY(), 2) < 35 * 35) {
                    current = routers.get(i);
                    return;
                }
            }
            current = null;
        }
        if (e.getButton() == MouseEvent.BUTTON3) {
            List<String> routers = new ArrayList<>();
            Map<String, List<Link>> graph = routerManager.getGraph();
            for (String router : graph.keySet()) {
                routers.add(router);
            }
            for (int i = 0; i < routers.size(); i++) {
                int[] myPosition = getPosition(i, routers.size());
                if (Math.pow(myPosition[0] - e.getX(), 2) + Math.pow(myPosition[1] - e.getY(), 2) < 35 * 35) {
                    try {
                        routerManager.removeRouter(routers.get(i));
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    current = null;
                }
            }
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
