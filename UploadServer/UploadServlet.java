import java.io.*;
import java.util.Arrays;

public class UploadServlet extends HttpServlet {
   protected void doGet(HttpServletRequest request, HttpServletResponse response) {
      try {
         System.out.println("in doGet of original UploadServlet");
         OutputStream out = response.getOutputStream();

         // Serve the HTML file
         File htmlFile = new File("Form.html");
         FileInputStream fileInputStream = new FileInputStream(htmlFile);
         String content = "HTTP/1.1 200 OK\r\n" +
               "Content-Type: text/html\r\n" +
               "Content-Length: " + htmlFile.length() + "\r\n" +
               "\r\n";
         out.write(content.getBytes());
         byte[] buffer = new byte[4096];
         int bytesRead;
         while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
         }
         fileInputStream.close();
         out.close();

      } catch (Exception ex) {
         System.err.println(ex);
      }
   }

   protected void doPost(HttpServletRequest request, HttpServletResponse response) {
      try {
         InputStream in = request.getInputStream();
         int contentLength = 0;
         String boundary = "";
         String line;

         while ((line = readRequestLine(in)) != null && !line.isEmpty()) {
            String lower = line.toLowerCase();
            if (lower.startsWith("content-length:")) {
               contentLength = Integer.parseInt(line.substring(15).trim());
            } else if (lower.startsWith("content-type:") && lower.contains("boundary=")) {
               boundary = line.substring(lower.indexOf("boundary=") + 9).trim().replace("\"", "");
            }
         }
         if (boundary.isEmpty() || contentLength <= 0)
            throw new UploadException("Not a multipart/form-data request");

         byte[] body = in.readNBytes(contentLength);
         MultipartParser mp = MultipartParser.parse(body, boundary);
         if (mp.fileBytes == null) throw new UploadException("No file was uploaded");

         String caption = clean(mp.fields.getOrDefault("caption", ""));
         String date    = clean(mp.fields.getOrDefault("date", ""));
         String name    = clean(mp.fileName);

         File dir = new File(ServerConfig.INSTANCE.uploadDir);
         dir.mkdirs();
         try (OutputStream fos = new FileOutputStream(new File(dir, caption + "_" + date + "_" + name))) {
            fos.write(mp.fileBytes);
         }
         System.out.println("Saved " + caption + "_" + date + "_" + name);

         // Functional style: stream -> sorted -> map -> join
         String items = Arrays.stream(dir.list())
               .sorted(String.CASE_INSENSITIVE_ORDER)
               .map(f -> "<li>" + f + "</li>")
               .collect(java.util.stream.Collectors.joining());

         send(response, "200 OK",
               "<!DOCTYPE html><html><body><h2>Uploaded files</h2><ul>" + items + "</ul></body></html>");
      } catch (Exception ex) {
         System.err.println(ex);
         try { send(response, "400 Bad Request", "<html><body>Upload failed: " + ex.getMessage() + "</body></html>"); }
         catch (IOException ignored) { }
      }
   }

      private static String clean(String s) {
      return s.replaceAll("[^A-Za-z0-9._-]", "_");
   }


   private void send(HttpServletResponse res, String status, String html) throws IOException {
      byte[] b = html.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      OutputStream out = res.getOutputStream();
      out.write(("HTTP/1.1 " + status + "\r\n"
            + "Content-Type: text/html; charset=UTF-8\r\n"
            + "Content-Length: " + b.length + "\r\n"
            + "Connection: close\r\n\r\n").getBytes());
      out.write(b);
      out.flush();
   }

   private String readRequestLine(InputStream is) throws IOException {
      StringBuilder sb = new StringBuilder();
      int c;
      while ((c = is.read()) != -1) {
         if (c == '\n') {
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\r') sb.setLength(sb.length() - 1);
            return sb.toString();
         }
         sb.append((char) c);
      }
      return sb.length() > 0 ? sb.toString() : null;
   }
}