package ext.sim.tools;

import bgu.dcr.az.api.DeepCopyable;

public class MaxSumEdge implements Comparable<MaxSumEdge>,DeepCopyable  {
	

	private NodeId[] variableNode;
	private int weight;
	
	public MaxSumEdge(int i, int j) {
		this.variableNode = new NodeId[2];
		this.variableNode[0] = new NodeId(i);
		this.variableNode[1] = new NodeId(j);
	}

	public int getVariableId(int i) {
		return getNode(i).getId1();
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	@Override
	public int compareTo(MaxSumEdge o) {
		return this.weight - o.weight;
	}
	
	public String toString() {
		return "<"+getNode(0)+","+getNode(1)+">: "+weight;
	}

	public NodeId getNode(int i) {
		return variableNode[i];
	}

	@Override
	public Object deepCopy() {
		MaxSumEdge m = new MaxSumEdge(getVariableId(0), getVariableId(1));
		return m;
	}
}
