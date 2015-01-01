/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.util.*;



//TODO:  I'm going to limit this to a few basic headings now.
/*
VASSALS & AGENTS

Trooper        Logician
Ecologist      Tek Priest/ess
Runner         Navigator
Pseer          Jil Baru
               Glaive Archon
Jovian         Collective
Changeling     
Krech          Kommando
               Palatine
Noble          Xenopath

Archivist      Vendor
Enforcer       Performer
Auditor        Bartender
Physician      Pyon
Engineer       Dreg
//*/




public class Background extends Index.Entry implements Session.Saveable {
  
  
  final public static Index <Background> INDEX = new Index <Background> ();
  
  final public String name, info;
  final protected Class baseClass;
  final protected ImageAsset costume, portrait;
  
  final public int standing;
  final public int guild;
  final Table <Skill, Integer> baseSkills = new Table <Skill, Integer> ();
  final Table <Trait, Float> traitChances = new Table <Trait, Float  > ();
  final List <Traded> gear = new List <Traded> ();
  
  
  
  protected Background(
    Class baseClass,
    String name, String info, String costumeTex, String portraitTex,
    int standing, int guild, Object... args
  ) {
    super(INDEX, name);
    this.baseClass = baseClass;
    this.name = name;
    this.info = info;
    
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
      else if (o instanceof Traded) {
        ///I.say("  "+name+" has gear: "+o);
        gear.add((Traded) o);
      }
    }
  }
  
  
  public static Background loadConstant(Session s) throws Exception {
    return INDEX.loadFromEntry(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    INDEX.saveEntry(this, s.output());
  }
  
  
  
  /**  Data access-
    */
  public int skillLevel(Skill s) {
    final Integer level = baseSkills.get(s);
    return level == null ? 0 : (int) level;
  }
  
  
  public List <Skill> skills() {
    final List <Skill> b = new List <Skill> () {
      //protected float queuePriority(Skill r) { return r.traitID; }
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
      //protected float queuePriority(Trait r) { return r.traitID; }
    };
    for (Trait t : traitChances.keySet()) b.queueAdd(t);
    return b;
  }
  
  
  public List <Traded> properGear() {
    return gear;
  }
  
  
  /**  Factory methods-
    */
  public Actor sampleFor(Base base) {
    return new Human(this, base);
  }
  
  
  
  /**  Rendering and interface helper methods-
    */
  final static String COSTUME_DIR = "media/Actors/human/";
  
  
  protected ImageAsset costumeFor(String texName) {
    return ImageAsset.fromImage(Backgrounds.class, COSTUME_DIR+texName);
  }

  protected ImageAsset portraitFor(String texName) {
    return ImageAsset.fromImage(Backgrounds.class, COSTUME_DIR+texName);
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








