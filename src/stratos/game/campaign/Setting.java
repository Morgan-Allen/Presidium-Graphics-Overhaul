

package stratos.game.campaign;
import stratos.game.common.*;
import stratos.util.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.campaign.Sectors.*;



//  TODO:  Merge this with your work on sector-states.


public class Setting {
  
  
  final Table <Object, Table <Object, Float>> relations = new Table();
  
  
  public Setting() {
    //  TODO:  Finish these up, and create some dedicated classes for the
    //  purpose.
    
    //  House Altair-
    //    Enemies:  Rigel-Procyon and Taygeta, Hive Urym
    //    Allies:  Fomalhaut and Calivor
    //    Bonus to Commoner relations, penalty to Noble and Native relations
    setRelations(PLANET_HALIBAN, true,
      Base.KEY_NATIVES, -0.25f,
      Base.KEY_ARTILECTS, -0.5f,
      Base.KEY_WILDLIFE, 0.0f,
      
      PLANET_AXIS_NOVENA, -0.25f,
      PLANET_PAREM_V, -0.5f,
      PLANET_URYM_HIVE, -0.25f,
      PLANET_SOLIPSUS_VIER, 0.25f,
      PLANET_CALIVOR, 0.5f
    );
    
    //  House Suhail-
    //    Enemies:  Rigel-Procyon and Fomalhaut, Hive Urym
    //    Allies:  Ophiuchus-Rana
    //    Bonus to Native relations
    setRelations(PLANET_ASRA_NOVI, true,
      Base.KEY_NATIVES, 0.4f,
      Base.KEY_ARTILECTS, -0.75f,
      Base.KEY_WILDLIFE, 0.2f,
      
      PLANET_SOLIPSUS_VIER, -0.25f,
      PLANET_PAREM_V, -0.5f,
      PLANET_URYM_HIVE, -0.25f,
      PLANET_NORUSEI, 0.5f
    );
    
    //  House Rigel-Procyon-
    //    Enemies:  Altair and Fomalhaut, Calivor
    //    Allies:  Hive Urym
    //    Bonus to Artilect relations, penalty to Commoner relations
    setRelations(PLANET_PAREM_V, true,
      Base.KEY_NATIVES, 0.0f,
      Base.KEY_ARTILECTS, 0.25f,
      Base.KEY_WILDLIFE, -0.2f,
      
      PLANET_HALIBAN, -0.5f,
      PLANET_SOLIPSUS_VIER, -0.25f,
      PLANET_URYM_HIVE, 0.65f
    );
  }
  
  
  void setRelation(Object a, Object b, float value, boolean symmetric) {
    Table <Object, Float> AR = relations.get(a);
    if (AR == null) relations.put(a, AR = new Table());
    AR.put(b, value);
    if (symmetric) setRelation(b, a, value, false);
  }
  
  
  void setRelations(Object a, boolean symmetric, Object... tableVals) {
    final Table vals = Table.make(tableVals);
    for (Object k : vals.keySet()) {
      final Object v = vals.get(k);
      if (v instanceof Float) {
        setRelation(a, k, (Float) v, symmetric);
      }
      else I.complain("ILLEGAL RELATION TYPE: "+v+" FOR "+k);
    }
  }
  
  
  //  TODO:  Make use of these?
  public void saveState(Session s) throws Exception {
  }
  
  
  public void loadState(Session s) throws Exception {
  }
  
  
  
  /**  Public queries/access methods-
    */
  public float defaultRelations(Base base, Base other) {
    final Table BR = relations.get(keyFor(base));
    if (BR == null) return 0;
    final Float val = (Float) BR.get(keyFor(other));
    return val != null ? val : 0;
  }

  
  private static Object keyFor(Base base) {
    if (base.primal) return base.title;
    final Object home = base.commerce.homeworld();
    if (home != null) return home;
    return Base.KEY_FREEHOLD;
  }
}






