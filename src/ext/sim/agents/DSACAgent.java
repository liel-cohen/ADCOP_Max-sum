package ext.sim.agents;

import bgu.dcr.az.api.ano.Algorithm;
/**
 *	Written by Tommer as DSA-B, but for DCOP is also identical to DSA-C. 
 * @author Liel
 *
 */
@Algorithm(name = "DSA-C", useIdleDetector=false)
public class DSACAgent extends DSAAgent {
	
	protected Integer calcDelta() {
		int ans = this.getSubmitedCurrentAssignment();
		double delta = this.localView.calcAddedCost(this.getId(), ans, this.getProblem());
		double tmpDelta = delta;
		for(Integer i : this.getDomain()) {
			double tmp = this.localView.calcAddedCost(this.getId(), i, this.getProblem());
			if(tmp <= tmpDelta && ans!=i) {
				tmpDelta = tmp;
				ans = i;
			}
		}
		if(delta == tmpDelta) {
			return null;
		}
		return ans;
	}//Find and return the best new assignment according to the latest local view, or Null if no better assignment exists
}
