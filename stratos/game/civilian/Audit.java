/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.civilian ;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.user.*;
import stratos.util.*;



public class Audit extends Plan implements Economy {
  
  
  
  /**  Data fields, constructors and save/load functions-
    */
  final public static float
    MILITANT_BONUS   = 2.0f,
    MILITANT_RATION  = 50,
    RULER_STIPEND    = 1000,
    BASE_BRIBE_SIZE  = 50 ;
  
  
  final static int
    STAGE_EVAL   = -1,
    STAGE_AUDIT  =  0,
    STAGE_REPORT =  1,
    STAGE_DONE   =  2 ;
  
  private static boolean verbose = false ;
  
  
  private int stage = STAGE_EVAL ;
  private Venue audited ;
  private float balance = 0 ;
  
  public int checkBonus = 0 ;
  
  
  public Audit(Actor actor, Venue toAudit) {
    super(actor, toAudit) ;
    this.audited = toAudit ;
  }
  
  
  public Audit(Session s) throws Exception {
    super(s) ;
    stage = s.loadInt() ;
    audited = (Venue) s.loadObject() ;
    balance = s.loadFloat() ;
    checkBonus = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(stage) ;
    s.saveObject(audited) ;
    s.saveFloat(balance) ;
    s.saveInt(checkBonus) ;
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  public float priorityFor(Actor actor) {
    return ROUTINE ;
  }
  
  
  public static Venue nextToAuditFor(Actor actor) {
    
    final World world = actor.world() ;
    final Venue work = (Venue) actor.mind.work() ;
    final Batch <Venue> batch = new Batch <Venue> () ;
    world.presences.sampleFromKey(work, world, 10, batch, work.base()) ;
    
    Venue picked = null ;
    float bestRating = 0, rating ;
    for (Venue v : batch) {
      //if (v.privateProperty()) continue ;
      rating = Math.abs(v.inventory().credits()) / 100f ;
      rating -= Plan.rangePenalty(v, actor) ;
      rating -= Plan.competition(Audit.class, v, actor) ;
      if (rating > bestRating) { bestRating = rating ; picked = v ; }
    }
    //I.sayAbout(actor, "Chosen for audit: "+picked) ;
    return picked ;
  }
  
  
  public static Venue nearestAdminFor(Actor actor, boolean welfare) {
    final World world = actor.world() ;
    final Upgrade WS = AuditOffice.RELIEF_AUDIT ;
    
    for (Object o : world.presences.sampleFromKey(
      actor, world, 5, null, SERVICE_ADMIN
    )) {
      final Venue v = (Venue) o ;
      if (v.base() != actor.base()) continue ;
      if (welfare && v.structure.upgradeBonus(WS) == 0) {
        continue ;
      }
      return v ;
    }
    return null ;
  }
  
  

  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    I.sayAbout(actor, "Getting next audit step... "+this.hashCode()) ;
    I.sayAbout(actor, "Stage was: "+stage) ;
    
    if (stage == STAGE_EVAL) {
      if (audited == null && Rand.num() > (Math.abs(balance) / 1000f)) {
        audited = nextToAuditFor(actor) ;
      }
      if (audited != null) stage = STAGE_AUDIT ;
      else stage = STAGE_REPORT ;
    }
    I.sayAbout(actor, "Stage is now: "+stage+", audited: "+audited) ;
    
    if (stage == STAGE_AUDIT) {
      final Action audit = new Action(
        actor, audited,
        this, "actionAudit",
        Action.TALK, "Auditing "+audited
      ) ;
      final Tile e = audited.mainEntrance() ;
      if (e == null || e.blocked()) {
        audit.setMoveTarget(Spacing.nearestOpenTile(audited.origin(), actor)) ;
      }
      return audit ;
    }
    if (stage == STAGE_REPORT) {
      final Action report = new Action(
        actor, actor.mind.work(),
        this, "actionFileReport",
        Action.TALK, "Filing Report"
      ) ;
      return report ;
    }
    return null ;
  }
  
  
  public boolean actionAudit(Actor actor, Venue audited) {
    final float balance = auditForBalance(actor, audited) ;
    this.balance += balance ;
    stage = STAGE_EVAL ;
    
    I.sayAbout(actor, "Just audited: "+audited) ;
    this.audited = null ;
    return true ;
  }
  
  
  public boolean actionFileReport(Actor actor, Venue office) {
    fileEarnings(actor, office, balance) ;
    stage = STAGE_DONE ;
    return true ;
  }
  
  
  public static void fileEarnings(Actor actor, Venue office, float balance) {
    final Base base = office.base() ;
    base.incCredits(balance) ;
    if (balance > 0) {
      office.chat.addPhrase((int) balance+" credits in profit") ;
    }
    if (balance < 0) {
      office.chat.addPhrase((int) (0 - balance)+" credits in debt") ;
    }
  }
  
  
  public static float auditForBalance(Actor audits, Venue venue) {
    
    if (venue.base() == null) return 0 ;
    
    float sumWages = 0, sumSalaries = 0, sumSplits = 0 ;
    
    final BaseProfiles BP = venue.base().profiles ;
    final int numW = venue.personnel.workers().size() ;
    final Profile profiles[] = new Profile[numW] ;
    final float salaries[] = new float[numW] ;
    
    int i = 0 ; for (Actor works : venue.personnel.workers()) {
      final Profile p = BP.profileFor(works) ;
      profiles[i] = p ;
      final int KR = BP.querySetting(AuditOffice.KEY_RELIEF) ;
      
      final float
        salary = p.salary(),
        relief = AuditOffice.RELIEF_AMOUNTS[KR],
        payInterval = p.daysSinceWageEval(venue.world()),
        wages = ((salary / Background.NUM_DAYS_PAY) + relief) * payInterval;
      
      if (verbose && I.talkAbout == audits) {
        I.say(works+" is due: "+wages+" over "+payInterval+" days") ;
        I.say("Salary: "+salary+", relief: "+relief) ;
        I.say("Wages ALREADY DUE: "+p.paymentDue()) ;
      }
      
      salaries[i++] = salary ;
      p.incPaymentDue(wages) ;
      sumWages += wages ;
      sumSalaries += salary ;
    }

    final float surplus = venue.stocks.credits() ;
    
    if (verbose && I.talkAbout == audits) {
      I.say(audits+" auditing "+venue+", surplus: "+surplus) ;
      I.say("Sum of salaries is: "+sumSalaries) ;
    }
    
    i = 0 ;
    if (surplus > 0 && sumSalaries > 0) for (Profile p : profiles) {
      final float split = salaries[i++] * surplus / (sumSalaries * 10) ;
      if (verbose && I.talkAbout == audits) {
        I.say(p.actor+" getting profit bonus: "+split) ;
      }
      
      p.incPaymentDue(split) ;
      sumSplits += split ;
    }
    
    final float balance = venue.stocks.credits() ;
    venue.stocks.incCredits(0 - balance) ;
    venue.stocks.taxDone() ;
    
    I.sayAbout(venue, "Balance is now: "+venue.stocks.credits()) ;
    return balance ;
    //  TODO:  Restore this in some form later.
    /*
    float
      balance = venue.stocks.credits(),
      waste = (Rand.num() + base.crimeLevel()) / 2f ;
    
    final float honesty =
      (audits.traits.traitLevel(HONOURABLE) / 2f) -
      (audits.traits.traitLevel(ACQUISITIVE) * waste) ;
    
    if (Rand.num() > (honesty + 1f) / 2) {
      waste *= 1.5f ;
      final float bribe = BASE_BRIBE_SIZE * waste ;
      balance -= bribe ;
      audits.gear.incCredits(bribe) ;
    }
    else {
      if (audits.traits.test(ACCOUNTING, 15, 5)) waste  = 0 ;
      if (audits.traits.test(ACCOUNTING, 5 , 5)) waste /= 2 ;
    }
    final int
      profit = (int) (balance / (1 + waste)),
      losses = (int) (balance * (1 + waste)) ;
    
    
    if (profit > 0) {
      venue.stocks.incCredits(0 - profit) ;
      venue.stocks.taxDone() ;
      return profit ;
    }
    if (losses < 0) {
      venue.stocks.incCredits(0 - losses) ;
      venue.stocks.taxDone() ;
      return losses ;
    }
    venue.stocks.taxDone() ;
    return 0 ;
    //*/
  }
  
  
  public static float taxesDue(Actor actor) {
    final int bracket = actor.vocation().standing ;
    if (bracket == Background.SLAVE_CLASS) return 0 ;
    if (bracket == Background.RULER_CLASS) return 0 ;
    
    final BaseProfiles BP = actor.base().profiles ;
    int taxLevel = 0 ;
    if (bracket == Background.LOWER_CLASS) {
      taxLevel = BP.querySetting(AuditOffice.KEY_LOWER_TAX) ;
    }
    if (bracket == Background.MIDDLE_CLASS) {
      taxLevel = BP.querySetting(AuditOffice.KEY_MIDDLE_TAX) ;
    }
    if (bracket == Background.LOWER_CLASS) {
      taxLevel = BP.querySetting(AuditOffice.KEY_UPPER_TAX) ;
    }
    final int
      percent = AuditOffice.TAX_PERCENTS[taxLevel],
      savings = AuditOffice.TAX_EXEMPTED[taxLevel] ;
    
    float afterSaving = actor.gear.credits() - savings ;
    if (afterSaving < 0) return 0 ;
    afterSaving = Math.min(afterSaving, actor.gear.unTaxed()) ;
    return afterSaving * percent / 100f ;
  }
  
  
  public static float propertyValue(Venue venue) {
    float value = 0 ;
    if (venue instanceof Holding) {
      final Holding home = (Holding) venue ;
      value += home.upgradeLevel() * 25 ;
      value += home.structure.buildCost() ;
    }
    return value ;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    if (stage == STAGE_AUDIT && audited != null) {
      d.append("Auditing: ") ;
      d.append(audited) ;
      d.append(" (Balance "+balance+")") ;
    }
    else if (stage == STAGE_REPORT) {
      d.append("Filing a financial report at ") ;
      d.append(actor.mind.work()) ;
      d.append(" (Balance "+balance+")") ;
    }
    else d.append("Auditing "+audited) ;
  }
}








