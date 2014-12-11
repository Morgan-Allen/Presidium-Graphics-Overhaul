

package stratos.start;
import stratos.game.actors.Human;
import stratos.game.base.Suspensor;
import stratos.game.economic.Economy;
import stratos.game.wild.Species;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.sfx.*;
import stratos.graphics.solids.*;
import stratos.graphics.terrain.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



public class DebugGraphics {
  
  
  final static CutoutModel
    CM = CutoutModel.fromImage(
      DebugGraphics.class,
      "media/Buildings/military/bastion.png", 7, 5
    ),
    VM = CutoutModel.fromImage(
      DebugGraphics.class,
      "media/Buildings/merchant/stock_exchange.png", 4, 2
    ),
    GM[][] = CutoutModel.fromImageGrid(
      DebugGraphics.class, "media/Buildings/artificer/all_molds.png",
      4, 5, 1, 1
    );
  final static MS3DModel
    MICOVORE_MODEL = MS3DModel.loadFrom(
      "media/Actors/fauna/", "Micovore.ms3d",
      DebugGraphics.class, "FaunaModels.xml", "Micovore"
    ),
    SUSPENSOR_MODEL = MS3DModel.loadFrom(
      "media/Vehicles/", "Barge.ms3d", Suspensor.class,
      "VehicleModels.xml", "Suspensor"
    ),
    HUMAN_MODEL = MS3DModel.loadFrom(
      "media/Actors/human/", "male_final.ms3d",
      Human.class, "HumanModels.xml", "MalePrime"
    ),
    DROPSHIP_MODEL =  MS3DModel.loadFrom(
      "media/Vehicles/", "dropship.ms3d", Species.class,
      "VehicleModels.xml", "Dropship"
    ),
    SM = DROPSHIP_MODEL;
  
  final static ShotFX.Model
    BEAM_FX_MODEL = new ShotFX.Model(
      "laser_beam_fx", DebugGraphics.class,
      "media/SFX/blast_beam.gif",
      0.05f, 0,
      0.10f, 3, true, true
    ),
    SPEAR_FX_MODEL = new ShotFX.Model(
      "spear_fx", DebugGraphics.class,
      "media/SFX/spear_throw.gif", 0.1f, 0.0f, 0.12f, 1.2f, false, false
    ),
    FM = BEAM_FX_MODEL;
  
  
  public static void main(String args[]) {
    final VisualDebug debug = new VisualDebug() {
      protected void loadVisuals() {
        final SolidSprite SS = (SolidSprite) SM.makeSprite();
        sprites.add(SS);
        SS.position.y = 2;
        SS.scale = 1.5f;
        
        
        
        final SolidSprite spriteA = (SolidSprite) HUMAN_MODEL.makeSprite();
        spriteA.showOnly(AnimNames.MAIN_BODY);
        spriteA.setOverlaySkins(
          AnimNames.MAIN_BODY,
          DebugHumanSprites.SKIN_A.asTexture(),
          DebugHumanSprites.SKIN_B.asTexture()
        );
        spriteA.togglePart("pistol", true);
        spriteA.scale = 2.0f;
        spriteA.position.set(-2, 2, 0);
        //sprites.add(spriteA);
        
        
        for (int i = 10; i-- > 0;) {
          final Sprite CS = CM.makeSprite();
          CS.position.set(i, -i, 0);
          CS.fog = (i + 1) / 10f;
          CS.colour = Colour.transparency(CS.fog);
          CS.scale = 0.5f;
          //sprites.add(CS);
        }
        
        final BuildingSprite BS = BuildingSprite.fromBase(VM, 4, 2);
        BS.position.set(-4, -4, 0);
        BS.updateItemDisplay(
          Economy.PARTS.model, 15, -1.5f, 1.5f
        );
        BS.updateItemDisplay(
          Economy.CARBS.model, 15, -1.5f, 0.5f
        );
        BS.updateItemDisplay(
          Economy.SOMA.model, 15, -1.5f, -0.5f
        );
        BS.updateItemDisplay(
          Economy.ANTIMASS.model, 15, -1.5f, -1.5f
        );
        BS.updateCondition(0.0f, false, false);
        BS.toggleFX(BuildingSprite.POWER_MODEL, true);
        BS.toggleFX(BuildingSprite.WATER_MODEL, true);
        BS.toggleFX(BuildingSprite.BLAST_MODEL, true);
        sprites.add(BS);
        
        final TalkFX FX1 = new TalkFX() {
          int count = 0;
          public void readyFor(Rendering r) {
            if (this.numPhrases() < 2) {
              addPhrase("Testing "+(count++), TalkFX.FROM_LEFT);
            }
            super.readyFor(r);
          }
        };
        FX1.position.set(0, 0, 2);
        //sprites.add(FX1);
        
        final ShieldFX FX2 = new ShieldFX() {
          public void readyFor(Rendering r) {
            if (Rand.index(Rendering.FRAMES_PER_SECOND) <= 2) {
              final Vec3D point = new Vec3D();
              final float angle = (float) (Math.PI * 2 * Rand.num());
              point.x = 10 * Nums.sin(angle);
              point.y = 10 * Nums.cos(angle);
              this.attachBurstFromPoint(point, Rand.yes());
            }
            super.readyFor(r);
          }
        };
        FX2.scale = 1.5f;
        FX2.position.set(-2, 2, 0);
        //sprites.add(FX2);
        
        final ShotFX FX3 = new ShotFX(FM) {
          float lastTime = 0;
          
          public void readyFor(Rendering r) {
            SS.attachPoint("fire", this.origin);
            final float time = Rendering.activeTime() * 3;
            if ((int) lastTime != (int) time) refreshShot();
            lastTime = time;
            super.readyFor(r);
          }
        };
        FX3.position.set(-2, 2, 0);
        FX3.origin.set(-1, 1, 0);
        FX3.target.set(-4, 4, 0);
        //sprites.add(FX3);
        
        PlayLoop.rendering().backColour = new Colour(0, 0, 1, 0);
      }


      protected void onRendering(Sprite sprite) {
        if (sprite.model() == CM) {
          final float f = sprite.fog, a = f * (1 - f) * 4;
          sprite.colour = Colour.transparency(a);
          sprite.fog = (f + 0.01f) % 1;
        }
        if (sprite.model() == SM) {
          final float progress = Rendering.activeTime() * 6 / 10f;
          sprite.setAnimation("descend", progress % 1, false);
          //sprite.setAnimation(AnimNames.FIRE, progress % 1, true);
          //sprite.rotation += 120 / Rendering.FRAMES_PER_SECOND;
        }
        if (sprite.model() == HUMAN_MODEL) {
          sprite.rotation += 120 / Rendering.FRAMES_PER_SECOND;
        }
      }
    };
    
    PlayLoop.setupAndLoop(
      debug, "stratos.graphics",
      DebugHumanSprites.class, Economy.class
    );
  }
}




