package fastOrForcedToFollow;

/**
 * @author mpaulsen
 *
 */
public class Runner {

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
	public static final double omega = 1.25; 

	/**
	 * Dead horizontal space on bicycle paths that is unused. Based on Buch & Greibe (2015).
	 */
	public static final double deadSpace = 0.4;


	/*
	 * Simulation parameters
	 */
		/**
	 * The random seed used for the the population.
	 */
	public static final int seed = 5355633;
	//As a backup: 5355633;


	/*
	 * Desired speed distribution parameters -- Johnson SU   ,  see https://en.wikipedia.org/wiki/Johnson's_SU-distribution
	 */

	/**
	 * The minimum allowed desired speed (lower bound for truncation).
	 */
	public static final double minimumAllowedDesiredSpeed = 2;  // Lowest allowed desired speed (lower truncation of distribution);

	/**
	 * One of the parameters for Johnson's SU-distribution, as estimated based on data from COWI.
	 */
	public static final double JohnsonGamma = -2.745957257392118;

	/**
	 * One of the parameters for Johnson's SU-distribution, as estimated based on data from COWI.
	 */
	public static final double JohnsonXsi = 3.674350833333333;

	/**
	 * One of the parameters for Johnson's SU-distribution, as estimated based on data from COWI.
	 */
	public static final double JohnsonDelta = 4.068155531972158;

	/**
	 * One of the parameters for Johnson's SU-distribution, as estimated based on data from COWI.
	 */
	public static final double JohnsonLambda = 3.494609779450189;


	/*
	 * Desired speed distribution parameters -- Logistic
	 */
	/**
	 * Mean value of the logistic distribution estimated based on data from COWI.
	 */
	public static final double mu = 6.085984;

	/**
	 * Scale value of the logistic distribution estimated based on data from COWI.
	 */
	public static final double s = 0.610593; 


	/*
	 * Safety distance parameters
	 */
	/**
	 * Constant term in the square root model for safety distance.
	 */
	public static final double theta_0 = -4.220641337789;
	/**
	 * Square root term in the square root model for safety distance.
	 */
	public static final double theta_1 =  4.602161217943;
	/**
	 * Constant term in the square root model for standard deviation of safety distance.
	 */
	public static final double zeta_0 =  -4.3975231775567600;
	/**
	 * Square root term in the square root model for standard deviation of safety distance.
	 */
	public static final double zeta_1 =  3.1095184592753986;
	/**
	 * Average length of a bicycle according to Andresen et al. (2014),
	 * Basic Driving Dynamics of Cyclists, In: Simulation of Urban Mobility;
	 */
	public static final double lambda_c = 1.73; 


}
