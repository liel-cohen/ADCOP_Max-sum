package ext.sim.agents;

import bgu.dcr.az.api.agt.SimpleAgent;
import bgu.dcr.az.api.ano.Algorithm;
import bgu.dcr.az.api.ano.WhenReceived;
import bgu.dcr.az.api.tools.Assignment;

@Algorithm(name="DSA", useIdleDetector=false)
public abstract class DSAAgent extends SimpleAgent {
	protected Assignment localView;
	protected double p;
	long NUMOFCYCLES = 2001;
	static boolean debug;


	@Override
	public void start() {
		//debug = true;
		localView = new Assignment();
		p = 0.77;
		int value = random(this.getDomain());
		this.submitCurrentAssignment(value);
		send("ValueMessage", value).toNeighbores();
	}


	@WhenReceived("ValueMessage")
	public void handleValueMessage(int value) {
		localView.assign(getCurrentMessage().getSender(), value);
	}//Update your local view according to all messages from your neighbors


	@Override
	public void onMailBoxEmpty() {
		final long systemTime = getSystemTimeInTicks();
		if(systemTime + 1 == NUMOFCYCLES) {
			finish();
		}
		Integer newValue = calcDelta();
		if(Math.random() < p && newValue != null) {
			submitCurrentAssignment(newValue);
			send("ValueMessage", newValue).toNeighbores();
		}//If exists a better value than your old one, switch to it in a probability of p
	}

	protected Integer calcDelta() {
		return null;
	}//The implementation is on the inheritances 
	
	
	public static void DEBUG(String s) {
		if (debug) 
			System.out.println(s);
	}

}
