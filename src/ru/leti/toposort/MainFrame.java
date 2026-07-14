package ru.leti.toposort;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class MainFrame extends JFrame {
    private final GraphModel graph = new GraphModel();
    private final TopologicalSorter sorter = new TopologicalSorter();
    private final GraphFileService graphFileService = new GraphFileService();
    private final ResultFileService resultFileService = new ResultFileService();
    private final GraphPanel graphPanel;

    private TopologicalSortResult lastResult = null;
    private int currentStepIndex = -1;
    private final Timer autoTimer;

    private final JTextArea explanationArea = new JTextArea();
    private final JTextArea queueArea = createReadOnlyTextArea();
    private final JTextArea resultArea = createReadOnlyTextArea();
    private final JTextArea indegreesArea = createReadOnlyTextArea();

    private final JLabel stepLabel = new JLabel("Шаг: алгоритм не запущен");
    private final JLabel currentLabel = new JLabel("Текущий элемент: —");
    private final JLabel vertexCountLabel = new JLabel("Вершин: 0");
    private final JLabel edgeCountLabel = new JLabel("Рёбер: 0");
    private final JLabel statusLabel = new JLabel("Готово. Финальная версия приложения.");

    private final JButton runButton = new JButton("Запустить алгоритм");
    private final JButton previousButton = new JButton("Предыдущий шаг");
    private final JButton nextButton = new JButton("Следующий шаг");
    private final JButton autoButton = new JButton("Автозапуск");
    private final JButton stopButton = new JButton("Стоп");
    private final JButton saveResultButton = new JButton("Сохранить результат");
    private final JSpinner intervalSpinner = new JSpinner(new SpinnerNumberModel(1000, 200, 10000, 100));

    public MainFrame() {
        super("Визуализатор топологической сортировки — финальная версия");

        graph.loadDemoGraph();
        graphPanel = new GraphPanel(graph, this::handleGraphChanged, this::setExplanation);
        autoTimer = new Timer((Integer) intervalSpinner.getValue(), e -> advanceAutomatically());
        autoTimer.setInitialDelay((Integer) intervalSpinner.getValue());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 800));
        setPreferredSize(new Dimension(1380, 900));
        setLayout(new BorderLayout());

        add(createToolbar(), BorderLayout.NORTH);
        add(createGraphArea(), BorderLayout.CENTER);
        add(createRightPanel(), BorderLayout.EAST);
        add(createBottomPanel(), BorderLayout.SOUTH);

        explanationArea.setText(
                "Финальная версия визуализатора топологической сортировки.\n"
                        + "Граф можно построить вручную или открыть из файла. Алгоритм Кана выполняется "
                        + "по шагам вручную либо автоматически с выбранным интервалом. Доступен шаг назад.\n"
                        + "Текущая вершина и ребро выделяются на графе, а очередь, степени и результат отображаются справа."
        );
        explanationArea.setLineWrap(true);
        explanationArea.setWrapStyleWord(true);
        explanationArea.setEditable(false);

        configureActions();
        refreshGraphInfo();
        resetAlgorithmState();
        pack();
        setLocationRelativeTo(null);
    }

    private static JTextArea createReadOnlyTextArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setRows(2);
        return area;
    }

    private void configureActions() {
        runButton.addActionListener(e -> startStepByStepSort());
        previousButton.addActionListener(e -> showPreviousStep());
        nextButton.addActionListener(e -> showNextStep());
        autoButton.addActionListener(e -> startAutoRun());
        stopButton.addActionListener(e -> stopAutoRun("Автоматическое выполнение остановлено."));
        saveResultButton.addActionListener(e -> saveResultFile());
        intervalSpinner.addChangeListener(e -> {
            int interval = (Integer) intervalSpinner.getValue();
            autoTimer.setDelay(interval);
            autoTimer.setInitialDelay(interval);
        });
    }

    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        JButton addVertexButton = new JButton("Добавить вершину");
        JButton addEdgeButton = new JButton("Добавить ребро");
        JButton moveVertexButton = new JButton("Переместить вершину");
        JButton deleteVertexButton = new JButton("Удалить вершину");
        JButton deleteEdgeButton = new JButton("Удалить ребро");
        JButton openButton = new JButton("Открыть граф");
        JButton saveButton = new JButton("Сохранить граф");
        JButton clearButton = new JButton("Очистить граф");
        JButton demoButton = new JButton("Демо-граф");
        JButton aboutButton = new JButton("О разработчиках");

        addVertexButton.addActionListener(e -> {
            graphPanel.setMode(Mode.ADD_VERTEX);
            setExplanation("Режим добавления вершин: щёлкните по рабочей области.");
        });
        addEdgeButton.addActionListener(e -> {
            graphPanel.setMode(Mode.ADD_EDGE);
            setExplanation("Режим добавления рёбер: выберите начальную, затем конечную вершину.");
        });
        moveVertexButton.addActionListener(e -> {
            graphPanel.setMode(Mode.MOVE_VERTEX);
            setExplanation(
                    "Режим перемещения вершины: нажмите на вершину и перетащите её в новое место."
            );
        });
        deleteVertexButton.addActionListener(e -> {
            graphPanel.setMode(Mode.DELETE_VERTEX);
            setExplanation("Режим удаления вершины: нажмите на вершину. Все связанные рёбра будут удалены.");
        });
        deleteEdgeButton.addActionListener(e -> {
            graphPanel.setMode(Mode.DELETE_EDGE);
            setExplanation("Режим удаления ребра: нажмите рядом с линией нужного ребра.");
        });
        openButton.addActionListener(e -> openGraphFile());
        saveButton.addActionListener(e -> saveGraphFile());
        clearButton.addActionListener(e -> clearGraph());
        demoButton.addActionListener(e -> loadDemoGraph());
        aboutButton.addActionListener(e -> showAboutDialog());

        toolbar.add(addVertexButton);
        toolbar.add(addEdgeButton);
        toolbar.add(moveVertexButton);
        toolbar.add(deleteVertexButton);
        toolbar.add(deleteEdgeButton);
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
        JScrollPane scrollPane = new JScrollPane(
                graphPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        scrollPane.setBorder(BorderFactory.createTitledBorder("Рабочая область с графом"));
        scrollPane.setPreferredSize(new Dimension(820, 560));
        return scrollPane;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setPreferredSize(new Dimension(365, 585));
        panel.setBorder(BorderFactory.createTitledBorder("Управление алгоритмом"));

        JPanel buttons = new JPanel(new GridLayout(0, 1, 6, 6));
        buttons.add(runButton);
        buttons.add(previousButton);
        buttons.add(nextButton);
        buttons.add(autoButton);
        buttons.add(stopButton);

        JPanel intervalPanel = new JPanel(new BorderLayout(6, 0));
        intervalPanel.add(new JLabel("Интервал, мс:"), BorderLayout.WEST);
        intervalPanel.add(intervalSpinner, BorderLayout.CENTER);
        buttons.add(intervalPanel);
        buttons.add(saveResultButton);

        JPanel statePanel = new JPanel();
        statePanel.setLayout(new BoxLayout(statePanel, BoxLayout.Y_AXIS));
        statePanel.setBorder(BorderFactory.createTitledBorder("Состояние алгоритма"));
        statePanel.add(stepLabel);
        statePanel.add(currentLabel);
        statePanel.add(vertexCountLabel);
        statePanel.add(edgeCountLabel);
        statePanel.add(createLabeledScrollPane("Очередь", queueArea, 58));
        statePanel.add(createLabeledScrollPane("Текущий результат", resultArea, 68));
        statePanel.add(createLabeledScrollPane("Входящие степени", indegreesArea, 155));

        panel.add(buttons, BorderLayout.NORTH);
        panel.add(statePanel, BorderLayout.CENTER);
        return panel;
    }

    private JScrollPane createLabeledScrollPane(String title, JTextArea area, int height) {
        JScrollPane scrollPane = new JScrollPane(
                area,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        scrollPane.setBorder(BorderFactory.createTitledBorder(title));
        scrollPane.setPreferredSize(new Dimension(335, height));
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return scrollPane;
    }

    private JPanel createBottomPanel() {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createTitledBorder("Подробное пояснение текущего шага"));

        JScrollPane explanationScroll = new JScrollPane(
                explanationArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        explanationScroll.setPreferredSize(new Dimension(1050, 195));
        bottom.add(explanationScroll, BorderLayout.CENTER);

        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        bottom.add(statusLabel, BorderLayout.SOUTH);
        return bottom;
    }

    private void openGraphFile() {
        stopAutoRun(null);
        JFileChooser chooser = createTextFileChooser("Открыть граф");
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        try {
            GraphModel loaded = graphFileService.load(file.toPath());
            graph.replaceWith(loaded);
            graphPanel.updateCanvasSize();
            resetAlgorithmState();
            refreshGraphInfo();
            graphPanel.repaint();
            setExplanation(
                    "Граф успешно загружен из файла «" + file.getName() + "».\n"
                            + "Вершин: " + graph.getVertexCount() + ", рёбер: " + graph.getEdgeCount() + "."
            );
        } catch (IllegalArgumentException exception) {
            showError("Некорректный файл графа", exception.getMessage());
        } catch (SecurityException exception) {
            showError("Ошибка доступа", "Операционная система запретила доступ к файлу.");
        } catch (Exception exception) {
            showError("Не удалось открыть граф", safeMessage(exception));
        }
    }

    private void saveGraphFile() {
        JFileChooser chooser = createTextFileChooser("Сохранить граф");
        chooser.setSelectedFile(new File("graph.txt"));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            graphFileService.save(graph, chooser.getSelectedFile().toPath());
            setExplanation("Граф сохранён в файл «" + chooser.getSelectedFile().getName() + "».");
        } catch (SecurityException exception) {
            showError("Ошибка доступа", "Нет прав на запись в выбранную папку.");
        } catch (Exception exception) {
            showError("Не удалось сохранить граф", safeMessage(exception));
        }
    }

    private void saveResultFile() {
        if (lastResult == null) {
            showInformation("Сначала запустите алгоритм, чтобы получить результат.");
            return;
        }

        JFileChooser chooser = createTextFileChooser("Сохранить результат");
        chooser.setSelectedFile(new File("topological_sort_result.txt"));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            resultFileService.save(graph, lastResult, chooser.getSelectedFile().toPath());
            setExplanation("Результат сохранён в файл «" + chooser.getSelectedFile().getName() + "».");
        } catch (SecurityException exception) {
            showError("Ошибка доступа", "Нет прав на запись в выбранную папку.");
        } catch (Exception exception) {
            showError("Не удалось сохранить результат", safeMessage(exception));
        }
    }

    private JFileChooser createTextFileChooser(String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new FileNameExtensionFilter("Текстовые файлы (*.txt)", "txt"));
        return chooser;
    }

    private void clearGraph() {
        stopAutoRun(null);
        graph.clear();
        graphPanel.updateCanvasSize();
        resetAlgorithmState();
        refreshGraphInfo();
        graphPanel.repaint();
        setExplanation("Граф очищен. Можно построить новый граф или открыть его из файла.");
    }

    private void loadDemoGraph() {
        stopAutoRun(null);
        graph.loadDemoGraph();
        graphPanel.updateCanvasSize();
        resetAlgorithmState();
        refreshGraphInfo();
        graphPanel.repaint();
        setExplanation("Загружен демонстрационный ориентированный ациклический граф.");
    }

    private void startStepByStepSort() {
        stopAutoRun(null);
        try {
            lastResult = sorter.sort(graph);
            currentStepIndex = 0;
            displayCurrentStep();
        } catch (RuntimeException exception) {
            resetAlgorithmState();
            showError("Ошибка алгоритма", safeMessage(exception));
        }
    }

    private void showNextStep() {
        if (lastResult == null) {
            startStepByStepSort();
            return;
        }
        if (currentStepIndex < lastResult.getSteps().size() - 1) {
            currentStepIndex++;
            displayCurrentStep();
        }
    }

    private void showPreviousStep() {
        stopAutoRun(null);
        if (lastResult != null && currentStepIndex > 0) {
            currentStepIndex--;
            displayCurrentStep();
        }
    }

    private void startAutoRun() {
        if (graph.getVertexCount() == 0) {
            startStepByStepSort();
            return;
        }
        if (lastResult == null || currentStepIndex >= lastResult.getSteps().size() - 1) {
            lastResult = sorter.sort(graph);
            currentStepIndex = 0;
            displayCurrentStep();
        }
        int interval = (Integer) intervalSpinner.getValue();
        autoTimer.setDelay(interval);
        autoTimer.setInitialDelay(interval);
        autoTimer.start();
        setStatus("Автоматическое выполнение запущено. Интервал: " + interval + " мс.");
        updateControls();
    }

    private void advanceAutomatically() {
        if (lastResult == null || currentStepIndex >= lastResult.getSteps().size() - 1) {
            stopAutoRun("Автоматическое выполнение завершено.");
            return;
        }
        currentStepIndex++;
        displayCurrentStep();
        if (currentStepIndex >= lastResult.getSteps().size() - 1) {
            stopAutoRun("Автоматическое выполнение завершено.");
        }
    }

    private void stopAutoRun(String message) {
        if (autoTimer != null && autoTimer.isRunning()) {
            autoTimer.stop();
        }
        if (message != null) {
            setStatus(message);
        }
        updateControls();
    }

    private void displayCurrentStep() {
        if (lastResult == null || lastResult.getSteps().isEmpty() || currentStepIndex < 0) {
            return;
        }
        AlgorithmStep step = lastResult.getSteps().get(currentStepIndex);
        graphPanel.setAlgorithmState(step);

        stepLabel.setText("Шаг: " + (currentStepIndex + 1) + " из " + lastResult.getSteps().size());
        if (step.getCurrentEdge() != null) {
            currentLabel.setText(
                    "Текущее ребро: " + step.getCurrentEdge().getFrom() + " -> " + step.getCurrentEdge().getTo()
            );
        } else if (step.getCurrentVertex() != null) {
            currentLabel.setText("Текущая вершина: " + step.getCurrentVertex());
        } else {
            currentLabel.setText("Текущий элемент: —");
        }

        queueArea.setText(formatList(step.getQueue()));
        resultArea.setText(formatOrder(step.getResult()));
        indegreesArea.setText(formatIndegrees(step.getIndegrees()));
        explanationArea.setText(step.getDescription());
        explanationArea.setCaretPosition(0);

        if (step.isFinished()) {
            setStatus(step.isSuccess() ? "Алгоритм завершён успешно." : "Алгоритм завершён: обнаружен цикл или граф пуст.");
        } else {
            setStatus("Отображён шаг " + (currentStepIndex + 1) + ".");
        }
        updateControls();
    }

    private String formatList(List<Integer> values) {
        return values.isEmpty() ? "[]" : values.toString();
    }

    private String formatOrder(List<Integer> values) {
        if (values.isEmpty()) {
            return "—";
        }
        StringJoiner joiner = new StringJoiner(" -> ");
        for (Integer value : values) {
            joiner.add(String.valueOf(value));
        }
        return joiner.toString();
    }

    private String formatIndegrees(Map<Integer, Integer> indegrees) {
        if (indegrees.isEmpty()) {
            return "—";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Integer, Integer> entry : indegrees.entrySet()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(entry.getKey()).append(": ").append(entry.getValue());
        }
        return builder.toString();
    }

    private void handleGraphChanged() {
        resetAlgorithmState();
        refreshGraphInfo();
    }

    private void resetAlgorithmState() {
        if (autoTimer != null) {
            autoTimer.stop();
        }
        lastResult = null;
        currentStepIndex = -1;
        graphPanel.clearAlgorithmState();
        stepLabel.setText("Шаг: алгоритм не запущен");
        currentLabel.setText("Текущий элемент: —");
        queueArea.setText("—");
        resultArea.setText("—");
        indegreesArea.setText("—");
        updateControls();
    }

    private void refreshGraphInfo() {
        vertexCountLabel.setText("Вершин: " + graph.getVertexCount());
        edgeCountLabel.setText("Рёбер: " + graph.getEdgeCount());
    }

    private void updateControls() {
        boolean hasResult = lastResult != null && !lastResult.getSteps().isEmpty();
        boolean autoRunning = autoTimer != null && autoTimer.isRunning();
        boolean hasPrevious = hasResult && currentStepIndex > 0;
        boolean hasNext = hasResult && currentStepIndex < lastResult.getSteps().size() - 1;

        runButton.setEnabled(!autoRunning);
        previousButton.setEnabled(!autoRunning && hasPrevious);
        nextButton.setEnabled(!autoRunning && hasNext);
        autoButton.setEnabled(!autoRunning && graph.getVertexCount() > 0);
        stopButton.setEnabled(autoRunning);
        saveResultButton.setEnabled(hasResult);
        intervalSpinner.setEnabled(!autoRunning);
    }

    private void setExplanation(String text) {
        explanationArea.setText(text);
        explanationArea.setCaretPosition(0);
        setStatus(text.replace('\n', ' '));
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
        statusLabel.setToolTipText(text);
    }

    private void showInformation(String message) {
        JOptionPane.showMessageDialog(this, message, "Информация", JOptionPane.INFORMATION_MESSAGE);
        setExplanation(message);
    }

    private void showError(String title, String message) {
        String safe = message == null || message.isBlank() ? "Неизвестная ошибка." : message;
        JOptionPane.showMessageDialog(this, safe, title, JOptionPane.ERROR_MESSAGE);
        explanationArea.setText(title + ":\n" + safe);
        explanationArea.setCaretPosition(0);
        setStatus(title + ": " + safe);
    }

    private String safeMessage(Exception exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

    private void showAboutDialog() {
        String html = "<html><div style='width:390px'>"
                + "<h2>Визуализатор топологической сортировки</h2>"
                + "<b>Разработчики:</b><br>"
                + "Баневич Дмитрий, группа 4382<br>"
                + "Боков Максим, группа 4382<br>"
                + "Овчаренко Ярослав, группа 4383<br><br>"
                + "<b>Технологии:</b> Java 17, Swing, GitHub<br>"
                + "<b>Версия:</b> Release 1.0<br><hr>"
                + "<b>Любимый мем бригады:</b><br>"
                + "— Можно, а зачем?<br>"
                + "</div></html>";
        JOptionPane.showMessageDialog(this, html, "О разработчиках", JOptionPane.INFORMATION_MESSAGE);
    }
}
