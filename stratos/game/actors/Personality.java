


package stratos.game.actors;
import static stratos.game.actors.Qualities.*;
import stratos.util.*;
import java.util.Map.Entry;



/*  I'm basing this system (loosely) on the Big 5 personality traits and some
proposed (loose) correlations between them:

http://www.researchgate.net/publication/40455341_The_nature_and_structure_of_co
       rrelations_among_Big_Five_ratings_the_halo-alpha-beta_model/file/9fcfd50
       9b3e067d9ef.pdf
http://www.psychology.iastate.edu/faculty/caa/abstracts/2010-2014/12BA.pdf

I'm not a psychologist, please don't sue me, I just work here

ON TO THE DETAILS
  Personality traits:  Extraversion, Neophilia, Amorality, Self Concern and
  Neuroticism, from top to bottom.
  
  [Ambitious, Energetic, Outgoing   ] vs. [Humble, Relaxed, Solitary     ]
  [Creative, Curious, Impulsive     ] vs. [Traditional, Simple, Stubborn ]
  [Subversive, Naturalist, Indulgent] vs. [Dutiful, Meticulous, Abstinent]
  [Cruel, Dishonest, Acquisitive    ] vs. [Empathic, Truthful, Generous  ]
  [Defensive, Nervous, Critical     ] vs. [Calm, Fearless, Positive      ]
           Excitable (Yang)           vs.        Impassive (Yin)          
  
  Entries correlate above and below (switching at top-to-bottom), negatively
  with those opposite, and positively within a given bracket.
//*/
public class Personality {
  
  final private static Trait TRAIT_MATRIX[][] = {
    {AMBITIOUS, ENERGETIC, OUTGOING   }, {HUMBLE, RELAXED, SOLITARY     },
    {CREATIVE, CURIOUS, IMPULSIVE     }, {TRADITIONAL, SIMPLE, STUBBORN },
    {SUBVERSIVE, NATURALIST, INDULGENT}, {DUTIFUL, METICULOUS, ABSTINENT},
    {CRUEL, DISHONEST, ACQUISITIVE    }, {EMPATHIC, TRUTHFUL, GENEROUS  },
    {DEFENSIVE, NERVOUS, CRITICAL     }, {CALM, FEARLESS, POSITIVE      },
    //                <vertical associations swap here>
    {           EXCITABLE             }, {         IMPASSIVE            }
  },    YANG[] = TRAIT_MATRIX[10]      ,    YIN[] = TRAIT_MATRIX[11];
  
  final private static int NUM_ROWS = 10;
  final private static Table <Trait, Table> correlates = new Table();
  
  
  protected static Trait[] setupRelations(Trait personalityTraits[]) {
    for (int row = 0; row < NUM_ROWS ; row++) {
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
    for (int i = YANG.length ; i-- > 0;) {
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
}











