package ext.sim.agents;

/**
 * 19-01-14
 * This version is 'incorrect' for ADCOPs, because if one agent holds a variable and a function node,
 * it sets the variables to always be first (which is wrong). Think, for example, of two agents A1 and A2
 * They are constrained between one another, and each one of them holds one variable node and one function
 * node (A1 - X1 and F12, A2 - X2 and F21 accordingly). In A2, F21 suppose to be before X2, but here the
 *  opposite occurs. YET - it produces interesting results.
 *  Go to 'NodeId' class, and see the CompareTo method for further explanations.
 *  
 * 18-01-14
 * A bug in the personal preferences was fixed (a bug in seeds). See variableNode class. 
 *  
 * 09-08-13 
 * I've added (after Roie's request) a feature that allow the use of UNARY CONSTRAINTS.
 * The difference is that now, variable nodes choose a set of preferences for their values (meaning, now
 * every value of every variable is now added with a small random integer).
 * This can also be interpreted as personal preferences. Every variable now choose at the beginning of the
 * run its preferences (small scale integer), and sums it along with the original message (which contains large
 * scale integer, representing constraint costs).
 * For example: suppose variable X3 (3 values in his domain) want to send this message :"3000, 0, 7000".
 * X3 also have its constant preferences: "2, 4, 6". The message he will send will be: "3002, 4, 7006".
 * By this we also achieve a new way for BREAKING TIES, instead of adding dust to the actual constraints costs.
 * 
 * NOTE: in case you want to stop the use of those unary constraints... simply remove from "VariabelNode" every
 * notion of the word "Preferences" ('addPreferences', 'setPreferences').
 *


 **/

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

//import com.sun.javafx.css.CalculatedValue;

import bgu.dcr.az.api.agt.SimpleAgent;
import bgu.dcr.az.api.ano.Algorithm;
import bgu.dcr.az.api.ano.Variable;
import bgu.dcr.az.api.ano.WhenReceived;
import bgu.dcr.az.api.prob.NeighboresSet;
import ext.sim.tools.FunctionNode;
import ext.sim.tools.MaxSumMessage;
import ext.sim.tools.MaxSumNode;
import ext.sim.tools.MyFileWriter;
import ext.sim.tools.NodeId;
import ext.sim.tools.VariableNode;
import ext.sim.tools.NodeId.NodeType;

@Algorithm(name="MaxSum", useIdleDetector=false)
public class MaxSumAgent extends SimpleAgent {
	
	@Variable(name="innerOrder", description="Inner order of constraints in the problem. Either SIO or PIO or DIO", defaultValue="SIO")
	protected String innerOrderString = "SIO";
	@Variable(name="asymmetric", description="makes the agent asymmetric", defaultValue="false")
	protected boolean asymmetric = false;
	@Variable(name="standardSort", description="Sets the entire algorithm to work with a standard sort for each constraint", defaultValue="true")
	protected boolean standardSort = true;
	@Variable(name="diminishingFactor", description="when messages are sent from variable to function, the dimishing factor is multiplied with the message vector", defaultValue="1")
	protected double diminishingFactor = 1;
	// use diminishingFactor (0...1) for applying a diminishing factor to messages
	// use diminishingFactor=-1 for applying MPLP algorithm
	// use diminishingFactor=-2 for applying MPLP "one at a time" algorithm
	
	//LIEL
	@Variable(name="dampingFactorVar", description="Damping factor (weight of old data) for Varible => Function message", defaultValue="0.0")
	double dampingFactorVar = 0.0;
	@Variable(name="dampingFactorFunc", description="Damping factor (weight of old data) for Function => Varible message", defaultValue="0.0")
	double dampingFactorFunc = 0.0;

	@Variable(name="dustOn", description="to unable dust, turn to FALSE", defaultValue="true")
	boolean dustOn = true;
	
	@Variable(name="NUMOFCYCLES", description="Number of cycles to run", defaultValue="2001")
	long NUMOFCYCLES = 2001;
	
	@Variable(name="cycleMultiplyAD", description="AD cycle length is n multiplied by this", defaultValue="4")
	int cycleMultiplyAD = 4;
	
	@Variable(name="addCostToConst", description="Add cost to each constraint entry", defaultValue="0")
	int addCostToConst = 0;
	
	//LIEL end
	
	Map<NodeId,MaxSumNode> nodes;
	long systemTime;
	static boolean debug;
	
	Random random;
	int selectBestOf = 1;
	int CYCLE;
	int convergence;
//    MyFileWriter fileWriter = null;
    boolean writeToFile = false;
    boolean changedDampingAlready = false;
    double dampingFactorVarOrig;
    double dampingFactorFuncOrig;
    
	/**  CHECK NodeId class for the sortLevel **/
	/** for changes in order, also check at 'MaxSumAD' class **/
	
    @Override
    public void start() 
    {
    	debug = false;
    	random = new Random();
    	setCycle();
    	setRandomAssignment();
    	initNodes();
    	initNodesNeighbors();
    	printNodes();
    	dampingFactorVarOrig = dampingFactorVar;
    	dampingFactorFuncOrig = dampingFactorFunc;
    	
    	//System.out.println(getNeighbors().size());
    }
    
    
    protected void setRandomAssignment() {
		this.submitCurrentAssignment(0);
	}//choose a 0 assignment (so it won't be empty) at the beginning

