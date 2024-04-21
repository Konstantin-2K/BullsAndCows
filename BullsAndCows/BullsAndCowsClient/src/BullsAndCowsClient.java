import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class BullsAndCowsClient {
    private static JFrame frame;
    private static JTextField guessField;
    private static JTextField ipField;
    private static JTextField portField;
    private static JButton submitButton;
    private static JLabel resultLabel;
    private static JButton connectButton;
    private static JButton readyButton;
    private static Socket socket;
    private static PrintWriter writer;
    private static Scanner reader;
    private static final int CODE_LENGTH = 4;
    private static boolean gameOver = false;

    public static void main(String[] args) {
        frame = new JFrame("Bulls and Cows");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel gamePanel = new JPanel();
        gamePanel.setLayout(new BoxLayout(gamePanel, BoxLayout.Y_AXIS));

        JPanel connectionPanel = new JPanel(new FlowLayout());
        connectionPanel.add(new JLabel("IP:"));
        ipField = new JTextField(10);
        connectionPanel.add(ipField);
        connectionPanel.add(new JLabel("Port:"));
        portField = new JTextField(5);
        connectionPanel.add(portField);
        connectButton = new JButton("Connect");
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (ipField.getText().trim().split("\\.").length < 4 || ipField.getText().trim().split("\\.").length > 4) {
                    JOptionPane.showMessageDialog(frame, "Invalid IP address! Please enter a correct IP.");
                } else if (portField.getText().trim().length() < 4 || portField.getText().trim().length() > 5) {
                    JOptionPane.showMessageDialog(frame, "Invalid port! Please enter a correct port.");
                } else {
                    connectToServer();
                }
            }
        });
        connectionPanel.add(connectButton);
        gamePanel.add(connectionPanel);

        guessField = new JTextField(10);
        submitButton = new JButton("Submit");
        readyButton = new JButton("Ready");
        readyButton.setEnabled(false);
        resultLabel = new JLabel();

        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String guess = guessField.getText().trim();
                if (guess.matches("[a-zA-Z]+")) {
                    JOptionPane.showMessageDialog(frame, "Invalid guess! Please enter only numbers.");
                } else if (guess.length() != CODE_LENGTH) {
                    JOptionPane.showMessageDialog(frame, "Invalid guess! Please enter a " + CODE_LENGTH + "-digit number.");
                } else {
                    sendGuess(guess);
                    guessField.setText("");
                    guessField.requestFocus();
                }
            }
        });

        readyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetGame();
            }
        });

        JPanel guessPanel = new JPanel(new FlowLayout());
        guessPanel.add(new JLabel("Your Guess:"));
        guessPanel.add(guessField);
        guessPanel.add(submitButton);
        gamePanel.add(guessPanel);

        JPanel resultPanel = new JPanel(new FlowLayout());
        resultPanel.add(new JLabel("Result:"));
        resultPanel.add(resultLabel);
        gamePanel.add(resultPanel);

        JPanel readyPanel = new JPanel(new FlowLayout());
        readyPanel.add(readyButton);
        gamePanel.add(readyPanel);

        frame.add(gamePanel, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
    }

    private static void connectToServer() {
        String ip = ipField.getText().trim();
        int port = Integer.parseInt(portField.getText().trim());

        try {
            socket = new Socket(ip, port);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new Scanner(socket.getInputStream());
            String serverMessage = reader.nextLine();
            resultLabel.setText(serverMessage);
            submitButton.setEnabled(true);
            guessField.requestFocus();
            connectButton.setEnabled(false);
            readyButton.setEnabled(false);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!gameOver && reader.hasNextLine()) {
                        String result = reader.nextLine();
                        displayResult(result);
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Failed to connect to the server", "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void sendGuess(String guess) {
        writer.println(guess);
    }

    private static void displayResult(String result) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                resultLabel.setText(result);
                if (result.equals("Game over!")) {
                    gameOver = true;
                    submitButton.setEnabled(false);
                    readyButton.setEnabled(true);
                    JOptionPane.showMessageDialog(frame, "Game over! Press READY to start a new game.", "Game Over", JOptionPane.INFORMATION_MESSAGE);
                } else if (result.equals("Start new game")) {
                    resetGame();
                    resultLabel.setText("");
                }
            }
        });
    }

    private static void resetGame() {
        writer.println("Ready");
        gameOver = false;
        submitButton.setEnabled(true);
        readyButton.setEnabled(false);
        resultLabel.setText("");
        gameOver = false;

    }
}