package ext.sim.tools;

public class NodeId implements java.lang.Comparable<NodeId> {
	public enum NodeType {Variable, Function}; //meaning: NodeType can only be 'Variable' or 'Function'
	public enum innerOrder {SIO, PIO, DIO}; //For asymmetric problems only, determines the inner order 

	private NodeType type;
	private int id1;
	private int id2;
	private boolean standardSort;
	private double weirdSortProb;
	public innerOrder innerOrder;
	//private double levelOfOrder = 0; //value of 1 means completely STANDARD order, 0 means non-standard (weird) order. Default value: 0.

	public NodeId(int id) {
		setId1(id);
		setId2(-1);
		setType(NodeType.Variable);
		setWeirdSortProb(Math.random()/2); //Probability to have a weird sort with another node
	} //constructor for variable nodes

	public NodeId(int id, int otherId) {	
		//setId1(Math.min(id, otherId));
		//setId2(Math.max(id, otherId));
		setId1(id);
		setId2(otherId);
		setType(NodeType.Function);
		setWeirdSortProb(Math.random()/2); //Probability to have a weird sort with another node
	} //constructor for function nodes (works on asymmetric problems too)

	public NodeId(int id, int otherId, boolean standardSort) {	
		this(id, otherId);
		setStandardSort(standardSort);
	} //constructor for function nodes (works on asymmetric problems too). Used for determining if a sort for each constraint would be standard or not

	public boolean equals(Object o){
		NodeId n = (NodeId)o;
		if (n == null) {
			return false;
		}
		return this.equals(n);
	}

	public boolean equals(NodeId n) {
		return (getId1() == n.getId1() && getId2() == n.getId2());
	}

	public String toString() {
		if (getId2() == -1) {
			return "(" + getId1() + ")";
		} else {
			return "(" + getId1() + "," + getId2() + ")";
		}
	}

	@Override
	public int compareTo(NodeId n) {
		
		if (n == null) {
			return -1;
		} //Comparing to an empty node

		if (getId1() != n.getId1()) {
			return getId1() - n.getId1();
		} //Comparing two different agents

		if (getType() == NodeType.Function && n.getType() == NodeType.Function) {
			return getId2() - n.getId2();
		} //Comparing two function nodes of the SAME agent

		if (getType() == NodeType.Variable && n.getType() == NodeType.Variable) {
			return 0;
		} //Comparing two variable nodes of the SAME agent

		if (getType() == NodeType.Variable && n.getType() == NodeType.Function) {
			if (n.isStandardSort())
				return getId1() - n.getId2();
			else
				return -1;
		} //Comparing a variable node to a function node of the SAME agent

		if (getType() == NodeType.Function && n.getType() == NodeType.Variable) {
			if (this.isStandardSort())
				return getId2() - n.getId1(); 
			else
				return 1;
		} //Comparing a function node to a variable node of the SAME agent
		
		System.out.println(this + ": SOMETHING WENT WRONG! you're not suppose to be here");
		return 1;
	} //this method can also handle ADCOPs, **** both standard and non-standard sort ****
	
	public int isBefore(NodeId n, String innerOrderString) { //replacing the CompareTo method
		setInnerOrder(innerOrderString.toUpperCase());
		if (n == null) {
			return -1;
		} //Comparing to an empty node

		if (getId1() != n.getId1()) {
			return getId1() - n.getId1();
		} //Comparing two different agents

		if (getType() == NodeType.Function && n.getType() == NodeType.Function) {
			return getId2() - n.getId2();
		} //Comparing two function nodes of the SAME agent

		if (getType() == NodeType.Variable && n.getType() == NodeType.Variable) {
			return 0;
		} //Comparing two variable nodes of the SAME agent

		if (getType() == NodeType.Variable && n.getType() == NodeType.Function) {
			switch (innerOrder) {
			case SIO:
				return getId1() - n.getId2();
			case PIO:
				return -1;
			case DIO:
				if (this.getId1() > n.getId2())
					return -1;
				else
					return 1;
			default:
				throw new AssertionError("Unsupported inner order type " + innerOrder);
			}
		} //Comparing a variable node to a function node of the SAME agent

		if (getType() == NodeType.Function && n.getType() == NodeType.Variable) {
			switch (innerOrder) {
			case SIO:
				return getId2() - n.getId1(); 
			case PIO:
				return 1;
			case DIO:
				if (this.getId2() > n.getId1())
					return -1;
				else
					return 1;
			default:
				throw new AssertionError("Unsupported inner order type " + innerOrder);
			}
		} //Comparing a function node to a variable node of the SAME agent

		System.out.println(this + ": SOMETHING WENT WRONG! you're not suppose to be here");
		return 1;
	}
	
