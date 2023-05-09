import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;

public class TCPSocketTest {
    public static void main(String args[]) {
        if ( args.length == 0 ){
            System.out.println("Usage is either \"java -jar SocketServerClient.jar\" with args: \n\tserver port | client host port");
            System.exit(0);
        }
        if (args[0].equalsIgnoreCase("server")) {
            int port = Integer.parseInt(args[1]);
            new TCPServer(port);
        }
        else if ( args[0].equalsIgnoreCase("client") ){
            String host = args[1];
            int port = Integer.parseInt(args[2]);
            new TCPClient(host, port);
        }
    }

    private static class TCPServer {
        public TCPServer(int port) {
            ServerSocket serverSocket;
            Socket clientSocket;
            PrintWriter out;
            BufferedReader in;
            try {
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
                    out.println(dataObj + " - " + "Received: " + inputLine );
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class TCPClient {
        private static final long CONN_RETRY_INT = 2000;
        private static final int CLIENT_SLEEP = 5000;

        public TCPClient(String host, int port) {
            Socket clientSocket;
            PrintWriter out;
            BufferedReader in;
            while (true) {
                try {
                    clientSocket = new Socket(host, port);
                    clientSocket.setSoTimeout(0);
                    out = new PrintWriter(clientSocket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    int sleepInterval = CLIENT_SLEEP;
                    String msg = "Ping";
                    String NoMsgRcd = "Server Not Responding";
                    while (true) {
                        out.println(msg);
                        String resp = in.readLine();
                        Thread.sleep(sleepInterval);
                        if (resp == null) {
                            System.out.println(LocalDateTime.now() + " - " + NoMsgRcd );
                            break;
                        }
                        System.out.println(resp);
                    }
                } catch (SocketException se) {
                    String err = se.getMessage();
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
    }
}
