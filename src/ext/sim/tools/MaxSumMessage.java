package ext.sim.tools;

import java.util.Arrays;

import bgu.dcr.az.api.DeepCopyable;

public class MaxSumMessage implements DeepCopyable {
	

	private NodeId sender;
	private NodeId receiver;
	long[] table;

	public MaxSumMessage(NodeId sender, NodeId receiver) {
		setSender(sender);
		setReceiver(receiver);
	}

	public MaxSumMessage(NodeId id, NodeId i, long[] v) {
		this(id, i);
		table = new long[v.length];
		copyTable(v, table);
		
	}//a message is composed of: sender, receiver and the table of values
	

	protected void copyTable(long[] from, long[] to) {
		for (int x=0; x<from.length; x++) {
			to[x] = from[x];
		}
	}

	/**
	 * Returns a copy of the message
	 */
	@Override
	public Object deepCopy() {
		MaxSumMessage m = new MaxSumMessage(getSender(), getReceiver());
		m.table = new long[table.length];
		copyTable(table, m.table);
		return m;
	}
	
	public String toString() {
		return ""+ getSender() + "-->"+ getReceiver() +" : "+ Arrays.toString(table) ;
	}

	public NodeId getReceiver() {
		return receiver;
	}

	public void setReceiver(NodeId receiver) {
		this.receiver = receiver;
	}

	public NodeId getSender() {
		return sender;
	}

	public void setSender(NodeId sender) {
		this.sender = sender;
	}
	
	public long[] getTable()
	{
		return table;
	}

}
