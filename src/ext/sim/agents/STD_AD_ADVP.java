package ext.sim.agents;
import bgu.dcr.az.api.ano.Algorithm;
import ext.sim.agents.MaxSumAgent;
import ext.sim.tools.FunctionNode;
import ext.sim.tools.FunctionNodeVP;
import ext.sim.tools.MaxSumMessage;
import ext.sim.tools.MaxSumVPMessage;
import ext.sim.tools.NodeId.NodeType;

//***************
//This heuristic works as follow:
//Start with regular MaxSum for 2 CYLCEs, then perform AD for 2 other CYCLEs, and finally perform regular MaxSumADVP
// (4,STD),(4,AD),(the rest,ADVP)
//***************

@Algorithm(name="STD_AD_ADVP", useIdleDetector=false)
public class STD_AD_ADVP extends MaxSumAgent {

	public void sendMessage(MaxSumMessage m) {
		MaxSumVPMessage mVP = new MaxSumVPMessage(m, getVariableNode().getValue());
		if (systemTime < 2*CYCLE) {
			super.sendMessage(m);
		}//Do regular MaxSum
		else if (systemTime < 4*CYCLE) {
			if (!isBefore(m.getSender(), m.getReceiver()))
				return;
			super.sendMessage(m);
		}//Perform AD	
		else {			
			if (!isBefore(m.getSender(), m.getReceiver()))
				return;
			else {
				if (m.getSender().getType() == NodeType.Variable) 
					super.sendMessage(mVP);
				else
					super.sendMessage(m);
			}
		}//Perform ADVP
	}


	protected void initFunctionNodes() {
		int otherId = getId()+1;
		while (otherId < getNumberOfVariables()) {
			if (isConstrained(getId(), otherId)) {
				FunctionNode n = new FunctionNodeVP(getId(), otherId, this, addCostToConst);
				nodes.put(n.getId(), n);
			}
			otherId++;
		}
	}

	
}
