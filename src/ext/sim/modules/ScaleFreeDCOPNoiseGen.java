package ext.sim.modules;

import bgu.dcr.az.api.ano.Register;
import bgu.dcr.az.api.prob.Problem;
import java.util.Random;

/**
 * Scale free DCOP (symmetric) with Noise (dust) addition or only with costs multiplied by a 1000 (for UC)
 *
 * Used the Barabasi-Albert model
 * Needs a large network to get the scale free effect
 */

/* //**********
//NOTE: this problem generator creates a connected Scale Free problem, with NOISE addition

These comment lines are in case you are using unary constraints. Meaning, in case you want
to break ties by adding personal preferences (AKA unary constraints), in this class -
you should only multiply the cost of each constraint by a 1000. The random integer will be added later
via the "VariableNode" class.

//********** */

@Register(name = "dcop-scale-free-noise")
public class ScaleFreeDCOPNoiseGen extends ScaleFreeDCOPGen {

    protected void buildConstraint(int i, int j, Problem p, Random rand) {
        for (int vi = 0; vi < p.getDomain().size(); vi++) {
            for (int vj = 0; vj < p.getDomain().size(); vj++) {
                final int cost = minCost + rand.nextInt(maxCost);
                int noiseCost = cost*1000; //See comment below
                p.setConstraintCost(i, vi, j, vj, noiseCost);
                p.setConstraintCost(j, vj, i, vi, noiseCost);
            }
        }
    }
    
	/*
	 * Without the use of UC (need to add dust to the constraints directly):
	 * 	int noiseCost = cost*1000 + rand.nextInt(20); 
	 * 
	 * With the use of UC (the tie breaking will occur through the preferences):
	 *  int noiseCost = cost*1000; 
	 */
   
}
