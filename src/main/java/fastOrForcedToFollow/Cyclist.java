package fastOrForcedToFollow;

import org.matsim.core.mobsim.qsim.qnetsimengine.QCycleAsVehicle;

/**
 * @author mpaulsen
 *
 */
public class Cyclist {


	private final String id;
	private final double desiredSpeed;
	private double speed = -1; // Current speed
	private double tStart = 0; // Time at which the cyclist entered the link.
	private double tEarliestExit = 0;
	private final LinkTransmissionModel ltm;
	private Link currentLink = null;
	private QCycleAsVehicle qCyc = null;

	public Cyclist(String id, double desiredSpeed, double theta_0, double theta_1) {
		this.id = id;
		this.desiredSpeed = desiredSpeed;
		this.ltm = new LinkTransmissionModel(theta_0, theta_1);
	}

	public static Cyclist createGlobalCyclist(String id, double desiredSpeed) {
		return new Cyclist(id, desiredSpeed, Runner.theta_0, Runner.theta_1);
	}

	public static Cyclist createIndividualisedCyclist(String id, double desiredSpeed, double z_c) {
		return new Cyclist(id, desiredSpeed, Runner.theta_0 + z_c * Runner.zeta_0,
				Runner.theta_1 + z_c * Runner.zeta_1);
	}

	public void setQCycleAsVehicle(QCycleAsVehicle qCyc) {
		this.qCyc = qCyc;
	}

	public QCycleAsVehicle getQCycleAsVehicle() {
		return this.qCyc;
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

	public LinkTransmissionModel getLTM() {
		return this.ltm;
	}

	public void moveToNextQ(Link nextLink, double tEnd) {
	}

	public void setSpeed(double newCurrentSpeed) {
		if (newCurrentSpeed < desiredSpeed) {
			this.speed = newCurrentSpeed;
		} else {
			this.speed = desiredSpeed;
		}
	}

	public boolean speedFitsOnLink(final double speed, final Link link) {
		return this.ltm.getSafetyBufferDistance(speed) + link.getOccupiedSpace() < link.getTotalLaneLength()
				|| link.getOccupiedSpace() < 0.1;
	}

	public PseudoLane selectPseudoLane(Link receivingLink) {
		return this.ltm.selectPseudoLane(receivingLink, this.desiredSpeed, this.tEarliestExit);
	}

	public double getVMax(final PseudoLane pseudoLane) {
		return Math.min(desiredSpeed, this.ltm.getVMax(pseudoLane, this.tEarliestExit));
	}

	public double getTEarliestExit() {
		return this.tEarliestExit;
	}

	public double getTStart() {
		return this.tStart;
	}

	public void setTEarliestExit(double time) {
		this.tEarliestExit = time;
	}

	public void setTStart(double time) {
		this.tStart = time;
	}

	public boolean isNotInFuture(double now) {
		return this.getTEarliestExit() <= now;
	}

	public double getSafetyBufferDistance(double speed) {
		return this.ltm.getSafetyBufferDistance(speed);
	}

	public double getSpeed() {
		return this.speed;
	}

	public Link getCurrentLink() {
		return this.currentLink;
	}

	public void setCurrentLink(Link newLink) {
		this.currentLink = newLink;
	}
}
