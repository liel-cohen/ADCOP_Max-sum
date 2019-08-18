package ext.sim.tools;

import java.util.Arrays;
import java.util.Random;
import java.util.Vector;

import com.sun.org.apache.bcel.internal.generic.GETSTATIC;

import ext.sim.agents.MaxSumAgent;
import ext.sim.tools.NodeId.NodeType;

public class VariableNode extends MaxSumNode {

	int value;
	int preferences[];
	Random rand;

	public VariableNode(int id, MaxSumAgent a, boolean dustOn) {
		super(a);
		this.setId(new NodeId(id));
		this.getId().setType(NodeType.Variable);
		rand = new Random(agent.getId()); //inside the brackets is the seed number
		setPreferences(dustOn); //Add unary constraints (AKA dust)
	}


	public void initNeighbors() {
		for (int i=0; i<agent.getNumberOfVariables(); i++) {
			if (agent.isConstrained(agent.getId(), i)) {
				if (agent.getAsymmetric()) { //in case the problem is asymmetric, we will have to define the extra (function nodes) neighbors for the variable 
					neighbors.add(new NodeId(agent.getId(), i, agent.isStandardSort()));
					neighbors.add(new NodeId(i, agent.getId(), agent.isStandardSort()));
				}

				else 
					neighbors.add(new NodeId(Math.min(agent.getId(), i), Math.max(agent.getId(), i), agent.isStandardSort()));

			}
		}
	}// creates a list of function node neighbors to the variable node
	/*
	public int selectBestValue() {
		double[] table = getMessagesUtilSum();
		int val = 0;
		double util = Integer.MAX_VALUE;
		for (int v=0; v<agent.getDomain().size(); v++) {
			if (table[v] < util) {
				util = table[v];
				val = v;
			}
		}
		MaxSumAgent.DEBUG("node "+ this + " chose value " + val + " based on " + Arrays.toString(table));
		value = val;
		return val;
	} //helps the agent choose minimal value assignment
	 */
	public int selectBestValue(int k) {
		long[] table = getMessagesUtilSum();
		addPreferences(table); //Add the variable's preferences (unary constraints, used as tie-breakers)
		Vector<Long> vectorTable = new Vector<Long>();
		int[] bestK = new int[k];
		long util;

		for (int i = 0; i < table.length; i++) {
			vectorTable.addElement(table[i]);
		}
		for (int i = 0; i < bestK.length; i++) {
			util = Integer.MAX_VALUE;
			bestK[i] = 0;
			for (int v=0; v<agent.getDomain().size(); v++) {
				if (vectorTable.elementAt(v) < util) {
					util = vectorTable.elementAt(v);
					bestK[i] = v;
				}
			}
			vectorTable.set(bestK[i], (long) Integer.MAX_VALUE);
		}

		value = chooseValue(bestK);
		int attempts = 0;
		while (table[value] >= Integer.MAX_VALUE && attempts<k) {
			attempts++;
			value = chooseValue(bestK);
		}//ADDED FOR THE HEURISTIC, tries to avoid selection of large values

		MaxSumAgent.DEBUG("node "+ this + " chose value " + value + " based on " + Arrays.toString(table));
		return value;
	} //helps the agent choose minimal value assignment

	private int chooseValue(int[] bestK) {
		int index = rand.nextInt(bestK.length);
		return bestK[index];
	}

	/**
	 * For each value, sums the cost from all messages	
	 * @return long[]
	 */
	public long[] getMessagesUtilSum() {
		long[] res = new long[agent.getDomain().size()];
		for (int v=0; v < agent.getDomain().size(); v++) { //for each domain val
			long u = 0;
			for (NodeId n: messages.keySet()) {
				u = u + messages.get(n).table[v];
			}
			res[v] = u;
		}
		return res;
	}//For each value, sums the cost from all messages	

