import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.Scanner;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class SendEmailClient {
    private static String host = "smtp.kth.se";
    private static int port = 587;
    private static char[] buffer = new char[10000];
    private static String command;
    private static int charsRead = -1;
    private static SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    private static Scanner scanner = new Scanner(System.in);
    private static final String USERNAME_BASE64 = "VXNlcm5hbWU6";
    private static final String PASSWORD_BASE64 = "UGFzc3dvcmQ6";

    public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {
        String line="";
        Socket socket = new Socket(host, port);
        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Setup: starting TLS connection

        System.out.println("\n\n-- Starting SMTP -- \n\n\tTo exit, type q\n\n");
        System.out.println(readFromBuffer(reader));
        toServer(writer, "ehlo smtp.kth.se");
        System.out.println(readFromBuffer(reader));

        toServer(writer, "starttls");
        if ((line = readFromBuffer(reader)).contains("Ready to start TLS")) {
            socket = (SSLSocket) sslSocketFactory.createSocket(socket, host, port, true);
            writer = new PrintWriter(socket.getOutputStream());
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
        System.out.println(line);
        toServer(writer, "ehlo smtp.kth.se");
        System.out.println(readFromBuffer(reader));

        // TSL started
        writeFromCommandLine(writer, false);
        do {
            line = readFromBuffer(reader);
            System.out.println(line);
            if(line.contains(USERNAME_BASE64) || line.contains(PASSWORD_BASE64)){
                writeFromCommandLine(writer, true);
            } else if (line.contains("354 End data with")){
                scanner.useDelimiter("\r\n.\r\n");
                String emailBody ="";
                emailBody+= scanner.next();
                scanner.reset();
                //Consume the . character
                scanner.nextLine();
                scanner.nextLine();  
                System.out.println("Sending to server: " + emailBody);
                emailBody += "\r\n.\r\n";
                writer.print(emailBody);
                writer.flush();
            } else {
                writeFromCommandLine(writer, false);
            }
            

        } while (!command.equals("q"));
    }

    private static String readFromBuffer(BufferedReader reader) throws IOException {
        String line="";
        while ((charsRead = reader.read(buffer)) != -1) {
            if (charsRead < 1000) {
                line += String.valueOf(buffer).substring(0, charsRead);
                break;
            } else {
                line += String.valueOf(buffer);
            }
        }
        return line;
    }

    private static void toServer(PrintWriter writer, String msg) {
        writer.print(msg + "\r\n");
        writer.flush();
        System.out.println(msg + "\n");
    }

    private static void writeFromCommandLine(PrintWriter writer, boolean encode) {
        command = scanner.nextLine();
        if (isQ(command)) {
            quit();
        }
        if(encode){
            toServer(writer, encodeToBase64(command));
        } else {
            toServer(writer, command);
        }
        
    }

    private static boolean isQ(String s) {
        return s.equals("q");
    }

    private static void quit() {
        System.out.println("Exiting program");
        System.exit(0);
    }

    private static String encodeToBase64(String string){
        return Base64.getEncoder().encodeToString(string.getBytes());
    }
}
