/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.mainscreen;
import stratos.start.*;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.verse.*;
import stratos.user.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;
import stratos.content.hooks.*;



public class SelectCrewPane extends MenuPane {
  
  
  final Expedition expedition;
  
  private UINode
    advisHeader,
    advisFooter,
    colonHeader,
    colonFooter;
  private Text
    colonLabels[];
  
  
  public SelectCrewPane(HUD UI, Expedition expedition) {
    super(UI, MainScreen.MENU_NEW_GAME_CREW);
    this.expedition = expedition;
  }
  
  
  protected void fillListing(List< UINode> listing) {
    
    listing.add(advisHeader = createTextItem("Advisors", 1.2f, null, 1));
    for (final Background b : Expedition.ADVISOR_BACKGROUNDS) {
      listing.add(new TextButton(UI, "  "+b.name, 0.75f) {
        protected void whenClicked() { toggleAdvisor(b); }
        protected boolean toggled() { return hasAdvisor(b); }
        protected boolean enabled() { return canToggleAdvisor(b); }
      });
    }
    listing.add(advisFooter = createTextItem(
      "Advisors are particularly skilled or talented individuals who can be "+
      "sent on crucial missions or perform administrative duties.",
      0.75f, Colour.LITE_GREY, 3
    ));
    
    listing.add(colonHeader = createTextItem("Colonists", 1.2f, null, 1));
    colonLabels = new Text[Expedition.COLONIST_BACKGROUNDS.length];
    int labelIndex = 0;
    
    for (final Background b : Expedition.COLONIST_BACKGROUNDS) {
      
      final UIGroup counter = new UIGroup(UI);
      final Text label = new Text(UI, UIConstants.INFO_FONT);
      colonLabels[labelIndex++] = label;
      label.scale = 0.75f;
      label.setText(b.name);
      label.setToLineSize();
      label.alignAcross(0, 0.5f);
      label.attachTo(counter);
      
      TextButton plus = new TextButton(UI, " + ", 1) {
        protected void whenClicked() { incColonists(b, 1); }
        protected boolean enabled() { return canIncColonists(b, 1); }
      };
      plus.alignAcross(0.5f, 0.65f);
      plus.attachTo(counter);
      
      TextButton minus = new TextButton(UI, " - ", 1) {
        protected void whenClicked() { incColonists(b, -1); }
        protected boolean enabled() { return canIncColonists(b, -1); }
      };
      minus.alignAcross(0.65f, 0.8f);
      minus.attachTo(counter);
      
      counter.alignTop(0, 15);
      listing.add(counter);
    }
    listing.add(colonFooter = createTextItem(
      "Colonists provide the backbone of your workforce, giving you a "+
      "headstart in establishing defences or trade.",
      0.75f, Colour.LITE_GREY, 3
    ));
    
    listing.add(new TextButton(UI, "  Begin Game", 1) {
      protected void whenClicked() { pushNextPane(); }
      protected boolean enabled() { return canProgress(); }
    });
  }
  
  
  protected void updateState() {
    final MainScreen screen = MainScreen.current();
    screen.display.showLabels   = true ;
    screen.display.showWeather  = false;
    screen.worldsDisplay.hidden = true ;
    screen.crewDisplay.hidden   = false;
    
    int numA = numAdvisors(), maxA = maxAdvisors();
    updateTextItem(advisHeader, "Advisors ("+numA+"/"+maxA+")", null);
    
    int numC = numColonists(), maxC = maxColonists();
    updateTextItem(colonHeader, "Colonists ("+numC+"/"+maxC+")", null);
    
    if (colonLabels != null) for (int i = colonLabels.length; i-- > 0;) {
      final Background b = Expedition.COLONIST_BACKGROUNDS[i];
      final Text t = colonLabels[i];
      final int numM = expedition.numMigrants(b);
      final Colour tint = numM > 0 ? Text.LINK_COLOUR : Colour.LITE_GREY;
      t.setText("");
      t.append("  "+b.name+" ("+numM+")", tint);
    }
    
    screen.crewDisplay.setupFrom(expedition, true);
    
    super.updateState();
  }
  
  
  private void updateHelpFor(UINode footer, String helpText) {
    updateTextItem(footer, helpText, Colour.LITE_GREY);
  }
  
  
  
  /**  Handling advisor selection-
    */
  private void toggleAdvisor(Background b) {
    final Actor a = expedition.firstMigrant(b);
    if (a != null) expedition.removeMigrant(a);
    else expedition.addAdvisor(b);
    updateHelpFor(advisFooter, b.info);
  }
  
  
  private boolean hasAdvisor(Background b) {
    return expedition.firstMigrant(b) != null;
  }
  
  
  private boolean canToggleAdvisor(Background b) {
    if (hasAdvisor(b)) return true;
    return numAdvisors() < maxAdvisors();
  }
  
  
  private int numAdvisors() {
    return expedition.advisors().size();
  }
  
  
  private int maxAdvisors() {
    return expedition.maxAdvisors();
  }
  
  
  
  /**  Handling colonist selection-
    */
  private void incColonists(Background b, int inc) {
    if (inc > 0) expedition.addColonist(b);
    else expedition.removeMigrant(expedition.firstMigrant(b));
    updateHelpFor(colonFooter, b.info);
  }
  
  
  private boolean canIncColonists(Background b, int inc) {
    if (inc > 0) return numColonists() < maxColonists();
    else return expedition.numMigrants(b) > 0;
  }
  
  
  private int numColonists() {
    return expedition.colonists().size();
  }
  
  
  private int maxColonists() {
    return expedition.maxColonists();
  }
  
  
  
  /**  Other navigation tasks.
    */
  private boolean canProgress() {
    if (numAdvisors () != maxAdvisors ()) return false;
    if (numColonists() != maxColonists()) return false;
    return true;
  }
  
  
  private void pushNextPane() {
    String prefix = SaveUtils.uniqueVariant(expedition.leader().fullName());
    final Verse verse = MainScreen.currentVerse();
    expedition.setTitleGranted(Expedition.TITLE_KNIGHTED);
    
    SectorScenario hook = verse.scenarioFor(expedition.destination());
    if (hook == null) hook = new SectorScenario(expedition, verse, prefix);
    hook.beginScenario(expedition, prefix);
  }
  
  
  protected void navigateBack() {
    super.navigateBack();
  }
  
}





