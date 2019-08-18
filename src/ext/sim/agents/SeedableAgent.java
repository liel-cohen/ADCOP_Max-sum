package ext.sim.agents;

/**
 * Interface for agents that support seeding through setting of a problem's <code>"alg-seed"</code>
 * field of its metadata.
 * @author Steven
 *
 */
public interface SeedableAgent {
	/**
	 * Gets the seed, if it has been set.
	 * @return The seed.
	 */
	public long getSeed();
	/**
	 * Checks whether the agent has been seeded.
	 * @return <code>true</code> if the agent has been seeded, <code>false</code> otherwise.
	 */
	public boolean seeded();
}
