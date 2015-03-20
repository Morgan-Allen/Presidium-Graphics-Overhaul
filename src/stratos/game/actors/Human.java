/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import stratos.game.wild.Species;


//  TODO:  Replace with 'Person' or 'Citizen'?

//  TODO:  You also need to ensure that the media gets updated when an actor
//         changes job.

public class Human extends Actor implements Qualities {
  
  
  /**  Methods and constants related to preparing media and sprites-
    */
  private static boolean
    mediaVerbose = false;
  
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  final Career career;
  
  
  public Human(Background vocation, Base base) {
    this(new Career(vocation), base);
  }
  
  
  public Human(Career career, Base base) {
    this.career = career;
    assignBase(base);
    career.applyCareer(this, base);
    initSpriteFor(this);
  }
  
  
  public Human(Session s) throws Exception {
    super(s);
    career = new Career(this);
    career.loadState(s);
    initSpriteFor(this);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    career.saveState(s);
  }
  
  
  protected ActorMind initMind() { return new HumanMind(this); }
  
  public Background vocation() { return career.vocation(); }
  
  public Career career() { return career; }
  
  public Species species() { return Species.HUMAN; }
  
  public void setVocation(Background b) {
    career.recordVocation(b);
  }
  
  
  
  /**  Utility methods/constants for creating human-citizen sprites and
    *  portraits-
    */
  //
  //  All this stuff is intimately dependant on the layout of the collected
  //  portraits image specified- do not modify without close inspection.
  
