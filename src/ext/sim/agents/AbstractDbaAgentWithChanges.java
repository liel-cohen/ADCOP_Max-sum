package ext.sim.agents;

import java.util.ArrayList;
import java.util.Set;

import ext.sim.tools.Pair;
import bgu.dcr.az.api.ano.Variable;
import bgu.dcr.az.api.ano.WhenReceived;
import bgu.dcr.az.api.tools.Assignment;

/**
 * Abstract base class for DBA-like algorithms.
 * @author Steven
 *
 */
public abstract class AbstractDbaAgentWithChanges extends AbstractSeedableAgent {
	
	@Variable(name="max-cycles", description="Maximum number of cycles to run the algorithm", defaultValue="2000")
	long maxCycles = 2000;
	
	/**
	 * The assignment of this agent and its neighbors, as reported through value messages.
	 */
	protected Assignment localView;

	protected int myImprovement;
	protected int newValue;
	protected boolean canMove;
	protected boolean quasiLocalMinimum;

	/**
	 * Statistic for the number of completed iterations the agent was in a QLM.
	 */
	protected long qlmDuration;
	/**
	 * Statistic for how many consecutive breakouts the agent has had.
	 */
	protected long breakoutDuration;
	protected long numQlms;
	
	protected Mode mode;
	
	@Override
	protected void __start() {
		qlmDuration = 0;
		breakoutDuration = 0;
		numQlms = 0;
		localView = new Assignment();
		myImprovement = 0;
		// choose a random value
		int val = random(getDomain());
		localView.assign(getId(), val);
		submitCurrentAssignment(val);
		send("ValueMessage", val).toNeighbores();
		mode = Mode.WAIT_VALUE;
	}

	@WhenReceived("ValueMessage")
	public void handleHandleValueMessage(int val) {
		int sender = getCurrentMessage().getSender();
		localView.assign(sender, val);
	}
	
	@WhenReceived("ImproveMessage")
	public void handleImproveMessage(int improvement) {
		int sender = getCurrentMessage().getSender();
		if (improvement > myImprovement) {
			canMove = false;
			if (improvement > 0) {
				quasiLocalMinimum = false;				
			}
		} else if (improvement == myImprovement && !winsTieAgainst(sender)) {
			canMove = false;
		}
		
		handleReceivedImprovement(sender, improvement);
	}
	
	protected abstract void handleReceivedImprovement(int sender, int improvement);

	/**
	 * Gets the number of constraints involving this agent that are currently NZ-violated.  Constraints
	 * are counted uniquely, i.e., for only one agent.  Currently this is done by assigning the count
	 * of a violated constraint to the involved agent with the lowest index.  This allows the total number
	 * of NZ-violated constraints in the system to be calculated by summing the unique NZ violations for
	 * all agents.  
	 * @return The number of NZ-violations.
	 */
	public int getUniqueNZViolations() {
		final int myId = getId();
		final int myVal = localView.getAssignment(myId);
		int viols = 0;
		for (int neighbor : getNeighbors()) {
			if (myId >= neighbor || !localView.isAssigned(neighbor)) {
				continue;
			}
			if (getConstraintCost(myId, myVal, neighbor, localView.getAssignment(neighbor)) > 0) {
				viols++;
			}
		}
		return viols;
	}
	
	public abstract int calculateEffCost(Assignment ass);
	
	public abstract boolean winsTieAgainst(int id);
	
	public abstract void breakout();

	/**
	 * Gets the total number of iterations the agent has run the breakout procedure.
	 * @return The number of breakouts.
	 */
	public abstract long getNumBreakouts();
	/**
	 * The total number of constraints that have been broken out of.
	 * @return
	 */
	public abstract long getNumConstraintBreakouts();
	
	/**
	 * Get the number of constraints currently violated. 
	 * @return
	 */
	public abstract int getNumViolated();
	
	public long getNumQlms() {
		return numQlms;
	}

	public boolean wasInQlm() {
		return qlmDuration > 0;
	}
	
	public long getQlmDuration() {
		return qlmDuration;
	}
	
	public boolean brokeOut() {
		return breakoutDuration > 0;
	}
	
	public long getBreakoutDuration() {
		return breakoutDuration;
	}

	public abstract long getNumRepeatBreakouts();
	
	protected Pair<Integer, Integer> getBestAlternative(int currCost) {
		Set<Integer> domain = getDomain();
		int currVal = localView.getAssignment(getId());
		ArrayList<Integer> bestVals = new ArrayList<>(domain.size() - 1);
		Integer bestCost = Integer.MAX_VALUE;
		for (int val : domain) {
			if (val != currVal) {
				localView.assign(getId(), val);
				int cost = calculateEffCost(localView);
				if (cost < bestCost) {
					bestVals.clear();
					bestCost = cost;
				}
				if (cost == bestCost) {
					bestVals.add(val);
				}
			}
		}
		// undo the changes to localView
		localView.assign(getId(), currVal);
		// return a randomly chosen best value
		if (bestVals.isEmpty()) {
			return new Pair<>(null, null);
		} else if (bestVals.size() == 1) {
			return new Pair<>(bestVals.get(0), bestCost);
		}
		int val = random(bestVals);
		return new Pair<>(val, bestCost);
	}

	protected void waitValue() {
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

	public void onMailBoxEmpty() {
		switch (mode) {
		case WAIT_VALUE:
			waitValue();
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
			this.finish(localView.getAssignment(getId()));
		}
	}
	
	protected static enum Mode {
		WAIT_VALUE, WAIT_CONSTRAINT ,WAIT_IMPROVE;
	}

}
