package ext.sim.tools;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ext.sim.agents.MaxSumAgent;

public class MaxSumNode {
	
	protected NodeId id;
	List<NodeId> neighbors;
	Map<NodeId, MaxSumMessage> messages; // Received
	Map<NodeId, MaxSumMessage> messagesSent;
	MaxSumAgent agent;
	boolean printMessage = false;
	int[] unaryConst;
	
	
	public MaxSumNode(MaxSumAgent a) {
		messages = new TreeMap<NodeId, MaxSumMessage>();
		messagesSent = new TreeMap<NodeId, MaxSumMessage>();
		neighbors = new LinkedList<NodeId>();
		agent = a;
		unaryConst = new int[agent.getDomain().size()];
	}
	
	public void initNeighbors() {
	} //functionNode and variableNode will implement this
	
	protected void sendMessage(NodeId receiver, long[] table) {
		NodeId sender = this.getId();
		MaxSumMessage m = new MaxSumMessage(sender, receiver, table);
		messagesSent.put(receiver, m); //LIEL
		agent.sendMessage(m);
		
	       
		if (printMessage) System.out.println(id+": "+MyFileWriter.printVector(table));

		
		
	}// create a new message and send it from sender to receiver

	/**
	 * sum all messages values besides a messages that is from the neighbor you want to send to
	 * 
	 * @param to
	 * @return long table[] the size of the domain
	 */
	protected long[] sumMessages(NodeId to) {
		long[] table = new long[agent.getDomain().size()]; //new table for the sums (size of the domain)
		for (NodeId n: messages.keySet()) { // for each neighbors most recent message from 
			if (!n.equals(to)) { // not including the neighbor we want to send the message to
				addValues(table, messages.get(n).table); //add neighbor's table to the sums table
			}
		}
		return table;
	} 
	
	/**
	 * Add values of array "from" to array "to"
	 * @param to
	 * @param from
	 */
	protected void addValues(long[] to, long[] from) {
		for (int i=0; i<to.length; i++) {
			to[i] = to[i] + from[i];
		}
	}
	
	
	public void handleMessage(MaxSumMessage m) {
		 NodeId k = m.getSender();
		if (messages.containsKey(k)) { //in case a previous message exists
			MaxSumAgent.DEBUG("node " + getId() + " : Msg " + messages.get(k) + " replaced with " + m);
		}
		messages.put(k, m);
	} //update message from sender k

	public int id1() {
		return getId().getId1();
	}
	
	public int id2() {
		return getId().getId2();
	}
	
	public String toString() {
		return getId().toString();
	}
	
	protected void subtractAverageValue(long[] table) {
		long alpha = 0;
		for (long x: table) {
			alpha = alpha + x;
		}
		alpha = alpha / table.length;
		for (int i=0; i<table.length; i++) {
			table[i] = table[i] - alpha;
		}
		
	}//Subtracts average value of the table from all cells
	
	/**
	 * Subtracts minimum value of the table from all table cells
	 * @param table
	 */
	protected void subtractMinimumValue(long[] table) {
		long alpha = Integer.MAX_VALUE;
		for (long x: table) { // find minimum cell
			alpha = Math.min(alpha, x);
		}
		for (int i=0; i<table.length; i++) { // subtract minimum cell value, from all cells
			table[i] = table[i] - alpha;
		}	
	}
	
	public void printNode() {
		MaxSumAgent.DEBUG(getId() + " :: " + neighbors);
	}

	public NodeId getId() {
		return id;
	}

	public void setId(NodeId id) {
		this.id = id;
	}
	
	
	////  ***  BoundedMaxSum  ***  ////
	
	public void disconnect(int i, int j, int[] constVector) {
		NodeId functionNodeToRemove = new NodeId(i,j);
		neighbors.remove(functionNodeToRemove);
		addValuesToUnaryConstraints(constVector);
		//System.out.println("node " + id + " just added unary vals: " + MaxSumNode.vectorToString(constVector));
	}

	public void sendMessages(double dampingFactor) { //For function node //LIEL
	}
	
	public void sendMessages(double diminishingFactor, double dampingFactor) { //For variable node //LIEL
	}
	
	//LIEL
	protected long[] dampTables(long[] oldTable, long[] newTable, double dampingFactor) 
	{
		long[] table = new long[agent.getDomain().size()];
		
		for (int i=0; i < table.length; i++) 
		{
			table[i] = Math.round((dampingFactor * oldTable[i]) + ((1 - dampingFactor) * newTable[i]));
		}//LIEL end
		
		return table;
	}
	
	public void addValuesToUnaryConstraints(int[] constVector)
	{
		for (int i = 0; i < unaryConst.length; i++) {
			unaryConst[i] = unaryConst[i] + constVector[i];
		}
		
		//System.out.println("node " + id + " new unary vec: " + MaxSumNode.vectorToString(unaryConst));
	}
	
	static public void printVector(int[] vector) {
		System.out.print("[");
		for (int i = 0; i < vector.length; i++) {
			System.out.print(vector[i] + ", ");
		}
		System.out.print("]");

	}
	
	static public String vectorToString(int[] vector) {
		String str = "[";
		for (int i = 0; i < vector.length; i++) {
			str = str + vector[i] + ", ";
		}
		str = str + "]";
		return str;
	}
	
	static public String vectorToString(long[] vector) {
		String str = "[";
		for (int i = 0; i < vector.length; i++) {
			str = str + vector[i] + ", ";
		}
		str = str + "]";
		return str;
	}


	
}