	/** 31-05-14
	 * isBefore1 is used for testing weird sort 1 method.
	 * isBefore2 is used for testing weird sort 2 method.
	 * the change is made via the 'MaxSumADAgent' class (not very elegant, but what can you do...)
	 */
	//For weird sort 1 (also look at 'MaxSumAD' class)
	public int isBefore1(NodeId n, double levelOfOrder) {
		if (n == null) {
			return -1;
		} //Comparing to an empty node

		if (getId1() != n.getId1()) {
			return getId1() - n.getId1();
		} //Comparing two different agents

		if (getType() == NodeType.Function && n.getType() == NodeType.Function) {
			return getId2() - n.getId2();
		} //Comparing two function nodes of the SAME agent

		if (getType() == NodeType.Variable && n.getType() == NodeType.Variable) {
			return 0;
		} //Comparing two variable nodes of the SAME agent


		if (getType() == NodeType.Variable && n.getType() == NodeType.Function) {
			if (n.isStandardSort() || (this.getWeirdSortProb()+n.getWeirdSortProb() < levelOfOrder))
				return getId1() - n.getId2();
			else 
				return -1;
		} //Comparing a variable node to a function node of the SAME agent

		if (getType() == NodeType.Function && n.getType() == NodeType.Variable) {
			if (this.isStandardSort() || (this.getWeirdSortProb()+n.getWeirdSortProb() < levelOfOrder))
				return getId2() - n.getId1(); 
			else 
				return 1;
		} //Comparing a function node to a variable node of the SAME agent
		
		System.out.println(this + ": SOMETHING WENT WRONG! you're not suppose to be here");		
		return 1;
		
	}
	
	
	//For weird sort 2 (also look at 'MaxSumAD' class)
	public int isBefore2(NodeId n, double levelOfOrder) {
		if (n == null) {
			return -1;
		} //Comparing to an empty node

		if (getId1() != n.getId1()) {
			return getId1() - n.getId1();
		} //Comparing two different agents

		if (getType() == NodeType.Function && n.getType() == NodeType.Function) {
			return getId2() - n.getId2();
		} //Comparing two function nodes of the SAME agent

		if (getType() == NodeType.Variable && n.getType() == NodeType.Variable) {
			return 0;
		} //Comparing two variable nodes of the SAME agent


		if (getType() == NodeType.Variable && n.getType() == NodeType.Function) {
			if (n.isStandardSort() || (this.getWeirdSortProb()+n.getWeirdSortProb() < levelOfOrder))
				return getId1() - n.getId2();
			else {
				if (this.getId1() > n.getId2())
					return -1;
				else
					return 1;
			}
				
		} //Comparing a variable node to a function node of the SAME agent

		if (getType() == NodeType.Function && n.getType() == NodeType.Variable) {
			if (this.isStandardSort() || (this.getWeirdSortProb()+n.getWeirdSortProb() < levelOfOrder))
				return getId2() - n.getId1(); 
			else {
				if (this.getId2() > n.getId1())
					return -1;
				else
					return 1;
			}
		} //Comparing a function node to a variable node of the SAME agent
		
		System.out.println(this + ": SOMETHING WENT WRONG! you're not suppose to be here");		
		return 1;
		
	}
	
	
	/** 01-06-14
	 * Used for trying out weird sort 1.
	 * Here, the number of weird sorted constraints is set according to a
	 * parameter named weirdSortAgents, always STARTING from the first agent.
	 * For example, weirdSortAgents=5 means that all constraints that are involved with
	 * the first 5 agents will be ordered via weird sort 1. The rest - will remain 
	 * standard. To use this - change it in MaxSumADAgent class.
	 */
	public int isBefore3(NodeId n, int weirdSortAgents) {
	
		if (n == null) {
			return -1;
		} //Comparing to an empty node

		if (getId1() != n.getId1()) {
			return getId1() - n.getId1();
		} //Comparing two different agents

		if (getType() == NodeType.Function && n.getType() == NodeType.Function) {
			return getId2() - n.getId2();
		} //Comparing two function nodes of the SAME agent

		if (getType() == NodeType.Variable && n.getType() == NodeType.Variable) {
			return 0;
		} //Comparing two variable nodes of the SAME agent


		if (getType() == NodeType.Variable && n.getType() == NodeType.Function) {
			if (n.isStandardSort() || Math.min((Math.min(this.getId1(), n.getId1())),n.getId2()) >= weirdSortAgents)
				return getId1() - n.getId2();
			else
				return -1;
		} //Comparing a variable node to a function node of the SAME agent

		if (getType() == NodeType.Function && n.getType() == NodeType.Variable) {
			if (this.isStandardSort() || Math.min((Math.min(this.getId1(), this.getId2())),n.getId1()) >= weirdSortAgents)
				return getId2() - n.getId1(); 
			else 
				return 1;
		} //Comparing a function node to a variable node of the SAME agent
		 
		System.out.println(this + ": SOMETHING WENT WRONG! you're not suppose to be here");		
		return 1;
		
	}

	
	
