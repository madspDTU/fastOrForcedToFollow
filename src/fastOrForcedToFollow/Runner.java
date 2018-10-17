package fastOrForcedToFollow;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;


public class Runner {
	public static final double widthOfLastLink = 2;
	public static final double widthOfFirstLinks = 3;
	public static final double lengthOfLinks = 100;

	public static final double omega = 1.25;
	public static double T = 3600*16;
	public static double TOnToy = 3600;

	public static double t = 1;
	public static int L = 10000;
	public static Link[] links = new Link[L];
	public static Link sourceLink;
	public static int N = 100000;
	public static int seed = 5355633;
	public static LinkedHashMap<Integer,Link> linksMap = new LinkedHashMap<Integer,Link>();

	private static final boolean useLogisticDistribution = false; //Based on the data from Cowi, this could be reasonable.
	private static final boolean useJohnsonDistribution = true; //Based on the data from Cowi, this seems reasonable.
	private static final double JohnsonGamma = -2.745957257392118;
	private static final double JohnsonXsi = 3.674350833333333;
	private static final double JohnsonDelta = 4.068155531972158;
	private static final double JohnsonLambda = 3.494609779450189;
	private static final double minimumAllowedSpeed = 2;  // Lowest allowed desired speed (lower truncation of distribution);


	public static double k = 5d;
	public static double lambda = 6.5d;
	public static final double mu = 6.085984;
	//public static final double s = 0.610593;  // Interestingly, results have more (reasonable) curvature, when using a higher scale...
	public static final double s = 0.610593;  // Interestingly, results have more (reasonable) curvature, when using a higher scale...
	public static double tau = 1d;
	public static final double deadSpace = 0.4; // Dead horizontal space on bicycle paths that no one will use. Based on Buch & Greibe (2015)
	//public static final double l = 1.93; //Average length of a bicycle measured in metres + minimum safety distance
	//public static final double l = 1.73; //Average length of a bicycle measured in metres + minimum safety distance
	//public static final double l = -1.970194143353036; //Average length of a bicycle measured in metres + minimum safety distance
	public static final double l = -4.220641337789; //For square root model;
	public static final double lambda_c = 1.73; //Average length of a bicycle according to
	//Andresen et al. (2014), Basic Driving Dynamics of Cyclists, In: Simulation of Urban Mobility;


	//public static final double t_safety = 0.72; //Safety time between cyclists according to Andresen.

	public static final double t_safetySqrt =  4.602161217943; 
	//public static final double t_safety =  1.88579462750199101; 
	public static final double t_safety =  0; 

	public static final double t_safety2Poly = 0; //Safety time between cyclists according to Andresen 0.
	//public static final double t_safety2Poly = -0.07403800101957327; //Safety time between cyclists according to Andresen 0.
	public static final double capacityMultiplier = 1;  // The totalLaneLength is multiplied with this number;

	public static final boolean useToyNetwork = true;

	public static LinkedList<Cyclist> cyclists= new LinkedList<Cyclist>();

	public static final boolean circuit = false;
	public static final boolean reportSpeeds = true;
	public static final String circuitString = circuit? "Circuit" : "Linear";
	@SuppressWarnings("rawtypes")
	public static final Class<? extends PriorityQueue> priorityQueueClassForLinks = PriorityQueue.class; 
	public static final LinkTransmissionModel ltm = new AdvancedSpatialLTM(); //So far the options are: BasicLTM, SpatialLTM, AdvancedSpatialLTM.

	public static HashMap<Integer, Double>[] notificationArray;
	public static PriorityQueue<LinkQObject> shortTermPriorityQueue;
	public static String baseDir = "Z:/git/fastOrForcedToFollow/output";
	public static double tieBreaker = 0;
	public static final boolean waitingTimeCountsOnNextLink = false; // if false, then it counts on the previous link (i.e. spillback)


