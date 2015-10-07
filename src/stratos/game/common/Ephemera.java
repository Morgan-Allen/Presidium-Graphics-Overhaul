/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.common;
import stratos.graphics.common.*;
import stratos.graphics.sfx.*;
import stratos.util.*;



//  TODO:  SEE IF YOU CAN MAKE THIS INDEPENDENT OF THE WORLD.  Could be useful
//  for other fade-FX, rather than insisting the rendering elements handle it.



/**  This class is used to add transitory or conditional special effects to the
  *  world.
  */
public class Ephemera {
  
  
  /**  Fields, constants, and save/load methods.
    */
  final Stage world;
  
  private Colour fadeColour = null;
  final Table <StageRegion, List <Ghost>> ghosts = new Table(100);
  
  
  protected Ephemera(Stage world) {
    this.world = world;
  }
  
  
  protected void loadState(Session s) throws Exception {
  }
  
  
  protected void saveState(Session s) throws Exception {
  }
  
  
  
  /**  Allowing for screen-fade FX:
    */
  public void applyFadeColour(Colour fade) {
    this.fadeColour = fade;
  }
  
  
  protected void applyScreenFade(Rendering rendering) {
    rendering.foreColour = null;
    if (fadeColour != null) {
      final Colour c = new Colour().set(fadeColour);
      c.a -= 1f / Rendering.FRAMES_PER_SECOND;
      if (c.a <= 0) fadeColour = null;
      else rendering.foreColour = fadeColour = c;
    }
  }
  
  
  
  /**  Adding ghost FX to the register-
    */
  static class Ghost implements Stage.Visible {
    
    int size;
    Vec3D offset = new Vec3D();
    
    float inceptTime;
    float duration = 2.0f;

    Target tracked = null;
    Sprite sprite;
    
    
    public void renderFor(Rendering r, Base b) {
      if (tracked != null && tracked.indoors()) return;
      final Vec3D p = sprite.position;
      sprite.fog = b.intelMap.fogAt((int) p.x, (int) p.y);
      if (sprite.fog == 0) return;
      sprite.readyFor(r);
    }
    
    
    public Sprite sprite() {
      return sprite;
    }
  }
  
  
  public Ghost addGhost(Target e, float size, Sprite s, float duration) {
    if (s == null) return null;
    final Ghost ghost = new Ghost();
    
    ghost.size       = (int) Nums.ceil(size);
    ghost.inceptTime = world.currentTime();
    ghost.sprite     = s;
    ghost.duration   = duration;
    ghost.tracked    = e;
    
    final Vec3D p = s.position;
    if (e instanceof Mobile) {
      final Mobile tracked = (Mobile) e;
      tracked.viewPosition(ghost.offset);
      ghost.offset.sub(s.position).scale(-1);
    }
    
    final StageRegion section = world.sections.sectionAt((int) p.x, (int) p.y);
    List <Ghost> SG = ghosts.get(section);
    if (SG == null) ghosts.put(section, SG = new List <Ghost> ());
    SG.add(ghost);
    return ghost;
  }
  
  
  public Sprite matchSprite(Target e, ModelAsset key) {
    final Ghost match = matchGhost(e, key);
    return match == null ? null : match.sprite();
  }
  
  
  public Ghost matchGhost(Target e, ModelAsset m) {
    final Vec3D p = e.position(null);
    final StageRegion section = world.sections.sectionAt((int) p.x, (int) p.y);
    List <Ghost> SG = ghosts.get(section);
    
    Ghost match = null;
    if (SG != null) for (Ghost g : SG) {
      if (g.tracked == e && g.sprite.model() == m) { match = g; break; }
    }
    return match;
  }
  
  
  public void updateGhost(Target e, float size, ModelAsset m, float duration) {
    //
    //  Search to see if a ghost exists in this area attached to the same
    //  element and using the same sprite-model.  If so, turn back the incept
    //  time.  Otherwise, initialise (and do the same.)
    Ghost match = matchGhost(e, m);
    if (match == null) match = addGhost(e, size, m.makeSprite(), duration);
    match.inceptTime = world.currentTime() + duration;
  }
  
  
  private boolean trackElement(
    Ghost ghost, StageRegion oldSection, List <Ghost> SG, Base base
  ) {
    if (! (ghost.tracked instanceof Element)) return true;
    
    final Vec3D p = ghost.sprite.position;
    final Element m = (Element) ghost.tracked;
    if (! m.visibleTo(base)) return false;
    
    m.viewPosition(p);
    p.add(ghost.offset);
    
    final StageRegion section = world.sections.sectionAt((int) p.x, (int) p.y);
    if (section == oldSection) return true;
    SG.remove(ghost);
    SG = ghosts.get(section);
    if (SG == null) ghosts.put(section, SG = new List <Ghost> ());
    SG.add(ghost);
    return true;
  }
  
  
  protected Batch <Ghost> visibleFor(Rendering rendering, Base base) {
    final Batch <Ghost> results = new Batch <Ghost> ();
    final float timeNow = world.timeMidRender();
    
    for (StageRegion section : world.visibleSections(rendering)) {
      final List <Ghost> SG = ghosts.get(section);
      if (SG != null) for (Ghost ghost : SG) {
        final float
          duration = ghost.duration,
          timeGone = timeNow - ghost.inceptTime;
        
        if (timeGone >= duration || ! trackElement(ghost, section, SG, base)) {
          SG.remove(ghost);
          continue;
        }
        else {
          final Sprite s = ghost.sprite;
          if (! rendering.view.intersects(s.position, ghost.size)) continue;
          s.colour = Colour.transparency((duration - timeGone) / duration);
          results.add(ghost);
        }
      }
    }
    return results;
  }
}





