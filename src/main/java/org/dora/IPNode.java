package org.dora;

class IPNode
{
  public IPNode(int ip, String[] location)
  {
    this.ip = ip;
    this.location = location;
  }

  private int ip = 0;
  private String[] location;

  public int getIp()
  {
    return ip;
  }

  public String[] getLocation()
  {
    return location;
  }

  public void setLocation(String[] location)
  {
    this.location = location;
  }
}
