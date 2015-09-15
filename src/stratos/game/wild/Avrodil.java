/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.actors.Choice;
import stratos.game.common.*;
import stratos.game.economic.Blueprint;
import stratos.game.wild.Species.Type;
import stratos.graphics.common.ModelAsset;
import stratos.graphics.cutout.CutoutModel;
import stratos.graphics.solids.MS3DModel;



public class Avrodil extends Fauna {
  

  
  final public static Species SPECIES = new Species(
    Avrodil.class,
    "Avrodil",
    "ENTER AVRODIL DESCRIPTION HERE",
    
    FILE_DIR+"AvrodilPortrait.png",
    MS3DModel.loadFrom(
      FILE_DIR, "Avrodil.ms3d", Avrodil.class,
      XML_FILE, "Avrodil"
    ),
    Type.PREDATOR,
    3.50f, //bulk
    0.35f, //speed
    0.85f  //sight
  ) {
    final ModelAsset NEST_MODEL = CutoutModel.fromImage(
      Avrodil.class, LAIR_DIR+"sporing_body.png", 3.5f, 3
    );
    final Blueprint BLUEPRINT = Nest.constructBlueprint(
      3, 2, this, NEST_MODEL
    );
    public Actor sampleFor(Base base) { return init(new Avrodil(base)); }
    public Blueprint nestBlueprint() { return BLUEPRINT; }
  };
  

  public Avrodil(Base base) {
    super(SPECIES, base);
  }
  
  
  public Avrodil(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  protected void initStats() {
    
  }
  
  
  protected void addReactions(Target seen, Choice choice) {
  }
  
  
  protected void addChoices(Choice choice) {
    super.addChoices(choice);
  }
  
  
  
}


