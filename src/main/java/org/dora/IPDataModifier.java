package org.dora;

import java.io.IOException;
import java.io.OutputStream;

public interface IPDataModifier
{
  boolean add(String ip, String[] location);

  void persist(OutputStream outputStream) throws IOException;
}
