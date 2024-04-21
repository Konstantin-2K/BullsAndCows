import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class BullsAndCowsServer {
    private static final int CODE_LENGTH = 4;
    private static JFrame frame;
    private static JLabel player1ResultLabel;
    private static JLabel player2ResultLabel;

    public static void main(String[] args) {
        int[] secretCode = generateSecretCode();

        frame = new JFrame("Bulls and Cows");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(300, 200));
        frame.setLayout(new BorderLayout());

        JPanel gamePanel = new JPanel();
        gamePanel.setLayout(new BoxLayout(gamePanel, BoxLayout.Y_AXIS));

        player1ResultLabel = new JLabel();
        player2ResultLabel = new JLabel();

        JPanel portPanel = new JPanel(new FlowLayout());
        portPanel.add(new JLabel("Port:"));
        JTextField portField = new JTextField(10);
        portPanel.add(portField);

        JButton startServerButton = new JButton("Start Server");
        startServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String portStr = portField.getText().trim();
                if (portStr.length() < 4 || portStr.length() > 5) {
                    JOptionPane.showMessageDialog(frame, "Invalid port! Please enter a correct port.");
                } else {
                    int port = Integer.parseInt(portStr);
                    startGame(port);
                }
            }
        });
        portPanel.add(startServerButton);
        gamePanel.add(portPanel);

        JPanel player1ResultPanel = new JPanel(new FlowLayout());
        player1ResultPanel.add(new JLabel("Player 1 Result:"));
        player1ResultPanel.add(player1ResultLabel);
        gamePanel.add(player1ResultPanel);

        JPanel player2ResultPanel = new JPanel(new FlowLayout());
        player2ResultPanel.add(new JLabel("Player 2 Result:"));
        player2ResultPanel.add(player2ResultLabel);
        gamePanel.add(player2ResultPanel);

        frame.add(gamePanel, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
    }

    private static Socket player1Socket;
    private static Socket player2Socket;

    private static void startGame(int port) {
        SwingWorker<Void, Void> gameWorker = new SwingWorker<>() {
            URL ipAddress;
            @Override
            protected Void doInBackground() {
                try {
                    ServerSocket serverSocket = new ServerSocket(port);
                    System.out.println("Server started. Waiting for connections...");
                    int[] secretCode = generateSecretCode();

                    while (true) {
                        ipAddress = new URL("https://myexternalip.com/raw");
                        BufferedReader input = new BufferedReader(new InputStreamReader(ipAddress.openStream()));
                        String ip = input.readLine();
                        System.out.printf("IP and PORT of the server to connect to: %s:%d%n", ip, port);

                        player1Socket = serverSocket.accept();
                        System.out.println("Player 1 connected.");
                        PrintWriter player1Writer = new PrintWriter(player1Socket.getOutputStream(), true);
                        player1Writer.println("Welcome to Bulls and Cows!");

                        player2Socket = serverSocket.accept();
                        System.out.println("Player 2 connected.");
                        PrintWriter player2Writer = new PrintWriter(player2Socket.getOutputStream(), true);
                        player2Writer.println("Welcome to Bulls and Cows!");

                        boolean gameOver = false;

                        while (!gameOver) {
                            int[] player1Guess = receiveGuess(player1Socket);
                            int[] player2Guess = receiveGuess(player2Socket);

                            int player1Bulls = countBulls(player1Guess, secretCode);
                            int player1Cows = countCows(player1Guess, secretCode);
                            int player2Bulls = countBulls(player2Guess, secretCode);
                            int player2Cows = countCows(player2Guess, secretCode);

                            String player1Result = "Bulls: " + player1Bulls + ", Cows: " + player1Cows;
                            String player2Result = "Bulls: " + player2Bulls + ", Cows: " + player2Cows;

                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    player1ResultLabel.setText(player1Result);
                                    player2ResultLabel.setText(player2Result);
                                }
                            });

                            player1Writer.println(player1Result);
                            player2Writer.println(player2Result);

                            if (player1Bulls == CODE_LENGTH || player2Bulls == CODE_LENGTH) {
                                gameOver = true;
                                player1Writer.println("Game over!");
                                player2Writer.println("Game over!");

                                boolean player1Ready = false;
                                boolean player2Ready = false;

                                while (!(player1Ready && player2Ready)) {
                                    String player1Signal = receiveSignal(player1Socket);
                                    String player2Signal = receiveSignal(player2Socket);

                                    if (player1Signal.equals("Ready")) {
                                        player1Ready = true;
                                        player1Writer.println("New game");
                                    }

                                    if (player2Signal.equals("Ready")) {
                                        player2Ready = true;
                                        player2Writer.println("New game");
                                    }
                                }

                                secretCode = generateSecretCode();
                                gameOver = false;
                                player1Writer.println("Start new game");
                                player2Writer.println("Start new game");
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }

        };

        gameWorker.execute();
    }

    private static String receiveSignal(Socket socket) throws IOException {
        Scanner scanner = new Scanner(socket.getInputStream());
        return scanner.nextLine();
    }

    private static int[] generateSecretCode() {
        Random random = new Random();
        int[] code = new int[CODE_LENGTH];

        for (int i = 0; i < CODE_LENGTH; i++) {
            code[i] = random.nextInt(10);
        }
        System.out.print("Secret code for current game: ");
        for (int i = 0; i < CODE_LENGTH; i++) {
            System.out.print(code[i]);
        }
        System.out.println();
        return code;
    }

    private static int[] receiveGuess(Socket socket) throws IOException {
        Scanner scanner = new Scanner(socket.getInputStream());
        String guessString = scanner.nextLine();

        int[] guess = new int[CODE_LENGTH];

        for (int i = 0; i < CODE_LENGTH; i++) {
            guess[i] = Character.getNumericValue(guessString.charAt(i));
        }

        return guess;
    }

    private static int countBulls(int[] guess, int[] secretCode) {
        int bulls = 0;

        for (int i = 0; i < CODE_LENGTH; i++) {
            if (guess[i] == secretCode[i]) {
                bulls++;
            }
        }

        return bulls;
    }

    private static int countCows(int[] guess, int[] secretCode) {
        int cows = 0;
        int[] secretCodeCopy = Arrays.copyOf(secretCode, CODE_LENGTH);

        for (int i = 0; i < CODE_LENGTH; i++) {
            if (guess[i] == secretCode[i]) {
                secretCodeCopy[i] = -1;
            }
        }

        for (int i = 0; i < CODE_LENGTH; i++) {
            if (guess[i] != secretCode[i]) {
                for (int j = 0; j < CODE_LENGTH; j++) {
                    if (guess[i] == secretCodeCopy[j]) {
                        cows++;
                        secretCodeCopy[j] = -1;
                        break;
                    }
                }
            }
        }

        return cows;
    }
}

