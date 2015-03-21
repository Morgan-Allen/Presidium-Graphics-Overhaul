

package stratos.game.wild;

import stratos.graphics.common.ModelAsset;
import stratos.graphics.solids.MS3DModel;


public class Tesseract {
  
  
  final public static ModelAsset
    MODEL_TESSERACT = MS3DModel.loadFrom(
      Artilect.FILE_DIR, "Tesseract.ms3d", Species.class,
      Artilect.XML_FILE, "Tesseract"
    )
  ;
}
