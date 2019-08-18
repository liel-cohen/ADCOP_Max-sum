package ext.sim.modules;

import java.util.Random;

import bgu.dcr.az.api.Agt0DSL;
import bgu.dcr.az.api.ano.Register;
import bgu.dcr.az.api.ano.Variable;
import bgu.dcr.az.api.ds.ImmutableSet;
import bgu.dcr.az.api.prob.Problem;
import bgu.dcr.az.api.prob.ProblemType;

/* //**********
//NOTE: this problem generator creates a connected Graph Coloring problem, with NOISE addition

These comment lines are in case you are using unary constraints. Meaning, in case you want
to break ties by adding personal preferences (AKA unary constraints), in this class -
you should only multiply the cost of each constraint by a 1000. The random integer will be added later
via the "VariableNode" class.

//********** */

@Register(name="adcop-graphcoloring-connected-noise")
public class GraphColoringConnectedNoiseGen_adcop extends GeneralDCOPGen {

	@Variable(name = "n", description = "number of variables", defaultValue = "2")
	int n = 2;
	@Variable(name = "d", description = "domain size", defaultValue = "2")
	int d = 2;
	@Variable(name = "p1", description = "p(constraint) between variables", defaultValue = "0.6")
	float p1 = 0.6f;
	@Variable(name = "max-cost", description = "cost of breaking a constraint", defaultValue = "10")
	int breakCost = 10;


	public void generate(Problem p, Random rand) 
	{
		p.initialize(ProblemType.ADCOP, n, new ImmutableSet<Integer>(Agt0DSL.range(0, d - 1)));
		addConstraints(p, rand);
		addConnectivity(p, rand);
		for (int i = 0; i < p.getNumberOfVariables(); i++) {
			for (int j = i+1; j < p.getNumberOfVariables(); j++) {
				if (p.isConstrained(i, j)) {
					turnDCOPToGraphColoring(i, j, p, rand, breakCost);
				}
			}
		}
		
		for (int i = 0; i < p.getNumberOfVariables(); i++) 
		{
			for (int j = i+1; j < p.getNumberOfVariables(); j++) {
				if (p.isConstrained(i, j)) {
					splitCost(i, j, p, rand);
				}
			}
		}
	}
	
	protected void turnDCOPToGraphColoring(int var1, int var2, Problem p, Random rand, int constraintCost) {
		int cost;
		
		for (int i = 0; i < p.getDomainSize(var1); i++) {
			for (int j = 0; j < p.getDomainSize(var2); j++) {
				
				if (i==j)  //this is the only case of cost in a graph coloring problem
					cost = constraintCost; 
				else  //the variables 'chose' different colors, hence no cost
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
	
	
	//Split each original costs randomly between the asymmetric costs (in asymmetric problems)
	//The costs here must be NOT-MULTIPLIED by 1000!!
	protected void splitCost(int var1, int var2, Problem p, Random rand) 
	{
		int originalCost, costVal1, costVal2;
		
		for (int val1 = 0; val1 < p.getDomainSize(var1); val1++) {
			for (int val2 = 0; val2 < p.getDomainSize(var2); val2++) {
				originalCost = p.getConstraintCost(var1, val1, var2, val2);
				if (originalCost > 0) 
					costVal1 = (rand.nextInt(originalCost) + 1);
				else 
					costVal1 = 0;
				costVal2 = originalCost - costVal1;
				p.setConstraintCost(var1, val1, var2, val2, costVal1 * 1000);
				p.setConstraintCost(var2, val2, var1, val1, costVal2 * 1000);
			}
		}
	}
}

