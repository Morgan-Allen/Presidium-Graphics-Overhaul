

package stratos.game.actors;
import static stratos.game.actors.Qualities.*;
import stratos.util.*;
import java.util.Map.Entry;



//  TODO:  See if this can't be simplified still further?

/*
TODO:  USE THESE
  Avarice    vs. Generosity
  Assured    vs. Nervous
  Aggressive vs. Empathic
  Honourable vs. Cunning
  Dutiful    vs. Subversive
  Whimsical  vs. Stubborn
  Curious    vs. Dull
  Sloth      vs. Restless
  Private    vs. Friendly
  Cold       vs. Passionate
  Indulgent  vs. Ascetic
  Meticulous vs. Impatient
  Civilised  vs. Wild Heart
  Creative   vs. Dogmatic
  Critical   vs. Blithe
  Ambitious  vs. Humble


Aggressive  vs. Empathic    (helping or harming others)
Ambitious   vs. Humble      (desire for promotion)
Greedy      vs. Generous    (give or receive money/gifts)
Solitary    vs. Outgoing    (conversation, group activity)
Critical    vs. Friendly    (politeness vs. accuracy)  <REDUNDANT?>
Cold        vs. Passionate  (mood-swings and relations)
Relaxed     vs. Restless    (sleep vs. physical exercise)
Cautious    vs. Fearless    (sensitivity to danger)

TODO:  <CROSS OVER HERE>

Truthful    vs. Deceptive             <REDUNDANT?>
Honourable  vs. Cunning     (use of deceit or cheap shots)
Dutiful     vs. Subversive  (conformity to authority)
Ignorant    vs. Curious     (gossip, exploring and study)
Stubborn    vs. Whimsical   (activity-persistance)
Traditional vs. Creative    (appeal of aesthetic novelty)
Urbane      vs. Naturalist  (appeal of civilisation)
Ascetic     vs. Indulgent   (appeal of vice and gluttony)

//*/

//  ...Wait a second.  Based on anecdotal experience, half of these are
//  bullshit.  Correlations should be weaker, at least.

public class Personality {
  
  final private static Trait TRAIT_MATRIX[][] = {
    {AMBITIOUS, ENERGETIC, OUTGOING   }, {HUMBLE, RELAXED, SOLITARY      },
    {CREATIVE, CURIOUS, IMPULSIVE     }, {TRADITIONAL, IGNORANT, STUBBORN},
    {SUBVERSIVE, NATURALIST, INDULGENT}, {DUTIFUL, URBANE, ABSTINENT     },
    {CRUEL, DISHONEST, ACQUISITIVE    }, {EMPATHIC, ETHICAL, GENEROUS   },
    {DEFENSIVE, NERVOUS, CRITICAL     }, {CALM, FEARLESS, POSITIVE       },
    //                <vertical associations swap here>
    {           EXCITABLE             }, {          IMPASSIVE            }
  },    YANG[] = TRAIT_MATRIX[10]      ,     YIN[] = TRAIT_MATRIX[11];
  
  final private static int NUM_ROWS = 10;
  final private static Table <Trait, Table> correlates = new Table();
  
  
  protected static Trait[] setupRelations(Trait personalityTraits[]) {
    for (int row = 0; row < NUM_ROWS; row++) {
      final Trait bracket[] = TRAIT_MATRIX[row];
      final boolean even = row % 2 == 0;
      final int invertIndex = row + (even ? 1 : -1);
      final Trait
        inverts[] = TRAIT_MATRIX[invertIndex],
        parents[] = even ? YANG : YIN;
      
      //  Here, we ascertain which trait brackets are considered adjacent, with
      //  the top and bottom rows (extraversion and neuroticism) being glued
      //  together, but in reverse-
      int aboveIndex = row - 2, belowIndex = row + 2;
      if (aboveIndex < 0) aboveIndex = (invertIndex + NUM_ROWS - 2) % NUM_ROWS;
      final Trait above[] = TRAIT_MATRIX[aboveIndex];
      if (belowIndex >= NUM_ROWS) belowIndex = (invertIndex + 2) % NUM_ROWS;
      final Trait below[] = TRAIT_MATRIX[belowIndex];
      
      for (int column = 0; column < bracket.length; column++) {
        
        //  Here, we set up horizontal correlations between traits within the
        //  same personality bracket, including negative correlations with
        //  those opposite.
        final Trait trait = bracket[column], invert = inverts[column];
        for (Trait t : bracket) {
          if (t == trait) correlate(trait, t, 1);
          correlate(trait, t, 0.5f);
        }
        for (Trait t : inverts) {
          if (t == invert) correlate(trait, t, -1);
          else correlate(trait, t, -0.5f);
        }
        
        //  Then, we set up vertical correlations between the adjacent trait
        //  categories-
        for (Trait t : above) correlate(trait, t, 0.25f);
        for (Trait t : below) correlate(trait, t, 0.25f);
        
        //  And finally, we treat the last row a little differently-
        for (Trait t : parents) {
          correlate(trait, t, 0.125f);
          correlate(t, trait, 0.125f);
        }
      }
    }
    
    //  Do some final housekeeping, compile the results, and return-
    for (int i = YANG.length; i-- > 0;) {
      correlate(YANG[i], YIN[i], -1);
      correlate(YIN[i], YANG[i], -1);
    }
    for (Trait t : personalityTraits) {
      final Table <Trait, Float> TC = correlates.get(t);
      if (TC == null) I.complain("NO CORRELATIONS FOR "+t+"!");
      final Trait c[] = new Trait[TC.size()];
      final float w[] = new float[TC.size()];
      
      int i = 0; for (Entry <Trait, Float> e : TC.entrySet()) {
        c[i] = e.getKey();
        w[i] = e.getValue();
        i++;
      }
      t.assignCorrelates(c, w);
    }
    correlates.clear();
    return personalityTraits;
  }
  
  
  private static void correlate(Trait t, Trait other, float weight) {
    Table <Trait, Float> TC = correlates.get(t);
    if (TC == null) correlates.put(t, TC = new Table <Trait, Float> ());
    final Float w = TC.get(other);
    if (w == null) TC.put(other, weight);
    else TC.put(other, w + weight);
  }
  
  
  
  /**  Development methods-
    */
  public static float traitChance(Trait t, Actor a) {
    final Trait cA[] = t.correlates();
    if (cA == null) return 0;
    
    float plus = 0, minus = 0;
    final float wA[] = t.correlateWeights();
    
    for (int n = cA.length; n-- > 0;) {
      final Trait c = cA[n];
      final float w = wA[n];
      final float level = a.traits.traitLevel(c) * w;
      if (level > 0) plus  += (1 - plus ) * level;
      if (level < 0) minus -= (1 - minus) * level;
    }
    
    return plus - minus;
  }
}














