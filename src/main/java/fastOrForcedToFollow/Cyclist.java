package fastOrForcedToFollow;


/**
 * @author mpaulsen
 *
 */
public final class Cyclist {


	private final String id;
	private final double desiredSpeed;
	private double speed; // Current speed
	private double tEarliestExit;
	private final LinkTransmissionModel ltm;

	private Cyclist(String id, double desiredSpeed, LinkTransmissionModel ltm) {
		this.id = id;
		this.desiredSpeed = desiredSpeed;
		this.ltm = ltm;
	}

	public static Cyclist createIndividualisedCyclistWithSqrtLTM(final String id, final double desiredSpeed, final double z_c, final double lambda_c){
		LinkTransmissionModel ltm = new SqrtLTM(Runner.theta_0 + z_c * Runner.zeta_0,	Runner.theta_1 + z_c * Runner.zeta_1,   lambda_c);
		return new Cyclist(id, desiredSpeed, ltm);
	}
	
	
	/**
	 * @return The bicycle length of the cyclist [m].
	 */
	public double getBicycleLength() {
		return this.ltm.getBicycleLength();
	}
	
	/**
	 * @return The desired speed (in m/s) of the cyclist.
	 */
	public double getDesiredSpeed() {
		return desiredSpeed;
	}

	/**
	 * @return The integer id of the cyclist.
	 */
	public String getId() {
		return id;
	}


	public void setSpeed(final double newCurrentSpeed) {
		if (newCurrentSpeed < desiredSpeed) {
			this.speed = newCurrentSpeed;
		} else {
			this.speed = desiredSpeed;
		}
	}

	public PseudoLane selectPseudoLane(final Link receivingLink) {
		return this.ltm.selectPseudoLane(receivingLink, this.desiredSpeed, this.tEarliestExit);
	}

	public double getVMax(final PseudoLane pseudoLane) {
		return Math.min(desiredSpeed, this.ltm.getLaneVMax(pseudoLane, this.tEarliestExit));
	}

	public double getTEarliestExit() {
		return this.tEarliestExit;
	}

	public void setTEarliestExit(final double time) {
		this.tEarliestExit = time;
	}

	public double getSafetyBufferDistance(final double speed) {
		return this.ltm.getSafetyBufferDistance(speed);
	}

	public double getSpeed() {
		return this.speed;
	}

}
