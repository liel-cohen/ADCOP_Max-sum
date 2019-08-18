package ext.sim.tools;


public class MaxSumVPMessage extends MaxSumMessage {

	int value;
	
	public MaxSumVPMessage(NodeId id, NodeId i, long[] t, int v) {
		super(id, i, t);
		value = v;
	}

	public MaxSumVPMessage(MaxSumMessage m, int v) {
		this(m.getSender(), m.getReceiver(), m.table, v);
	}
	
	public MaxSumVPMessage(NodeId sender, NodeId receiver) {
		super(sender, receiver);
	}

	@Override
	public Object deepCopy() {
		MaxSumVPMessage m = new MaxSumVPMessage(getSender(), getReceiver());
		m.table = new long[table.length];
		copyTable(table, m.table);
		m.value = value;
		return m;
	}
	
	public String toString() {
		return super.toString() + " { value: " + value + "}";
	}

}