  //  TODO:  Use an image-atlas here- or better yet, just individual images.
  //  You're using composites for efficiency anyway!
  final static String
    FILE_DIR = "media/Actors/human/",
    XML_FILE = "HumanModels.xml";
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
      Human.class, FILE_DIR+"portrait_base.png"
    ),
    BASE_FACES = ImageAsset.fromImage(
      Human.class, FILE_DIR+"face_portraits.png"
    );
  
  final static ImageAsset BLOOD_SKINS[] = ImageAsset.fromImages(
    Human.class, FILE_DIR,
    "desert_blood.gif",
    "wastes_blood.gif",
    "tundra_blood.gif",
    "forest_blood.gif"
  );
  
  
  final static Trait RACE_TRAITS_LISTING[] = {
    DESERT_BLOOD, WASTES_BLOOD, TUNDRA_BLOOD, FOREST_BLOOD
  };
  
  final static int
    DESERT_ID = 0, WASTES_ID = 1, TUNDRA_ID = 2, FOREST_ID = 3,
    RACE_TONE_SHADES[] = { 3, 0, 1, 2 },
    
    CHILD_FACE_OFF[] = {2, 0},
    ELDER_FACE_OFF[] = {2, 1},
    F_AVG_FACE_OFF[] = {1, 1},
    F_HOT_FACE_OFF[] = {0, 1},
    M_AVG_FACE_OFF[] = {1, 0},
    M_HOT_FACE_OFF[] = {0, 0};
  
  final static int
    M_HAIR_OFF[][] = {{5, 5}, {4, 5}, {3, 5}, {3, 4}, {4, 4}, {5, 4}},
    F_HAIR_OFF[][] = {{2, 5}, {1, 5}, {0, 5}, {0, 4}, {1, 4}, {2, 4}},
    BLACK_HAIR_INDEX = 3,
    WHITE_HAIR_INDEX = 5;
  
  final static int RACE_FACE_OFFSETS[][] = {
    {3, 4}, {0, 2}, {0, 4}, {3, 2}
  };
  
  
  public static Trait raceFor(Human c) {
    return RACE_TRAITS_LISTING[raceID(c)];
  }
  
  
  private static int raceID(Human c) {
    int ID = 0;
    float highest = 0;
    for (int i = 4; i-- > 0;) {
      final float blood = c.traits.traitLevel(RACE_TRAITS_LISTING[i]);
      if (blood > highest) { ID = i; highest = blood; }
    }
    return ID;
  }
  
  
  private static Composite faceComposite(Human c) {
    //  TODO:  Also refresh if equipment or vocation changes!
    final String key = ""+c.hashCode();
    final Composite cached = Composite.fromCache(key);
    if (cached != null) return cached;
    
    final boolean report = mediaVerbose && I.talkAbout == c;
    if (report) {
      I.say("\nGetting new face composite for "+c+" (key "+key+")");
    }
    
    final int PS = SelectionPane.PORTRAIT_SIZE;
    final Composite composite = Composite.withSize(PS, PS, key);
    composite.layer(PORTRAIT_BASE);
    
    final int bloodID = raceID(c);
    final boolean male = c.traits.male();
    final int ageStage = c.health.agingStage();
    
    if (report) {
      I.say("  Blood/male/age-stage: "+bloodID+" "+male+" "+ageStage);
    }
    
    int faceOff[], bloodOff[] = RACE_FACE_OFFSETS[bloodID];
    if (ageStage == 0) {
      faceOff = CHILD_FACE_OFF;
    }
    else {
      int looks = (int) c.traits.traitLevel(Trait.HANDSOME) + 2 - ageStage;
      if (looks > 0) faceOff = male ? M_HOT_FACE_OFF : F_HOT_FACE_OFF;
      else if (looks == 0) faceOff = male ? M_AVG_FACE_OFF : F_AVG_FACE_OFF;
      else faceOff = ELDER_FACE_OFF;
    }
    
    final int UV[] = new int[] {
      0 + (faceOff[0] + bloodOff[0]),
      5 - (faceOff[1] + bloodOff[1])
    };
    composite.layerFromGrid(BASE_FACES, UV[0], UV[1], 6, 6);
    
    if (ageStage > ActorHealth.AGE_JUVENILE) {
      final int hairGene = c.traits.geneValue("hair", 4);
      
      int hairID = BLACK_HAIR_INDEX;
      if (hairGene > RACE_TONE_SHADES[bloodID]) {
        hairID = BLACK_HAIR_INDEX - hairGene;
      }
      if (ageStage >= ActorHealth.AGE_SENIOR && hairGene > 1) {
        hairID = WHITE_HAIR_INDEX;
      }
      int fringeOff[] = (male ? M_HAIR_OFF : F_HAIR_OFF)[hairID];
      composite.layerFromGrid(BASE_FACES, fringeOff[0], fringeOff[1], 6, 6);
      
      ImageAsset portrait = c.career.vocation().portraitFor(c);
      if (portrait == null) portrait = c.career.birth().portraitFor(c);
      composite.layerFromGrid(portrait, 0, 0, 1, 1);
    }
    
    return composite;
  }
  
  
  private static void initSpriteFor(Human c) {
    final boolean male = c.traits.male();
    final SolidSprite s;
    if (c.sprite() == null) {
      s = (SolidSprite) (male ? MODEL_MALE : MODEL_FEMALE).makeSprite();
      c.attachSprite(s);
    }
    else s = (SolidSprite) c.sprite();
    
    ImageAsset skin = BLOOD_SKINS[raceID(c)];
    
    //s.applyOverlay(skin.asTexture(), AnimNames.MAIN_BODY, true);
    ImageAsset costume = c.career.vocation().costumeFor(c);
    if (costume == null) costume = c.career.birth().costumeFor(c);
    //s.applyOverlay(costume.asTexture(), AnimNames.MAIN_BODY, true);
    
    s.setOverlaySkins(
      AnimNames.MAIN_BODY,
      skin.asTexture(),
      costume.asTexture()
    );
    toggleSpriteGroups(c, s);
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
    //  TODO:  Move out to the CombatUtils class...
    final DeviceType DT = gear.deviceType();
    final Property work = mind.work();
    if (DT != null) {
      boolean shouldArm = senses.isEmergency();
      if (work != null && work.staff().onShift(this)) shouldArm = true;
      ((SolidSprite) sprite()).togglePart(DT.groupName, shouldArm);
    }
    
    
    //  TODO:  Also a bit of a hack.  Remove later.
    if (gear.shieldCharge() > gear.maxShields()) {
      CombatFX.applyShieldFX(gear.outfitType(), this, null, false);
    }
    
    super.renderFor(rendering, base);
  }
  
  
  protected float moveAnimStride() {
    return 1.11f;
  }
  
  
  protected float spriteScale() {
    //
    //  TODO:  make this a general 3D scaling vector, and incorporate other
    //  physical traits.
    return 1;
    /*
    final int stage = health.agingStage();
    final float scale = (float) Nums.pow(traits.relativeLevel(TALL) + 1, 0.1f);
    if (stage == 0) return 0.8f * scale;
    if (stage >= 2) return 0.95f * scale;
    return 1 * scale;
    //*/
  }
  
  
  public String fullName() {
    return career.fullName();
  }
  
  
  public Composite portrait(BaseUI UI) {
    return faceComposite(this);
  }
  
  
  public SelectionPane configPanel(SelectionPane panel, BaseUI UI) {
    return ActorDescription.configPanel(this, panel, UI);
  }
}








