import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;

public class TCPSocketTest {
    private static String service;
    private static int port;
    private static String host;
    private static boolean log_ping;
    private static boolean log_failures;
    private static int retryConnection = 2000;
    private static int sleepInterval = 5000;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage is either \"java -jar SocketServerClient.jar\"" +
                    "\n\t-t [server | client]" +
                    "\n\t-h host" +
                    "\n\t-p port" +
                    "\n\t-n #use to log successful pings" +
                    "\n\t-f #use to log connection failures" +
                    "\n\t-r (ms) connection retry interval" +
                    "\n\t-s (ms) application ping sleep time"
            );
            System.exit(0);
        }

        for (int i = 0; i < args.length; i++) {
            String args_i = args[i];
            if (args_i.startsWith("-")) {
                char command = args_i.charAt(1);

                switch (command) {
                    case 't':
                        service = args[i + 1];
                        break;
                    case 'p':
                        port = Integer.parseInt(args[i + 1]);
                        break;
                    case 'h':
                        host = args[i + 1];
                        break;
                    case 'n':
                        log_ping = true;
                        break;
                    case 'f':
                        log_failures = true;
                        break;
                    case 'r':
                        retryConnection = Integer.parseInt(args[i + 1]);
                        break;
                    case 's':
                        sleepInterval = Integer.parseInt(args[i + 1]);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + command);
                }
            }
        }
        if (service.equalsIgnoreCase("server")) {
            new TCPServer(port);
        } else if (service.equalsIgnoreCase("client")) {
            TCPClient client = new TCPClient(host, port);
            client.setCLIENT_SLEEP(sleepInterval);
            client.setConnRetryInt(retryConnection);
            client.read();
        }
    }

    private static class TCPServer {
        public TCPServer(int port) {
            ServerSocket serverSocket = null;
            Socket clientSocket = null;
            PrintWriter out;
            BufferedReader in;
            while (true) {
                try {
                    if (serverSocket != null && serverSocket.isBound()) {
                        serverSocket.close();
                    }
                    if (clientSocket != null && clientSocket.isBound()) {
                        clientSocket.close();
                    }
                    serverSocket = new ServerSocket(port);
                    clientSocket = serverSocket.accept();
                    out = new PrintWriter(clientSocket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                    String inputLine;
                    LocalDateTime dataObj;
                    while ((inputLine = in.readLine()) != null) {
                        if (".".equals(inputLine)) {
                            out.println("Done!");
                            break;
                        }
                        dataObj = LocalDateTime.now();
                        out.println(dataObj + " - " + "Received: " + inputLine);
                    }

                } catch (SocketException socketExceptione) {
                    socketExceptione.printStackTrace();
                    continue;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class TCPClient {
        private final String host;
        private final int port;
        private long CONN_RETRY_INT = 2000;
        private int CLIENT_SLEEP = 5000;
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;


        public void setCLIENT_SLEEP(int CLIENT_SLEEP) {
            this.CLIENT_SLEEP = CLIENT_SLEEP;
        }

        public void setConnRetryInt(int ms) {
            this.CONN_RETRY_INT = ms;
        }


        public void read() {
            while (true) {
                try {
                    clientSocket = new Socket(host, port);
                    clientSocket.setSoTimeout(0);
                    out = new PrintWriter(clientSocket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String msg = "Ping";
                    String NoMsgRcd = "Server Not Responding";
                    while (true) {
                        out.println(msg);
                        String resp = in.readLine();
                        Thread.sleep(CLIENT_SLEEP);
                        if (resp == null) {
                            if (log_ping)
                                System.out.println(LocalDateTime.now() + " - " + NoMsgRcd);
                            break;
                        }
                        if (log_ping)
                            System.out.println(resp);
                    }
                } catch (SocketException se) {
                    String err = se.getMessage();
                    if (log_failures)
                        System.out.println(LocalDateTime.now() + " - " + err);
                    try {
                        Thread.sleep(CONN_RETRY_INT);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;

                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public TCPClient(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }
}