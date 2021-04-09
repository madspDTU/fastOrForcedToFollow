package fastOrForcedToFollow.configgroups;

import org.matsim.core.config.ReflectiveConfigGroup;

public class FFFConfigGroup extends ReflectiveConfigGroup {

	static final String GROUP_NAME = "fastOrForcedToFollow";

	public static final String DESIRED_SPEED = "v_0";
	public static final String HEADWAY_DISTANCE_INTERCEPT = "theta_0";
	public static final String HEADWAY_DISTANCE_SLOPE= "theta_1";
	public static final String BICYCLE_LENGTH = "lambda_c";

	
	
	/**
	 * Excess travel time factor forced onto tReady and tEnd. 
	 */
	private double correctionFactor = 1.;
	
	public double getCorrectionFactor() {
		return this.correctionFactor;
	}

	public void setCorrectionFactor(final double correctionFactor) {
		this.correctionFactor = correctionFactor;
	}

	/**
	 * Average length of a bicycle according to Andresen et al. (2014),
	 * Basic Driving Dynamics of Cyclists, In: Simulation of Urban Mobility;
	 */
	private double lambda_c = 1.73;
	
	/**
	 * Maximum length [m] of link. Default is 60 metres.
	 */
	private double lMax = 60.;
	
	/**
	 * Beta-parameter from Paulsen et al. (2019), regarding the beta distribution.
	 */
	
	private double beta_alpha =   1.8647308191839824;
	
	/**
	 * Constant term in the square root model for headway distance. 
	 * From Paulsen et al. (2019), the beta distribution.
	 */
	private double theta_0 = -4.3570197635081049;
	/**
	 * Square root term in the square root model for headway distance.
	 * From Paulsen et al. (2019), the beta distribution.
	 */
	private double theta_1 =  4.7128195220959830;
	/**
	 * Constant term in the square root model for standard deviation of headway distance.
	 * From Paulsen et al. (2019), the beta distribution.
	 */
	private double zeta_0 =  -9.6741585958330667;
	/**
	 * Square root term in the square root model for standard deviation of headway distance.
	 * From Paulsen et al. (2019), the beta distribution.
	 */
	private double zeta_1 =  6.8406631453876896;

	/**
	 * One of the parameters for Johnson's SU-distribution, as estimated based on data from COWI.
	 */
	private double johnsonGamma = -2.745957257392118;

	/**
	 * One of the parameters for Johnson's SU-distribution, as estimated based on data from COWI.
	 */
	private double johnsonXsi = 3.674350833333333;

	/**
	 * One of the parameters for Johnson's SU-distribution, as estimated based on data from COWI.
	 */
	private double johnsonDelta = 4.068155531972158;

	/**
	 * One of the parameters for Johnson's SU-distribution, as estimated based on data from COWI.
	 */
	private double johnsonLambda = 3.494609779450189;
	
	/**
	 * The minimum allowed desired speed (lower bound for truncation).
	 */
	private double minimumAllowedDesiredSpeed = 2.;
	
	/**
	 * The maximum allowed desired speed (upper bound for truncation).
	 */
	private double maximumAllowedDesiredSpeed = 50./3.6;
	
	/**
	 * Additional width needed to gain another efficient lane. Based on Buch & Greibe (2015).
	 */
	private double efficientLaneWidth = 1.25; 

	/**
	 * Dead horizontal space on bicycle paths that is unused. Based on Buch & Greibe (2015).
	 */
	private double unusedWidth = 0.4;

	/**
	 * Number of lanes used for bicycle links.
	 * Only relevant if the FFFNetworkReader has not been 
	 * used for constructing separated networks initially.
	 */
	private int defaultNumberOfLanesPerLink = 1;


	public double getEfficientLaneWidth() {
		return efficientLaneWidth;
	}

	public void setEfficientLaneWidth(double efficientLaneWidth) {
		this.efficientLaneWidth = efficientLaneWidth;
	}

	public double getUnusedWidth() {
		return unusedWidth;
	}

	public void setUnusedWidth(double unusedWidth) {
		this.unusedWidth = unusedWidth;
	}

	public double getMinimumAllowedDesiredSpeed() {
		return minimumAllowedDesiredSpeed;
	}

	public void setMinimumAllowedDesiredSpeed(double minimumAllowedDesiredSpeed) {
		this.minimumAllowedDesiredSpeed = minimumAllowedDesiredSpeed;
	}
	
	public void setMaximumAllowedDesiredSpeed(double maximumAllowedDesiredSpeed) {
		this.maximumAllowedDesiredSpeed = maximumAllowedDesiredSpeed;
	}
	
	public double getMaximumAllowedDesiredSpeed() {
		return maximumAllowedDesiredSpeed;
	}

	public double getJohnsonGamma() {
		return johnsonGamma;
	}

	public void setJohnsonGamma(double johnsonGamma) {
		this.johnsonGamma = johnsonGamma;
	}

	public double getJohnsonXsi() {
		return johnsonXsi;
	}

	public void setJohnsonXsi(double johnsonXsi) {
		this.johnsonXsi = johnsonXsi;
	}

	public double getJohnsonDelta() {
		return johnsonDelta;
	}

	public void setJohnsonDelta(double johnsonDelta) {
		this.johnsonDelta = johnsonDelta;
	}

	public double getJohnsonLambda() {
		return johnsonLambda;
	}

	public void setJohnsonLambda(double johnsonLambda) {
		this.johnsonLambda = johnsonLambda;
	}

	public double getTheta_0() {
		return theta_0;
	}

	public void setTheta_0(double theta_0) {
		this.theta_0 = theta_0;
	}

	public double getTheta_1() {
		return theta_1;
	}

	public void setTheta_1(double theta_1) {
		this.theta_1 = theta_1;
	}

	public double getZeta_0() {
		return zeta_0;
	}

	public void setZeta_0(double zeta_0) {
		this.zeta_0 = zeta_0;
	}

	public double getZeta_1() {
		return zeta_1;
	}

	public void setZeta_1(double zeta_1) {
		this.zeta_1 = zeta_1;
	}

	//	@StringGetter( "maximumLengthOfSublink" )
	public double getLMax() {
		return lMax;
	}

	//	@StringSetter( "maximumLengthOfSublink" )
	public void setLMax(final double lMax) {
		this.lMax = lMax;
	}

	//Remove comments once it has been fully developed (makes it available in .xml)	
	//	@StringGetter( "standardBicycleLength" )
	public double getLambda_c() {
		return lambda_c;
	}

	//	@StringSetter( "standardBicycleLength" )
	public void setLambda_c(final double lambda_c) {
		this.lambda_c = lambda_c;
	}

	public FFFConfigGroup() {
		super( GROUP_NAME );
	}

	public double getBeta_alpha() {
		return beta_alpha;
	}

	public void setBeta_alpha(final double beta_alpha) {
		this.beta_alpha = beta_alpha;
	}

	public int getDefaultNumberOfLanesPerLink() {
		return defaultNumberOfLanesPerLink;
	}
	
	public void setDefaultNumberOfLanesPerLink(final int numberOfLanesPerLink) {
		this.defaultNumberOfLanesPerLink = numberOfLanesPerLink;
	}
	
}
