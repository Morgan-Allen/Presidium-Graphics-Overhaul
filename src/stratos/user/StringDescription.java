/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.graphics.common.*;
import stratos.graphics.widgets.Image;
import stratos.graphics.widgets.Text.Clickable;
import stratos.util.*;



public class StringDescription implements Description {
  
  
  final StringBuffer buffer = new StringBuffer();
  

  public void append(String s, Clickable link, Colour c) {
    if (s != null) buffer.append(s);
    else if (link != null) buffer.append(link.fullName());
    else buffer.append("(none)");
  }
  
  public String toString() {
    return buffer.toString();
  }
  
  public void append(Object o) {
    if (o == null) append("(none)");
    else append(o.toString());
  }
  
  public void appendAll(Object... o) {
    for (Object i : o) append(i);
  }
  
  
  public void append(Clickable l, Colour c) { append(null, l, c); }
  public void append(Clickable l) { append(null, l, null); }
  public void append(String s, Clickable l) { append(s, l, null); }
  public void append(String s, Colour c) { append(s, null, c); }
  public void append(String s) { append(s, null, null); }
  
  public boolean insert(ImageAsset graphic, int maxSize) { return false; }
  public boolean insert(Image graphic, int maxSize) { return false; }
  
  
  public void appendList(String s, Object... l) {
    if (l.length == 0) return;
    append(s);
    append(s);
    for (Object o : l) {
      if (o == l[0]) append(" ");
      else if (o == Visit.last(l)) append(" and ");
      else append(", ");
      append(o);
    }
  }
  
  
  public void appendList(String s, Series l) {
    appendList(s, l.toArray());
  }
}
