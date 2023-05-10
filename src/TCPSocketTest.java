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

    public static void main(String args[]) {
        if ( args.length == 0 ){
            System.out.println("Usage is either \"java -jar SocketServerClient.jar\" \n\t" +
                    "\n\t-t [server | client]" +
                    "\n\t-h host " +
                    "\n\t-p port " +
                    "\n\t-n #use to log successful pings" +
                    "\n\t-f #use to log connection failures" +
                    "\n\t-r (ms) connection retry interval" +
                    "\n\t-s (ms) application ping sleep time"
            );
            System.exit(0);
        }
        
        for( int i=0; i< args.length; i++ ){
            String args_i = args[i];
            if ( args_i.startsWith("-") ){
                char command = args_i.charAt(1);
                
                switch (command)
                {
                    case 't':
                        String command_arg_service = args[i+1];
                        service = command_arg_service;
                        break;
                    case 'p':
                        String command_arg_port = args[i+1];
                        port = Integer.parseInt(command_arg_port);
                        break;
                    case 'h':
                        String command_arg_host = args[i+1];
                        host = command_arg_host;
                        break;
                    case 'n': // Log ping
                        log_ping = true;
                        break;
                    case 'f': // Log connection failures
                        log_failures = true;
                        break;
                    case 'r': // retry interval for connection failures
                        String command_arg_retryTime = args[i+1];
                        retryConnection = Integer.parseInt(command_arg_retryTime);;
                        break;
                    case 's': // retry interval for connection failures
                        String command_arg_sleepInterval = args[i+1];
                        sleepInterval = Integer.parseInt(command_arg_sleepInterval);;
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + command);
                }
            }
        }
        if (service.equalsIgnoreCase("server")) {
            new TCPServer(port);
        }
        else if ( service.equalsIgnoreCase("client") ){
            TCPClient client = new TCPClient(host, port);
            client.setCLIENT_SLEEP(sleepInterval);
            client.setConnRetryInt(retryConnection);
            client.read();
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

        public void setConnRetryInt(int ms){
            this.CONN_RETRY_INT = ms;
        }


        public void read(){
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
                            if ( log_ping )
                                System.out.println(LocalDateTime.now() + " - " + NoMsgRcd );
                            break;
                        }
                        if ( log_ping )
                            System.out.println(resp);
                    }
                } catch (SocketException se) {
                    String err = se.getMessage();
                    if ( log_failures )
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
