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

@Register(name = "dcop-scale-free-hard-cost-1ofD-noise")
public class ScaleFreeDCOPNoiseGen_HardConst extends ScaleFreeDCOPGen 
{
	
	//Add actual costs between variables
	protected void buildConstraint(int var1, int var2, Problem p, Random rand) 
	{
		for (int i = 0; i < p.getDomainSize(var1); i++) 
		{
			int j = rand.nextInt(p.getDomainSize(var2)); 
			p.setConstraintCost(var1, i, var2, j, maxCost*1000);
			p.setConstraintCost(var2, j, var1, i, maxCost*1000);	
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
