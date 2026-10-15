public class UploadAsyncTask extends AsyncTask {

   private UploadClient uploadClient = new UploadClient();

   @Override
   protected String doInBackground() {
      return uploadClient.uploadFile();
   }

   @Override
   protected void onPostExecute(String result) {
      System.out.println(result);
   }
}