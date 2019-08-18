package ext.sim.modules;

import java.util.Random;


import bgu.dcr.az.api.Agt0DSL;
import bgu.dcr.az.api.ano.Register;
import bgu.dcr.az.api.ds.ImmutableSet;
import bgu.dcr.az.api.prob.Problem;
import bgu.dcr.az.api.prob.ProblemType;

//**********
//NOTE: this problem generator creates a connected DCOP problem, with noise addition
//if you want to use the standard no-noise edition, you can use the built-in generator "dcop-connected"
//This one does exactly the same as "dcop-connected", except it multiplies each constraint by a 1000 
//(used for adding unary constraints later via the VariableNode class)
//**********

@Register(name="dcop-connected-noise")
public class DCOPConnectedNoiseGen extends GeneralDCOPGen {

	public void generate(Problem p, Random rand) {
		p.initialize(ProblemType.DCOP, n, new ImmutableSet<Integer>(Agt0DSL.range(0, d - 1)));
		addConstraints(p, rand);
		addConnectivity(p, rand);
		for (int i = 0; i < p.getNumberOfVariables(); i++) {
			for (int j = i+1; j < p.getNumberOfVariables(); j++) {
				if (p.isConstrained(i, j)) {
					buildConstraint(i, j, p, rand);
				}
			}
		}
	}


	//Add actual costs between variables
	protected void buildConstraint(int var1, int var2, Problem p, Random rand) 
	{
		int cost;
		for (int i = 0; i < p.getDomainSize(var1); i++) 
		{
			for (int j = 0; j < p.getDomainSize(var2); j++) 
			{
				cost = rand.nextInt(maxCost+1);
				if (addToCost)
				{
					cost = cost + 100;
				}
				p.setConstraintCost(var1, i, var2, j, cost * 1000);
				p.setConstraintCost(var2, j, var1, i, cost * 1000);				
			}
		}
	}
}
