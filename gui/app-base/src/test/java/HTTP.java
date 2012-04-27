/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

/**
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
public class HTTP
{
   public static String post(String urlString, InputStream inputStream)
         throws Exception
   {

      String userPassword = "admin:admin";
      String encoding = new sun.misc.BASE64Encoder().encode (userPassword.getBytes());

      HttpURLConnection conn = null;
      BufferedReader br = null;
      DataOutputStream dos = null;
      DataInputStream inStream = null;

      InputStream is = null;
      OutputStream os = null;
      boolean ret = false;
      String StrMessage = "";


      String lineEnd = "\r\n";
      String twoHyphens = "--";
      String boundary =  "*****";


      int bytesRead, bytesAvailable, bufferSize;

      byte[] buffer;

      int maxBufferSize = 1*1024*1024;

      String responseFromServer = "";

      try
      {
         //------------------ CLIENT REQUEST

         // open a URL connection to the Servlet

         URL url = new URL(urlString);


         // Open a HTTP connection to the URL

         conn = (HttpURLConnection) url.openConnection();
         conn.setRequestProperty ("Authorization", "Basic " + encoding);

         // Allow Inputs
         conn.setDoInput(true);

         // Allow Outputs
         conn.setDoOutput(true);

         // Don't use a cached copy.
         conn.setUseCaches(false);

         // Use a post method.
         conn.setRequestMethod("POST");

         conn.setRequestProperty("Connection", "Keep-Alive");

         conn.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);

         dos = new DataOutputStream( conn.getOutputStream() );

         dos.writeBytes(twoHyphens + boundary + lineEnd);
         dos.writeBytes("Content-Disposition: form-data; name=\"upload\";"
               + " filename=\"" + UUID.randomUUID().toString() +"\"" + lineEnd);
         dos.writeBytes(lineEnd);

         // create a buffer of maximum size
         bytesAvailable = inputStream.available();
         bufferSize = Math.min(bytesAvailable, maxBufferSize);
         buffer = new byte[bufferSize];

         // read file and write it into form...
         bytesRead = inputStream.read(buffer, 0, bufferSize);

         while (bytesRead > 0)
         {
            dos.write(buffer, 0, bufferSize);
            bytesAvailable = inputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            bytesRead = inputStream.read(buffer, 0, bufferSize);
         }

         // send multipart form data necesssary after file data...

         dos.writeBytes(lineEnd);
         dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

         // close streams

         inputStream.close();
         dos.flush();
         dos.close();

      }
      catch (MalformedURLException ex)
      {
         throw ex;
      }

      catch (IOException ioe)
      {
         throw ioe;
      }


      //------------------ read the SERVER RESPONSE

      StringBuffer sb = new StringBuffer();

      try
      {
         inStream = new DataInputStream ( conn.getInputStream() );
         String str;
         while (( str = inStream.readLine()) != null)
         {
            sb.append(str).append("");
         }
         inStream.close();

      }
      catch (IOException ioex)
      {
         System.out.println("From (ServerResponse): "+ioex);

      }


      return sb.toString();

   }
}
