package ext.sim.tools;

import ext.sim.agents.STD_VP_AD_ADVP;
import ext.sim.agents.ADVP_1AD_2ADVP;
import ext.sim.agents.ADVP_2AD_2ADVP;
import ext.sim.agents.ADVP_VP_ADVP;
import ext.sim.agents.MaxSumADAgent;
import ext.sim.agents.MaxSumAgent;
import ext.sim.agents.STD_AD_ADVP;
import ext.sim.agents.MaxSumVPAgent;
import ext.sim.agents.MaxSumVPOrderedAgent;

public class FunctionNodeVP extends FunctionNode {

	public FunctionNodeVP(int id, int otherId, MaxSumAgent a, int addCostToConst) {
		super(id, otherId, a, addCostToConst);
	}

	protected void addValues(long[][] cTable, MaxSumMessage msg) 
	{
		if (msg instanceof MaxSumVPMessage && isBefore(msg.getSender(), this.getId())) 
		{ 
			MaxSumVPMessage mVP = (MaxSumVPMessage)msg;		
			long[] t = new long[agent.getDomain().size()];
			for (int x=0; x<agent.getDomain().size(); x++) 
			{
				t[x] = Integer.MAX_VALUE;
			}
			t[mVP.value] = msg.table[mVP.value];		
			super.addValues(cTable, t);
		} 
		else {		
			super.addValues(cTable, msg);
		}
	}
	
	private boolean isBefore(NodeId id1, NodeId id2) {
		//MaxSumADAgent a = (MaxSumADAgent)agent; //CHANGED FOR THE HEURISTIC!!
		//ADVPFirstDoVPAgent a = (ADVPFirstDoVPAgent)agent;

		MaxSumAgent a1 = (MaxSumAgent)agent;

		if (a1.getAlgorithmName().equals("STD_VP_AD_ADVP")) {
			STD_VP_AD_ADVP a2 = (STD_VP_AD_ADVP)agent;
			return a2.isBefore(id1, id2);
		}
		if (a1.getAlgorithmName().equals("STD_AD_ADVP")) {
			STD_AD_ADVP a2 = (STD_AD_ADVP)agent;
			return a2.isBefore(id1, id2);
		}
		if (a1.getAlgorithmName().equals("ADVP_1AD_2ADVP")) {
			ADVP_1AD_2ADVP a2 = (ADVP_1AD_2ADVP)agent;
			return a2.isBefore(id1, id2);
		}
		if (a1.getAlgorithmName().equals("ADVP_2AD_2ADVP")) {
			ADVP_2AD_2ADVP a2 = (ADVP_2AD_2ADVP)agent;
			return a2.isBefore(id1, id2);
		}
		if (a1.getAlgorithmName().equals("ADVP_VP_ADVP")) {
			ADVP_VP_ADVP a2 = (ADVP_VP_ADVP)agent;
			return a2.isBefore(id1, id2);
		}
		if (a1.getAlgorithmName().equals("MaxSumVP")) {
			MaxSumVPAgent a2 = (MaxSumVPAgent)agent;
			return a2.isBefore(id1, id2);
		}
		if (a1.getAlgorithmName().equals("MaxSumVPOrdered")) {
			MaxSumVPOrderedAgent a2 = (MaxSumVPOrderedAgent)agent;
			return a2.isBefore(id1, id2);
		}
		else {
			MaxSumADAgent b = (MaxSumADAgent) agent;
			return b.isBefore(id1, id2);
		}
	}		
	/* CHANGED FOR THE HEURISTIC! PREVIOUS CODE:
	MaxSumADAgent a = (MaxSumADAgent)agent; //CHANGED FOR THE HEURISTIC!!
	return a.isBefore(id1, id2);
	*/
	
}
