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
//Start with AD for 1 CYLCE, then perform ADVP for 1 CYCLE (as in MaxSumADVP),
//then, alternately do 1 CYCLE (2 phase) VP, and 1 CYCLE (2 phases) ADVP
//(2,AD),(2,ADVP),(2,VP),(2,ADVP)
//***************

@Algorithm(name="ADVP_VP_ADVP", useIdleDetector=false)
public class ADVP_VP_ADVP extends MaxSumAgent {

	public void sendMessage(MaxSumMessage m) {
		MaxSumVPMessage mVP = new MaxSumVPMessage(m, getVariableNode().getValue());
		if (systemTime < CYCLE) {
			if (!isBefore(m.getSender(), m.getReceiver()))
				return;
			super.sendMessage(m);
		}//Perform AD for 1 cycle
		
		else if (systemTime%(2*CYCLE) <= CYCLE) {
		//else if (systemTime%(4*CYCLE) < 2*CYCLE) {
			if (!isBefore(m.getSender(), m.getReceiver()))
				return;
			else {
				if (m.getSender().getType() == NodeType.Variable) 
					super.sendMessage(mVP);
				else
					super.sendMessage(m);
			}
		}//Perform ADVP for 1 cycle
		
		else {			
				if (m.getSender().getType() == NodeType.Variable) 
					super.sendMessage(mVP);
				else
					super.sendMessage(m);
		}//Perform VP for 1 cycle
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
