package ext.sim.agents;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import bgu.dcr.az.api.ano.Algorithm;
import bgu.dcr.az.api.ano.WhenReceived;
import bgu.dcr.az.api.tools.Assignment;
import ext.sim.tools.MaxSumEdge;
import ext.sim.tools.MaxSumNode;
import ext.sim.tools.NodeId;

@Algorithm(name="BoundedMaxSum", useIdleDetector=false)
public class BoundedMaxSumAgent extends MaxSumAgent {

	MaxSumEdge lastEdge;
	List<MaxSumEdge> edges, spanningTreeEdges;
	boolean treePhaseDone = false;
	int treeCycle;

	@Override
	public void start() {
		super.start();
		if (isFirstAgent()) {
			buildSpanningTree();
		}
	}

	private void buildSpanningTree() {
		edges = new LinkedList<MaxSumEdge>();
		spanningTreeEdges = new LinkedList<MaxSumEdge>();
		int numberOfSpanningTreeEdges = countVariables() - 1;
		addAllEdges(edges);
		Collections.sort(edges);
		DEBUG("All edges (sorted from smallest weight): \n" + edges);
		while (spanningTreeEdges.size() < numberOfSpanningTreeEdges) { //Kruskal's algorithm for minimal spanning tree
			takeLastEdge();
			if (checkConnection(lastEdge)) {
				spanningTreeEdges.add(lastEdge);
				DEBUG("Edge " + lastEdge + " was added to the spanning tree");
			}
			else 
				edges.add(0, lastEdge); //append to beginning of list
		}	
		DEBUG("SPANNING TREE edges: \n" + spanningTreeEdges);
		sendDisconnectMessages();
		notifyTreePhaseDone();
	}

	private int countVariables() {
		Set<Integer> countSet = new HashSet<Integer>();
		for (int i = 0; i < getNumberOfVariables(); i++) {
			for (int j = i+1; j < getNumberOfVariables(); j++) {
				if (isConstrained(i, j)) {
					countSet.add(i);
					countSet.add(j);
				}
			}	
		}
		return countSet.size();
	} //counts the number of active variables in the problem (needed for unconnected problems)

	private void addAllEdges(List<MaxSumEdge> edges) {
		for (int i=0; i<getNumberOfVariables(); i++) {
			for (int j=i+1; j<getNumberOfVariables(); j++) {
				if (isConstrained(i, j)) {
					MaxSumEdge e = new MaxSumEdge(i,j);
					edges.add(e);
					calcEdgeWeight(e);
					if (asymmetric) { //for asymetric problems
						e = new MaxSumEdge(j,i);
						edges.add(e);
						calcEdgeWeight(e);
					}
				}
			}
		}
	} //add all edges (i,j) to the edges list

	private void calcEdgeWeight(MaxSumEdge e) {
		int var1 = e.getVariableId(0);
		int var2 = e.getVariableId(1);
		int maxDiff = -1;
		int max = -1;
		int min = Integer.MAX_VALUE;

		for (int v1=0; v1<getDomainSize(); v1++) {
			for (int v2=0; v2<getDomainSize(); v2++) {
				Assignment a = new Assignment();
				a.assign(var1, v1);
				a.assign(var2, v2);
				int cost = getProblem().calculateCost(a);
				min = Math.min(min, cost);
				max = Math.max(max, cost);
				maxDiff = Math.max(maxDiff, max - min);		
			}
		}
		e.setWeight(maxDiff);
	} //a weight of an edge (a,b) is the max difference between its costs in the constraint

	protected void takeLastEdge() {
		lastEdge = edges.remove(edges.size()-1);
		treeCycle++;
		DEBUG("treeCycle " + treeCycle +": trying to add edge " + lastEdge);
	}

	private boolean checkConnection(MaxSumEdge candidateEdge) {
		NodeId var1 = new NodeId(candidateEdge.getVariableId(0));
		NodeId var2 = new NodeId(candidateEdge.getVariableId(1));
		boolean flag1, flag2; 
		flag1 = flag2 = false;	
		if (spanningTreeEdges.isEmpty()) 
			return true;
		for (MaxSumEdge e : spanningTreeEdges) {
			if (var1.equals(e.getNode(0)) || var1.equals(e.getNode(1))) 
				flag1 = true;
			if (var2.equals(e.getNode(0)) || var2.equals(e.getNode(1)))
				flag2 = true;
		}
		return (flag1 && !flag2) || (!flag1 && flag2);
	}//One node in the candidateEdge is not part of the subgraph, and the other node is.

	private void sendDisconnectMessages() {
		for (MaxSumEdge e : edges) {
			send("Disconnect", e).to(e.getVariableId(0));
			send("Disconnect", e).to(e.getVariableId(1));
		}
	}

	@WhenReceived("Disconnect")
	public void handleDisconnect(MaxSumEdge e) {
		NodeId currentNode = new NodeId(getId());
		MaxSumNode node = nodes.get(currentNode);
		if (currentNode.equals(e.getNode(0))) { //if you're a, remove function [a,b] from neighbors and nodes list
			node.disconnect(e.getVariableId(0), e.getVariableId(1), new int[getDomain().size()]); //remove function [a,b] from your neighbors list
			NodeId functionNodeToRemove = new NodeId(getId(), e.getVariableId(1));
			nodes.remove(functionNodeToRemove); //remove function [a,b] from your nodes list
			DEBUG("\nAgent " + getId() + " removed " + functionNodeToRemove + " from his nodes list");
			/*if (asymmetric) {
				node.disconnect(e.getVariableId(1), e.getVariableId(0)); //if asymmetric agent, then function [b,a] has to be removed from neighbor list too
			}*/
		}
		else if (currentNode.equals(e.getNode(1))) { //if you're b, remove function [a,b] from neighbors list
			node.disconnect(e.getVariableId(0), e.getVariableId(1), new int[getDomain().size()]);
			/*
			if (asymmetric) {
				node.disconnect(e.getVariableId(1), e.getVariableId(0)); //if asymmetric agent, then function [b,a] has to be removed from neighbor list too
				NodeId functionNodeToRemove = new NodeId(getId(), e.getVariableId(0));
				nodes.remove(functionNodeToRemove); //remove function [a,b] from your nodes list
				DEBUG("\nAgent " + getId() + " removed " + functionNodeToRemove + " from his nodes list (for asymmetric problems)");	
			}*/
		}
	}


	private void notifyTreePhaseDone() {
		//NodeId node = new NodeId(getId());
		for (int i=0; i<getNumberOfVariables(); i++) {
			send("TreePhaseDone").to(i);
		}		
	} //inform all agents (including yourself) that the spanning tree buildup is complete

	@WhenReceived("TreePhaseDone")
	public void handleTreePhaseDone() {
		treePhaseDone = true;
		DEBUG("Agent " + this.getId() + " reported TreePhaseDone, systemTime="+systemTime);
	}


	public void onMailBoxEmpty() {
		//final long systemTime = getSystemTimeInTicks();
		//if (treePhaseDone || systemTime > 100) {
		if (treePhaseDone) {
			super.onMailBoxEmpty();
		}	
	}
}
