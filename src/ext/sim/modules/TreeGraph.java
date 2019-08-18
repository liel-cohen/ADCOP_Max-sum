package ext.sim.modules;

import java.util.Random;

import bgu.dcr.az.api.ano.Register;
import bgu.dcr.az.api.ano.Variable;
import bgu.dcr.az.api.prob.Problem;
import bgu.dcr.az.api.prob.ProblemType;
import bgu.dcr.az.exen.pgen.AbstractProblemGenerator;

@Register(name = "TreeGraph")
public class TreeGraph extends AbstractProblemGenerator {

	@Variable(name = "n", description = "number of variables", defaultValue="10")
    int n = 10;
    
	@Variable(name = "d", description = "domain size", defaultValue="5")
    int d = 5;
	
	@Variable(name = "s", description = "number of sons", defaultValue="2")
    int s = 2;
	
	@Variable(name = "p2", description = "the probability for a positive cost between values values of 2 variables", defaultValue="0.9")
	double p2 = 0.9;
	
	@Variable(name = "max-cost", description = "max possible constraint cost", defaultValue="10000")
	int maxCost = 10000;
	
	Problem p;
	Random rand;
	boolean debug = true;
	int numOfNodes = 1;
	
    @Override
    public void generate(Problem p, Random rand) 
    {
    	this.p = p;
    	this.rand = rand;
    	
    	p.initialize(ProblemType.DCOP, n, d);
    	
    	span(0,0);
    	int level = 1;
    	int i = 1;
    	
    	while (numOfNodes < n)
    	{
    		if (i == firstInRow(level+1))
    		{
    			level = level + 1;
    		}
    		
    		span(i,level);
    		i++;
    	}
    	
    }
    
    public void span(int node, int level)
    {
      	int firstSonPos = firstSon(node, level);
    	
    	for (int i = 0; i < s; i++)
    	{
    		if (numOfNodes < n)
    		{
    			connectNodes(node, firstSonPos+i);
        		numOfNodes = numOfNodes + 1;
    		}
    	}
    }
    
    public int firstInRow(int level)
    {
    	int firstInRowRes = 0;
    	Double powTmp = new Double(0);
    	
    	for (int i = 0; i < level; i++)
    	{
    		powTmp = Math.pow(s, i);
    		firstInRowRes = firstInRowRes + powTmp.intValue();
    	}
    	
    	return firstInRowRes;
    }
    
    public int numInRow(int node, int level)
    {
    	return node - firstInRow(level) + 1;
    }
    
    public int firstSon(int node, int level)
    {
    	return firstInRow(level+1) + s * (numInRow(node, level) - 1) ;
    }
    
    public void connectNodes(int v1, int v2)
    {
    	if (v1 != v2 && !p.isConstrained(v1, v2))
    	{
    		if (debug) System.out.println("\nnow connecting " + v1 +" & " + v2);
        	
    		for (int val1 = 0; val1 < d; val1++)
    		{
    			for (int val2 = 0; val2 < d; val2++)
        		{
    				int cost = 0;
    				
    				if (rand.nextDouble() <= p2)
    				{
    					cost = rand.nextInt(maxCost);
    				}
    				
    				p.setConstraintCost(v1, val1, v2, val2, cost);  
					p.setConstraintCost(v2, val2, v1, val1, cost);
        		}
    		}
    	}
    }
}
