/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package code.util ;

//  TODO:  Get rid of references to widgets package- move the Clickable
//  interface here, and rename to Link?
import code.graphics.common.*;
import code.graphics.widgets.Image;
import code.graphics.widgets.Text;
import code.graphics.widgets.Text.Clickable;
import code.util.*;


public interface Description {
  
  
  public void append(String s, Clickable link, Colour c) ;
  public void append(Clickable link, Colour c) ;
  public void append(Clickable link) ;
  public void append(String s, Clickable link) ;
  public void append(String s, Colour c) ;
  public void append(String s) ;
  public void append(Object o) ;
  
  public void appendList(String s, Series l) ;
  public void appendList(String s, Object... l) ;
  
  //public boolean insert(ImageAsset graphic, int maxSize) ;
  //public boolean insert(Texture graphic, int maxSize) ;
  
  
  public abstract static class Link implements Clickable {
    
    final String name ;
    
    public Link(String name) { this.name = name ; }
    
    public String fullName() { return name ; }
  }
}














