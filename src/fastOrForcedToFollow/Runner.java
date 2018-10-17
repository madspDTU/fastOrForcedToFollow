package fastOrForcedToFollow;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.Random;


/**
 * @author mpaulsen
 *
 */
public class Runner {

	/*
	 * Network parameters
	 */
	/**
	 * The width (in metres) of the last link in the link series.
	 */
	public static final double widthOfLastLink = 2;
	
	
	/**
	 * The width (in metres) of all links but the last link in the link series.
	 */
	public static final double widthOfFirstLinks = 3;
	
	/**
	 * The length of each link in the link series.
	 */
	public static final double lengthOfLinks = 100;
	
	/**
	 * The numbers of links in serial used in the simulation.
	 */
	public static final int L = 3;
	
	/**
	 * A capacity multiplier that can be used to increase the total lane length, see {@link Link#getTotalLaneLength()}.
	 */
	public static final double capacityMultiplier = 1;  // The totalLaneLength is multiplied with this number;

	
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
	 * The base directory for storing output.
	 */
	public static String baseDir = "Z:/git/fastOrForcedToFollow/output/ToyNetwork";
	
	/**
	 * The span of the simulation period (in seconds).
	 */
	public static final double T = 3600;
	
	/**
	 * The timestep used in the simulation.
	 */
	public static final double timeStep = 1d;
	
	/**
	 * Maximum number of cyclist to enter the system during the simulation.
	 */
	public static final int maxN = 10000;
	
	/**
	 * The number of cyclists subtracted from the previous simulation until reaching 0 cyclists.
	 */
	public static final int stepSize = 50;
	
	/**
	 * The random seed used for the the population.
	 */
	public static final int seed = 5355633;
	
	/**
	 * Whether or not to report speeds (takes the majority of the time, but results cannot be analysed without).
	 */
	public static final boolean reportSpeeds = true;
	
	/**
	 * The type of Q used for the priorityQ for links. Can be PriorityQueue.class or FibonacciHeap.class.
	 */
	@SuppressWarnings("rawtypes")
	public static final Class<? extends PriorityQueue> priorityQueueClassForLinks = PriorityQueue.class; 

	
	/*
	 * Desired speed distribution parameters -- General
	 */
	/**
	 * The minimum allowed desired speed (lower bound for truncation).
	 */
	private static final double minimumAllowedDesiredSpeed = 2;  // Lowest allowed desired speed (lower truncation of distribution);
	
	/**
	 * The distribution used for desired speeds. Valid options are (so far) "JohnsonSU" and "Logistic".
	 */
	private static final String desiredSpeedDistribution = "JohnsonSU";

	
	/*
	 * Desired speed distribution parameters -- Johnson SU   ,  see https://en.wikipedia.org/wiki/Johnson's_SU-distribution
	 */
	/**
	 * One of the parameters for Johnson's SU-distribution, as estimated based on data from COWI.
	 */
	private static final double JohnsonGamma = -2.745957257392118;
	
	/**
	 * One of the parameters for Johnson's SU-distribution, as estimated based on data from COWI.
	 */
	private static final double JohnsonXsi = 3.674350833333333;
	
	/**
	 * One of the parameters for Johnson's SU-distribution, as estimated based on data from COWI.
	 */
	private static final double JohnsonDelta = 4.068155531972158;
	
	/**
	 * One of the parameters for Johnson's SU-distribution, as estimated based on data from COWI.
	 */
	private static final double JohnsonLambda = 3.494609779450189;
	

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
	 * Type of <code>LinkTransmissionModel</code> used. Valid values are BasicLTM, SpatialLTM, AdvancedSpatialLTM.
	 */
	public static final LinkTransmissionModel ltm = new AdvancedSpatialLTM(); 
	
	/**
	 * Constant term in the square root model for safety distance.
	 */
	public static final double theta_0 = -4.220641337789;
	/**
	 * Square root term in the square root model for safety distance.
	 */
	public static final double theta_1 =  4.602161217943; //Square root term in square root model;
	/**
	 * Average length of a bicycle according to Andresen et al. (2014),
	 * Basic Driving Dynamics of Cyclists, In: Simulation of Urban Mobility;
	 */
	public static final double lambda_c = 1.73; 
	
	
	/*
	 * Required fields (used by other classes) with no input values.
	 */
	/**
	 * The simulation clock.
	 */
	public static double t;
	
	/**
	 * An array containing all the links.
	 */
	public static Link[] links;
	
	/** 
	 * The sourceLink where cyclist stay until they enter the system.
	 */
	public static Link sourceLink;
	
	/**
	 * The current number of cyclists being simulated.
	 */
	public static int N;
	
	/**
	 * The map containing all links.
	 */
	public static LinkedHashMap<Integer,Link> linksMap; 
	
