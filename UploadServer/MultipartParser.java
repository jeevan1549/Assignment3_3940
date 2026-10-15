import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

public class MultipartParser {
   public final Map<String, String> fields = new HashMap<>();
   public String fileName;
   public byte[] fileBytes;

   private static final byte[] HEADER_END = "\r\n\r\n".getBytes();
   private static final Pattern NAME = Pattern.compile("(?:^|[;\\s])name=\"([^\"]*)\"");
   private static final Pattern FILE = Pattern.compile("filename=\"([^\"]*)\"");

   public static MultipartParser parse(byte[] body, String boundary) throws UploadException {
      MultipartParser result = new MultipartParser();
      byte[] delim = ("--" + boundary).getBytes(StandardCharsets.ISO_8859_1);
      byte[] nextDelim = ("\r\n--" + boundary).getBytes(StandardCharsets.ISO_8859_1);

      int pos = indexOf(body, delim, 0);
      if (pos < 0) throw new UploadException("Boundary not found in body");

      while (true) {
         pos += delim.length;
         if (pos + 1 < body.length && body[pos] == '-' && body[pos + 1] == '-') break;
         pos += 2;

         int headerEnd = indexOf(body, HEADER_END, pos);
         int next = headerEnd < 0 ? -1 : indexOf(body, nextDelim, headerEnd + 4);
         if (next < 0) throw new UploadException("Malformed multipart body");

         String headers = new String(body, pos, headerEnd - pos, StandardCharsets.UTF_8);
         int dataStart = headerEnd + 4;

         Matcher n = NAME.matcher(headers), f = FILE.matcher(headers);
         if (n.find()) {
            if (f.find()) {
               if (!f.group(1).isEmpty()) {
                  result.fileName = f.group(1);
                  result.fileBytes = Arrays.copyOfRange(body, dataStart, next);
               }
            } else {
               result.fields.put(n.group(1),
                     new String(body, dataStart, next - dataStart, StandardCharsets.UTF_8));
            }
         }
         pos = next + 2;
      }
      return result;
   }

   private static int indexOf(byte[] data, byte[] pat, int from) {
      outer:
      for (int i = from; i <= data.length - pat.length; i++) {
         for (int j = 0; j < pat.length; j++) if (data[i + j] != pat[j]) continue outer;
         return i;
      }
      return -1;
   }
}