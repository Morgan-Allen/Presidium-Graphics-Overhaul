

package stratos.game.campaign;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.civilian.*;
import stratos.util.*;



public class BaseSetup {
  
  
  
  
  public static void establishRelations(Series <? extends Actor>... among) {
    for (Series <? extends Actor> sF : among) for (Actor f : sF) {
      for (Series <? extends Actor> tF : among) for (Actor t : tF) {
        if (f == t || f.memories.hasRelation(t)) continue;
        
        float initRelation = 0 ;
        for (int n = 10 ; n-- > 0 ;) {
          initRelation += Dialogue.tryChat(f, t) ;
        }
        f.memories.setRelation(t, initRelation, Rand.num()) ;
      }
    }
  }
  
  
  /*
  //  TODO:  Humans in general might want a method like this, during the setup
  //  process.
  public static void establishRelations(Venue venue) {
    
    final World world = venue.world() ;
    final Batch <Actor>
      from = new Batch <Actor> (),
      to = new Batch <Actor> () ;
    for (Actor a : venue.personnel.residents()) from.add(a) ;
    for (Actor a : venue.personnel.workers()) from.add(a) ;
    
    final Batch <Venue> nearby = new Batch <Venue> () ;
    world.presences.sampleFromKey(venue, world, 5, nearby, Venue.class) ;
    for (Venue v : nearby) {
      for (Actor a : v.personnel.residents()) to.add(a) ;
      for (Actor a : v.personnel.workers()) to.add(a) ;
    }
    
    for (Actor f : from) for (Actor t : to) {
      float initRelation = 0 ;
      for (int n = 10 ; n-- > 0 ;) {
        initRelation += Dialogue.tryChat(f, t) * 10 ;
      }
      f.memories.initRelation(t, initRelation, Rand.num()) ;
    }
  }
  //*/
}





