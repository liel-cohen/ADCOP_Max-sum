package ext.sim.agents;

//import bgu.dcr.az.api.*;
import java.util.*;

import bgu.dcr.az.api.agt.*;
import bgu.dcr.az.api.ano.*;
import bgu.dcr.az.api.tools.*;
import bgu.dcr.az.api.ano.WhenReceived;

@Algorithm(name="SimpleSBB", useIdleDetector=false)
public class SimpleSBBAgent extends SimpleAgent {

	
	int upperBound;
	HashSet<Integer> currentDomain;
	int addedCost;
	
	//relevant for first agent only
	Assignment solution;
	int solutionCost;
	
    @Override
    public void start() {
    	upperBound = Integer.MAX_VALUE;
    	currentDomain = new HashSet<Integer>(getDomain());
    	if (isFirstAgent()) {
    		Assignment cpa = new Assignment();
        	cpa.assign(getId(), 0);
        	send("CPA", cpa, 0).toNextAgent();
        	currentDomain.remove(0);
        	solutionCost = Integer.MAX_VALUE;
        }
    }

    private int calcAddedCost(Assignment cpa, int val){
    	int sum = 0;
    	for (Integer agent : cpa.assignedVariables()){
    		sum += getConstraintCost(getId(), val, agent, cpa.getAssignment(agent));
    	}
    	return sum;
    }
    
	private void findAssignment(Assignment cpa, int lowerBound) {
		Iterator<Integer> iter = currentDomain.iterator();
		boolean assignmentFound = false;
		while (!assignmentFound && !currentDomain.isEmpty()){
			int value = iter.next();
			iter.remove();
			addedCost = calcAddedCost(cpa, value);
			if (lowerBound + addedCost < upperBound){
				cpa.assign(getId(), value);
				if (isLastAgent()){
					upperBound = lowerBound + addedCost;
					broadcast("Upper Bound", upperBound);
					send("Possible Solution", cpa, upperBound).toFirstAgent();
					cpa.unassign(getId());
				} else {
					send("CPA", cpa, lowerBound + addedCost).toNextAgent();
					assignmentFound = true;
				}
			}
		}
		if (!assignmentFound){// && currentDomain.isEmpty()){
			addedCost = 0;
			backTrack(cpa, lowerBound);
		}
	}

	private void backTrack(Assignment cpa, int lowerBound) {
		cpa.unassign(getId());
		currentDomain = new HashSet<Integer>(getDomain());
		if (isFirstAgent()){
			finish(solution);
		} else{
			send("BackTrack CPA", cpa, lowerBound - addedCost).toPreviousAgent();
		}
	}
	
	@WhenReceived("CPA")
	public void handleCPA(Assignment cpa, int lowerBound){
		findAssignment(cpa, lowerBound);
	}

	@WhenReceived("BackTrack CPA")
	public void handleBackTrackCPA(Assignment cpa, int lowerBound){
		cpa.unassign(getId());
		findAssignment(cpa, lowerBound - addedCost);
	}
	
	@WhenReceived("Upper Bound")
	public void handleUpperBound(int newUpperBound){
		upperBound = newUpperBound;
	}

	@WhenReceived("Possible Solution")
	public void handlePossibleSolution(Assignment cpa, int cost){
		if (cost < solutionCost){
			solution = cpa;
			solutionCost = cost;
		}
	}

	@WhenReceived("Solution")
	public void handleSolution(Assignment solution){
		int value = solution.findMinimalCostValue(getId(), currentDomain, getProblem());
		solution.assign(getId(), value);
		finish(solution);
	}

}
