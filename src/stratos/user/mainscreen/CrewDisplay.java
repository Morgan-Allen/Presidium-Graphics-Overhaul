/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.mainscreen;
import stratos.game.common.*;
import stratos.game.verse.*;
import stratos.user.*;
import stratos.graphics.widgets.*;
import stratos.util.*;




public class CrewDisplay extends UIGroup {
  
  final static int
    HEADER_HEIGHT = MainScreen.MARGIN,
    BOX_HEIGHT    = MainScreen.CAROUSEL_HIGH;
  
  final Text header;
  final UIGroup leaderBox, crewBox;
  final List <Image> crewPics = new List();
  
  
  
  public CrewDisplay(HUD UI) {
    super(UI);
    
    header = new Text(UI, UIConstants.INFO_FONT);
    header.alignAcross(0, 1);
    header.alignTop(0, HEADER_HEIGHT);
    header.attachTo(this);
    
    leaderBox = new UIGroup(UI);
    leaderBox.alignLeft(0, BOX_HEIGHT);
    leaderBox.alignTop(HEADER_HEIGHT, BOX_HEIGHT);
    leaderBox.attachTo(this);
    
    crewBox = new UIGroup(UI);
    crewBox.alignHorizontal(BOX_HEIGHT, 0);
    crewBox.alignTop(HEADER_HEIGHT, BOX_HEIGHT);
    crewBox.attachTo(this);
  }
  
  
  private Image portraitFor(final Actor a) {
    final Composite p = a.portrait(UI);
    if (p == null) return new Image(UI, Image.FULL_TRANSPARENCY);
    return new Image(UI, p.texture()) {
      protected String info() { return a.fullName(); }
    };
  }
  
  
  protected void setupFrom(Expedition e, boolean showCrew) {
    
    final int HALF_B = BOX_HEIGHT / 2;
    int count;
    
    for (Image pic : crewPics) pic.detach();
    
    if (e.leader() != null) {
      final Image pic = portraitFor(e.leader());
      pic.alignToFill();
      pic.attachTo(leaderBox);
      crewPics.add(pic);
      header.setText("Expedition lead by: "+e.leader().fullName());
    }
    
    if (! showCrew) return;
    
    count = 0;
    for (Actor a : e.advisors()) {
      final Image pic = portraitFor(a);
      pic.alignTop (0               , HALF_B);
      pic.alignLeft(HALF_B * count++, HALF_B);
      pic.attachTo(crewBox);
      crewPics.add(pic);
    }
    
    count = 0;
    for (Actor a : e.colonists()) {
      final Image pic = portraitFor(a);
      pic.alignTop (HALF_B          , HALF_B);
      pic.alignLeft(HALF_B * count++, HALF_B);
      pic.attachTo(crewBox);
      crewPics.add(pic);
    }
  }
  
  
}












