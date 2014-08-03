

package stratos.game.campaign;

import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.civilian.*;
import stratos.start.Assets;
import stratos.util.*;
import stratos.game.plans.DialogueUtils;

import java.lang.reflect.Constructor;



public class BaseSetup {
  
  
  private static Class           allFT[] = null;
  private static FacilityProfile allFP[] = null;
  
  public static Class[] facilityTypes() {
    if (allFT != null) return allFT;
    
    final Batch <Class>           allTypes    = new Batch();
    final Batch <FacilityProfile> allProfiles = new Batch();
    
    for (Class baseClass : Assets.loadPackage("stratos.game.base")) {
      if (! Installation.class.isAssignableFrom(baseClass)) continue;
      allTypes.add(baseClass);
      
      if (! Venue.class.isAssignableFrom(baseClass)) continue;
      try {
        final Constructor c = baseClass.getConstructor(Base.class);
        final Venue v = (Venue) c.newInstance((Base) null);
        allProfiles.add(v.profile);
      }
      catch (Exception e) {}// I.report(e); continue; }
    }
    
    allFT = allTypes   .toArray(Class          .class);
    allFP = allProfiles.toArray(FacilityProfile.class);
    return allFT;
  }
  
  
  public static FacilityProfile[] facilityProfiles() {
    facilityTypes();
    return allFP;
  }
  
  
  public static void establishRelations(Series <? extends Actor>... among) {
    for (Series <? extends Actor> sF : among) for (Actor f : sF) {
      for (Series <? extends Actor> tF : among) for (Actor t : tF) {
        if (f == t || f.relations.hasRelation(t)) continue;
        
        float initRelation = 0;
        for (int n = 10; n-- > 0;) {
          initRelation += DialogueUtils.tryChat(f, t);
        }
        f.relations.setRelation(t, initRelation, Rand.num());
      }
    }
  }
  
  
  /*
  //  TODO:  Humans in general might want a method like this, during the setup
  //  process.
  public static void establishRelations(Venue venue) {
    
    final World world = venue.world();
    final Batch <Actor>
      from = new Batch <Actor> (),
      to = new Batch <Actor> ();
    for (Actor a : venue.personnel.residents()) from.add(a);
    for (Actor a : venue.personnel.workers()) from.add(a);
    
    final Batch <Venue> nearby = new Batch <Venue> ();
    world.presences.sampleFromKey(venue, world, 5, nearby, Venue.class);
    for (Venue v : nearby) {
      for (Actor a : v.personnel.residents()) to.add(a);
      for (Actor a : v.personnel.workers()) to.add(a);
    }
    
    for (Actor f : from) for (Actor t : to) {
      float initRelation = 0;
      for (int n = 10; n-- > 0;) {
        initRelation += Dialogue.tryChat(f, t) * 10;
      }
      f.memories.initRelation(t, initRelation, Rand.num());
    }
  }
  //*/
}





