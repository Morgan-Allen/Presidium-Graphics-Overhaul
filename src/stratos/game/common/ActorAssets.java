

package stratos.game.common;
import stratos.graphics.common.*;
import stratos.graphics.sfx.Label;
import stratos.graphics.sfx.TalkFX;



public class ActorAssets {
  
  
  
  final public static TalkFX.TalkModel TALK_MODEL = new TalkFX.TalkModel(
    "model_talk_basic", ActorAssets.class,
    "media/GUI/", "FontVerdana.xml", "textBubble.png"
  );
  
  final public static Label.LabelModel LABEL_MODEL = new Label.LabelModel(
    "model_label_basic", ActorAssets.class,
    "media/GUI/", "FontVerdana.xml"
  );
  

}
