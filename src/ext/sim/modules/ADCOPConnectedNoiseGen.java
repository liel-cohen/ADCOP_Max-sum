package ext.sim.modules;

import java.util.Random;

import bgu.dcr.az.api.Agt0DSL;
import bgu.dcr.az.api.ano.Register;
import bgu.dcr.az.api.ds.ImmutableSet;
import bgu.dcr.az.api.prob.Problem;
import bgu.dcr.az.api.prob.ProblemType;

//**********
//NOTE: this problem generator creates a connected ADCOP problem, with multiplication of the value by a 1000,
//which is used for adding personal preferences later.
//(used for adding unary constraints later via the VariableNode class)
//**********

@Register(name="adcop-connected-noise")
public class ADCOPConnectedNoiseGen extends GeneralDCOPGen {	

	public void generate(Problem p, Random rand) {
		p.initialize(ProblemType.ADCOP, n, new ImmutableSet<Integer>(Agt0DSL.range(0, d - 1)));
		addConstraints(p, rand);
		addConnectivity(p, rand);
		for (int i = 0; i < p.getNumberOfVariables(); i++) {
			for (int j = i+1; j < p.getNumberOfVariables(); j++) {
				if (p.isConstrained(i, j)) {
					buildConstraint(i, j, p, rand);
				}
			}
		}
		for (int i = 0; i < p.getNumberOfVariables(); i++) {
			for (int j = i+1; j < p.getNumberOfVariables(); j++) {
				if (p.isConstrained(i, j)) {
					splitCost(i, j, p, rand);
				}
			}
		}
	}


	//Add actual costs between variables
	protected void buildConstraint(int var1, int var2, Problem p, Random rand) {
		int cost;
		for (int i = 0; i < p.getDomainSize(var1); i++) {
			for (int j = 0; j < p.getDomainSize(var2); j++) {
				cost = rand.nextInt(maxCost+1);
				if (addToCost)
				{
					cost = cost + 100;
				}
				p.setConstraintCost(var1, i, var2, j, cost);
			}
		}
	}

}
