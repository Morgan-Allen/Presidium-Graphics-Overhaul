

package stratos.game.campaign;
import stratos.game.base.*;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.util.*;



//  TODO:  You must create a location interface.
//  Cash.  Promotion.  Artifact.
//  Policy.  Pardon.  Marriage.
//  Declare Mission.
//  Under Orders.

//  TODO:  These should mediate trade & defence dynamics.
//  Vassal.  Liege.  Alliance.
//  Vendetta.  Rebel.  Uprising.
//  Closed.  Neutral.  Trading.

//  Assign personnel to strike, recon, security or contact duties.



public class BaseDemands implements Economy {
  
  
  Location location, nearby;
  float population;
  int climate;
  
  final Tally <Object>
    demand = new Tally <Object> (),
    supply = new Tally <Object> ();
  Table <Object, Venue>
    samples = new Table <Object, Venue> ();
  
  
  //  Population x housing level determines fundamental demand for various
  //  goods.
  //  Danger & squalor create demands for security classes & life support or
  //  ambience structures.
  //  Presence of personnel and structures creates base-line demand for parts,
  //  devices, outfits, et cetera.
  
  
  void updateDemands() {
    demand.clear();
    
    //  TODO:  In principle, the system below should be adaptable to providing
    //  housing as well, if you can represent that as an output Service?
    //  TODO:  Multiple passes may be needed here to cover multiple steps in
    //  the supply chain, unless you order the conversions carefully.
    
    for (Background b : Background.allBackgrounds()) {
      final int incomeBracket = b.standing;
      final float population = supply.valueFor(b);
      final int upgradeLevel = (incomeBracket - 1) * 2;
      
      final Conversion c = HoldingUpgrades.materials(upgradeLevel);
      for (Item i : c.raw) {
        demand.add(population * i.amount, i.type);
      }
    }
    
    //
    //  Now, for each good being demanded, find conversions which produce it,
    //  and increment demand for (A) their raw materials, (B) their production
    //  facilities, and (C) their associated skills.
    for (Conversion c : Conversion.allConversions()) {
      float needed = demand.valueFor(c.out.type);
      needed /= c.out.amount;
      
      demand.add(needed, c.facility);
      for (Item i : c.raw) demand.add(i.amount * needed, i.type);
      for (Skill s : c.skills) demand.add(needed, s);
    }
    
    
    //
    //  TODO:  Two other major subsystems need to be incorporated here- trade
    //  and defence policy.
    
    
    //
    //  Finally, increment demand for particular vocations, based on the skills
    //  they afford.
    for (Background b : Background.allBackgrounds()) {
      float sumNeed = 0;
      for (Skill s : b.skills()) sumNeed += demand.valueFor(s);
      demand.add(sumNeed, b);
    }
    
    //
    //  TODO:  Weight toward certain facilities and policies based on
    //  availability of resources and personality of the base's ruling body.
  }
  
  
}




/*
demand.add(population, SERVICE_HOUSING);

//  TODO:  You need to increase demand for the various housing types, based
//         on shortage of the housing service.
//  ...the type of housing depends on income levels, which in turn depends
//  on the level of various vocations (and their associated pay grades.)
//  Personnel will also need food, along with devices/outfits.

for (Object o : Installations.housingTypes()) {
  final Holding h = (Holding) samples.get(o);
  float amountH = supply.valueFor(o);
  
  for (Service s : HoldingUpgrades.MATERIALS[h.upgradeLevel()]) {
    demand.add(amountH, s);
  }
}
//*/