    protected void printNodes() {
		for (NodeId i: nodes.keySet()) {   
    		MaxSumNode n = nodes.get(i);
    		n.printNode();
		}
	}

    protected void initNodesNeighbors() {
		for (NodeId i: nodes.keySet()) {
    		MaxSumNode n = nodes.get(i);
    		n.initNeighbors();
    	}   		
	}//for every function/variable node, creates an updated neighbor list

	protected void initNodes() {
		nodes = new TreeMap<NodeId, MaxSumNode>();
		initVariableNode();
		initFunctionNodes();		
	}//initialize one variable node and all relevant function nodes

	protected void initVariableNode() {
		VariableNode n = new VariableNode(getId(), this, dustOn);
		nodes.put(n.getId(), n);		
	}//initialize one variable node for an agent
	
	protected void initFunctionNodes() {
		int otherId = getId()+1; //updating all the function nodes that are AFTER the variable (index-wise)
		while (otherId < getNumberOfVariables()) {
			if (isConstrained(getId(), otherId)) {
				FunctionNode n = new FunctionNode(getId(), otherId, this, addCostToConst);
				nodes.put(n.getId(), n);
			}
			otherId++;
		}
		if (asymmetric) { //for asymmetric agents, we generate extra function nodes 
			otherId = getId()-1; //updating all the function nodes that are BEFORE the variable (index-wise)
			while (otherId >= 0) {
				if (isConstrained(getId(), otherId)) {
					FunctionNode n = new FunctionNode(getId(), otherId, this, addCostToConst);
					nodes.put(n.getId(), n);
				}
				otherId--;
			}
		}
	}//initialize all function nodes for current variable node


	public void sendMessage(MaxSumMessage m) {
		int receiverAgent = getAgentId(m.getReceiver());
		send("UtilMsg", m).to(receiverAgent);
		debugMessage(m);
		
	}

	protected void debugMessage(MaxSumMessage m) {
		DEBUG("Msg : " + m.toString());
	}

	
	protected int getAgentId(NodeId n) {
		if (n==null) {
			DEBUG("null");
		}
		return n.getId1();
	}

	@WhenReceived("UtilMsg")
	public void handleUtilMsg(MaxSumMessage m) {
		MaxSumNode n = getReceiverNode(m);
		n.handleMessage(m);		
	} //receiver n updates the new message

	private MaxSumNode getReceiverNode(MaxSumMessage m) {
		MaxSumNode n = nodes.get(m.getReceiver());
		return n;
	}

	public void onMailBoxEmpty() {
		final long systemTime = getSystemTimeInTicks();
		this.systemTime = systemTime;
        DEBUG("\ncycle " + systemTime + " with agent " + getId());
		int v = getVariableNode().selectBestValue(selectBestOf); //the variable node sums all messages he got and chooses the lowest value
		this.submitCurrentAssignment(v); //choose the assignment
		sendMessages();//sending messages from all the nodes of the AGENT
		
		if (systemTime == NUMOFCYCLES) {
        	DEBUG(this.getId() + " finish, systemTime="+systemTime);
    		finish(v);
        }
	}

	/**
	 * Returns the variable node of this agent
	 * 
	 * @return VariableNode
	 */
	protected VariableNode getVariableNode() { 
		return (VariableNode) nodes.get(new NodeId(this.getId()));
	}

	protected void sendMessages() 
	{
		//if ((systemTime % 50) == 0) System.out.println("var "+dampingFactorVar + ", func " + dampingFactorFunc + ", cycle " + systemTime);
		
		for (NodeId i: nodes.keySet()) 
		{   
			MaxSumNode n = nodes.get(i);
			
			if (nodes.get(i) instanceof VariableNode)
			{
				if (getAlgorithmName().equals("MaxSumVPOrdered") && systemTime > CYCLE && systemTime % getNumberOfVariables() != getId())
				{
					n.sendMessages(diminishingFactor, 1.0);
				}
				else
				{
					n.sendMessages(diminishingFactor, dampingFactorVar);
				}
				
			}
			else n.sendMessages(dampingFactorFunc); //propagate a message to all of n's neighbors
    	}
	}//for each node in the agent (either function or variable) send a relevant message to all node's neighbors
	
	public static void DEBUG(String s) {
		if (debug) 
			System.out.println(s);
	}
	

	public long getMyConstraintCost(int id1, int v1, int id2, int v2) {
		//DEBUG("I'm agent: " + getId() +"! , Variables " + id1 + "," + id2 + "; " + "cell " + v1 + "," + v2 + " cost: " + getConstraintCost(id1, v1, id2, v2));
		return getConstraintCost(id1, v1, id2, v2);
}
	
	public boolean getAsymmetric() {
		return asymmetric;
	}
	
	public boolean isStandardSort() {
		return standardSort;
	}
	
	
	////  ***  MaxSumAD / MaxSumADVP  ***  ////
	
	public boolean isBefore(NodeId sender, NodeId receiver) {
		if (systemTime%CYCLE < CYCLE/2) 
			return sender.compareTo(receiver) <= 0;
		else 
			return sender.compareTo(receiver) > 0;
	}
	
	
	public void setCycle() {
		CYCLE = getNumberOfVariables() * cycleMultiplyAD;
		convergence = (int) NUMOFCYCLES;

	}
	
}