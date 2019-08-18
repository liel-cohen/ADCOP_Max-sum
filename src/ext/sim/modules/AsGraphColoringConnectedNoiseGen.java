package ext.sim.modules;

import java.util.Random;

import bgu.dcr.az.api.Agt0DSL;
import bgu.dcr.az.api.ano.Register;
import bgu.dcr.az.api.ano.Variable;
import bgu.dcr.az.api.ds.ImmutableSet;
import bgu.dcr.az.api.prob.Problem;
import bgu.dcr.az.api.prob.ProblemType;

/* //**********
//NOTE: this problem generator creates a connected ASYMMETRIC Graph Coloring problem, with NOISE addition
//********** */

@Register(name="as-graphcoloring-connected-noise")
public class AsGraphColoringConnectedNoiseGen extends GraphColoringConnectedNoiseGen {

	public void generate(Problem p, Random rand) {
		p.initialize(ProblemType.ADCOP, n, new ImmutableSet<Integer>(Agt0DSL.range(0, d - 1)));
		addConstraints(p, rand);
		addConnectivity(p, rand);
		for (int i = 0; i < p.getNumberOfVariables(); i++) {
			for (int j = i+1; j < p.getNumberOfVariables(); j++) {
				if (p.isConstrained(i, j)) {
					turnDCOPToGraphColoring(i, j, p, rand, (breakCost/2));
					splitCost(i,  j,  p, rand);
				}
			}
		}
	}
	
	
	protected void splitCost(int var1, int var2, Problem p, Random rand) {
		int originalCost, costVal1, costVal2;
		
		for (int val1 = 0; val1 < p.getDomainSize(var1); val1++) {
			for (int val2 = 0; val2 < p.getDomainSize(var2); val2++) {
				originalCost = p.getConstraintCost(var1, val1, var2, val2)/1000;
				if (originalCost > 0) 
					costVal1 = (rand.nextInt(originalCost) + 1);
				else 
					costVal1 = 0;
				costVal2 = originalCost - costVal1;
				p.setConstraintCost(var1, val1, var2, val2, costVal1*1000);
				p.setConstraintCost(var2, val2, var1, val1, costVal2*1000);
			}
		}
	}

}
