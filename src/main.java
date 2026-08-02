import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;
import java.io.*;

public class main {
    private static JFrame frame;
    private static JLabel fileLabel, statusLabel;
    private static File selectedFile = null;
    private static boolean darkTheme = true;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) { e.printStackTrace(); }

        //garantir que a pasta save/ exista
        new File("save").mkdirs();

        frame = new JFrame("DDumper"); //nome da janela
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(520, 380);
        frame.setLocationRelativeTo(null);
        frame.setMinimumSize(new Dimension(450, 300));
        
        //icone no canto do app
        ImageIcon icon = new ImageIcon("assets/icon.png");
        if (icon.getIconWidth() > 0) { // verifica se carregou
            frame.setIconImage(icon.getImage());
        }


        //barra do menu superior
        JMenuBar menuBar = new JMenuBar();

        JMenu menuEdit = new JMenu("Edit");
        JMenu menuTheme = new JMenu("Theme");
        JRadioButtonMenuItem itemDark = new JRadioButtonMenuItem("Dark", true);
        JRadioButtonMenuItem itemLight = new JRadioButtonMenuItem("Light");
        ButtonGroup group = new ButtonGroup();
        group.add(itemDark); group.add(itemLight);
        menuTheme.add(itemDark); menuTheme.add(itemLight);
        menuEdit.add(menuTheme);
        itemDark.addActionListener(e -> setTheme(true));
        itemLight.addActionListener(e -> setTheme(false));

        JMenu menuView = new JMenu("View");
        JMenuItem mOpenSave = new JMenuItem("Open save folder");
        mOpenSave.addActionListener(e -> {
            try { Desktop.getDesktop().open(new File("save")); }
            catch (Exception ex) { ex.printStackTrace(); }
        });
        menuView.add(mOpenSave);

        JMenu menuAbout = new JMenu("About");
        JMenuItem itemRepo = new JMenuItem("Repository");
        itemRepo.addActionListener(e -> {
            try { Desktop.getDesktop().browse(new java.net.URI("https://github.com/CaioKotan/DDumper")); }
            catch (Exception ex) { ex.printStackTrace(); }
        });
        JMenuItem itemAuthor = new JMenuItem("Author");
        itemAuthor.addActionListener(e ->
            JOptionPane.showMessageDialog(frame,
                "DDumper v1.0 ( first release )\nBuilt with Java Swing + FlatLaf\nCore processing in C.\nby Caio A. Nog",
                "About", JOptionPane.INFORMATION_MESSAGE));
        menuAbout.add(itemRepo);
        menuAbout.add(itemAuthor);

        menuBar.add(menuEdit);
        menuBar.add(menuView);
        menuBar.add(menuAbout);
        frame.setJMenuBar(menuBar);

        // borda da "saida" do dumper.c
        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // botao de selecionar arquivo + painel
        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        top.setOpaque(false);
        JButton btnChoose = new JButton("Choose File");
        btnChoose.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btnChoose.setPreferredSize(new Dimension(130, 32));
        fileLabel = new JLabel("No file selected");
        fileLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        top.add(btnChoose);
        top.add(fileLabel);

        // area do texto do output do dumper.c
        JTextArea log = new JTextArea(8, 40);
        log.setEditable(false);
        log.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollLog = new JScrollPane(log);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Output"));

        // botao pra dumpar
        JButton btnDump = new JButton("Dump Binary");
        btnDump.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnDump.setPreferredSize(new Dimension(180, 36));
        btnDump.setEnabled(false);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.add(btnDump);

        center.add(top, BorderLayout.NORTH);
        center.add(scrollLog, BorderLayout.CENTER);
        center.add(bottom, BorderLayout.SOUTH);

        //onclick do botão
        btnChoose.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                selectedFile = fc.getSelectedFile();
                fileLabel.setText(selectedFile.getName());
                btnDump.setEnabled(true);
                log.setText("File selected: " + selectedFile.getAbsolutePath() + "\n");
                log.append("Click 'Dump Binary' to start.\n");
            }
        });

        btnDump.addActionListener(e -> {
            if (selectedFile == null || !selectedFile.exists()) return;

            btnDump.setEnabled(false);
            statusLabel.setText("Running...");
            log.append("\n--- Starting dump ---\n");

            SwingWorker<Void, String> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    try {
                        File exe = new File("bin/dumper.exe");
                        if (!exe.exists()) {
                            publish("ERROR: bin/dumper.exe not found!");
                            publish("Run: gcc -o bin/dumper.exe src/dumper.c");
                            return null;
                        }

                        ProcessBuilder pb = new ProcessBuilder(
                            exe.getAbsolutePath(),
                            selectedFile.getAbsolutePath()
                        );
                        pb.directory(new File("."));
                        Process process = pb.start();

                        //leitura da saida do arquivo dumper.c com parametros enviados
                        BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            publish(line);
                        }

                        //leitura de eventuais erros
                        BufferedReader errReader = new BufferedReader(
                            new InputStreamReader(process.getErrorStream()));
                        while ((line = errReader.readLine()) != null) {
                            publish("[STDERR] " + line);
                        }

                        int code = process.waitFor();
                        publish("Exit code: " + code);

                        if (code == 0) {
                            //mostra os arquivos na pasta save/
                            File saveDir = new File("save");
                            File[] files = saveDir.listFiles();
                            if (files != null) {
                                publish("Files in save/:");
                                for (File f : files) {
                                    publish("  " + f.getName() + " (" + f.length() + " bytes)");
                                }
                            }
                        }

                    } catch (Exception ex) {
                        publish("JAVA ERROR: " + ex.getMessage());
                    }
                    return null;
                }

                @Override
                protected void process(java.util.List<String> chunks) {
                    for (String line : chunks) {
                        log.append(line + "\n");
                        log.setCaretPosition(log.getDocument().getLength());
                    }
                }

                @Override
                protected void done() {
                    btnDump.setEnabled(true);
                    statusLabel.setText("Done - check Output and save/ folder");
                }
            };
            worker.execute();
        });

        frame.add(center, BorderLayout.CENTER);

        //footer? não sei o nome disso, "informações no canto do aplicativo"
        statusLabel = new JLabel("  Select a file to dump its binary content");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        frame.add(statusLabel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }
    private static void setTheme(boolean dark) {
        if (dark == darkTheme) return;
        darkTheme = dark;
        try {
            UIManager.setLookAndFeel(dark ? new FlatDarkLaf() : new FlatLightLaf());
            SwingUtilities.updateComponentTreeUI(frame);
            frame.pack();
        } catch (Exception ex) { ex.printStackTrace(); }
    }
}