	/**
	 * A linked list containing all cyclists.
	 */
	public static LinkedList<Cyclist> cyclists;
	
	/**
	 * The notificationArray that for every ??????//TODO
	 */
	public static HashMap<Integer, Double>[] notificationArray;
	
	/**
	 * The short term priority queue that for every ??????//TODO
	 */
	public static PriorityQueue<LinkQObject> shortTermPriorityQueue;
	
	/**
	 * A tie breaker maintained in <code>Runner</code> that is passed on to Q-object in order to ensure correct ordering.
	 */
	public static double tieBreaker;



	public static void main(String[] args) throws IOException, InterruptedException, InstantiationException, IllegalAccessException{
		for( N = maxN; N >= stepSize; N -= stepSize){
			double startTime = System.currentTimeMillis();
			System.out.println("Start of a simulation with " + N + " cyclists and " + L + " links.");
			simulationPreparation();
			System.out.println("1st part (Initialisation) finished after " + (System.currentTimeMillis()-startTime)/1000d + " seconds.");
			simulation();
			System.out.println("2nd part (Mobility Simul) finished after " + (System.currentTimeMillis()-startTime)/1000d + " seconds.");
			if(reportSpeeds){	
				exportSpeeds();
			}
			System.out.println("3rd part (Xporting stuff) finished after " + (System.currentTimeMillis()-startTime)/1000d + " seconds.");
			System.out.format(Locale.US, "%.3f microseconds per cyclist-link-interaction in total.%n%n", 
					(System.currentTimeMillis()-startTime)/((double) N)/((double) L)*1000d);
		}
	}

	@SuppressWarnings("unchecked")
	public static void generalPreparation(){
		t =0;
		notificationArray = (HashMap<Integer, Double>[]) Array.newInstance(HashMap.class, (int) T+1);
		for(int i = 0; i < T+1; i++){
			notificationArray[i] = new HashMap<Integer, Double>();
		}
		tieBreaker = 0d;
	}

	public static void simulationPreparation() throws InstantiationException, IllegalAccessException{
		generalPreparation();
		networkPreparation();
		populationPreparation();
	}


