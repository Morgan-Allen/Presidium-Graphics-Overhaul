/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.widgets;
import stratos.util.*;



//  TODO:  Implement header and footer views, plus scrolling...


public abstract class ListingPane extends UIGroup {
  
  
  UIGroup content;
  List <UINode> listing = new List();
  ListingPane before;
  
  
  public ListingPane(HUD UI) {
    super(UI);
    initBackground();
    content = new UIGroup(UI);
    content.alignToFill();
    content.attachTo(this);
    initForeground();
  }
  
  
  protected void updateState() {
    boolean report = I.used60Frames && false;
    if (report) {
      I.say("\nUpdating listing...");
      I.say("  Relative bound: "+relBound);
      I.say("  Absolute bound: "+absBound);
      I.say("  Final bound:    "+trueBounds());
    }
    
    if (listing.empty()) refreshListing();
    
    final float spacing = listSpacing();
    float absDown = 0, relDown = 0;
    for (UINode listed : listing) {
      listed.alignAcross(0, 1);
      listed.absBound.ypos(0 - (absDown + listed.absBound.ydim() + spacing));
      listed.relBound.ypos(1 - relDown);
      absDown += listed.absBound.ydim() + spacing;
      relDown += listed.relBound.ydim();
      if (report) I.say("  Rel/Abs down: "+relDown+"/"+absDown);
    }
    super.updateState();
  }
  
  
  protected abstract void initBackground();
  protected abstract float listSpacing();
  protected abstract void fillListing(List <UINode> listing);
  protected abstract void initForeground();
  
  
  protected void refreshListing() {
    for (UINode l : listing) l.detach();
    fillListing(listing = new List());
    for (UINode l : listing) l.attachTo(content);
  }
  
  
  protected List <UINode> listing() {
    return listing;
  }
  
  
  protected ListingPane before() {
    return before;
  }
  
  
  protected void navigateForward(ListingPane next, boolean matchFrame) {
    final UINode parent = parent();
    next.before = this;
    detach();
    if (matchFrame) next.alignToMatch(this);
    next.attachTo(parent);
  }
  
  
  protected void navigateBack() {
    if (before == null) return;
    final UINode parent = parent();
    detach();
    before.attachTo(parent);
    before = null;
  }
  
  
  protected ListingPane rootPane() {
    ListingPane last = this;
    while (last.before != null) last = last.before;
    return last;
  }
}









