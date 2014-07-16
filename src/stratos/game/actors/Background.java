/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.actors;
import stratos.game.building.*;
import stratos.game.campaign.Sector;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.util.*;


//
//  TODO:  Backgrounds need to include their own descriptions.


//TODO:  I'm going to limit this to a few basic headings now.
/*
VASSALS & AGENTS

Trooper        Logician
Explorer       Priest/ess
Runner         Spacer
Seer           Changer
               Glaive Knight
Jovian         Collective
Changeling     
Krech          Noble

Enforcer       Performer
Auditor        Minder
Labourer/Pyon  Trader
Physician      
Engineer       Brave
Ecologist      Gatherer
//*/




public class Background implements Session.Saveable {
  
  
  private static int nextID = 0;
  final public int ID = nextID++;
  private static Batch <Background> all = new Batch();
  
  
  final public String name;
  final protected ImageAsset costume, portrait;
  
  final public int standing;
  final public int guild;
  final Table <Skill, Integer> baseSkills = new Table <Skill, Integer> ();
  final Table <Trait, Float> traitChances = new Table <Trait, Float> ();
  final List <Service> gear = new List <Service> ();
  
  
  
  protected Background(
    String name, String costumeTex, String portraitTex,
    int standing, int guild, Object... args
  ) {
    this.name = name;
    ///I.say("Declaring background: "+name);
    
    if (costumeTex == null) this.costume = null;
    else this.costume = costumeFor(costumeTex);
    
    if (portraitTex == null) this.portrait = null;
    else this.portrait = portraitFor(portraitTex);
    
    this.standing = standing;
    this.guild = guild;
    
    int level = 10;
    float chance = 0.5f;
    for (int i = 0; i < args.length; i++) {
      final Object o = args[i];
      if      (o instanceof Integer) { level  = (Integer) o; }
      else if (o instanceof Float  ) { chance = (Float)   o; }
      else if (o instanceof Skill) {
        baseSkills.put((Skill) o, level);
      }
      else if (o instanceof Trait) {
        traitChances.put((Trait) o, chance);
      }
      else if (o instanceof Service) {
        ///I.say("  "+name+" has gear: "+o);
        gear.add((Service) o);
      }
    }
    all.add(this);
  }
  
  
  static Background[] allBackgrounds() {
    return all.toArray(Background.class);
  }
  
  
  public static Background loadConstant(Session s) throws Exception {
    return Backgrounds.ALL_BACKGROUNDS[s.loadInt()];
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(ID);
  }
  
  
  
  /**  Data access-
    */
  public int skillLevel(Skill s) {
    final Integer level = baseSkills.get(s);
    return level == null ? 0 : (int) level;
  }
  
  
  public List <Skill> skills() {
    final List <Skill> b = new List <Skill> () {
      protected float queuePriority(Skill r) { return r.traitID; }
    };
    for (Skill s : baseSkills.keySet()) b.queueAdd(s);
    return b;
  }
  
  
  public float traitChance(Trait t) {
    final Float chance = traitChances.get(t);
    return chance == null ? 0 : (float) chance;
  }
  
  
  public List <Trait> traits() {
    final List <Trait> b = new List <Trait> () {
      protected float queuePriority(Trait r) { return r.traitID; }
    };
    for (Trait t : traitChances.keySet()) b.queueAdd(t);
    return b;
  }
  
  
  public List <Service> properGear() {
    return gear;
  }
  
  
  
  /**  Rendering and interface helper methods-
    */
  final static String COSTUME_DIR = "media/Actors/human/";
  
  
  protected static ImageAsset costumeFor(String texName) {
    return ImageAsset.fromImage(COSTUME_DIR+texName, Backgrounds.class);
  }

  protected static ImageAsset portraitFor(String texName) {
    return ImageAsset.fromImage(COSTUME_DIR+texName, Backgrounds.class);
  }
  
  
  public String toString() {
    return name;
  }
  
  
  public String nameFor(Actor actor) {
    return name;
  }
  
  
  public ImageAsset costumeFor(Actor actor) {
    return costume;
  }
  
  
  public ImageAsset portraitFor(Actor actor) {
    return portrait;
  }
}








