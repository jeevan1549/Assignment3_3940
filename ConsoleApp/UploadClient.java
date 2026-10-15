import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;

public class UploadClient {
    private static final String HOST = "localhost";
    private static final int PORT = 8082;

    public UploadClient() { }

    public String uploadFile() {
        return uploadFile("Android Logo", LocalDate.now().toString(), "AndroidLogo.png");
    }

    public String uploadFile(String caption, String date, String filePath) {
        String responseBody = "";
        try {
            Path path = Path.of(filePath);
            byte[] fileBytes = Files.readAllBytes(path);
            String mime = Files.probeContentType(path);
            if (mime == null) mime = "application/octet-stream";

            String boundary = "----JavaBoundary" + System.currentTimeMillis();
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            writeTextPart(body, boundary, "caption", caption);
            writeTextPart(body, boundary, "date", date);
            body.write(("--" + boundary + "\r\n"
                  + "Content-Disposition: form-data; name=\"fileName\"; filename=\""
                  + path.getFileName() + "\"\r\n"
                  + "Content-Type: " + mime + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(fileBytes);
            body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            String head = "POST / HTTP/1.1\r\n"
                  + "Host: " + HOST + ":" + PORT + "\r\n"
                  + "Content-Type: multipart/form-data; boundary=" + boundary + "\r\n"
                  + "Content-Length: " + body.size() + "\r\n"
                  + "Connection: close\r\n\r\n";

            Socket socket = new Socket(HOST, PORT);
            OutputStream out = socket.getOutputStream();
            out.write(head.getBytes(StandardCharsets.ISO_8859_1));
            out.write(body.toByteArray());
            out.flush();
            socket.shutdownOutput();

            BufferedReader in = new BufferedReader(
                  new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
               // skip the HTTP status line and response headers
            }
            StringBuilder listing = new StringBuilder();
            while ((line = in.readLine()) != null) {
               listing.append(line).append("\n");
            }
            responseBody = listing.toString();
            socket.close();
        } catch (Exception e) {
            System.err.println(e);
        }
        return responseBody;
    }

    private static void writeTextPart(OutputStream out, String boundary, String name, String value)
          throws IOException {
        out.write(("--" + boundary + "\r\n"
              + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
              + value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }
}