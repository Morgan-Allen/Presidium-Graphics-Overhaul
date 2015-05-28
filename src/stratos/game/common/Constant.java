

package stratos.game.common;
import stratos.graphics.widgets.*;
import stratos.start.*;
import stratos.user.*;
import stratos.util.*;


public abstract class Constant extends Index.Entry implements Text.Clickable {
  
  
  private static boolean verbose = false;
  
  final static Table <String, Object> allConstants = new Table();
  
  final String name;
  
  
  protected Constant(Index index, String key, String name) {
    super(index, key);
    this.name = name;
    final String asString = toString();
    if (validAsString(asString)) allConstants.put(asString, this);
  }
  
  
  protected abstract void describeHelp(Description d, Selectable prior);
  
  

  /**  Hyperlink support methods-
    */
  final static String OPEN_CAP  = "{", SHUT_CAP  = "}";
  final static char   OPEN_CHAR = '{', SHUT_CHAR = '}';
  
  
  private boolean validAsString(String asString) {
    //
    //  This is needed for reference-substitution within help-systems, so we
    //  just ensure that subclasses don't override toString() with something
    //  illegible-
    if (asString.startsWith(OPEN_CAP) && asString.endsWith(SHUT_CAP)) {
      return true;
    }
    else I.complain(
      "\nA HELP-ITEM'S toString() VALUE MUST BE ENCLOSED BY "+
      "'"+OPEN_CAP+"' AND '"+SHUT_CAP+"', VALUE WAS: "+asString
    );
    return false;
  }
  
  
  public String toString() {
    return OPEN_CAP+name+SHUT_CAP;
  }
  
  
  public String fullName() {
    return name;
  }
  
  
  public void whenClicked() {
    //
    //  By default, we simply create a fresh selection pane and allow
    //  subclasses to fill in a more detailed description.
    final BaseUI UI = BaseUI.current();
    if (UI == null) return;
    final SelectionPane lastPane = UI.currentSelectionPane();
    final Selectable prior = lastPane == null ? null : lastPane.selected;
    //
    //  (We also include a reference to the subject of the last opened pane (if
    //  any), which can be contextually useful.)
    final SelectionPane help = new SelectionPane(UI, lastPane, null) {
      protected void updateText(
        BaseUI UI, Text headerText, Text detailText, Text listingText
      ) {
        super.updateText(UI, headerText, detailText, listingText);
        describeHelp(detailText, prior);
      }
    };
    UI.setInfoPane(help);
  }
  
  
  protected void substituteReferences(String helpString, Description d) {
    final boolean report = verbose && PlayLoop.isFrameIncrement(60);
    StringBuffer scanned = null;
    boolean inItem = false;
    final char chars[] = helpString.toCharArray();
    //
    //  In essence, we search for any terms 'capped' between opening/closing
    //  brackets, and try to substitute links to their matching Constant.
    for (int i = 0; i < chars.length;) {
      if (scanned == null) scanned = new StringBuffer();
      final char c = chars[i];
      final boolean ends = ++i == chars.length;
      
      if ((c == OPEN_CHAR && ! inItem) || ends) {
        d.append(scanned);
        scanned = null;
        inItem = true;
      }
      else if (c == SHUT_CHAR && inItem) {
        final String key = OPEN_CHAR+scanned.toString()+SHUT_CHAR;
        if (report) I.say("\n  Found key: "+key);
        final Object item = allConstants.get(key);
        d.append(item);
        scanned = null;
        inItem = false;
      }
      else {
        scanned.append(c);
      }
    }
  }
}










