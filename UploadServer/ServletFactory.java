public class ServletFactory {
   public static HttpServlet create(String className) throws UploadException {
      try {
         return (HttpServlet) Class.forName(className).getDeclaredConstructor().newInstance();
      } catch (ReflectiveOperationException e) {
         throw new UploadException("Cannot create servlet " + className, e);
      }
   }
}