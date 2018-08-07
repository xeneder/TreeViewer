package com.logsearcher;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Основной класс приложения. Здесь происходит формирование пользовательского интерфейса, а также обработчиков событий
 * и поиск по папкам и файлам.
 */
public class Main extends Application {

    /** Matcher, формирующийся при нажатии кнопки Search, и использующийся далее при нажатии кнопок Next и Back. */
    private Matcher matcher;

    /** Список найденных на данный момент совпадений в последнем открытом файле. */
    private List<Selection> selections = new ArrayList<>();

    /** Выбранная на данный момент запись из списка {@link #selections}. */
    private int currentSelection = 0;

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("LogSearcher");
        primaryStage.setScene(initInterface(primaryStage));
        primaryStage.setMinWidth(640);
        primaryStage.setMinHeight(300);
        primaryStage.show();
    }

    /**
     * Метод, инициализирующий интерфейс и обработчики. Интерфейс включает в себя
     * <ul>
     *     <li>fileTextField — поле, в котором отображается текущий файл</li>
     *     <li>button — кнопка, вызывающая выбор папки, в которой будет осуществляться поиск</li>
     *     <li>hBox1 — контейнер fileTextField и button</li>
     *     <li>searchTextField — поле для ввода поискового запроса</li>
     *     <li>extensionLabel, extensionTextField, afterLabel — вспомогательные надписи</li>
     *     <li>extensionTextField — поле для ввода расширения</li>
     *     <li>hBox2 — контейнер searchTextField, extensionTextField, extensionLabel, extensionTextField и afterLabel</li>
     *     <li>searchButton — кнопка поиска. Кнопка по-умолчанию, т.е. при нажатии Enter в любом поле будет выбрана она.</li>
     *     <li>backButton, nextButton — кнопки навигации по текущей вкладке</li>
     *     <li>backButton, nextButton — кнопки навигации по текущей вкладке</li>
     *     <li>hBox3 — контейнер searchButton, backButton, nextButton</li>
     *     <li>TreeView — отображает древо папок и файлов. Файлы подсвечены красным цветом.</li>
     *     <li>leftVBox — левый контейнер для hBox1, 2, 3</li>
     *     <li>tabPane — поле для вкладок, каждая из которых содержит TextArea с текстом файла.</li>
     *     <li>rightVBox — правый контейнер для tabPane</li>
     *     <li>rightVBox — правый контейнер для tabPane</li>
     *     <li>mainBox — общий HBox для leftVBox и rightVBox.</li>
     * </ul>
     * Обработчики {@link #fileSelectionHandlers(TextField, TreeView, TabPane)},
     * {@link #folderSelectionHandler(Stage, File[], TreeItem, DirectoryChooser,
     * TextField, Button, TextField, TextField, TabPane)},
     * {@link #searchQueryHandler(TextField, TextField, File[], TreeItem, Button, TabPane)},
     * {@link #nextBackHandlers(Button, Button, TabPane)},
     * {@link #changeTabHandler(TabPane, TextField)}
     * описаны отдельно.
     * @param stage Главная Stage.
     * @return Уже собранная Scene.
     */
    private Scene initInterface(Stage stage) {
        final File[] f = new File[1];
        TreeItem<File> rootItem = new TreeItem<>(new File("Please choose a folder and enter a query"));

        //создаём DirectoryChooser
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose directory");

        //создаём UI для выбора папки
        TextField fileTextField = new TextField();
        fileTextField.setEditable(false);
        Button button = new Button("Choose directory");

        //добавляем в HBox №1
        HBox hBox1 = new HBox();
        hBox1.getChildren().addAll(fileTextField, button);
        HBox.setHgrow(fileTextField, Priority.ALWAYS);
        hBox1.setSpacing(8);

        //создаём поисковый TextField, задаём его свойства и Listener
        TextField searchTextField = new TextField();

        searchTextField.setPromptText("Search...");

        //создаём Label и TextField для ввода расширения
        Label extensionLabel = new Label("in *.");

        TextField extensionTextField = new TextField();
        extensionTextField.setText("log");

        Label afterLabel = new Label("files");

        //добавляем в HBox №2
        HBox hBox2 = new HBox();
        hBox2.getChildren().addAll(searchTextField, extensionLabel, extensionTextField, afterLabel);
        HBox.setHgrow(searchTextField, Priority.ALWAYS);
        hBox2.setSpacing(8);

        //создаём три кнопки: поиск, назад и далее
        Button searchButton = new Button("Search");
        Button backButton = new Button("Back");
        Button nextButton = new Button("Next");
        searchButton.setAlignment(Pos.BASELINE_RIGHT);
        backButton.setAlignment(Pos.BASELINE_RIGHT);
        nextButton.setAlignment(Pos.BASELINE_RIGHT);

        //добавляем в HBox №3
        HBox hBox3 = new HBox();
        hBox3.getChildren().addAll(searchButton, backButton, nextButton);
        hBox2.setSpacing(8);
        hBox3.setAlignment(Pos.BASELINE_RIGHT);

        //создаём TreeView, задаём свойства
        TreeView<File> treeView = new TreeView<>(rootItem);
        treeView.setEditable(true);

        //задаём CellFactory
        treeView.setCellFactory(new Callback<>(){
            @Override
            public TreeCell<File> call(TreeView<File> p) {
                return new TreeCell<>() {
                    @Override
                    public void updateItem(File item, boolean empty) {
                        super.updateItem(item, empty);

                        if (empty) {
                            setText(null);
                        } else {
                            setText(getItem().getName());
                            if (item.isFile()) {
                                setStyle("-fx-text-fill: #c62828;");
                            } else {
                                setStyle("-fx-text-fill: black;");
                            }
                        }
                    }
                };
            }
        });

        //создаём левый VBox
        VBox leftVBox = new VBox();
        VBox.setVgrow(treeView, Priority.ALWAYS);
        leftVBox.getChildren().addAll(hBox1, hBox2, hBox3, treeView);
        leftVBox.setMinWidth(400);
        leftVBox.setSpacing(8);

        //создаём правый TabPane
        TabPane tabPane = new TabPane();

        //создаём правый VBox
        VBox rightVBox = new VBox();
        rightVBox.getChildren().add(tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        //создаём общий HBox
        HBox mainBox = new HBox();
        mainBox.setPadding(new Insets(8, 8, 8, 8));
        mainBox.setSpacing(8);
        HBox.setHgrow(rightVBox, Priority.ALWAYS);
        mainBox.getChildren().addAll(leftVBox, rightVBox);

        //обработчики
        folderSelectionHandler(stage, f, rootItem, directoryChooser, fileTextField, button, searchTextField,
                extensionTextField, tabPane);
        searchQueryHandler(searchTextField, extensionTextField, f, rootItem, searchButton, tabPane);
        fileSelectionHandlers(searchTextField, treeView, tabPane);
        nextBackHandlers(nextButton, backButton, tabPane);
        changeTabHandler(tabPane, searchTextField);

        return new Scene(mainBox, (double) 640, (double) 480);
    }

    /**
     * Обработчик выбора папки для поиска
     * @param stage Окно, поверх которого будет показан диалог
     * @param f Папка, которая будет выбрана
     * @param rootItem Корневой элемент TreeView
     * @param directoryChooser Интерфейс выбора каталога
     * @param fileTextField Поле, в котором написан адре каталога для поиска
     * @param button Кнопка для выбора каталога
     * @param searchTextField Поле для поискового запроса
     * @param extensionTextField Поле для ввода расширения
     * @param tabPane Поле для вкладок, каждая из которых содержит TextArea с текстом файла
     */
    private void folderSelectionHandler(Stage stage, File[] f, TreeItem<File> rootItem,
                                        DirectoryChooser directoryChooser, TextField fileTextField,
                                        Button button, TextField searchTextField, TextField extensionTextField,
                                        TabPane tabPane) {
        button.setOnAction(e -> {
            f[0] = directoryChooser.showDialog(stage);
            if (f[0] != null) {
                String filepath = f[0].getAbsolutePath();
                fileTextField.setText(filepath);
                refreshFileTree(searchTextField.getText(), extensionTextField.getText(), f[0],
                        rootItem);
                refreshTab(tabPane.getSelectionModel().getSelectedItem(), searchTextField);
            }
        });
    }

    /**
     * Обработчик поискового запроса (кнопка Search либо нажатие Enter).
     * TODO: придумать как различить обновление поискового запроса для поиска по файлу и для поиска по каталогу
     * @param searchTextField Поле для поискового запроса
     * @param extensionTextField Поле для ввода расширения
     * @param f Папка, которая будет выбрана
     * @param rootItem Корневой элемент TreeView
     * @param searchButton Кнопка поиска
     * @param tabPane Поле для вкладок, каждая из которых содержит TextArea с текстом файла
     */
    private void searchQueryHandler(TextField searchTextField, TextField extensionTextField, File[] f,
                                    TreeItem<File> rootItem, Button searchButton, TabPane tabPane) {
        searchButton.setDefaultButton(true);
        searchButton.setOnAction(e -> {
            refreshFileTree(searchTextField.getText(), extensionTextField.getText(), f[0],
                    rootItem);
            refreshTab(tabPane.getSelectionModel().getSelectedItem(), searchTextField);
        });
    }

    /**
     * Обработчики выбора файла в TreeView с мыши и клавиатуры.
     * TODO: убрать повторное открытие уже открытого файла по клику.
     * TODO: исправить баг с открытием ранее выбранного файла при раскрытии/закрытии древа каталогов.
     * @param searchTextField Поле для поискового запроса
     * @param treeView Просмотрщик дерева каталогов и файлов.
     * @param tabPane Поле для вкладок, каждая из которых содержит TextArea с текстом файла
     */
    private void fileSelectionHandlers(TextField searchTextField, TreeView<File> treeView, TabPane tabPane) {
        treeView.setOnMouseClicked(e -> fileSelectionHandler(searchTextField, treeView, tabPane));
        treeView.setOnKeyPressed(e -> {
            if (e.getCode().equals(KeyCode.ENTER)) {
                fileSelectionHandler(searchTextField, treeView, tabPane);
            }
        });
    }

    /**
     * Общий метод для разных обработчиков выбора файла в TreeView.
     * @param searchTextField Поле для поискового запроса
     * @param treeView Просмотрщик дерева каталогов и файлов.
     * @param tabPane Поле для вкладок, каждая из которых содержит TextArea с текстом файла
     */
    private void fileSelectionHandler(TextField searchTextField, TreeView<File> treeView, TabPane tabPane) {
        TreeItem<File> selectedItem = treeView.getSelectionModel().getSelectedItem();

        Task<Tab> task = new Task<>() {
            @Override
            protected Tab call() {
                StringBuilder content = new StringBuilder();
                try {
                    String s;
                    String path = selectedItem.getValue().getCanonicalPath();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                            new FileInputStream(path), StandardCharsets.UTF_8));
                    while ((s = bufferedReader.readLine()) != null) {
                        content.append(s).append('\n');
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                TextArea contentArea = new TextArea(content.toString());
                contentArea.setEditable(false);

                Tab tab = new Tab();
                tab.setText(selectedItem.getValue().getName());
                tab.setContent(contentArea);
                return tab;
            }
        };

        task.setOnSucceeded(e -> {
            tabPane.getTabs().add(task.getValue());
            tabPane.getSelectionModel().select(task.getValue());

            refreshTab(tabPane.getSelectionModel().getSelectedItem(), searchTextField);
        });

        if (selectedItem != null) {
            File selectedFile = selectedItem.getValue();
            if (selectedFile.exists() && selectedFile.isFile()) {
                new Thread(task).start();
            }
        }
    }

    /**
     * Обработчики кнопок "Back" и "Next" для навигации по открытому в TabPane файлу.
     * TODO: добавить возможность начинать заново без переключения вкладок. Возможно, зациклить поиск.
     * @param nextButton Кнопка "вперёд" для навигации по открытому файлу
     * @param backButton Кнопка "назад" для навигации по открытому файлу
     * @param tabPane Область открытых вкладок
     */
    private void nextBackHandlers(Button nextButton, Button backButton,
                                  TabPane tabPane) {
        nextButton.setOnAction(e -> {
            Tab tab = tabPane.getSelectionModel().getSelectedItem();
            if (tab != null) {
                TextArea textArea = (TextArea) tab.getContent();
                if (matcher != null) {
                        if (currentSelection >= selections.size() - 1) {
                            boolean found = matcher.find();
                            if (found) {
                                textArea.selectRange(matcher.start(), matcher.end());
                                selections.add(new Selection(matcher.start(), matcher.end()));
                                currentSelection++;
                            }
                        } else {
                            currentSelection++;
                            textArea.selectRange(selections.get(currentSelection).getStart(),
                                    selections.get(currentSelection).getEnd());
                        }
                }
            }
        });

        backButton.setOnAction(e -> {
            Tab tab = tabPane.getSelectionModel().getSelectedItem();
            if (tab != null) {
                TextArea textArea = (TextArea) tab.getContent();
                currentSelection--;
                if (currentSelection >= 0) {
                    textArea.selectRange(selections.get(currentSelection).getStart(),
                            selections.get(currentSelection).getEnd());
                } else {
                    currentSelection = 0;
                }
            }
        });
    }

    /**
     * Обработчик смены открытой вкладки.
     * @param tabPane Область вкладок
     * @param searchTextField Поле для поискового запроса
     */
    private void changeTabHandler(TabPane tabPane, TextField searchTextField) {
        tabPane.getSelectionModel().selectedItemProperty().addListener(
                (ov, t, t1) -> refreshTab(t1, searchTextField)
        );
    }

    /**
     * Общий метод для обновления TreeView.
     * @param query Поисковый запрос
     * @param extension Расширение искомого файла
     * @param file Корневой каталог
     * @param rootItem Корневой элемент TreeView
     */
    private void refreshFileTree(String query, String extension, File file,
                                 TreeItem<File> rootItem) {
        Task<List<TreeItem<File>>> task = new Task<>() {
            @Override
            public List<TreeItem<File>> call() {
                return addItems(file, query, extension);
            }
        };

        task.setOnSucceeded(e -> {
            if (file != null) {
                rootItem.getChildren().addAll(task.getValue());
                rootItem.setExpanded(true);
                rootItem.setValue(new File("Done!"));
            }
        });

        rootItem.setValue(new File("Loading..."));
        rootItem.getChildren().clear();
        new Thread(task).start();
    }

    /**
     * Общий метод для обновления TabPane.
     * @param tab Открытая вкладка
     * @param searchTextField Поле для поискового запроса
     */
    private void refreshTab(Tab tab, TextField searchTextField) {
        if (tab != null) {
            TextArea textArea = (TextArea) tab.getContent();
            String content = textArea.getText();

            Pattern pattern = Pattern.compile(searchTextField.getText().toLowerCase());
            matcher = pattern.matcher(content.toLowerCase());
            boolean found = matcher.find();
            selections.clear();
            currentSelection = 0;
            if (found) {
                textArea.selectRange(matcher.start(), matcher.end());
                selections.add(new Selection(matcher.start(), matcher.end()));
            }
        }
    }

    /**
     * Возвращает древовидную структуру, которая прикрепляется к корневому узлу TreeView.
     * Рекурсивно проходится по всем файлам и папкам.
     * @param file Корневой каталог
     * @param query Текст запроса
     * @param extension Расширение искомых файлов
     * @return Список узлов, лежащих в корневом каталоге, в которые вложены их дочерние каталоги и файлы
     */
    private List<TreeItem<File>> addItems(File file, String query, String extension) {
        List<TreeItem<File>> list = new ArrayList<>();
        for (File f : Objects.requireNonNull(file.listFiles())) {
            TreeItem<File> item = new TreeItem<>(f);
            if (f.isDirectory()) {
                item.getChildren().addAll(addItems(f, query, extension));
                list.add(item);
            } else if (f.isFile()) {
                if (f.getName().endsWith('.' + extension) || extension.equals("*")) {
                    if (query.equals("")) {
                        list.add(item);
                    } else {
                        if (searchQuery(f, query)) {
                            list.add(item);
                        }
                    }
                }
            }
        }
        return list;
    }

    /**
     * Производит поисковый запрос до тех пор, пока не будет обнаружена искомая последовательность.
     * @param f Файл, в котором выполняется поиск
     * @param query Поисковый запрос
     * @return Найдена ли искомая строка
     */
    private boolean searchQuery(File f, String query) {
        //поиск в файле
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.toLowerCase().contains(query.toLowerCase())) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
