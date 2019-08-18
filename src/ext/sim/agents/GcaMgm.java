package ext.sim.agents;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import bgu.dcr.az.api.ano.Algorithm;
import bgu.dcr.az.api.ano.WhenReceived;
import bgu.dcr.az.api.tools.Assignment;
import ext.sim.tools.Pair;

@Algorithm(name="GCA_MGM", useIdleDetector=false)
public class GcaMgm extends abstractEntriesCountableAgent {

	long numViols = 0;
	private int[][][] localConstraintCosts;
	private Map<Integer, Integer> neighborsLRs = new HashMap<Integer,Integer>();
	private Assignment tempNewAgentView = new Assignment();
	
	@Override
	public int calculateEffCost(Assignment cpa) {
		//return getProblem().calculateCost(ass);
		
		int cost = 0;
		
		int value = cpa.getAssignment(getId());
		for (int otherAgent = 0; otherAgent < this.localConstraintCosts.length; otherAgent++) {
			
			if (cpa.isAssigned(otherAgent) && (otherAgent != getId()))
			{
				Integer otherAgentAssignment = cpa.getAssignment(otherAgent);
				cost += getConstraintCost(value , otherAgent, otherAgentAssignment);
			}
		}
		
		return cost + getConstraintCost(getId(), value);
	}

	private int getConstraintCost(Integer myAssignment, int otherAgent, Integer otherAgentAssignment) {
		getConstraintCost(getId(),myAssignment,otherAgent,otherAgentAssignment);
		return this.localConstraintCosts[otherAgent][myAssignment][otherAgentAssignment];
	}
	
	protected void initializeLocalConstraintCosts() {
		this.localConstraintCosts = new int[getNumberOfVariables()][][];
		
		for (int i = 0; i < getNumberOfVariables(); i++) {
			this.localConstraintCosts[i] = new int[getDomainSize()][getDomainOf(i).size()];
		}
		
		for(int i=0; i < this.localConstraintCosts.length; i++)
		{
			for(int j = 0; j<this.localConstraintCosts[i].length; j++)
			{
				for (int k = 0; k<this.localConstraintCosts[i][j].length; k++)
				{
					this.localConstraintCosts[i][j][k] = getConstraintCost(getId(), j, i, k);
				}
			}
		}
	}
	
	private int getLocalConstraintCost(int neighbor, Integer myAssignment,
			Integer neighborAssignment) {
		return this.localConstraintCosts[neighbor][myAssignment][neighborAssignment];
	}
	
	private void setLocalConstraintCost(int sender, Integer myAssignment, Integer neighborAssignment, int cost) {
		this.localConstraintCosts[sender][myAssignment][neighborAssignment] = cost;
	}
	
	@Override
	public boolean winsTieAgainst(int id) {
		return getId() < id;
	}

	@Override
	public void breakout() {
		// do nothing ... we're MGM!
	}

	@Override
	public long getNumBreakouts() {
		return 0;
	}
	
	@Override
	public long getNumRepeatBreakouts() {
		return 0;
	}

	@Override
	public long getNumConstraintBreakouts() {
		return 0;
	}
	
	@Override
	public int getNumViolated() {
		// return the NZ violations
		final int myId = getId();
		final int myVal = localView.getAssignment(myId);
		int numViols = 0;
		for (int neighbor : getNeighbors()) {
			if (this.getConstraintCost(myId, myVal, neighbor, localView.getAssignment(neighbor)) > 0) {
				numViols++;
			}
		}
		return numViols;
	}

	@Override
	protected void handleReceivedImprovement(int sender, int improvement) {
		this.neighborsLRs.put(sender,improvement);
	}
	
	@WhenReceived("ValueMessage")
	public void handleHandleValueMessage(int val) {
		int sender = getCurrentMessage().getSender();
		this.tempNewAgentView.assign(sender, val);
	}
	
	protected void waitValue() {
		
		for(Integer agent : tempNewAgentView.assignedVariables())
		{
			if (neighborsLRs.containsKey(agent))
			{
				Integer neighborsNewAssignment = tempNewAgentView.getAssignment(agent);
				int newCost = getLocalConstraintCost(agent,localView.getAssignment(getId()),neighborsNewAssignment);
				int oldCost = getLocalConstraintCost(agent, localView.getAssignment(getId()), localView.getAssignment(agent));
				
				int delta = newCost - oldCost;
				
				if (delta > 0)
				{
					Integer myAssignment = localView.getAssignment(getId());
					int constraintCost = getConstraintCost(myAssignment, agent, neighborsNewAssignment);
					setLocalConstraintCost(agent, myAssignment, neighborsNewAssignment,0);
					send("ConstraintCost", myAssignment, neighborsNewAssignment, constraintCost).to(agent);
				}
			}
		}
		
		updateLocalView();
	}

	private void updateLocalView() {
		for(Integer agent : tempNewAgentView.assignedVariables())
		{
			this.localView.assign(agent, tempNewAgentView.getAssignment(agent));
		}
		tempNewAgentView = new Assignment();
	}
	
	@WhenReceived("ConstraintCost")
	public void handleConstraintCost(Integer senderValue, Integer receiverValue, Integer cost){
		int sender = getCurrentMessage().getSender();
		this.localConstraintCosts[sender][receiverValue][senderValue] += cost;
		
		abstractEntriesCountableAgent.entriesCounter.incrementAndGet();
		abstractEntriesCountableAgent.pairsEntriesCounters[sender][getId()].incrementAndGet();
	}
	
	private void waitConstraint() {
		int currCost = calculateEffCost(localView);
		
		Pair<Integer, Integer> bestAlt = getBestAlternative(currCost);
		newValue = bestAlt.getFirst();
		myImprovement = currCost - bestAlt.getSecond();

		if (myImprovement > 0) {
			canMove = true;
			quasiLocalMinimum = false;
		} else {
			canMove = false;
			quasiLocalMinimum = true;
		}
		send("ImproveMessage", myImprovement).toNeighbores();
	}
	
	protected void waitImprove() {
		if (quasiLocalMinimum) {
			qlmDuration++;
			numQlms++;
			breakout();
		} else {
			qlmDuration = 0;
			breakoutDuration = 0;
		}
		if (canMove) {
			submitCurrentAssignment(newValue);
			localView.assign(getId(), newValue);
			send("ValueMessage", newValue).toNeighbores();
		}
	}

	private static AtomicInteger repeatCounter = new AtomicInteger();
	
	public void onMailBoxEmpty() {
		switch (mode) {
		case WAIT_VALUE:
			waitValue();
			mode = Mode.WAIT_CONSTRAINT;
			break;
		case WAIT_CONSTRAINT:
			waitConstraint();
			mode = Mode.WAIT_IMPROVE;
			break;
		case WAIT_IMPROVE:
			waitImprove();
			mode = Mode.WAIT_VALUE;
			break;
		default:
			throw new AssertionError("Unknown mode \"" + mode + "\".");
		}

		long time = getSystemTimeInTicks();
		if (time >= maxCycles) {
			
			if (isLastAgent())
			{
				try {
					
					MyFileWriter writer = MyFileWriter.getInstance();
					
					repeatCounter.incrementAndGet();
					
					try {
						writer.write(abstractEntriesCountableAgent.getReport());
						
						if (repeatCounter.intValue() == 50)
						{
							writer.close();
						}
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				} catch (FileNotFoundException | UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			this.finish(localView.getAssignment(getId()));
		}
	}
}
