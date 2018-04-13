package org.dora;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class StreamUtils
{
  public static byte[] toBytes(InputStream inputStream)
  {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      byte[] block = new byte[4096];
      int size;
      while ((size = inputStream.read(block)) > 0) {
        outputStream.write(block, 0, size);
      }
      return outputStream.toByteArray();
    }
    catch (IOException ex) {
      ex.printStackTrace();
      return null;
    }
  }
}
