package ext.sim.agents;

import bgu.dcr.az.api.ano.Algorithm;
import ext.sim.tools.FunctionNode;
import ext.sim.tools.FunctionNodeVP;
import ext.sim.tools.MaxSumMessage;
import ext.sim.tools.MaxSumVPMessage;
import ext.sim.tools.NodeId.NodeType;

@Algorithm(name="MaxSumVP", useIdleDetector=false)
public class MaxSumVPAgent extends MaxSumAgent {
	boolean VP;	
	
	protected void initFunctionNodes() {
		int otherId = getId()+1;
		while (otherId < getNumberOfVariables()) {
			if (isConstrained(getId(), otherId)) {
				FunctionNode n = new FunctionNodeVP(getId(), otherId, this, addCostToConst);
				nodes.put(n.getId(), n);
			}
			otherId++;
		}
		
		if (asymmetric) { //for asymmetric agents, we generate extra function nodes 
			otherId = getId()-1; //updating all the function nodes that are BEFORE the variable (index-wise)
			while (otherId >= 0) {
				if (isConstrained(getId(), otherId)) {
					FunctionNode n = new FunctionNodeVP(getId(), otherId, this, addCostToConst);
					nodes.put(n.getId(), n);
				}
				otherId--;
			}
		}
	}
	
	public void sendMessage(MaxSumMessage m) {
		final long systemTime =  getSystemTimeInTicks();
		if (systemTime > CYCLE && m.getSender().getType() == NodeType.Variable) {
			MaxSumVPMessage mVP = new MaxSumVPMessage(m, getVariableNode().getValue());
			super.sendMessage(mVP);
		} else {
			super.sendMessage(m);
		}		
	}

}
