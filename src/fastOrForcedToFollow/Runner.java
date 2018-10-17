package fastOrForcedToFollow;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.Random;


public class Runner {

	//Network parameters
	public static final double widthOfLastLink = 2;
	public static final double widthOfFirstLinks = 3;
	public static final double lengthOfLinks = 100;
	public static final int L = 3;
	public static final double capacityMultiplier = 1;  // The totalLaneLength is multiplied with this number;

	// Pseudolane partition parameters
	public static final double omega = 1.25;    // Additional width needed to gain another efficient lane. Based on Buch & Greibe (2015).
	public static final double deadSpace = 0.4; // Dead horizontal space on bicycle paths that is unused. Based on Buch & Greibe (2015)

	//Simulation parameters
	public static String baseDir = "Z:/git/fastOrForcedToFollow/output/ToyNetwork";
	public static final double T = 3600;
	public static final double tau = 1d;
	public static final int maxN = 10000;
	public static final int stepSize = 50;
	public static final int seed = 5355633;
	public static final boolean reportSpeeds = true;
	@SuppressWarnings("rawtypes")
	public static final Class<? extends PriorityQueue> priorityQueueClassForLinks = PriorityQueue.class; 

	//Desired speed distribution parameters -- General
	private static final double minimumAllowedSpeed = 2;  // Lowest allowed desired speed (lower truncation of distribution);
	private static final String desiredSpeedDistribution = "Johnson"; //Johnson, Logistic

	//Desired speed distribution parameters -- Johnson
	private static final double JohnsonGamma = -2.745957257392118;
	private static final double JohnsonXsi = 3.674350833333333;
	private static final double JohnsonDelta = 4.068155531972158;
	private static final double JohnsonLambda = 3.494609779450189;

	//Desired speed distribution parameters -- Logistic
	public static final double mu = 6.085984;
	public static final double s = 0.610593;  // Interestingly, results have more (reasonable) curvature, when using a higher scale...

	//Safety distance parameters
	public static final LinkTransmissionModel ltm = new AdvancedSpatialLTM(); 
	//Options are: BasicLTM, SpatialLTM, AdvancedSpatialLTM.
	public static final double l = -4.220641337789; //For square root model;
	public static final double lambda_c = 1.73; //Average length of a bicycle according to
	//Andresen et al. (2014), Basic Driving Dynamics of Cyclists, In: Simulation of Urban Mobility;
	public static final double t_safetySqrt =  4.602161217943; 
	
	//Required fields with no input values
	public static double t;
	public static Link[] links;
	public static Link sourceLink;
	public static int N;
	public static LinkedHashMap<Integer,Link> linksMap; 
	public static LinkedList<Cyclist> cyclists;
	public static HashMap<Integer, Double>[] notificationArray;
	public static PriorityQueue<LinkQObject> shortTermPriorityQueue;
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

	public static void populationPreparation() throws InstantiationException, IllegalAccessException{
		Random random = new Random(seed);
		Random random2 = new Random(seed+9);
		for(int i = 0; i<100; i++){
			random.nextDouble();
			random2.nextDouble();
		}

		cyclists= new LinkedList<Cyclist>();
		for(int n = 0; n < N; n++){
			double w = -1;
			while( w < minimumAllowedSpeed){
				double u = random.nextDouble();
				switch(desiredSpeedDistribution){
				case "Johnson":
					w = JohnsonLambda * Math.sinh( (AuxStats.qNorm(u) - JohnsonGamma) / JohnsonDelta) + JohnsonXsi;
					break;
				case "Logistic":
					w = mu-Math.log(1/u-1)*s;
					break;
				default: 
					throw new IllegalArgumentException("Valid distributions are: Johnson, Logistic");
				}
			}

			double time = 0;
			time = random2.nextDouble()*T;
			LinkedList<Link> defaultRoute = new LinkedList<Link>();
			for( int i = 0; i < L; i++){
				defaultRoute.addLast(links[i]);
			}
			Cyclist cyclist = ltm.createCyclist(n, w, defaultRoute, ltm);
			cyclists.add(cyclist);

			sourceLink.getOutQ().add(new CyclistQObject(time, cyclist));	
			cyclist.sendNotification(sourceLink.getId(), time);
		}
	}


	@SuppressWarnings("unchecked")
	public static void simulation() throws InstantiationException, IllegalAccessException, IOException{
		for( ;t < T; t += tau){
			int i = (int) (t / tau);
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
		exportCyclistSpeeds(baseDir + "/Cyclists/" + (int) lengthOfLinks, N);
		exportCyclistDesiredSpeeds(baseDir + "/Cyclists/DesiredSpeeds");

		for(Link link : links){
			link.exportSpeeds(baseDir + "/Links/" + (int) lengthOfLinks);
			link.exportDensities(baseDir + "/Links/" + (int) lengthOfLinks);
			link.exportFlows(baseDir + "/Links/" + (int) lengthOfLinks);
			link.exportSpeedTimes(baseDir + "/Links/" + (int) lengthOfLinks);
			link.exportOutputTimes(baseDir + "/Links/" + (int) lengthOfLinks);
		}
	}

	public static void createFolderIfNeeded(String folder){
		File file = new File(folder);
		if(!file.exists()){
			file.mkdirs();
		}
	}

	public static void exportCyclistDesiredSpeeds(String baseDir) throws IOException{
		createFolderIfNeeded(baseDir);
		FileWriter writer = new FileWriter(baseDir + "/CyclistCruisingSpeeds_" + Runner.ltm.getClass().getName() + "_" 
				+ Runner.N + "Persons.csv");
		writer.append("CyclistId;CruisingSpeed\n");
		for(Cyclist cyclist : cyclists){
			writer.append(cyclist.getId() + ";"  + cyclist.getDesiredSpeed() + "\n");
		}
		writer.flush();
		writer.close();
	}

	public static void exportCyclistSpeeds(String baseDir, int itN) throws IOException{
		createFolderIfNeeded(baseDir);
		FileWriter writer = new FileWriter(baseDir + "/CyclistSpeeds_" + Runner.ltm.getClass().getName() + "_" 
				+ itN + "Persons.csv");

		System.out.println(baseDir + "/CyclistSpeeds_" + Runner.ltm.getClass().getName() + "_" 
				+ itN + "Persons.csv");
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
