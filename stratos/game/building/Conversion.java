/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.building ;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.util.*;




public class Conversion implements Economy {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public Item raw[], out ;
  final public Skill skills[] ;
  final public float skillDCs[] ;
  final public Class venueType ;
  
  
  public String toString() {
    return raw[0]+" to "+out ;
  }
  

  public Conversion(Object... args) {
    //
    //  Initially, we record raw materials first, and assume a default
    //  quantity of 1 each (this is also the default used for skill DCs.)
    //allConversions.add(this) ;
    float num = 1 ;
    boolean recRaw = true ;
    //
    //  Set up temporary storage variables.
    String venueName = null ;
    Item out = null ;
    Batch rawB = new Batch(), skillB = new Batch() ;
    Batch rawN = new Batch(), skillN = new Batch() ;
    //
    //  Iterate over all arguments-
    for (Object o : args) {
      if (o instanceof Integer) num = (Integer) o ;
      else if (o instanceof Float) num = (Float) o ;
      else if (o instanceof String) venueName = (String) o ;
      else if (o instanceof Skill) { skillB.add(o) ; skillN.add(num) ; }
      else if (o == TO) recRaw = false ;
      else if (o instanceof Service) {
        if (recRaw) { rawB.add(o) ; rawN.add(num) ; }
        else { out = Item.withAmount((Service) o, num) ; }
      }
    }
    //
    //  Then assign the final tallies-
    int i ;
    raw = new Item[rawB.size()] ;
    for (i = 0 ; i < rawB.size() ; i++) {
      raw[i] = Item.withAmount(
        (Service) rawB.atIndex(i), (Float) rawN.atIndex(i)
      ) ;
      //if (raw[i].quality == -1) I.complain(raw[i]+" HAS NO QUALITY!") ;
    }
    this.out = out ;
    this.venueType = venueClass(venueName);
    skills = (Skill[]) skillB.toArray(Skill.class) ;
    skillDCs = Visit.fromFloats(skillN.toArray()) ;
  }
  
  
  static Class venueClass(String venueName) {
    if (venueName == null) return null;
    try { return Class.forName("stratos.game.base."+venueName) ; }
    catch (ClassNotFoundException e) {
      I.say("WARNING: No class in stratos.game.base: "+venueName) ;
    }
    return null;
  }
  
  
  static Conversion[] parse(Object args[][]) {
    Conversion c[] = new Conversion[args.length] ;
    for (int i = c.length ; i-- > 0 ;) c[i] = new Conversion(args[i]) ;
    return c ;
  }
  
  
  /**  Various save/load utility methods (may have to rework this later.)
    */
  private Conversion(Session s) throws Exception {
    raw = new Item[s.loadInt()] ;
    for (int i = 0 ; i < raw.length ; i++) {
      raw[i] = Item.loadFrom(s) ;
    }
    out = Item.loadFrom(s) ;
    skills = new Skill[s.loadInt()] ;
    skillDCs = new float[skills.length] ;
    for (int i = 0 ; i < skills.length ; i++) {
      skills[i] = (Skill) Abilities.ALL_TRAIT_TYPES[s.loadInt()] ;
      skillDCs[i] = s.loadFloat() ;
    }
    venueType = s.loadClass() ;
  }
  
  
  public static Conversion loadFrom(Session s) throws Exception {
    return new Conversion(s) ;
  }
  
  
  private void saveState(Session s) throws Exception {
    s.saveInt(raw.length) ;
    for (Item i : raw) Item.saveTo(s, i) ;
    Item.saveTo(s, out) ;
    s.saveInt(skills.length) ;
    for (int i = 0 ; i < skills.length ; i++) {
      s.saveInt(skills[i].traitID) ;
      s.saveFloat(skillDCs[i]) ;
    }
    s.saveClass(venueType) ;
  }
  
  
  public static void saveTo(Session s, Conversion c) throws Exception {
    c.saveState(s) ;
  }
  
  
  protected int rawPriceValue() {
    int sum = 0 ; for (Item i : raw) sum += i.type.basePrice ;
    return sum ;
  }
}




