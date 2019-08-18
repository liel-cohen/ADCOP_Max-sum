package ext.sim.tools;

import ext.sim.agents.MaxSumAgent;
import ext.sim.tools.NodeId.NodeType;

public class FunctionNode extends MaxSumNode 
{
	int addCostToConst;
	
	public FunctionNode(int id, int otherId, MaxSumAgent a, int addCostToConst) 
	{
		super(a);
		this.setId(new NodeId(id, otherId, agent.isStandardSort()));
		this.getId().setType(NodeType.Function);
		this.addCostToConst = addCostToConst;
	}

	public void initNeighbors() 
	{
		neighbors.add(new NodeId(id1()));
		neighbors.add(new NodeId(id2()));	
	} //each function node (a,b) has two variable node neighbors: (a) and (b)		

	public void sendMessages(double dampingFactor) 
	{
		for (NodeId to: neighbors) {
			sendMessage(to, dampingFactor);
		}
	} //send a message to all of the function node's neighbors

	private void sendMessage(NodeId to, double dampingFactor) {
		long[][] cTable = getConstraintTable(to);
		MaxSumMessage msg = getOtherIdMsg(to); 
		if (msg != null) {
			addValues(cTable, msg);
		}
		long[] res = getBestValues(cTable);
		//subtractAverageValue(res);
		subtractMinimumValue(res);
		
		long[] oldTable = new long[agent.getDomain().size()];
		
		//LIEL
		if (dampingFactor > 0 && messagesSent.containsKey(to)) // in case a previous message exists
		{ 	
			oldTable = messagesSent.get(to).table;
			res = dampTables(oldTable, res, dampingFactor);
		}
		//LIEL end
		
		sendMessage(to, res);
	}

	protected long[] getBestValues(long[][] cTable) {
		long[] res = new long[agent.getDomain().size()];
		for (int x=0; x<agent.getDomain().size(); x++) {
			long u = Integer.MAX_VALUE;
			for (int y=0; y<agent.getDomain().size(); y++) {
				if (cTable[x][y] < u) {
					u = cTable[x][y];
				}
			}
			res[x] = u;
		}
		return res;
	}

	protected void addValues(long[][] cTable, MaxSumMessage msg) {
		addValues(cTable, msg.table);		
	}

	protected void addValues(long[][] cTable, long[] t) {
		for (int x=0; x<agent.getDomain().size(); x++)
			for (int y=0; y<agent.getDomain().size(); y++) {
				cTable[x][y] = cTable[x][y] + t[y];
			}
	}
		
	protected MaxSumMessage getOtherIdMsg(NodeId to) {
		if (neighbors.size() == 1)
			return null;
		
		NodeId[] a = new NodeId[2];
		neighbors.toArray(a);
		if (to.equals(a[0])) {
			return messages.get(a[1]);
		} else {
			return messages.get(a[0]);
		}
	}

	protected long[][] getConstraintTable(NodeId to) 
	{
		int toId = to.getId1();
		int otherId = toId == id1() ? id2() : id1(); //if my Id equals to toId, meaning we are in the same agent, and a message should be sent to the other Id since its a function node we are sending the message to. else, its a variable node we are sending the message to, and his Id is different from mine.
		long[][] res = new long[agent.getDomain().size()][agent.getDomain().size()];
		for (int x=0; x<agent.getDomain().size(); x++)
		{
			for (int y=0; y<agent.getDomain().size(); y++) 
			{
				//System.out.println("orig " + agent.getMyConstraintCost(toId, x, otherId, y));
				res[x][y] = agent.getMyConstraintCost(toId, x, otherId, y) + addCostToConst;
				//System.out.println("new " + res[x][y]);

			}
		//MaxSumAgent.DEBUG("constraint (" + toId + "," + otherId + ") : " + Arrays.deepToString(res));
		}
		return res;
	} //returns constraints between two nodes:
	//case 1: (a,b)->(a) constraints cost of [a,b]
	//case 2: (a,b)->(b) constraints cost of [a,b]
	
}