	//For weird sort 4 (also look at 'MaxSumAD' class)
	//This is a combination between weird sort 1 and weird sort 2. The variable levelOfOrder determines
	//the level of weird sort 1. For example, levelOfOrder = 0.2 means 20% of the constraints would be
	//ordered by weird sort 1, 80% by weird sort 2.
	public int isBefore4(NodeId n, double levelOfOrder) {
		if (n == null) {
			return -1;
		} //Comparing to an empty node

		if (getId1() != n.getId1()) {
			return getId1() - n.getId1();
		} //Comparing two different agents

		if (getType() == NodeType.Function && n.getType() == NodeType.Function) {
			return getId2() - n.getId2();
		} //Comparing two function nodes of the SAME agent

		if (getType() == NodeType.Variable && n.getType() == NodeType.Variable) {
			return 0;
		} //Comparing two variable nodes of the SAME agent


		if (getType() == NodeType.Variable && n.getType() == NodeType.Function) {
			if (this.getWeirdSortProb()+n.getWeirdSortProb() < levelOfOrder)
				return -1;
			else {
				if (this.getId1() > n.getId2())
					return -1;
				else
					return 1;
			}
				
		} //Comparing a variable node to a function node of the SAME agent

		if (getType() == NodeType.Function && n.getType() == NodeType.Variable) {
			if (this.getWeirdSortProb()+n.getWeirdSortProb() < levelOfOrder)
				return 1;
			else {
				if (this.getId2() > n.getId1())
					return -1;
				else
					return 1;
			}
		} //Comparing a function node to a variable node of the SAME agent
		
		System.out.println(this + ": SOMETHING WENT WRONG! you're not suppose to be here");		
		return 1;
		
	}
	
	
	public void setInnerOrder (String IO) {
		this.innerOrder = innerOrder.valueOf(IO);
	}

	public int getId1() {
		return id1;
	}

	public void setId1(int id1) {
		this.id1 = id1;
	}

	public NodeType getType() {
		return type;
	}

	public void setType(NodeType type) {
		this.type = type;
	}

	public int getId2() {
		return id2;
	}

	public void setId2(int id2) {
		this.id2 = id2;
	}

	public boolean isStandardSort() {
		return standardSort;
	}

	public void setStandardSort(boolean standardSort) {
		this.standardSort = standardSort;
	}

	public double getWeirdSortProb() {
		return weirdSortProb;
	}

	public void setWeirdSortProb(double weirdSortProb) {
		this.weirdSortProb = weirdSortProb;
	}
	
}	