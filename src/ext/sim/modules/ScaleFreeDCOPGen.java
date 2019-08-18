package ext.sim.modules;

import bgu.dcr.az.api.Agt0DSL;
import bgu.dcr.az.api.ano.Register;
import bgu.dcr.az.api.ano.Variable;
import bgu.dcr.az.api.ds.ImmutableSet;
import bgu.dcr.az.api.exp.InvalidValueException;
import bgu.dcr.az.api.prob.Problem;
import bgu.dcr.az.api.prob.ProblemType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import bgu.dcr.az.exen.pgen.AbstractProblemGenerator;

/**
 * Changed with Steven's code modification ["while (!v.isEmpty())"] 03/10/14
 * Scale free DCOP (symmetric)
 *
 * Used the Barabasi-Albert model
 * Needs a large network to get the scale free effect
 */

@Register(name = "dcop-scale-free")
public class ScaleFreeDCOPGen extends AbstractProblemGenerator {
	@Variable(name = "n", description = "number of variables", defaultValue="10")
    int n = 10;
    @Variable(name = "d", description = "domain size", defaultValue="2")
    int d = 2;
    @Variable(name = "max-cost", description = "maximal cost of constraint", defaultValue="10")
    int maxCost = 10;
    @Variable(name = "min-cost", description = "minimal cost of constraint", defaultValue="0")
    int minCost = 0;
    @Variable(name = "core-size", description = "The number of vertices in the core of the graph", defaultValue="5")
    int core = 5; 			
    @Variable(name = "growth", description = "The number of vertices any new edge will be connected to (smaller than the core-size)", defaultValue="2")
    int growth = 2;
    
    @Override
    public void generate(Problem p, Random rand) 
    {
    	p.initialize(ProblemType.DCOP, n, new ImmutableSet<Integer>(Agt0DSL.range(0, d - 1)));
    	double[] degree = new double[n];
    	double totalDeg = 0;
    	if ((core < 2) || (core >= n) || (growth>core))
    		throw new InvalidValueException("BA model - bad parameters: n="+n+", core="+core+" and growth="+growth);
    	ArrayList<Integer> v = new ArrayList<Integer>();
    	ArrayList<Integer> coreSet = new ArrayList<Integer>();
		for (int i=0; i<n; i++)
			v.add(i);
		
		for (int i=0; i<core; i++){
			Integer vcs = v.get(rand.nextInt(v.size()));
			coreSet.add(vcs);
			v.remove(vcs);
		}
    		
		/* first make sure that each one of the nodes is connected with at least two edges by making it a ring */
		Collections.shuffle(coreSet,rand);
		for (int i=0; i<core; i++){
			Integer v1 = coreSet.get(i);
			Integer v2 = coreSet.get((i+1) % coreSet.size());
			buildConstraint(v1, v2, p, rand);
			degree[v1]++;
			degree[v2]++;
		}
		
		/* now we add more edges to the ring */
		for (int i=0; i<core; i++){
			Integer v1 = coreSet.get(0);
			coreSet.remove(v1);
			for (int j=0; j<growth-degree[v1]; j++){
				Integer v2 = coreSet.get(rand.nextInt(coreSet.size()));
				/* not very elegant or efficient but should work (will work better as core becomes larger than growth) */
				if (p.isConstrained(v1, v2)){
					j--;
					continue;
				}
				buildConstraint(v1, v2, p, rand);
				degree[v1]++;
				degree[v2]++;
				totalDeg+=2;
			}
			coreSet.add(v1);
		}
		
		/* finally we add the remaining vertices to the network */
		Collections.shuffle(v,rand);
		while (!v.isEmpty()) {
			Integer v1 = v.get(0);
			for (int j=0; j<growth; j++){
				Integer v2 = coreSet.get(rand.nextInt(coreSet.size()));
				if (!p.isConstrained(v1, v2) && rand.nextDouble() < degree[v2]/totalDeg){
					buildConstraint(v1, v2, p, rand);
					degree[v1]++;
					degree[v2]++;
					totalDeg+=2;
				} else {
					j--;
					continue;
				}
			}
			coreSet.add(v1);
			v.remove(v1);
		}
    }
    
    protected void buildConstraint(int i, int j, Problem p, Random rand) {
        for (int vi = 0; vi < p.getDomain().size(); vi++) {
            for (int vj = 0; vj < p.getDomain().size(); vj++) {
                final int cost = minCost + rand.nextInt(maxCost);
                p.setConstraintCost(i, vi, j, vj, cost);
                p.setConstraintCost(j, vj, i, vi, cost);
                
            }
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Generating : ").append("n = ").append(n).append("\nd = ").append(d).append("\nmaxCost = ").append(maxCost);
        return sb.toString();
    }
}
