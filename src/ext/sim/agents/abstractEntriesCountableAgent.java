package ext.sim.agents;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.atomic.AtomicInteger;

import bgu.dcr.az.api.tools.Assignment;

public abstract class abstractEntriesCountableAgent extends AbstractDbaAgentWithChanges{

	protected static AtomicInteger entriesCounter = new AtomicInteger();
	protected static AtomicInteger[][] pairsEntriesCounters;
	protected static AtomicInteger numberOfConstraintedPairs = new AtomicInteger();
	//protected static AtomicInteger[] numberOfConstraintsOfAgent;
	
	@Override
	protected void __start() {
		super.__start();
		
		if (isFirstAgent())
		{
			int n = getNumberOfVariables();
			
			entriesCounter = new AtomicInteger();
			abstractEntriesCountableAgent.pairsEntriesCounters = new AtomicInteger[n][n];
			//numberOfConstraintsOfAgent = new AtomicInteger[n];
			
			for (int i = 0; i < pairsEntriesCounters.length; i++) {
				for (int j = 0; j < pairsEntriesCounters[i].length; j++) {
					pairsEntriesCounters[i][j] = new AtomicInteger();
				}
			}
			
			numberOfConstraintedPairs = new AtomicInteger();
			for (int i = 0; i < getNumberOfVariables(); i++) {
				for (int j = i+1; j < getNumberOfVariables(); j++) {
					if (getProblem().isConstrained(i, j))
					{
						numberOfConstraintedPairs.incrementAndGet();
						//numberOfConstraintsOfAgent[i].incrementAndGet();
						//numberOfConstraintsOfAgent[j].incrementAndGet();
					}
				}
			}
		}
	}
	
	protected static double getAveragePairsEntriesCount()
	{
		double sum = 0;
		
		for (int i = 0; i < pairsEntriesCounters.length; i++) {
			for (int j = 0; j < pairsEntriesCounters[i].length; j++) {
				sum += pairsEntriesCounters[i][j].intValue();
			}
		}
		
		return (sum/(numberOfConstraintedPairs.intValue()*2));
	}
	
	protected static String getReport()
	{
		int[] maxEntriesSentByAgents = new int[pairsEntriesCounters.length];
		
		for (int i = 0; i < maxEntriesSentByAgents.length; i++) {
			maxEntriesSentByAgents[i] = getMaxEntriesSentByAgent(i);
		}
		
		int maxEntriesSentByAnyAgent = getMax(maxEntriesSentByAgents);
		double averageEntriesSentByAnAgents = getAverage(maxEntriesSentByAgents);
		
		String s = "";
		
		s += abstractEntriesCountableAgent.entriesCounter + "\t";
		s += abstractEntriesCountableAgent.getAveragePairsEntriesCount() + "\t";
		s += maxEntriesSentByAnyAgent + "\t";
		s += averageEntriesSentByAnAgents;
		
		
		return s;
	}
	
	private static double getAverage(int[] arr) {
		return ((double) getSum(arr)) / arr.length;
	}

	private static int getSum(int[] arr) {
		int sum = 0;

		for (int i = 0; i < arr.length; i++) {
				sum += arr[i];
		}

		return sum;
	}

	private static int getMax(int[] arr) {
		int max = 0;
		
		for (int i = 0; i < arr.length; i++) {
			if (arr[i]>max)
			{
				max = arr[i];
			}
		}
		
		return max;
	}

	private static int getMaxEntriesSentByAgent(int agent) {
		
		int maxEntries = 0;
		
		for (int i = 0; i < pairsEntriesCounters[agent].length; i++) {
			
			int entries = pairsEntriesCounters[agent][i].intValue();
			
			if (entries > maxEntries)
			{
				maxEntries = entries;
			}
		}
		
		return maxEntries;
	}

	protected abstract void handleReceivedImprovement(int sender, int improvement);

	public abstract int calculateEffCost(Assignment ass);

	public abstract boolean winsTieAgainst(int id);

	public abstract void breakout();

	public abstract long getNumBreakouts();

	public abstract long getNumConstraintBreakouts();

	public abstract int getNumViolated();

	public abstract long getNumRepeatBreakouts();

	protected abstract void initializeLocalConstraintCosts();

}
