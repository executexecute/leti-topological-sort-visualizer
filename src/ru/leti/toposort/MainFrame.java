package ru.leti.toposort;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;

public class MainFrame extends JFrame {
    private final GraphModel graph = new GraphModel();
    private final TopologicalSorter sorter = new TopologicalSorter();
    private final GraphFileService graphFileService = new GraphFileService();
    private final ResultFileService resultFileService = new ResultFileService();
    private final GraphPanel graphPanel;

    private TopologicalSortResult lastResult = null;

    private final JTextArea explanationArea = new JTextArea();
    private final JLabel queueLabel = new JLabel("Начальная очередь: —");
    private final JLabel resultLabel = new JLabel("Результат: —");
    private final JLabel stepLabel = new JLabel("Версия: альфа");
    private final JLabel vertexCountLabel = new JLabel("Вершин: 0");
    private final JLabel edgeCountLabel = new JLabel("Рёбер: 0");
    private final JLabel statusLabel = new JLabel("Готово. Альфа-версия: загрузка графа и вывод итогового результата.");

    public MainFrame() {
        super("Визуализатор топологической сортировки — альфа-версия");

        graph.loadDemoGraph();

        graphPanel = new GraphPanel(graph, this::refreshStatePanel, this::setExplanation);
        graphPanel.setHighlightedVertex(0);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 680));
        setLayout(new BorderLayout());

        add(createToolbar(), BorderLayout.NORTH);
        add(createGraphArea(), BorderLayout.CENTER);
        add(createRightPanel(), BorderLayout.EAST);
        add(createBottomPanel(), BorderLayout.SOUTH);

        explanationArea.setText(
                "Альфа-версия приложения.\n"
                        + "Можно загрузить ориентированный граф из файла, отобразить его в окне и вывести итоговый результат топологической сортировки."
        );
        explanationArea.setLineWrap(true);
        explanationArea.setWrapStyleWord(true);
        explanationArea.setEditable(false);

        refreshStatePanel();
        pack();
        setLocationRelativeTo(null);
    }

    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        JButton addVertexButton = new JButton("Добавить вершину");
        JButton addEdgeButton = new JButton("Добавить ребро");
        JButton moveButton = new JButton("Переместить");
        JButton openButton = new JButton("Открыть");
        JButton saveButton = new JButton("Сохранить граф");
        JButton clearButton = new JButton("Очистить");
        JButton demoButton = new JButton("Демо-граф");
        JButton aboutButton = new JButton("О разработчиках");

        addVertexButton.addActionListener(e -> {
            graphPanel.setMode(Mode.ADD_VERTEX);
            setExplanation("Режим добавления вершин: щёлкните по рабочей области, чтобы создать вершину.");
        });

        addEdgeButton.addActionListener(e -> {
            graphPanel.setMode(Mode.ADD_EDGE);
            setExplanation("Режим добавления рёбер: выберите начальную и конечную вершину.");
        });

        moveButton.addActionListener(e -> {
            graphPanel.setMode(Mode.MOVE);
            setExplanation("Режим перемещения: перетащите вершину мышью.");
        });

        openButton.addActionListener(e -> openGraphFile());
        saveButton.addActionListener(e -> saveGraphFile());

        clearButton.addActionListener(e -> {
            graph.clear();
            lastResult = null;
            graphPanel.setHighlightedVertex(null);
            refreshStatePanel();
            graphPanel.repaint();
            setExplanation("Граф очищен. Можно добавить вершины вручную или загрузить граф из файла.");
        });

        demoButton.addActionListener(e -> {
            graph.loadDemoGraph();
            lastResult = null;
            graphPanel.setHighlightedVertex(0);
            refreshStatePanel();
            graphPanel.repaint();
            setExplanation("Загружен демонстрационный ациклический граф.");
        });

        aboutButton.addActionListener(e -> showAboutDialog());

        toolbar.add(addVertexButton);
        toolbar.add(addEdgeButton);
        toolbar.add(moveButton);
        toolbar.addSeparator();
        toolbar.add(openButton);
        toolbar.add(saveButton);
        toolbar.add(clearButton);
        toolbar.add(demoButton);
        toolbar.addSeparator();
        toolbar.add(aboutButton);

        return toolbar;
    }

    private JScrollPane createGraphArea() {
        JScrollPane scrollPane = new JScrollPane(graphPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Рабочая область с графом"));
        scrollPane.setPreferredSize(new Dimension(760, 540));
        return scrollPane;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(310, 540));
        panel.setBorder(BorderFactory.createTitledBorder("Управление алгоритмом"));

        JPanel buttons = new JPanel(new GridLayout(0, 1, 6, 6));

        JButton runButton = new JButton("Запустить алгоритм");
        JButton saveResultButton = new JButton("Сохранить результат");
        JButton previousButton = new JButton("Предыдущий шаг");
        JButton nextButton = new JButton("Следующий шаг");
        JButton autoButton = new JButton("Автозапуск");
        JButton stopButton = new JButton("Стоп");

        runButton.addActionListener(e -> runTopologicalSort());
        saveResultButton.addActionListener(e -> saveResultFile());

        previousButton.addActionListener(e -> showAlphaMessage("Пошаговый режим и шаг назад будут реализованы в бета-версии."));
        nextButton.addActionListener(e -> showAlphaMessage("Пошаговый режим будет реализован в бета-версии. В альфа-версии выводится итоговый результат."));
        autoButton.addActionListener(e -> showAlphaMessage("Автоматическое выполнение шагов будет реализовано в финальной версии."));
        stopButton.addActionListener(e -> showAlphaMessage("Остановка автозапуска будет доступна после добавления автоматического режима."));

        buttons.add(runButton);
        buttons.add(saveResultButton);
        buttons.add(previousButton);
        buttons.add(nextButton);
        buttons.add(autoButton);
        buttons.add(stopButton);

        JPanel statePanel = new JPanel(new GridLayout(0, 1, 6, 6));
        statePanel.setBorder(BorderFactory.createTitledBorder("Состояние"));
        statePanel.add(stepLabel);
        statePanel.add(vertexCountLabel);
        statePanel.add(edgeCountLabel);
        statePanel.add(queueLabel);
        statePanel.add(resultLabel);

        panel.add(buttons, BorderLayout.NORTH);
        panel.add(statePanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBottomPanel() {

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createTitledBorder("Пояснение"));

        JScrollPane explanationScroll = new JScrollPane(explanationArea);
        explanationScroll.setPreferredSize(new Dimension(900, 125));

        bottom.add(explanationScroll, BorderLayout.CENTER);


        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        bottom.add(statusLabel, BorderLayout.SOUTH);

        return bottom;
    }

    private void openGraphFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Открыть файл графа");

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        try {
            GraphModel loaded = graphFileService.load(file.toPath());
            graph.replaceWith(loaded);
            lastResult = null;
            graphPanel.setHighlightedVertex(null);
            refreshStatePanel();
            graphPanel.repaint();
            setExplanation("Граф загружен из файла:\n" + file.getName() + "\nТеперь можно запустить топологическую сортировку.");
        } catch (Exception exception) {
            showError("Не удалось загрузить граф", exception.getMessage());
        }
    }

    private void saveGraphFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Сохранить граф");
        chooser.setSelectedFile(new File("graph.txt"));

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            graphFileService.save(graph, chooser.getSelectedFile().toPath());
            setExplanation("Граф сохранён в файл:\n" + chooser.getSelectedFile().getName());
        } catch (Exception exception) {
            showError("Не удалось сохранить граф", exception.getMessage());
        }
    }

    private void saveResultFile() {
        if (lastResult == null) {
            showAlphaMessage("Сначала запустите алгоритм, чтобы получить результат.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Сохранить результат");
        chooser.setSelectedFile(new File("topological_sort_result.txt"));

        int dialogResult = chooser.showSaveDialog(this);
        if (dialogResult != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            resultFileService.save(graph, lastResult, chooser.getSelectedFile().toPath());
            setExplanation("Результат сохранён в файл:\n" + chooser.getSelectedFile().getName());
        } catch (Exception exception) {
            showError("Не удалось сохранить результат", exception.getMessage());
        }
    }

    private void runTopologicalSort() {
        TopologicalSortResult result = sorter.sort(graph);
        lastResult = result;

        StringBuilder text = new StringBuilder();
        text.append(result.getMessage()).append("\n\n");

        if (result.isSuccess()) {
            text.append("Топологический порядок:\n");
            text.append(result.getOrderAsText()).append("\n\n");
            text.append("В альфа-версии выводится итоговый результат алгоритма. ");
            text.append("Пошаговое выполнение будет добавлено в бета-версии.");

            if (!result.getOrder().isEmpty()) {
                graphPanel.setHighlightedVertex(result.getOrder().get(0));
            }
        } else {
            text.append("Частично построенный порядок:\n");
            text.append(result.getOrderAsText()).append("\n\n");
            text.append("Если в результате есть не все вершины, значит в графе остались вершины, входящие в цикл.");
            graphPanel.setHighlightedVertex(null);
        }

        resultLabel.setText("Результат: " + result.getOrderAsText());
        queueLabel.setText("Начальная очередь: " + getInitialQueueText(result));
        stepLabel.setText("Версия: альфа, результат получен");

        graphPanel.repaint();
        setExplanation(text.toString());
    }

    private String getInitialQueueText(TopologicalSortResult result) {
        StringBuilder queue = new StringBuilder();
        for (var entry : result.getInitialIndegrees().entrySet()) {
            if (entry.getValue() == 0) {
                if (queue.length() > 0) {
                    queue.append(", ");
                }
                queue.append(entry.getKey());
            }
        }
        return queue.length() == 0 ? "—" : queue.toString();
    }

    private void refreshStatePanel() {
        vertexCountLabel.setText("Вершин: " + graph.getVertexCount());
        edgeCountLabel.setText("Рёбер: " + graph.getEdgeCount());
        resultLabel.setText("Результат: " + (lastResult == null ? "—" : lastResult.getOrderAsText()));
        queueLabel.setText("Начальная очередь: —");
        statusLabel.setText("Вершин: " + graph.getVertexCount() + ", рёбер: " + graph.getEdgeCount() + ".");
    }

    private void setExplanation(String text) {
        explanationArea.setText(text);
        //statusLabel.setText(text.replace('\n', ' '));
    }

    private void showAlphaMessage(String message) {
        setExplanation(message);
        JOptionPane.showMessageDialog(this, message, "Альфа-версия", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String title, String message) {
        setExplanation(title + ":\n" + message);
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(
                this,
                "Визуализатор топологической сортировки\n\n"
                        + "Баневич Дмитрий, 4382\n"
                        + "Боков Максим, 4382\n"
                        + "Овчаренко Ярослав, 4383",
                "О разработчиках",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}
