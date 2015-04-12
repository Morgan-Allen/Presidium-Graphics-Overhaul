

package stratos.game.base;
import stratos.game.common.*;
import stratos.util.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.base.Sectors.*;



//  TODO:  Merge this with your work on sector-states.
//  TODO:  Merge this with the Offworld class!


public class Setting {
  
  
  final static Setting SETTING = new Setting();
  final Table <Object, Table <Object, Float>> relations = new Table();
  
  
  private Setting() {
    //  TODO:  Finish these up, and create some dedicated classes for the
    //  purpose.
    setRelations(Base.KEY_ARTILECTS, -1.0f, true, (Object[]) ALL_PLANETS);
    setRelations(Base.KEY_VERMIN   , -0.5f, true, (Object[]) ALL_PLANETS);
    setRelations(Base.KEY_NATIVES  ,  0.0f, true, (Object[]) ALL_PLANETS);
    setRelations(Base.KEY_WILDLIFE ,  0.2f, true, (Object[]) ALL_PLANETS);
    
    //  House Altair-
    //    Enemies:  Rigel-Procyon and Taygeta, Hive Urym
    //    Allies:  Fomalhaut and Calivor
    //    Bonus to Commoner relations, penalty to Noble and Native relations
    setRelations(PLANET_HALIBAN, false,
      Base.KEY_NATIVES  , -0.25f,
      Base.KEY_ARTILECTS, -0.5f ,
      Base.KEY_WILDLIFE ,  0.0f ,
      
      PLANET_AXIS_NOVENA  , -0.25f,
      PLANET_PAREM_V      , -0.5f ,
      PLANET_URYM_HIVE    , -0.25f,
      PLANET_SOLIPSUS_VIER,  0.25f,
      PLANET_CALIVOR      ,  0.5f
    );
    
    //  House Suhail-
    //    Enemies:  Rigel-Procyon and Fomalhaut, Hive Urym
    //    Allies:  Ophiuchus-Rana
    //    Bonus to Native relations, penalty to Merchant relations
    setRelations(PLANET_ASRA_NOVI, false,
      Base.KEY_NATIVES  ,  0.4f ,
      Base.KEY_ARTILECTS, -0.75f,
      Base.KEY_WILDLIFE ,  0.2f ,
      
      PLANET_SOLIPSUS_VIER, -0.25f,
      PLANET_PAREM_V      , -0.5f ,
      PLANET_URYM_HIVE    , -0.25f,
      PLANET_NORUSEI      ,  0.5f
    );
    
    //  House Rigel-Procyon-
    //    Enemies:  Altair and Fomalhaut, Calivor
    //    Allies:  Hive Urym
    //    Bonus to Artilect and Noble relations, penalty to Commoner relations
    setRelations(PLANET_PAREM_V, false,
      Base.KEY_NATIVES  ,  0.0f ,
      Base.KEY_ARTILECTS,  0.25f,
      Base.KEY_WILDLIFE , -0.2f ,
      
      PLANET_HALIBAN      , -0.5f ,
      PLANET_SOLIPSUS_VIER, -0.25f,
      PLANET_URYM_HIVE    ,  0.65f
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
  
  
  void setRelations(
    Object a, float value, boolean symmetric, Object... others
  ) {
    for (Object k : others) {
      setRelation(a, k, value, symmetric);
    }
  }
  
  
  
  /**  Public queries/access methods-
    */
  public static float defaultRelations(Base base, Base other) {
    final Object BK = keyFor(base), OK = keyFor(other);
    final Table BR = SETTING.relations.get(BK);
    if (BR == null) return 0;
    final Float val = (Float) BR.get(OK);
    return val != null ? val : 0;
  }
  
  
  private static Object keyFor(Base base) {
    if (base.isNative()) return Base.KEY_NATIVES;
    if (base.primal) return base.title();
    final Object home = base.commerce.homeworld();
    if (home != null) return home;
    return Base.KEY_FREEHOLD;
  }
}










