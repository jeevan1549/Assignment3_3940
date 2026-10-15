import java.net.*;
import java.io.*;

public class UploadServerThread extends Thread {
   private Socket socket = null;

   public UploadServerThread(Socket socket) {
      super("DirServerThread");
      this.socket = socket;
   }

   public void run() {
      System.out.println("Thread running");
      try {
         // Use BufferedInputStream to read the request
         BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
         in.mark(8192);
         String requestLine = readLine(in); // our own byte-level read
         in.reset();

         if (requestLine != null) {
            String[] requestParts = requestLine.split(" ");
            String method = requestParts[0];
            String uri = requestParts[1];

            HttpServletRequest req = new HttpServletRequest(in);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            HttpServletResponse res = new HttpServletResponse(baos);

            HttpServlet httpServlet = ServletFactory.create("UploadServlet"); // Factory + reflection

            // Check the HTTP Request Type
            if ("GET".equalsIgnoreCase(method) && "/".equals(uri)) {
               System.out.println("Calling UploadServlet's doGet");
               httpServlet.doGet(req, res);
            } else if ("POST".equalsIgnoreCase(method)) {
               System.out.println("Calling UploadServlet's doPost");
               httpServlet.doPost(req, res);
            } else {
               // Handle unsupported methods
               res.getOutputStream().write("HTTP/1.1 405 Method Not Allowed\r\n\r\n".getBytes());
            }

            // Send response back to client
            OutputStream out = socket.getOutputStream();
            out.write(baos.toByteArray());
            out.flush();
         }

         socket.close();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   private static String readLine(InputStream is) throws IOException {
      StringBuilder sb = new StringBuilder();
      int c;
      while ((c = is.read()) != -1 && c != '\n') {
         if (c != '\r') sb.append((char) c);
      }
      return sb.length() > 0 ? sb.toString() : null;
   }
}