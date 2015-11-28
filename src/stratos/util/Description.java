/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.util;
import stratos.graphics.common.*;



public interface Description {
  
  public void append(String s, Clickable link, Colour c);
  public void append(Clickable link, Colour c);
  public void append(Clickable link);
  public void append(String s, Clickable link);
  public void append(String s, Colour c);
  public void append(String s);
  public void append(Object o);
  
  public void appendAll(Object... o);
  
  public void appendList(String s, Series l);
  public void appendList(String s, Object... l);
  
  
  public static interface Clickable {
    String fullName();
    void whenClicked(Object context);
  }
  
  
  public abstract static class Link implements Clickable {
    
    final String name;
    public Link() { this(""); }
    public Link(String name) { this.name = name; }
    
    public String fullName() { return name; }
  }
}














