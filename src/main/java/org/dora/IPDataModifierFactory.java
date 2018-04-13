package org.dora;

public class IPDataModifierFactory
{
  public static IPDataModifier load(String file)
  {
    final IPDataModifierImpl ipDataModifier = new IPDataModifierImpl();
    ipDataModifier.load(file);
    return ipDataModifier;
  }
}