	//Dronning Louises bro sydgående
	// 3500 cykler mellem 8 og 9 i sydgående retning.
	// Sampl hver af disse med et ankomsttidspunkt U(0,3600)
	// 4 meter bred.
	// 200 meter lang.



	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException, InterruptedException, InstantiationException, IllegalAccessException{

		//	System.out.println("Waiting for <Enter> until CPU Sampling has started");
		//	System.in.read();

		int stepSize = 50;
		for( int itN = 10000; itN >= stepSize; itN -= stepSize){
			//int itN = 2050;{
			//for( int itN = stepSize; itN <= 10000; itN += stepSize){
			N = itN;
			if(circuit){
				N = itN/25;
				if( N > 100){
					N = 100;
				}
			}
			//{	
			t =0 ;
			double startTime = System.currentTimeMillis();
			Random random3 = new Random(seed + 171);
			System.out.println("Start of a simulation with " + N + " cyclists.");

			if(useToyNetwork){
				L = 3;
				links = new Link[L];
				for(int i = 0; i < L; i++){
					if(i == (L-1)){
						links[i] = new Link(i,widthOfLastLink,lengthOfLinks);
					} else {
						links[i] = new Link(i,widthOfFirstLinks,lengthOfLinks);
					}
					linksMap.put(links[i].getId(), links[i]);
				}
				T = TOnToy;
				if(!baseDir.contains("ToyNetwork")){
					baseDir += "/ToyNetwork";
				}
			} else {
				for(int counter = 0; counter < L; counter++){
					links[counter] = new Link(counter,random3.nextInt(2)+1, random3.nextDouble()*390d + 10d);
				}
			}

			sourceLink = new Link(-1,1,0);
			linksMap.put(sourceLink.getId(),sourceLink);

			notificationArray = (HashMap<Integer, Double>[]) Array.newInstance(HashMap.class, (int) T+1);
			for(int i = 0; i < T+1; i++){
				notificationArray[i] = new HashMap<Integer, Double>();
			}

			// Initialising random number generators;
			Random random = new Random(seed);
			Random random2 = new Random(seed+9);
			for(int i = 0; i<100; i++){
				random.nextDouble();
				random2.nextDouble();
			}

			cyclists.clear();
			for(int n = 0; n < N; n++){
				double w = -1;
				while( w < minimumAllowedSpeed){
					double u = random.nextDouble();
					if(useJohnsonDistribution){
						w = JohnsonLambda * Math.sinh( (AuxStats.qNorm(u) - JohnsonGamma) / JohnsonDelta) + JohnsonXsi;
					} else if(useLogisticDistribution){
						w = mu-Math.log(1/u-1)*s ;
					} else {
						//Weibull
						w = lambda*Math.pow((-Math.log(1d-u)),(1d/k));
					}
				}

				double time = 0;
				if(!circuit){
					time = random2.nextDouble()*T;
				} else {
					time = random2.nextDouble()*0.01;
				}
				/*	LinkedList<Link> defaultRoute = new LinkedList();
				for(int i = 0; i < L; i++){
					defaultRoute.addLast(links[i]);
				} */
				LinkedList<Link> defaultRoute = new LinkedList<Link>();
				if(useToyNetwork){
					for( int i = 0; i < L; i++){
						defaultRoute.addLast(links[i]);
					}
				} else {
					int nL = random3.nextInt(290)+10;
					for(int i = 0; i < nL; i++){
						defaultRoute.addLast(links[random.nextInt(L)]);
					}
				}
				Cyclist cyclist = ltm.createCyclist(n, w, defaultRoute, ltm);
				cyclists.add(cyclist);

				sourceLink.getOutQ().add(new CyclistQObject(time, cyclist));	
				cyclist.sendNotification(sourceLink.getId(), time);
			}

			System.out.println("1st part (Initialisation) finished after " + (System.currentTimeMillis()-startTime)/1000d + " seconds.");

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
			System.out.println("2nd part (Mobility Simul) finished after " + (System.currentTimeMillis()-startTime)/1000d + " seconds.");

			if(reportSpeeds){			
				exportCyclistSpeeds(baseDir + "/Cyclists/" + (int) lengthOfLinks, itN);
				exportCyclistDesiredSpeeds(baseDir + "/Cyclists/DesiredSpeeds");


				for(Link link : links){
					link.exportSpeeds(baseDir + "/Links/" + (int) lengthOfLinks);
					link.exportDensities(baseDir + "/Links/" + (int) lengthOfLinks);
					link.exportFlows(baseDir + "/Links/" + (int) lengthOfLinks);
					link.exportSpeedTimes(baseDir + "/Links/" + (int) lengthOfLinks);
					link.exportOutputTimes(baseDir + "/Links/" + (int) lengthOfLinks);
				}
			}


			System.out.println("3rd part (Xporting stuff) finished after " + (System.currentTimeMillis()-startTime)/1000d + " seconds.");
			System.out.println((System.currentTimeMillis()-startTime)/((double) N)/((double) L)*1000d +
					" microseconds per cyclist-link-interaction.\n");

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
				+ Runner.N + "Persons_" + Runner.circuitString + ".csv");
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
				+ itN + "Persons_" + Runner.circuitString + ".csv");

		System.out.println(baseDir + "/CyclistSpeeds_" + Runner.ltm.getClass().getName() + "_" 
				+ itN + "Persons_" + Runner.circuitString + ".csv");
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
