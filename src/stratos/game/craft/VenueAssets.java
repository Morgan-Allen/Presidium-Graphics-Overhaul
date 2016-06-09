

package stratos.game.craft;
import stratos.graphics.sfx.*;



public class VenueAssets {
  
  
  final public static PlaneFX.Model
    BLAST_MODEL = PlaneFX.animatedModel(
      "blast_model", VenueAssets.class,
      "media/SFX/blast_anim.gif", 5, 4, 25,
      (25 / 25f), 1.0f
    ),
    //  TODO:  try to find a way to derive these from the item-icons?
    
    POWER_MODEL = PlaneFX.imageModel(
      "power_model", VenueAssets.class,
      "media/Items/power.png" , 0.25f, 0, 0, true, true
    ),
    ATMO_MODEL  = PlaneFX.imageModel(
      "atmo_model", VenueAssets.class,
      "media/Items/atmo.png", 0.25f, 0, 0, true, true
    ),
    WATER_MODEL = PlaneFX.imageModel(
      "water_model", VenueAssets.class,
      "media/Items/water.png" , 0.25f, 0, 0, true, true
    );

}
