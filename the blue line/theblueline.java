import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.URLDecoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import javax.mail.*;
import javax.mail.internet.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class TheBlueLine {

    private static final String GMAIL = "namansharma041103@gmail.com";
    private static final String PASSWORD = "Drishti2008";
    private static Map<String, String> otpMap = new HashMap<>();
    private static int loginCount = 0;

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/login", new LoginHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port 8080");
    }

    static class RootHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            File file = new File("theblueline.html");
            FileInputStream fis = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            fis.close();
            t.sendResponseHeaders(200, bytes.length);
            OutputStream os = t.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class LoginHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equals(t.getRequestMethod())) {
                InputStream is = t.getRequestBody();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) > -1) {
                    baos.write(buffer, 0, len);
                }
                baos.flush();
                String body = new String(baos.toByteArray());
                is.close();
                baos.close();
                String email = parseEmail(body);
                if (!email.isEmpty()) {
                    String otp = generateOTP();
                    otpMap.put(email, otp);
                    sendOTP(email, otp);
                    loginCount++;
                    saveLoginCount();
                    String response = "OTP sent to " + email + ". Check your email.";
                    t.sendResponseHeaders(200, response.length());
                    OutputStream os = t.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } else {
                    String response = "Invalid email.";
                    t.sendResponseHeaders(400, response.length());
                    OutputStream os = t.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
            } else {
                t.sendResponseHeaders(405, -1);
            }
        }
    }

    private static String parseEmail(String body) {
        String[] parts = body.split("&");
        for (String part : parts) {
            if (part.startsWith("username=")) {
                try {
                    return URLDecoder.decode(part.substring(9), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    return "";
                }
            }
        }
        return "";
    }

    private static String generateOTP() {
        Random random = new Random();
        return String.valueOf(100000 + random.nextInt(900000));
    }

    private static void sendOTP(String to, String otp) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(GMAIL, PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(GMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject("Your OTP for The Blue Line");
            message.setText("Your OTP is: " + otp + ". Use it to login.");
            Transport.send(message);
            System.out.println("OTP sent to " + to);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    private static void saveLoginCount() {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter("login_count.txt", true));
            writer.println("Login attempt at " + new Date() + ": " + loginCount);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
