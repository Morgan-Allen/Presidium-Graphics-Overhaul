

package src.start;
import src.game.building.Economy;
import src.graphics.common.*;
import src.graphics.cutout.*;
import src.graphics.solids.*;
import src.graphics.sfx.*;
import src.util.*;

import org.apache.commons.math3.util.FastMath;



public class DebugGraphics {
  
  
  final static CutoutModel
    CM = CutoutModel.fromImage(
      "media/Buildings/military/bastion.png",
      DebugGraphics.class, 7, 5
    ),
    VM = CutoutModel.fromImage(
      "media/Buildings/merchant/stock_exchange.png",
      DebugGraphics.class, 4, 2
    );
  final static MS3DModel
    SM = MS3DModel.loadFrom(
      "media/Actors/fauna/", "Micovore.ms3d",
      DebugGraphics.class, "FaunaModels.xml", "Micovore"
    );
  final static ShotFX.Model
    FM = new ShotFX.Model(
      "laser_beam_fx", DebugGraphics.class,
      "media/SFX/blast_beam.gif",
      0.05f, 0,
      0.5f, 3, true, true
    );
  
  
  public static void main(String args[]) {

    PlayLoop.setupAndLoop(new VisualDebug() {
      protected void loadVisuals() {
        final Sprite SS = SM.makeSprite();
        sprites.add(SS);
        
        for (int i = 10 ; i-- > 0;) {
          final Sprite CS = CM.makeSprite();
          CS.position.set(i, -i, 0);
          CS.fog = (i + 1) / 10f;
          CS.colour = Colour.transparency(CS.fog);
          CS.scale = 0.5f;
          sprites.add(CS);
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
          Economy.FUEL_RODS.model, 15, -1.5f, -1.5f
        );
        BS.updateCondition(0.0f, false, false);
        sprites.add(BS);
        
        final TalkFX FX1 = new TalkFX() {
          int count = 0;
          public void update() {
            if (this.numPhrases() < 2) {
              addPhrase("Testing "+(count++), TalkFX.FROM_LEFT);
            }
            super.update();
          }
        };
        FX1.position.set(0, 0, 2);
        sprites.add(FX1);
        
        final ShieldFX FX2 = new ShieldFX() {
          public void update() {
            if (Rand.index(Rendering.FRAMES_PER_SECOND) <= 2) {
              final Vec3D point = new Vec3D();
              final float angle = (float) (Math.PI * 2 * Rand.num());
              point.x = 10 * (float) FastMath.sin(angle);
              point.y = 10 * (float) FastMath.cos(angle);
              this.attachBurstFromPoint(point, Rand.yes());
            }
            super.update();
          }
        };
        FX2.scale = 1.5f;
        FX2.position.set(-2, 2, 0);
        sprites.add(FX2);
        
        final ShotFX FX3 = new ShotFX(FM) {
          public void update() {
            super.update();
            if (Rand.index(Rendering.FRAMES_PER_SECOND) <= 1) {
              refreshShot();
            }
          }
        };
        FX3.position.set(-2, 2, 0);
        FX3.origin.set(-2, 1, 0);
        FX3.target.set(-2, 4, 0);
        sprites.add(FX3);
      }
      
      
      protected void onRendering(Sprite sprite) {
        if (sprite.model() == CM) {
          final float f = sprite.fog, a = f * (1 - f) * 4;
          sprite.colour = Colour.transparency(a);
          sprite.fog = (f + 0.01f) % 1;
          sprite.rotation += 90 / 60f;
        }
        if (sprite.model() == SM) {
          final float progress = Rendering.activeTime() * 6 / 10f;
          sprite.setAnimation(AnimNames.MOVE, progress % 1);
        }
      }
    });
  }
}






