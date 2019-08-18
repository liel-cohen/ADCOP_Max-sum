package ext.sim.agents;

import ext.sim.tools.MaxSumMessage;
import ext.sim.tools.NodeId;
import bgu.dcr.az.api.ano.Algorithm;
import bgu.dcr.az.api.ano.Variable;

@Algorithm(name="MaxSumAD", useIdleDetector=false)
public class MaxSumADAgent extends MaxSumAgent {
	@Variable(name="unOrderedAgents", description="how many agents are ordered by weird sort 1 method", defaultValue="0")
	protected int unOrderedAgents = 0;
	//sets the amount of agents that are ordered in a weird way, starting the first agent. A value of 0 means completely STANDARD sort
	//value of 1 means that every constraint which involves the first agent (X0) will be ordered in a weird way.
	@Variable(name="levelOfOrder", description="Level of non-standard order", defaultValue="0")
	protected double levelOfOrder = 0;
	//value of 1 means completely STANDARD order, 0 means non-standard (weird) order. Default value: 0.

	public void sendMessage(MaxSumMessage m) {
		if (!isBefore(m.getSender(), m.getReceiver()))
			return;
		super.sendMessage(m);
	}
	
	public void onMailBoxEmpty() {
		final long systemTime = getSystemTimeInTicks();
		if (systemTime%CYCLE == CYCLE/2 || systemTime%CYCLE == 0) DEBUG("\nSwitching direction (agent " + getId() +")");
		super.onMailBoxEmpty();		
	}	
	
	
	/** 23/02/14
	 * 	This is just for testing the really weird sort. When completed, delete the "isBefore" below
	 * 	and stay only with the one in comments.
	 * 
	 * */
	public boolean isBefore(NodeId sender, NodeId receiver) {
		if (systemTime%CYCLE < CYCLE/2) 
			return sender.isBefore(receiver, innerOrderString) <= 0;
		else 
			return sender.isBefore(receiver, innerOrderString) > 0;
	} 
	
	
	/** 17-05-14
	 * in case you want to use weird sort 1, change to 'isBefore1'
	 * in case you want to use weird sort 2, change to 'isBefore2'
	 * also, don't forget to remove the 'unOrderedAgents' parameter from arguments
	 * once you're done with the sort things, you can change this to use the 'compareTo' instead
	 * of the 'isBefore', as seen above.
	 
	public boolean isBefore(NodeId sender, NodeId receiver) {
		if (systemTime%CYCLE < CYCLE/2) 
			return sender.isBefore1(receiver, levelOfOrder) <= 0;
		else 
			return sender.isBefore1(receiver, levelOfOrder) > 0;
	}
*/
	public int getUnOrderedAgents() {
		return unOrderedAgents;
	}
}