	public void sendMessages(double diminishingFactor, double dampingFactor) { //LIEL
		long[] table = new long[agent.getDomain().size()];
		for (NodeId to: neighbors) //For each neighbor node
		{
			/*
			 * MPLP algorithm
			 */
			if (diminishingFactor == -1) { // MPLP algorithm
				table = messageMPLP(to);
				addUnaryConstraints(table);
				subtractMinimumValue(table);
				addPreferences(table);
				
			}

			else if (diminishingFactor == -2) { // MPLP algorithm, but only one at the time
				if (agent.getSystemTimeInTicks() % agent.getNumberOfVariables() == this.getId().getId1()) {
					table = messageMPLP(to);
					addUnaryConstraints(table);
					subtractMinimumValue(table);
					addPreferences(table);
				}
				else {
					if (messagesSent.containsKey(to)) { // in case a previous message exist
						table = messagesSent.get(to).table;
					}
					else { // no previous message, send only preferences (dust)
						//table = sumMessages(to);	
						addUnaryConstraints(table);
						subtractMinimumValue(table);
						addPreferences(table);
					}
				}
				MaxSumMessage lastMsg = new MaxSumMessage(this.getId(), to, table);
				messagesSent.put(to, lastMsg); // save the last message sent to this receiver
			}
			
			/*
			 * not MPLP algorithm @@@@@@@@@@
			 */
			else {
				table = sumMessages(to);
				
				//subtractAverageValue(table);
				//System.out.println(id + " before: "+ MaxSumNode.vectorToString(table));
				addUnaryConstraints(table);
				//System.out.println(id + " after: "+ MaxSumNode.vectorToString(table));
				subtractMinimumValue(table);
				addPreferences(table); //Add the variable's preferences (unary constraints, used as tie-breakers)
				diminishMessage(to, table, diminishingFactor);
				
				long[] oldTable = new long[agent.getDomain().size()];
				
				//LIEL
				if (dampingFactor > 0 && messagesSent.containsKey(to)) // in case a previous message exists
				{ 	
					oldTable = messagesSent.get(to).table;
					table = dampTables(oldTable, table, dampingFactor);
				}
				//LIEL end
			}
			
			sendMessage(to, table);
		}
	} //send a message to all of the variable node's neighbors


	protected long[] messageMPLP(NodeId to) {
		long[] tableOrigin = new long[agent.getDomain().size()];
		long[] table = new long[agent.getDomain().size()];
		double c = (double) 2 / (this.neighbors.size()+1); // MPLP algorithm
		for (NodeId n: messages.keySet()) {
			if (n.equals(to)) addValues(tableOrigin, messages.get(n).table); //get message from receiver (to)
			addValues(table, messages.get(n).table); //sum messages from all neighbors
		}
		for (int i = 0; i < table.length; i++) {
			//System.out.println("c * table[i] - tableOrigin[i]:  " + c + "*" + table[i] + "-" + tableOrigin[i]);
			table[i] = (long)(c * table[i]) - tableOrigin[i];
		}
		return table;
	}


	public int getValue() {
		return value;
	}

	private void setPreferences(boolean dustOn) {
		preferences = new int[agent.getDomain().size()];
		for (int i = 0; i < preferences.length; i++) {
			if (dustOn)
			{
				preferences[i] = rand.nextInt(20);
			}
			else
			{
				preferences[i] = 0;
			}
		}
	}//This method is used for adding unary constraints to problem, which are also used as tie-breakers ("dust")

	public int[] getPreferences() {
		return preferences;
	}

	private void addPreferences(long[] table) {
		for (int i = 0; i < table.length; i++) {
			table[i] = table[i] + preferences[i];
		}
	}
	
	private void addUnaryConstraints(long[] table) {
		for (int i = 0; i < table.length; i++) {
			table[i] = table[i] + unaryConst[i];
		}
	}

	private void diminishMessage(NodeId to, long[] table, double diminishingFactor) {
		for (int i = 0; i < table.length; i++) {
			table[i] = Math.round(table[i] * diminishingFactor);
		}
	}
	

	


	//// ******** For Max-Sum Simulated Annealing agent ******** ////
	
	public int selectRandomValue() {
		return rand.nextInt(agent.getDomain().size());
	}
	public long getDelta(int currentValue, int newValue) {
		long[] table = getMessagesUtilSum();		
		return (table[newValue] - table[currentValue]);
	}
	
	
	
	///// ******* For ADVP Distance from Best Solution Heuristic ******* ////

	public int selectBestValueHeuristic(double bias) {
		long[] table = getMessagesUtilSum();
		Vector<Integer> vectorTable = new Vector<Integer>();
		long bestValue = Integer.MAX_VALUE;
		int bestValueIndex = 0; 

		for (int i=0; i < agent.getDomain().size(); i++) {
			if (table[i] < bestValue) {
				bestValue = table[i];
				bestValueIndex = i;
			}
		} //first, find best value

		for (int i=0; i < agent.getDomain().size(); i++) {
			if (table[i] <= bestValue*(1+bias)) {
				vectorTable.addElement(i);
			}
		} //choose all values which are relatively close to the best result

		if (vectorTable.isEmpty()) 
			value = bestValueIndex;
		else 
			value = chooseValue(vectorTable);		
		return value;
	} 
	
	
	private int chooseValue(Vector<Integer> bestK) {
		int index = rand.nextInt(bestK.size());
		return bestK.elementAt(index);
	}

}
