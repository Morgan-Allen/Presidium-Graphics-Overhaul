

package src.game.common ;
import src.graphics.common.* ;
import src.graphics.sfx.* ;
import src.util.* ;
import src.game.common.WorldSections.Section ;



/**  This class is used to add transitory or conditional special effects to the
  *  world.
  */
public class Ephemera {
  
  
  /**  Fields, constants, and save/load methods.
    */
  final World world ;
  
  private Colour fadeColour = null ;
  final Table <WorldSections.Section, List <Ghost>> ghosts = new Table(100) ;
  
  
  protected Ephemera(World world) {
    this.world = world ;
  }
  
  
  protected void loadState(Session s) throws Exception {
  }
  
  
  protected void saveState(Session s) throws Exception {
  }
  
  
  
  /**  Allowing for screen-fade FX:
    */
  public void applyFadeColour(Colour fade) {
    this.fadeColour = fade ;
  }
  
  
  protected void applyScreenFade(Rendering rendering) {
    //  TODO:  RESTORE THIS FX
    /*
    rendering.foreColour = null ;
    if (fadeColour != null) {
      final Colour c = new Colour().set(fadeColour) ;
      c.a -= 1f / Rendering.FRAMES_PER_SECOND ;
      if (c.a <= 0) fadeColour = null ;
      else rendering.foreColour = fadeColour = c ;
    }
    //*/
  }
  
  
  
  /**  Adding ghost FX to the register-
    */
  static class Ghost implements World.Visible {
    
    int size ;
    Element tracked = null ;
    float inceptTime ;
    Sprite sprite ;
    float duration = 2.0f ;
    
    
    public void renderFor(Rendering r, Base b) {
      sprite.fog = 1.0f ;
      sprite.registerFor(r);
    }
    
    
    public Sprite sprite() {
      return sprite ;
    }
  }
  
  
  public Ghost addGhost(Element e, float size, Sprite s, float duration) {
    //  TODO:  TEST AND RESTORE;
    if (true) return null;
    if (s == null) return null ;
    final Ghost ghost = new Ghost() ;
    ghost.size = (int) Math.ceil(size) ;
    ghost.inceptTime = world.currentTime() ;
    ghost.sprite = s ;
    ghost.duration = duration ;
    ghost.tracked = e ;
    
    final Vec3D p = s.position ;
    if (e != null) e.viewPosition(p) ;
    final Section section = world.sections.sectionAt((int) p.x, (int) p.y) ;
    List <Ghost> SG = ghosts.get(section) ;
    if (SG == null) ghosts.put(section, SG = new List <Ghost> ()) ;
    SG.add(ghost) ;
    return ghost ;
  }
  
  
  public Ghost matchGhost(Element e, ModelAsset m) {
    final Vec3D p = e.sprite().position ;
    final Section section = world.sections.sectionAt((int) p.x, (int) p.y) ;
    List <Ghost> SG = ghosts.get(section) ;
    Ghost match = null ;
    if (SG != null) for (Ghost g : SG) {
      if (g.tracked == e && g.sprite.model() == m) { match = g ; break ; }
    }
    return match ;
  }
  
  
  public void updateGhost(Element e, float size, ModelAsset m, float duration) {
    //
    //  Search to see if a ghost exists in this area attached to the same
    //  element and using the same sprite-model.  If so, turn back the incept
    //  time.  Otherwise, initialise (and do the same.)
    Ghost match = matchGhost(e, m) ;
    if (match == null) match = addGhost(e, size, m.makeSprite(), duration) ;
    match.inceptTime = world.currentTime() + duration ;
  }
  
  
  private void trackElement(
    Ghost ghost, Section oldSection, List <Ghost> SG
  ) {
    final Vec3D p = ghost.sprite.position ;
    ghost.tracked.viewPosition(p) ;
    if (ghost.sprite instanceof SFX) p.z += ghost.tracked.height() / 2f ;
    final Section section = world.sections.sectionAt((int) p.x, (int) p.y) ;
    if (section == oldSection) return ;
    SG.remove(ghost) ;
    SG = ghosts.get(section) ;
    if (SG == null) ghosts.put(section, SG = new List <Ghost> ()) ;
    SG.add(ghost) ;
  }
  
  
  protected Batch <Ghost> visibleFor(Rendering rendering, Base base) {
    final Batch <Ghost> results = new Batch <Ghost> () ;
    final float timeNow = world.timeMidRender();
    
    for (Section section : world.visibleSections(rendering)) {
      final List <Ghost> SG = ghosts.get(section) ;
      if (SG != null) for (Ghost ghost : SG) {
        final float
          duration = ghost.duration,
          timeGone = timeNow - ghost.inceptTime;
        
        if (timeGone >= duration) {
          SG.remove(ghost) ;
          continue ;
        }
        else {
          final Sprite s = ghost.sprite ;
          if (! rendering.view.intersects(s.position, ghost.size)) continue ;
          s.colour = Colour.transparency((duration - timeGone) / duration) ;
          
          if (ghost.tracked != null) {
            if (! ghost.tracked.visibleTo(base)) continue ;
            s.fog = ghost.tracked.fogFor(base) ;
            trackElement(ghost, section, SG) ;
          }
          s.update() ;
          results.add(ghost) ;
        }
      }
    }
    return results ;
  }
}





