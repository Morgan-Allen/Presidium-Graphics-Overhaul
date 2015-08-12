/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import java.lang.reflect.*;

import stratos.graphics.common.Colour;
import stratos.graphics.widgets.Text;
import stratos.graphics.widgets.Text.*;
import stratos.util.*;



//  TODO:  You can almost certainly get rid of this, once you have proper
//  widget-usage worked out.


public class Call implements Clickable {
  
  
  final String label;
  final Object client, args;
  private Method method;
  
  
  private Call(String label, Object client, String methodName, Object args) {
    this.label = label;
    this.client = client;
    this.args = args;
    try {
      this.method = client.getClass().getMethod(methodName, Object[].class);
      method.setAccessible(true);
    }
    catch (Exception e) { I.report(e); }
  }
  
  
  public String fullName() {
    return label;
  }
  
  
  public void whenClicked() {
    try { method.invoke(client, args); }
    catch (Exception e) {
      I.say("\nPROBLEM INVOKING METHOD: "+method.getName());
      e.printStackTrace();
    }
  }
  
  
  public static void add(
    String label, Object client, String methodName, Description d,
    Object... args
  ) {
    d.append(new Call(label, client, methodName, args));
  }
  
  
  public static void add(
    String label, Colour c,
    Object client, String methodName, Description d,
    Object... args
  ) {
    if (c == null) c = Text.LINK_COLOUR;
    d.append(new Call(label, client, methodName, args), c);
  }
}







