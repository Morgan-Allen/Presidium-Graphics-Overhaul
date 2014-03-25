/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.actors ;
import stratos.game.building.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.game.planet.*;
import stratos.game.tactical.Combat;
import stratos.graphics.common.*;
import stratos.graphics.sfx.*;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;




public class Human extends Actor implements Abilities {
  
  
  /**  Methods and constants related to preparing media and sprites-
    */
  final static String
    FILE_DIR = "media/Actors/human/",
    XML_FILE = "HumanModels.xml" ;
  final public static ModelAsset
    MODEL_MALE = MS3DModel.loadFrom(
      FILE_DIR, "male_final.ms3d",
      Human.class, XML_FILE, "MalePrime"
    ),
    MODEL_FEMALE = MS3DModel.loadFrom(
      FILE_DIR, "female_final.ms3d",
      Human.class, XML_FILE, "FemalePrime"
    );
  
  final static ImageAsset
    PORTRAIT_BASE = ImageAsset.fromImage(
      FILE_DIR+"portrait_base.png", Human.class
    ),
    BASE_FACES = ImageAsset.fromImage(
      FILE_DIR+"face_portraits.png", Human.class
    );
  
  final static ImageAsset BLOOD_SKINS[] = ImageAsset.fromImages(
    Human.class, FILE_DIR,
    "desert_blood.gif",
    "tundra_blood.gif",
    "forest_blood.gif",
    "wastes_blood.gif"
  ) ;
  
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  Career career ;
  
  
  public Human(Background vocation, Base base) {
    this(new Career(vocation), base) ;
  }
  
  
  public Human(Career career, Base base) {
    this.career = career ;
    assignBase(base) ;
    career.applyCareer(this) ;
    initSpriteFor(this) ;
  }
  
  
  public Human(Session s) throws Exception {
    super(s) ;
    career = new Career(this) ;
    career.loadState(s) ;
    initSpriteFor(this) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    career.saveState(s) ;
  }
  
  
  protected ActorMind initAI() { return new HumanMind(this) ; }
  
  public Background vocation() { return career.vocation() ; }
  
  public Career career() { return career ; }
  
  public Species species() { return Species.HUMAN ; }
  
  public void setVocation(Background b) {
    career.recordVocation(b) ;
  }
  
  
  
  /**  Utility methods/constants for creating human-citizen sprites and
    *  portraits-
    */
  //
  //  All this stuff is intimately dependant on the layout of the collected
  //  portraits image specified- do not modify without close inspection.
  final static int
    CHILD_FACE_OFF[] = {2, 0},
    ELDER_FACE_OFF[] = {2, 1},
    F_AVG_FACE_OFF[] = {1, 1},
    F_HOT_FACE_OFF[] = {0, 1},
    M_AVG_FACE_OFF[] = {1, 0},
    M_HOT_FACE_OFF[] = {0, 0} ;
  
  final static int
    M_HAIR_OFF[][] = {{5, 5}, {4, 5}, {3, 5}, {3, 4}, {4, 4}, {5, 4}},
    F_HAIR_OFF[][] = {{2, 5}, {1, 5}, {0, 5}, {0, 4}, {1, 4}, {2, 4}} ;
  
