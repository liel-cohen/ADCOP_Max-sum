package ext.sim.agents;

import java.util.HashSet;
import java.util.Iterator;

//import bgu.dcr.az.api.*;
import bgu.dcr.az.api.agt.*;
import bgu.dcr.az.api.ano.*;
import bgu.dcr.az.api.tools.*;
import bgu.dcr.az.api.ano.WhenReceived;

@Algorithm(name="SFB", useIdleDetector=false)
public class SFBAgent extends SimpleAgent {

	@Variable(name = "super-SFB", description="sending LB_Request only to neigbours, and keeping cost matrix" , defaultValue="false")
	boolean superSFB;

	int[][] cost;
	int[] earlierConstraintAgent;
	int[] lbReports;
	//end of super-SFB variables
	
	static int problemNum = 1;	//for debugging purposes
	
	Assignment currentCpa;
	int currentCpaCost;
	int currentValCost;
	Iterator<Integer> domain;
	int lb;
	int ub;
	int pendingReports;
	
	//only for first agent
	Assignment bestSolution;
	int solutionCost;
			
    @Override
    public void start() {
//    	if (this.getId()==0){
//    		if (superSFB) log("super");
//    		else {
//    			log("problem num "+problemNum++);
//    			log("SFB");
//    		}
//    	}
    	if (superSFB){
    		initSuperSFB();
    	}
    	
    	domain = getDomain().iterator();
    	ub = Integer.MAX_VALUE;
        if (isFirstAgent()) {
        	solutionCost = Integer.MAX_VALUE;
            handleCPA(new Assignment(), 0, lbReports);
        }
    }

	@WhenReceived("CPA")
	public void handleCPA(Assignment newCpa, int cost, int[] oldReports){
		if (superSFB){
			lbReports = oldReports;
		}
		
		currentCpa = newCpa;
		
		if (domain.hasNext()){
			int val = domain.next();
			currentValCost = calcAddedCost(currentCpa, val);
			currentCpaCost = cost;
			currentCpa.assign(this.getId(), val);
			if (currentCpa.isFull(getProblem())){
				if (currentCpaCost + currentValCost < ub){								//this if can be around the report_Request sending as well
					broadcast("Upper_Bound", currentCpaCost + currentValCost);
					send("Possible_Solution", currentCpa, currentCpaCost + currentValCost).toFirstAgent();
				}
				currentCpa.unassign(this.getId());
				handleCPA(currentCpa, currentCpaCost, lbReports);
			} else {
				if (superSFB){
					HashSet<Integer> s = new HashSet<>(this.getNeighbors());
					Iterator<Integer> iter = s.iterator();
					while (iter.hasNext()){
						if (iter.next() < getId()){
							iter.remove();
						}
					}
					send("LB_Request", currentCpa, getId()).toAll(s);
					pendingReports = s.size();
					if (pendingReports==0){
						pendingReports=1;
						handleLBReport(0, getId());
					}
				}
				else {
					send("LB_Request", currentCpa, getId()).toAllAgentsAfterMe();
					pendingReports = getNumberOfVariables() - currentCpa.getNumberOfAssignedVariables();
					lb = 0;
				}
			}
		} else{
			if (isFirstAgent()){
				finish(bestSolution);
			} else {
				domain = getDomain().iterator();
				send("Back_Track").toPreviousAgent();
			}
		}
	}

	private int calcAddedCost(Assignment cpa, int val) {
		if (superSFB){
			return getLocalCost(getLastConstraintAgent(), val);
		}
		int cost = 0;
		for (Integer i : cpa.assignedVariables()){
			cost += getConstraintCost(i, cpa.getAssignment(i), getId(), val);
		}
		return cost;
	}

	@WhenReceived("Upper_Bound")
	public void handleUpperBound(int cost){
		if (cost < ub){
			ub = cost;
		}
	}

	@WhenReceived("Possible_Solution")
	public void handlePossibleSolution(Assignment solution, int cost){
		if (cost < solutionCost){
			bestSolution = solution;
			solutionCost = cost;
		}
	}

	@WhenReceived("LB_Request")
	public void handleLBRequest(Assignment cpa, int sender){
		if (superSFB){
			updateCost(cpa,sender);
		}
		else {
			int bestLB = Integer.MAX_VALUE;
			for (int d : getDomain()){
				int dCost = calcAddedCost(cpa, d);
				if (dCost < bestLB){
					bestLB = dCost;
				}
			}
			send("LB_Report", bestLB, getId()).to(sender);
		}
	}
	
	//******only relevant for super-SFB******
	private void initSuperSFB() {
		cost = new int[getNumberOfVariables()][getDomainSize()];
		earlierConstraintAgent = new int[getNumberOfVariables()];
		lbReports = new int[getNumberOfVariables()];
		
		int constrainedAgent = -1;
		for (int i=0; i<this.getId(); i++){
			if (isConstrained(i, this.getId())){
				earlierConstraintAgent[i] = constrainedAgent;
				constrainedAgent = i;
			}
		}
		earlierConstraintAgent[this.getId()] = constrainedAgent;
	}
	
	private void updateCost(Assignment cpa, int sender) {
		int bestLB = Integer.MAX_VALUE;
		for (int d=0; d<getDomainSize(); d++){
			cost[sender][d] = getLocalCost(getEarlierConstrainedAgent(sender), d);
			cost[sender][d] += getConstraintCost(this.getId(), d, sender, cpa.getAssignment(sender));
			if (cost[sender][d]<bestLB){
				bestLB = cost[sender][d];
			}
		}
		
		send("LB_Report", bestLB, getId()).to(sender);
	}

	private int getLocalCost(int upToAgent, int val) {
		if (upToAgent==-1){
			return 0;
		}
		else {
			return cost[upToAgent][val];
		}
	}
	
	private int getEarlierConstrainedAgent(int agent) {
		return earlierConstraintAgent[agent];
	}
	
	private int getLastConstraintAgent() {
		return getEarlierConstrainedAgent(this.getId());
	}
	//******end of super-SFB procedures******

	@WhenReceived("Back_Track")
	public void handleBackTrack(){
		currentCpa.unassign(this.getId());
		handleCPA(currentCpa, currentCpaCost, lbReports);
	}

	@WhenReceived("LB_Report")
	public void handleLBReport(int cost, int sender){
		if (superSFB){
			lbReports[sender] = cost;
		}
		lb += cost;
		pendingReports--;
		if (pendingReports==0){
			if (superSFB){
				lb = 0;
				for(int i=getId()+1; i<this.getNumberOfVariables(); i++){
					lb+=lbReports[i];
				}
			}
			
			if (currentCpaCost + currentValCost+ lb < ub){
				send("CPA", currentCpa, currentCpaCost + currentValCost, lbReports).toNextAgent();
			} else {
				currentCpa.unassign(this.getId());
				handleCPA(currentCpa, currentCpaCost, lbReports);
			}
		}
	}
}
