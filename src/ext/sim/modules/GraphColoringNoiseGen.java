package ext.sim.modules;

import java.util.Random;

import bgu.dcr.az.api.ano.Register;
import bgu.dcr.az.api.ano.Variable;
import bgu.dcr.az.api.prob.Problem;
import bgu.dcr.az.api.prob.ProblemType;
import bgu.dcr.az.exen.pgen.AbstractProblemGenerator;

/* //**********
//NOTE: this problem generator creates a connected Graph Coloring problem, with NOISE addition

These comment lines are in case you are using unary constraints. Meaning, in case you want
to break ties by adding personal preferences (AKA unary constraints), in this class -
you should only multiply the cost of each constraint by a 1000. The random integer will be added later
via the "VariableNode" class.

//********** */

@Register(name="graphcoloring-noise")
public class GraphColoringNoiseGen extends AbstractProblemGenerator {

	@Variable(name = "n", description = "number of variables", defaultValue = "2")
	int n = 2;
	@Variable(name = "d", description = "domain size", defaultValue = "2")
	int d = 2;
	@Variable(name = "p1", description = "p(constraint) between variables", defaultValue = "0.6")
	float p1 = 0.6f;
	@Variable(name = "max-cost", description = "cost of breaking a constraint", defaultValue = "10")
	int breakCost = 10;

	@Override
	public void generate(Problem p, Random r) {
		p.initialize(ProblemType.DCOP, n, d);

		for (int i = 0; i < p.getNumberOfVariables(); i++) 
			for (int j = 0; j < p.getNumberOfVariables(); j++) 
				if (r.nextDouble() < p1) 
					buildConstraint(i, j, p, r);

	}

	private void buildConstraint(int var1, int var2, Problem p, Random rand) {
		int cost;		
		for (int i = 0; i < p.getDomainSize(var1); i++) {
			for (int j = 0; j < p.getDomainSize(var2); j++) {
				if (i==j) 
					cost = breakCost * 1000; 
				else
					cost = 0;
				p.setConstraintCost(var1, i, var2, j, cost);
				p.setConstraintCost(var2, j, var1, i, cost);
			}
		}
	}
	
	/*
	 * Without the use of UC (need to add dust to the constraints directly):
	 * 	if: 	cost = breakCost * 1000 + rand.nextInt(20); 
	 * else: 	cost = rand.nextInt(20);
	 * 
	 * With the use of UC (the tie breaking will occur through the preferences):
	 * if: 		cost = breakCost * 1000;
	 * else: 	cost = 0; 
	 */
}