  final static int BLOOD_FACE_OFFSETS[][] = {
    {3, 4}, {0, 4}, {3, 2}, {0, 2}
  } ;
  final static int BLOOD_TONE_SHADES[] = { 3, 1, 2, 0 } ;
  
  
  private static int bloodID(Human c) {
    int ID = 0 ;
    float highest = 0 ;
    for (int i = 4 ; i-- > 0 ;) {
      final float blood = c.traits.traitLevel(BLOOD_TRAITS[i]) ;
      if (blood > highest) { ID = i ; highest = blood ; }
    }
    return ID ;
  }
  
  
  private static Composite faceComposite(Human c) {
    final String key = ""+c.hashCode();
    
    final Composite cached = Composite.fromCache(key);
    if (cached != null) return cached;
    
    final int PS = ActorPanel.PORTRAIT_SIZE;
    final Composite composite = Composite.withSize(PS, PS, key);
    composite.layer(PORTRAIT_BASE);
    
    final int bloodID = bloodID(c);
    final boolean male = c.traits.male();
    final int ageStage = c.health.agingStage();
    ///I.say("Blood/male/age-stage: "+bloodID+" "+male+" "+ageStage) ;
    
    int faceOff[], bloodOff[] = BLOOD_FACE_OFFSETS[bloodID] ;
    if (ageStage == 0) faceOff = CHILD_FACE_OFF ;
    else {
      int looks = (int) c.traits.traitLevel(Trait.HANDSOME) + 2 - ageStage ;
      if (looks > 0) faceOff = male ? M_HOT_FACE_OFF : F_HOT_FACE_OFF ;
      else if (looks == 0) faceOff = male ? M_AVG_FACE_OFF : F_AVG_FACE_OFF ;
      else faceOff = ELDER_FACE_OFF ;
    }
    
    final int UV[] = new int[] {
      0 + (faceOff[0] + bloodOff[0]),
      5 - (faceOff[1] + bloodOff[1])
    };
    composite.layerFromGrid(BASE_FACES, UV[0], UV[1], 6, 6);
    
    if (ageStage > ActorHealth.AGE_JUVENILE) {
      int hairID = c.traits.geneValue("hair", 6) ;
      if (hairID < 0) hairID *= -1 ;
      hairID = Visit.clamp(hairID + BLOOD_TONE_SHADES[bloodID], 6) ;
      
      if (ageStage >= ActorHealth.AGE_SENIOR) hairID = 5 ;
      else if (hairID == 5) hairID-- ;
      int fringeOff[] = (male ? M_HAIR_OFF : F_HAIR_OFF)[hairID] ;
      composite.layerFromGrid(BASE_FACES, fringeOff[0], fringeOff[1], 6, 6) ;
      
      ImageAsset portrait = c.career.vocation().portraitFor(c);
      if (portrait == null) portrait = c.career.birth().portraitFor(c) ;
      composite.layerFromGrid(portrait, 0, 0, 1, 1) ;
    }
    
    return composite ;
  }
  
  
  private static void initSpriteFor(Human c) {
    final boolean male = c.traits.male() ;
    final SolidSprite s ;
    if (c.sprite() == null) {
      s = (SolidSprite) (male ? MODEL_MALE : MODEL_FEMALE).makeSprite() ;
      c.attachSprite(s) ;
    }
    else s = (SolidSprite) c.sprite() ;
    
    ImageAsset skin = BLOOD_SKINS[bloodID(c)];
    
    //s.applyOverlay(skin.asTexture(), AnimNames.MAIN_BODY, true);
    ImageAsset costume = c.career.vocation().costumeFor(c) ;
    if (costume == null) costume = c.career.birth().costumeFor(c);
    //s.applyOverlay(costume.asTexture(), AnimNames.MAIN_BODY, true);
    
    s.setOverlaySkins(
      AnimNames.MAIN_BODY,
      skin.asTexture(),
      costume.asTexture()
    );
    toggleSpriteGroups(c, s) ;
  }
  
  
  //  TODO:  You might want to call this at more regular intervals?
  private static void toggleSpriteGroups(Human human, SolidSprite sprite) {
    for (String groupName : ((SolidModel) sprite.model()).partNames()) {
      boolean valid = AnimNames.MAIN_BODY.equals(groupName);
      final DeviceType DT = human.gear.deviceType();
      if (DT != null && DT.groupName.equals(groupName)) valid = true;
      if (! valid) sprite.togglePart(groupName, false);
    }
  }
  
  
  
