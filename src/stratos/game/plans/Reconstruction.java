

package stratos.game.plans;



//  Used to reconstitute the dead.  Wooo.

public class Reconstruction {
  
  
}

/*
    
    final Item recResult = treatResult(OT, TYPE_RECONSTRUCT, null);
    if (
      ofType(TYPE_RECONSTRUCT, null) &&
      patient.health.suspended() && (recResult == null || recResult.amount < 1)
    ) {
      treatDC = 20;
      majorSkill = ANATOMY;
      minorSkill = PHARMACY;
      
      accessory = Item.asMatch(REPLICANTS, patient);
      type = TYPE_RECONSTRUCT;
      if (recResult != null) result = recResult;
      return;
    }
  }
  
    
    if (type == TYPE_RECONSTRUCT) {
      //
      //  Gradually restore health.  Revive once in decent shape.
      final float maxHealth = patient.health.maxHealth();
      float regen = 5 * maxHealth * effect;
      if (verbose) {
        I.sayAbout(patient, "Regen effect/10 days: "+regen);
        I.sayAbout(patient, "Injury level: "+patient.health.injuryLevel());
      }
      patient.health.liftInjury(regen / MEDIUM_DURATION);
      if (
        patient.health.injuryLevel() < ActorHealth.REVIVE_THRESHOLD &&
        patient.health.isState(ActorHealth.STATE_SUSPEND)
      ) {
        patient.health.setState(ActorHealth.STATE_RESTING);
        patient.health.takeFatigue(maxHealth / 2f);
        reconstructFaculties(patient);
      }
      else patient.health.setState(ActorHealth.STATE_SUSPEND);
      depleteResult(patient, MEDIUM_DURATION);
      return true;
    }
  
  public static Item replicantFor(Actor patient) {
    final Batch <Item> matches = patient.gear.matches(SERVICE_TREAT);
    final Item treatResult = treatResult(matches, TYPE_RECONSTRUCT, null);
    if (treatResult != null) return null;

    float injury = patient.health.injuryLevel() - 0.5f;
    injury = Visit.clamp(injury * 2, 0, 1);
    final int quality = (int) (injury * 5);
    final Item ordered = Item.with(REPLICANTS, patient, 1, quality);
    
    return ordered;
  }
//*/