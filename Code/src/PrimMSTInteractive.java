import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerListener;
import org.graphstream.ui.view.ViewerPipe;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class PrimMSTInteractive {
    private static Graph graph = new SingleGraph("Prim's MST");
    private static Viewer viewer;
    private static JFrame controlPanel;
    private static Set<String> visitedNodes = new HashSet<>();
    private static javax.swing.Timer animationTimer;
    private static int animationSpeed = 500;
    private static String startNode = null;

    public static void main(String[] args) {
        System.setProperty("org.graphstream.ui", "swing");

        initializeGraph();
        createDefaultGraph();

        viewer = graph.display();
        viewer.setCloseFramePolicy(Viewer.CloseFramePolicy.HIDE_ONLY);

        // Apply stylesheet again after display to force render
        graph.setAttribute("ui.stylesheet", styleSheet);

        createControlPanel();
        setupViewerListener();
    }

    private static void initializeGraph() {
        graph.setStrict(false);
        graph.setAutoCreate(true);
        graph.setAttribute("ui.stylesheet", styleSheet);
        graph.setAttribute("ui.quality");
        graph.setAttribute("ui.antialias");
    }

    private static void createDefaultGraph() {
        graph.clear();
        visitedNodes.clear();

        addEdge("A", "B", 4);
        addEdge("A", "H", 8);
        addEdge("B", "C", 8);
        addEdge("B", "H", 11);
        addEdge("C", "D", 7);
        addEdge("C", "F", 4);
        addEdge("C", "I", 2);
        addEdge("D", "E", 9);
        addEdge("D", "F", 14);
        addEdge("E", "F", 10);
        addEdge("F", "G", 2);
        addEdge("G", "H", 1);
        addEdge("G", "I", 6);
        addEdge("H", "I", 7);

        for (Node node : graph) {
            node.setAttribute("ui.label", node.getId());
            node.setAttribute("ui.class", "default");
        }
    }

    private static void addEdge(String src, String dest, int weight) {
        String id = src + "-" + dest;
        if (graph.getEdge(id) == null && graph.getEdge(dest + "-" + src) == null) {
            Edge edge = graph.addEdge(id, src, dest); // undirected
            edge.setAttribute("weight", weight);
            edge.setAttribute("ui.label", String.valueOf(weight));
            edge.setAttribute("ui.class", "default");
        }
    }

    private static void createControlPanel() {
        controlPanel = new JFrame("Prim's MST Controls");
        controlPanel.setSize(550, 150);
        controlPanel.setLayout(new FlowLayout());

        JButton runPrimButton = new JButton("Run Prim's");
        JButton resetButton = new JButton("Reset Visits");
        JButton simulateButton = new JButton("Simulate Prim's");
        JButton clearButton = new JButton("Clear Graph");
        JButton randomButton = new JButton("Random Connect");
        JButton selectStartButton = new JButton("Select Start");

        JSlider speedSlider = new JSlider(100, 2000, animationSpeed);
        speedSlider.setInverted(true);
        speedSlider.setPaintTicks(true);
        speedSlider.setMajorTickSpacing(400);
        speedSlider.setPaintLabels(true);
        speedSlider.addChangeListener(e -> animationSpeed = speedSlider.getValue());

        runPrimButton.addActionListener(e -> runPrimAlgorithm());
        resetButton.addActionListener(e -> resetVisits());
        simulateButton.addActionListener(e -> simulatePrim());
        clearButton.addActionListener(e -> clearGraph());
        randomButton.addActionListener(e -> randomConnect());
        selectStartButton.addActionListener(e -> selectStartNode());

        controlPanel.add(runPrimButton);
        controlPanel.add(resetButton);
        controlPanel.add(simulateButton);
        controlPanel.add(clearButton);
        controlPanel.add(randomButton);
        controlPanel.add(selectStartButton);
        controlPanel.add(new JLabel("Speed:"));
        controlPanel.add(speedSlider);

        controlPanel.setVisible(true);
    }

    private static void setupViewerListener() {
        ViewerPipe fromViewer = viewer.newViewerPipe();
        fromViewer.addViewerListener(new ViewerListener() {
            @Override public void viewClosed(String viewName) {}

            @Override
            public void buttonPushed(String id) {
                if (startNode == null) {
                    startNode = id;
                    graph.getNode(id).setAttribute("ui.class", "start");
                    JOptionPane.showMessageDialog(controlPanel,
                            "Selected " + id + " as start node",
                            "Start Node",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }

            @Override public void buttonReleased(String id) {}
        });

        new Thread(() -> {
            while (true) {
                fromViewer.pump();
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    private static void runPrimAlgorithm() {
        if (startNode == null) {
            JOptionPane.showMessageDialog(controlPanel, "Please select a start node first!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        resetVisits();
        runPrim(startNode);
    }

    private static void simulatePrim() {
        if (startNode == null) {
            JOptionPane.showMessageDialog(controlPanel, "Please select a start node first!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        resetVisits();
        if (animationTimer != null) animationTimer.stop();

        animationTimer = new javax.swing.Timer(animationSpeed, new ActionListener() {
            final PriorityQueue<Edge> pq = new PriorityQueue<>(Comparator.comparingInt(e -> (int) e.getAttribute("weight")));
            final Set<String> visited = new HashSet<>();

            {
                visited.add(startNode);
                pq.addAll(graph.getNode(startNode).getEdgeSet());
                graph.getNode(startNode).setAttribute("ui.class", "visited");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (pq.isEmpty()) {
                    ((javax.swing.Timer)e.getSource()).stop();
                    return;
                }

                Edge edge = pq.poll();
                String n1 = edge.getNode0().getId();
                String n2 = edge.getNode1().getId();
                String next = visited.contains(n1) ? n2 : n1;

                if (visited.contains(next)) return;

                edge.setAttribute("ui.class", "considering");
                graph.getNode(next).setAttribute("ui.class", "considering");

                javax.swing.Timer pauseTimer = new javax.swing.Timer(animationSpeed / 2, ev -> {
                    if (!visited.contains(next)) {
                        edge.setAttribute("ui.class", "marked");
                        graph.getNode(next).setAttribute("ui.class", "visited");
                        visited.add(next);

                        for (Edge e2 : graph.getNode(next).getEdgeSet()) {
                            String opp = e2.getOpposite(graph.getNode(next)).getId();
                            if (!visited.contains(opp)) pq.add(e2);
                        }
                    }
                    ((javax.swing.Timer)ev.getSource()).stop();
                });
                pauseTimer.setRepeats(false);
                pauseTimer.start();
            }
        });
        animationTimer.start();
    }

    private static void runPrim(String startNode) {
        Set<String> visited = new HashSet<>();
        PriorityQueue<Edge> pq = new PriorityQueue<>(Comparator.comparingInt(e -> (int) e.getAttribute("weight")));

        visited.add(startNode);
        graph.getNode(startNode).setAttribute("ui.class", "visited");
        pq.addAll(graph.getNode(startNode).getEdgeSet());

        while (!pq.isEmpty()) {
            Edge edge = pq.poll();
            String n1 = edge.getNode0().getId();
            String n2 = edge.getNode1().getId();
            String next = visited.contains(n1) ? n2 : n1;
            if (visited.contains(next)) continue;

            edge.setAttribute("ui.class", "marked");
            visited.add(next);
            graph.getNode(next).setAttribute("ui.class", "visited");

            for (Edge e : graph.getNode(next).getEdgeSet()) {
                String opp = e.getOpposite(graph.getNode(next)).getId();
                if (!visited.contains(opp)) pq.add(e);
            }
        }
    }

    private static void resetVisits() {
        for (Node node : graph) {
            node.setAttribute("ui.class", node.getId().equals(startNode) ? "start" : "default");
        }
        for (Edge edge : graph.getEdgeSet()) {
            edge.setAttribute("ui.class", "default");
        }
        visitedNodes.clear();
    }

    private static void clearGraph() {
        graph.clear();
        startNode = null;
        visitedNodes.clear();
    }

    private static void randomConnect() {
        Random rand = new Random();
        clearGraph();

        int nodeCount = 5 + rand.nextInt(6);
        for (int i = 0; i < nodeCount; i++) {
            String id = Character.toString((char) ('A' + i));
            graph.addNode(id).setAttribute("ui.label", id);
        }

        for (Node n1 : graph) {
            for (Node n2 : graph) {
                if (!n1.equals(n2) &&
                        graph.getEdge(n1.getId() + "-" + n2.getId()) == null &&
                        graph.getEdge(n2.getId() + "-" + n1.getId()) == null &&
                        rand.nextDouble() < 0.3) {
                    int weight = 1 + rand.nextInt(20);
                    addEdge(n1.getId(), n2.getId(), weight);
                }
            }
        }
    }

    private static void selectStartNode() {
        startNode = null;
        JOptionPane.showMessageDialog(controlPanel,
                "Click on a node to select it as the start node.",
                "Select Start Node",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private static final String styleSheet = """
        graph {
            fill-color: black;
            padding: 50px;
        }
        node {
            fill-color: #1a1a1a;
            text-color: white;
            size: 20px;
            text-size: 18;
            stroke-mode: plain;
            stroke-color: white;
        }
        node.start {
            fill-color: #00ff00;
            size: 25px;
        }
        node.visited {
            fill-color: #ff6600;
        }
        node.considering {
            fill-color: #ffff00;
        }
        edge {
            fill-color: white;
            text-size: 16;
            size: 2px;
        }
        edge.marked {
            fill-color: yellow;
            size: 4px;
            shadow-mode: plain;
            shadow-color: yellow;
        }
        edge.considering {
            fill-color: #ff00ff;
            size: 3px;
        }
    """;
}