  /**  More usual rendering and interface methods-
    */
  public void renderFor(Rendering rendering, Base base) {
    
    //  If you're in combat, show the right gear equipped-
    //  TODO:  This is a bit of a hack.  Rework or generalise?
    final DeviceType DT = gear.deviceType() ;
    final Combat c = (Combat) matchFor(Combat.class) ;
    if (DT != null) {
      ((SolidSprite) sprite()).togglePart(DT.groupName, c != null) ;
    }
    super.renderFor(rendering, base);
  }
  
  
  protected float spriteScale() {
    //
    //  TODO:  make this a general 3D scaling vector, and incorporate other
    //  physical traits.
    final int stage = health.agingStage() ;
    final float scale = (float) Math.pow(traits.scaleLevel(TALL), 0.1f) ;
    if (stage == 0) return 0.8f * scale ;
    if (stage >= 2) return 0.95f * scale ;
    return 1 * scale ;
  }
  
  
  public String fullName() {
    return career.fullName() ;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return faceComposite(this);
  }
  
  
  public InfoPanel configPanel(InfoPanel panel, BaseUI UI) {
    if (panel == null) panel = new ActorPanel(
      UI, this, "STATUS", "SKILLS", "PROFILE"
    );
    final int categoryID = panel.categoryID();
    final Description d = panel.detail();
    if (categoryID == 0) describeStatus(d, UI) ;
    if (categoryID == 1) describeSkills(d, UI) ;
    if (categoryID == 2) describeProfile(d, UI) ;
    return panel;
  }
  
  
  //
  //  Some of this could be outsourced to the ActorGear classes, et cetera.
  private void describeStatus(Description d, HUD UI) {
    //
    //  Describe your job, place of work, and current residence:
    d.append("Is: ") ; describeStatus(d) ;
    final String VN = vocation().nameFor(this) ;
    d.append("\nVocation: ") ;
    if (mind.work() != null) {
      d.append(VN+" at ") ;
      d.append(mind.work()) ;
    }
    else d.append("Unemployed "+VN) ;
    d.append("\nResidence: ") ;
    if (mind.home() != null) {
      d.append(mind.home()) ;
    }
    else d.append("Homeless") ;
    //
    //  Describe your current health, outlook, or special FX.
    d.append("\n\nCondition: ") ;
    final Batch <String> healthDesc = health.conditionsDesc() ;
    for (String desc : healthDesc) {
      d.append("\n  "+desc) ;
    }
    final Batch <Condition> conditions = traits.conditions() ;
    for (Condition c : conditions) {
      final String desc = traits.levelDesc(c) ;
      if (desc != null) d.append("\n  "+desc) ;
    }
    if (healthDesc.size() == 0 && conditions.size() == 0) {
      d.append("\n  Okay") ;
    }
    //
    //  Describe your current gear and anything carried.
    d.append("\n\nInventory: ") ;
    
    final Item device = gear.deviceEquipped() ;
    if (device != null) d.append("\n  "+device) ;
    else d.append("\n  No device") ;
    d.append(" ("+((int) gear.attackDamage())+")") ;
    
    final Item outfit = gear.outfitEquipped() ;
    if (outfit != null) d.append("\n  "+outfit) ;
    else d.append("\n  Nothing worn") ;
    d.append(" ("+((int) gear.armourRating())+")") ;
    
    for (Item item : gear.allItems()) d.append("\n  "+item) ;
    d.append("\n  "+((int) gear.credits())+" Credits") ;
    if (gear.hasShields()) {
      d.append("\n  Fuel Cells: "+((int) gear.fuelCells())) ;
    }
  }
  
  
  private void describeSkills(Description d, HUD UI) {
    //
    //  Describe attributes, skills and psyonic techniques.
    d.append("Attributes: ") ;
    for (Skill skill : traits.attributes()) {
      final int level = (int) traits.traitLevel(skill) ;
      final int bonus = (int) traits.effectBonus(skill) ;
      d.append("\n  "+skill.name+" "+level+" ") ;
      d.append(Skill.attDesc(level), Skill.skillTone(level)) ;
      if (bonus != 0) {
        d.append((bonus >= 0 ? " (+" : " (-")+Math.abs(bonus)+")") ;
      }
    }
    d.append("\n\nSkills: ") ;
    final List <Skill> sorting = new List <Skill> () {
      protected float queuePriority(Skill skill) {
        return traits.traitLevel(skill) ;
      }
    } ;
    
    for (Skill skill : traits.skillSet()) sorting.add(skill) ;
    sorting.queueSort() ;
    for (Skill skill : sorting) {
      final int level = (int) traits.traitLevel(skill) ;
      final int bonus = (int) (
          traits.rootBonus(skill) +
          traits.effectBonus(skill)
      ) ;
      final Colour tone = Skill.skillTone(level) ;
      d.append("\n  "+skill.name+" "+level+" ", tone) ;
      if (bonus != 0) {
        d.append((bonus >= 0 ? "(+" : "(-")+Math.abs(bonus)+")") ;
      }
    }
    
    /*
    d.append("\n\nTechniques: ") ;
    for (Power p : traits.powers()) {
      d.append("\n  "+p.name) ;
    }
    //*/
  }
  
  
  private void describeProfile(Description d, HUD UI) {
    //
    //  Describe background, personality, relationships and memories.
    //  TODO:  Allow for a chain of arbitrary vocations in a career?
    d.append("Background: ") ;
    d.append("\n  "+career.birth()+" on "+career.homeworld()) ;
    d.append("\n  Trained as "+career.vocation().nameFor(this)) ;
    d.append("\n  "+traits.levelDesc(ORIENTATION)) ;
    d.append(" "+traits.levelDesc(GENDER)) ;
    d.append("\n  Age: "+health.exactAge()+" ("+health.agingDesc()+")") ;
    
    d.appendList("\n\nAppearance: " , descTraits(traits.physique   ())) ;
    d.appendList("\n\nPersonality: ", descTraits(traits.personality())) ;
    //d.appendList("\n\nMutations: "  , descTraits(traits.mutations  ())) ;
    
    d.append("\n\nRelationships: ") ;
    for (Relation r : mind.relations()) {
      d.append("\n  ") ;
      d.append(r.subject) ;
      d.append(" ("+r.descriptor()+")") ;
    }
  }
  
  private Batch <String> descTraits(Batch <Trait> traits) {
    final Actor actor = this ;
    final List <Trait> sorting = new List <Trait> () {
      protected float queuePriority(Trait r) {
        return 0 - actor.traits.traitLevel(r) ;
      }
    } ;
    for (Trait t : traits) sorting.queueAdd(t) ;
    final Batch <String> desc = new Batch <String> () ;
    for (Trait t : sorting) desc.add(this.traits.levelDesc(t)) ;
    return desc ;
  }
}








