package org.dora;

public class Tuple<L, R>
{
  public Tuple(L left, R right)
  {
    this.left = left;
    this.right = right;
  }

  public L getLeft()
  {
    return left;
  }

  private L left;

  public R getRight()
  {
    return right;
  }

  private R right;
}
