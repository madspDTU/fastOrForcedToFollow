package fastOrForcedToFollow;


/**
 * @author mpaulsen
 *
 */
public final class Cyclist {

	/**
	 * The desired (/maximum) speed [m/s] of the cyclist.
	 */
	private final double desiredSpeed;
	
	/**
	 * The link transmission model handling the length, safety distance, lane choice, and speed of the cyclist.
	 */
	private final LinkTransmissionModel ltm;
	
	/**
	 * The current speed [m/s] of the cyclist.
	 */
	private double speed;
	
	/**
	 * The time [s] at which the cyclist will get to the end of the current link, and thus have a non-zero probability of leaving the link.
	 */
	private double tEarliestExit;
	
	/**
	 * The current sublink index, i.e. at which sublink [zero-indiced] is the cyclist currently at.
	 */
	private int currentSublinkIndex;

	
	/**
	 * A static factory method for constructing a cyclist with a square root based link transmission model.
	 * 
	 * @param desiredSpeed The desired speed [m/s] of the cyclist.
	 * @param z_c The value in [0,1] determining the headway preferences of the cyclist.
	 * @param lambda_c The length of the cyclist's bicycle.
	 * @return A cyclist having desired speed, headway preferences, and a length based on the above.
	 */
	public static Cyclist createIndividualisedCyclistWithSqrtLTM(final double desiredSpeed, final double theta_0, final double theta_1, final double lambda_c){
		LinkTransmissionModel ltm = new SqrtLTM(theta_0, theta_1, lambda_c);
		return new Cyclist(desiredSpeed, ltm);
	}
	
	private Cyclist(double desiredSpeed, LinkTransmissionModel ltm) {
		this.desiredSpeed = desiredSpeed;
		this.ltm = ltm;
	}
	
	
	/**
	 * See corresponding method in {@link fastOrForcedToFollow.LinkTransmissionModel#getBicycleLength() LinkTransmissionModel}
	 */
	public double getBicycleLength() {
		return this.ltm.getBicycleLength();
	}
	

	public double getDesiredSpeed() {
		return desiredSpeed;
	}

	public int getCurrentLinkIndex(){
		return this.currentSublinkIndex;
	}

	/**
	 * See corresponding method in {@link fastOrForcedToFollow.LinkTransmissionModel#getSafetyBufferDistance(double) LinkTransmissionModel}
	 */
	public double getSafetyBufferDistance(final double speed) {
		return this.ltm.getSafetyBufferDistance(speed);
	}

	public double getSpeed() {
		return this.speed;
	}

	public double getTEarliestExit() {
		return this.tEarliestExit;
	}

	/**
	 * See corresponding method in {@link fastOrForcedToFollow.LinkTransmissionModel#getLaneVMax(PseudoLane, double) LinkTransmissionModel}
	 */
	public double getVMax(final PseudoLane pseudoLane) {
		return this.ltm.getLaneVMax(pseudoLane, this.tEarliestExit);
	}

	/**
	 * See corresponding method in {@link fastOrForcedToFollow.LinkTransmissionModel#selectPseudoLane(Sublink, double, double) LinkTransmissionModel}
	 */
	public PseudoLane selectPseudoLane(final Sublink receivingLink) {
		return this.ltm.selectPseudoLane(receivingLink, this.desiredSpeed, this.tEarliestExit);
	}
	
	public void resetCurrentLinkIndex(){
		this.currentSublinkIndex = 0;
	}
	
	public void incrementCurrentLinkIndex(){
		this.currentSublinkIndex++;
	}

	/**s
	 * @param newCurrentSpeed The provisional speed that the cyclist will get if it doesn't exceed his/her desired speed.
	 */
	public void setSpeed(final double newCurrentSpeed) {
		if (newCurrentSpeed < desiredSpeed) {
			this.speed = newCurrentSpeed;
		} else {
			this.speed = desiredSpeed;
		}
	}

	public void setTEarliestExit(final double time) {
		this.tEarliestExit = time;
	}

}