	/**
	 * Creates the network including a {@link Runner.sourceLink}.
	 * 
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public static void networkPreparation() throws InstantiationException, IllegalAccessException{
		links = new Link[L];
		linksMap = new LinkedHashMap<Integer,Link>();
		for(int i = 0; i < L; i++){
			if(i == (L-1)){
				links[i] = new Link(i,widthOfLastLink,lengthOfLinks);
			} else {
				links[i] = new Link(i,widthOfFirstLinks,lengthOfLinks);
			}
			linksMap.put(links[i].getId(), links[i]);
		}
		sourceLink = new Link(-1,1,0);
		linksMap.put(sourceLink.getId(),sourceLink);
	}

	
	
	/**
	 * Method preparing the cyclist population by assigning a desired speed,
	 * a link transmission model and an initial arrival time to the system
	 * for every cyclist in the population. The desired speed is based on
	 * the distribution chosen in {@link Runner#desiredSpeedDistribution}.
	 * 
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public static void populationPreparation() throws InstantiationException, IllegalAccessException{
		Random desiredSpeedRandom = new Random(seed);
		Random arrivalTimeRandom = new Random(seed+9);
		for(int i = 0; i<100; i++){
			desiredSpeedRandom.nextDouble();
			arrivalTimeRandom.nextDouble();
		}

		cyclists= new LinkedList<Cyclist>();
		for(int id = 0; id < N; id++){
			double speed = -1;
			while( speed < minimumAllowedDesiredSpeed){
				double u = desiredSpeedRandom.nextDouble();
				switch(desiredSpeedDistribution){
				case "JohnsonSU":
					speed = JohnsonLambda * Math.sinh( (ToolBox.qNorm(u) - JohnsonGamma) / JohnsonDelta) + JohnsonXsi;
					break;
				case "Logistic":
					speed = mu-Math.log(1/u-1)*s;
					break;
				default: 
					throw new IllegalArgumentException("Valid distributions are: JohnsonSU, Logistic");
				}
			}
			double time = arrivalTimeRandom.nextDouble()*T;
			LinkedList<Link> defaultRoute = new LinkedList<Link>(); // At the moment all routes are equal.
			for( int i = 0; i < L; i++){
				defaultRoute.addLast(links[i]);
			}
			Cyclist cyclist = ltm.createCyclist(id, speed, defaultRoute, ltm);
			cyclists.add(cyclist);

			sourceLink.getOutQ().add(new CyclistQObject(time, cyclist));	
			cyclist.sendNotification(sourceLink.getId(), time);
		}
	}


	/**
	 * The method including the actual simulation. 
	 * 
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static void simulation() throws InstantiationException, IllegalAccessException, IOException{
		for( ;t < T; t += timeStep){
			int i = (int) (t / timeStep);
			if( Math.round(t) % 3600 == 0 && t > 0){
				System.out.println("   " + ((int) t / 3600) + " hours simulated.");
			}
			shortTermPriorityQueue = (PriorityQueue<LinkQObject>) priorityQueueClassForLinks.newInstance();
			for(Integer linkId : notificationArray[i].keySet()){
				Link link = linksMap.get(linkId);
				// It is fully possible that tReady > tNotificationArray. This could happen if tWakeUp is increased after the notification,
				// e.g. due to the nextLink being fully occupied forcing tWakeUp to be increased to the Q-time of the following link.
				// Can the opposite be true? Yes! initially tWakeUp is not even defined yet, and will only be once congestion occurs.

				// This is a little weird, but since -1 is the null value of Double, an empty set will contain the key -1 but with a null value.
				if(notificationArray[i].get(linkId) != null){
					double maxTime = Math.max(link.getWakeUpTime(), notificationArray[i].get(linkId));
					link.setWakeUpTime(maxTime);
					linksMap.replace(linkId, link);
					if( maxTime > t && notificationArray.length > i+1){
						notificationArray[i+1].put(linkId, notificationArray[i+1].get(linkId));
					} else {
						// This ensures that the LQO constructed will actually use the correct time (maxTime)
						shortTermPriorityQueue.add(new LinkQObject(maxTime, link.getId()));
					}
				}
			}
			while(!shortTermPriorityQueue.isEmpty()){
				LinkQObject loq = shortTermPriorityQueue.poll();
				linksMap.get(loq.getId()).handleQOnNotification(loq);
			}
			for(int i_l = 0; i_l < L; i_l++){
				Link link = links[i_l];
				link.getDensityReport().
					addLast(link.getOutQ().size() / (link.getLength() * link.getNumberOfPseudoLanes() / 1000d) );
			}
		}
	}

	/**
	 * Export the speeds of links and cyclists from the simulation using <code>itN</code> cyclists.
	 * 
	 * @param itN The number of cyclists used in the simulation to be reported.
	 * 
	 * @throws IOException
	 */
	public static void exportSpeeds() throws IOException{
		exportCyclistSpeeds(baseDir + "/Cyclists/" + (int) lengthOfLinks);
		exportCyclistDesiredSpeeds(baseDir + "/Cyclists/DesiredSpeeds");

		for(Link link : links){
			link.exportSpeeds(baseDir + "/Links/" + (int) lengthOfLinks);
			link.exportDensities(baseDir + "/Links/" + (int) lengthOfLinks);
			link.exportFlows(baseDir + "/Links/" + (int) lengthOfLinks);
			link.exportSpeedTimes(baseDir + "/Links/" + (int) lengthOfLinks);
			link.exportOutputTimes(baseDir + "/Links/" + (int) lengthOfLinks);
		}
	}



	/**
	 * Exporting desired speeds to subfolders within the baseDir.
	 * 
	 * @param baseDir The base directory into which the output is stored.
	 * 
	 * @throws IOException
	 */
	public static void exportCyclistDesiredSpeeds(String baseDir) throws IOException{
		ToolBox.createFolderIfNeeded(baseDir);
		FileWriter writer = new FileWriter(baseDir + "/CyclistCruisingSpeeds_" + Runner.ltm.getClass().getName() + "_" 
				+ Runner.N + "Persons.csv");
		writer.append("CyclistId;CruisingSpeed\n");
		for(Cyclist cyclist : cyclists){
			writer.append(cyclist.getId() + ";"  + cyclist.getDesiredSpeed() + "\n");
		}
		writer.flush();
		writer.close();
	}

	/**
	 * Exporting cyclist speeds to subfolders within the baseDir.
	 * 
	 * @param baseDir The base directory into which the output is stored.
	 * 
	 * @throws IOException
	 */
	public static void exportCyclistSpeeds(String baseDir) throws IOException{
		ToolBox.createFolderIfNeeded(baseDir);
		FileWriter writer = new FileWriter(baseDir + "/CyclistSpeeds_" + Runner.ltm.getClass().getName() + "_" 
				+ N + "Persons.csv");

		System.out.println(baseDir + "/CyclistSpeeds_" + Runner.ltm.getClass().getName() + "_" 
				+ N + "Persons.csv");
		writer.append("CyclistId;LinkId;Time;Speed\n");
		for(Cyclist cyclist : cyclists){
			for(Double[] reportElement : cyclist.getSpeedReport()){
				if(reportElement[0] == 0 || reportElement[2] > 0){  //On all real links, the speed has to be positive.
					writer.append(cyclist.getId() + ";"  + reportElement[0] + ";" + reportElement[1] + ";" + reportElement[2] + "\n");
				}
			}
		}
		writer.flush();
		writer.close();
	}


}
