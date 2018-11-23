package fastOrForcedToFollow;

/**
 * @author mpaulsen
 *
 */
public final class Runner {

	// Suppress default constructor for noninstantiability
	private Runner(){
		throw new AssertionError();
	}
	
	/*
	 *  Pseudolane partition parameters
	 */
	/**
	 * Additional width needed to gain another efficient lane. Based on Buch & Greibe (2015).
	 */
	public static final double OMEGA = 1.25; 

	/**
	 * Dead horizontal space on bicycle paths that is unused. Based on Buch & Greibe (2015).
	 */
	public static final double DEAD_SPACE = 0.4;


	/*
	 * Simulation parameters
	 */
		/**
	 * The random seed used for the the population.
	 */
	public static final int SEED = 5355633;
	//As a backup: 5355633;


	/*
	 * Desired speed distribution parameters -- Johnson SU   ,  see https://en.wikipedia.org/wiki/Johnson's_SU-distribution
	 */

	/**
	 * The minimum allowed desired speed (lower bound for truncation).
	 */
	public static final double MINIMUM_ALLOWED_DESIRED_SPEED = 2;  // Lowest allowed desired speed (lower truncation of distribution);

	/**
	 * One of the parameters for Johnson's SU-distribution, as estimated based on data from COWI.
	 */
	public static final double JOHNSON_GAMMA = -2.745957257392118;

	/**
	 * One of the parameters for Johnson's SU-distribution, as estimated based on data from COWI.
	 */
	public static final double JOHNSON_XSI = 3.674350833333333;

	/**
	 * One of the parameters for Johnson's SU-distribution, as estimated based on data from COWI.
	 */
	public static final double JOHNSON_DELTA = 4.068155531972158;

	/**
	 * One of the parameters for Johnson's SU-distribution, as estimated based on data from COWI.
	 */
	public static final double JOHNSON_LAMBDA = 3.494609779450189;


	/*
	 * Safety distance parameters
	 */
	/**
	 * Constant term in the square root model for safety distance.
	 */
	public static final double THETA_0 = -4.220641337789;
	/**
	 * Square root term in the square root model for safety distance.
	 */
	public static final double THETA_1 =  4.602161217943;
	/**
	 * Constant term in the square root model for standard deviation of safety distance.
	 */
	public static final double ZETA_0 =  -4.3975231775567600;
	/**
	 * Square root term in the square root model for standard deviation of safety distance.
	 */
	public static final double ZETA_1 =  3.1095184592753986;
	/**
	 * Average length of a bicycle according to Andresen et al. (2014),
	 * Basic Driving Dynamics of Cyclists, In: Simulation of Urban Mobility;
	 */
	public static final double LAMBDA_C = 1.73; 


}
