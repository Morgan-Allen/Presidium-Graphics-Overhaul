/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.util ;


public class Coord {
  
  public int x, y ;
  
  
  public Coord() {}
  
  public Coord(Coord c) {
    this.x = c.x ;
    this.y = c.y ;
  }
  
  public Coord(int x, int y) {
    this.x = x ;
    this.y = y ;
  }
  
  public Coord(Coord c, int offX, int offY) {
    this.x = c.x + offX ;
    this.y = c.y + offY ;
  }
  
  public void setTo(Coord c) {
    this.x = c.x ;
    this.y = c.y ;
  }
  
  public boolean matches(Coord c) {
    return this.x == c.x && this.y == c.y ;
  }
  
  
  public String toString() {
    return "["+x+", "+y+"]" ;
  }
}



