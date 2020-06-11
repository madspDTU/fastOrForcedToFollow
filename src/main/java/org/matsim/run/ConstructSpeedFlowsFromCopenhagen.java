package org.matsim.run;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;

import fastOrForcedToFollow.eventhandlers.MultiModalBicycleDoorToDoorHandler;

public class ConstructSpeedFlowsFromCopenhagen {

	public static boolean roW = false;

	public static double highlightStartTime = 8*3600;
	public static double highlightEndTime = 9*3600;
	private static List<String> interestingPersonsList = null;
	//			Arrays.asList("98075_1_Person","135460_1_Person","116357_1_Person","226582_1_Person","355440_1_Person","336045_1_Person",
	//					"74274_1_Person","57223_1_Person","313082_2_Person","417066_2_Person","167114_1_Person","238307_4_Person",
	//					"277601_1_Person","92531_1_Person","121781_3_Person","94157_1_Person","77156_2_Person","475329_1_Person",
	//					"64169_1_Person","275407_1_Person","108720_2_Person","15448_4_Person","237937_2_Person","132548_1_Person",
	//					"60275_1_Person","95647_1_Person","179916_1_Person","562544_1_Person","142046_2_Person","134981_1_Person",
	//					"281975_1_Person","48024_1_Person","273073_2_Person","150295_1_Person","331011_2_Person","287842_1_Person",
	//					"93190_1_Person","389371_1_Person","282423_1_Person","272681_1_Person","281163_1_Person","282310_1_Person",
	//					"277391_1_Person","53265_1_Person","76102_1_Person","83026_1_Person","361666_1_Person","146629_1_Person",
	//					"67288_1_Person","137162_1_Person","73_2_Person","103888_1_Person","99790_1_Person","120461_1_Person",
	//					"70931_4_Person","116308_1_Person","88805_1_Person","348101_1_Person","149913_1_Person","104629_1_Person",
	//					"313682_3_Person","126072_2_Person","390477_1_Person","273838_1_Person","604502_3_Person","368624_1_Person",
	//					"102235_1_Person","93042_1_Person","183722_2_Person","134903_1_Person","365044_1_Person","141402_1_Person",
	//					"215151_2_Person","70640_2_Person","219785_3_Person","74242_1_Person","368829_1_Person","155222_2_Person",
	//					"338786_1_Person","275267_2_Person","273637_2_Person","113358_1_Person","333319_4_Person","17983_1_Person",
	//					"183688_3_Person","144774_1_Person","290190_1_Person","50376_1_Person","634208_2_Person","246106_1_Person",
	//					"230265_1_Person","639127_1_Person","199772_2_Person","224378_1_Person","86172_2_Person","231141_1_Person",
	//					"9319_2_Person","275326_1_Person","106815_2_Person","213273_3_Person","112394_1_Person","38219_1_Person",
	//					"507260_1_Person","105095_2_Person","304434_1_Person","93258_1_Person","86073_2_Person","313536_2_Person",
	//					"75369_1_Person","170071_1_Person","159597_1_Person","224038_2_Person","127080_1_Person","142096_4_Person",
	//					"301043_2_Person","183510_2_Person","142030_4_Person","363613_1_Person","274477_3_Person","148279_1_Person",
	//					"202920_1_Person","292005_2_Person","100661_1_Person","234896_1_Person","292420_1_Person","156805_5_Person",
	//					"101954_1_Person","104524_2_Person","267413_1_Person","186796_4_Person","263523_1_Person","259085_1_Person",
	//					"360942_1_Person","273671_2_Person","366939_1_Person","284673_1_Person","267144_1_Person","282940_1_Person",
	//					"30170_1_Person","229768_1_Person","279650_1_Person","18506_1_Person","393727_1_Person","89678_4_Person",
	//					"282272_1_Person","265365_2_Person","98811_1_Person","326512_1_Person","132583_1_Person","108395_2_Person",
	//					"21326_2_Person","63155_1_Person","163478_1_Person","184724_1_Person","141224_1_Person","94035_1_Person",
	//					"90932_1_Person","355273_2_Person","90127_1_Person","186053_1_Person","171702_1_Person","131622_1_Person",
	//					"3185_1_Person","5676_3_Person","364433_1_Person","214817_1_Person","625304_1_Person","83719_1_Person",
	//					"65959_1_Person","357009_1_Person","375966_1_Person","211056_1_Person","365195_1_Person","235606_2_Person",
	//					"335670_1_Person","140893_1_Person","84033_1_Person","13942_2_Person","333423_3_Person","232289_1_Person",
	//					"131609_1_Person","108784_2_Person","156928_4_Person","275577_1_Person","267152_1_Person","128823_1_Person",
	//					"167251_1_Person","185879_1_Person","355896_2_Person","129672_2_Person","9705_1_Person","263639_1_Person",
	//					"698483_1_Person","356798_1_Person","367972_1_Person","140955_2_Person","501741_2_Person","340410_1_Person",
	//					"120835_1_Person","304931_1_Person","661853_1_Person","22624_1_Person","293669_1_Person","276372_1_Person",
	//					"657455_1_Person","296659_2_Person","278154_1_Person","287071_2_Person","282496_1_Person","272073_1_Person",
	//					"283323_1_Person","260106_3_Person","905630_1_Person","261510_1_Person","316590_1_Person","264486_1_Person",
	//					"262365_1_Person","295454_1_Person","275577_2_Person","670839_1_Person","882_1_Person","8942_1_Person",
	//					"259564_1_Person","259212_1_Person","332354_1_Person","274201_1_Person","183126_3_Person","179685_1_Person",
	//					"116111_1_Person","139134_1_Person","373920_1_Person","471483_1_Person","89539_3_Person","265777_2_Person",
	//					"350394_2_Person","298432_1_Person","266238_3_Person","641140_2_Person","136859_1_Person","10697_1_Person",
	//					"207334_1_Person","81939_1_Person","108931_4_Person","265994_2_Person","266402_3_Person","336436_1_Person",
	//					"299750_1_Person","291922_2_Person","662683_1_Person","73181_1_Person","261153_1_Person","290767_2_Person",
	//					"355236_1_Person","326942_1_Person","313113_1_Person","54308_5_Person","288895_2_Person","138784_1_Person",
	//					"165044_1_Person","489765_1_Person","212713_1_Person","149473_1_Person","240463_1_Person","356484_2_Person",
	//					"294376_1_Person","54526_2_Person","77325_3_Person","68074_1_Person","74618_1_Person","150623_1_Person",
	//					"167998_1_Person","10301_2_Person","356816_1_Person","341801_1_Person","351518_1_Person","287152_3_Person",
	//					"8903_1_Person","23220_1_Person","142544_4_Person","167901_1_Person","111992_1_Person","913_1_Person",
	//					"116009_1_Person","113866_1_Person","98230_1_Person","290516_1_Person","41633_2_Person","367213_2_Person",
	//					"121356_1_Person","105389_2_Person","79516_1_Person","233888_1_Person","363299_1_Person","854180_1_Person",
	//					"271986_2_Person","130802_1_Person","69809_1_Person","174564_4_Person","38256_3_Person","57457_1_Person",
	//					"122763_3_Person","175445_3_Person","181885_1_Person","18077_1_Person","259750_2_Person","284910_1_Person",
	//					"25280_1_Person","164099_4_Person","651839_2_Person","314358_1_Person","151340_1_Person","852628_4_Person",
	//					"164969_1_Person","486_4_Person","260214_4_Person","155712_1_Person","370836_2_Person","367173_2_Person",
	//					"341800_1_Person","4829_1_Person","227907_1_Person","23261_1_Person","71828_1_Person","33391_1_Person",
	//					"203925_2_Person","15776_4_Person","11156_1_Person","153762_1_Person","370723_1_Person","23037_1_Person",
	//					"501041_1_Person","387530_1_Person","313635_3_Person","505962_1_Person","320662_1_Person","172916_2_Person",
	//					"154698_1_Person","179250_1_Person","8592_1_Person","501041_2_Person","174784_1_Person","17849_1_Person",
	//					"127209_1_Person","54737_3_Person","291904_2_Person","34621_1_Person","287631_1_Person","131055_1_Person",
	//					"46641_1_Person","43880_1_Person","106441_2_Person","24178_1_Person","24282_1_Person","23356_1_Person",
	//					"20343_1_Person","293935_1_Person","466127_2_Person","95568_1_Person","357296_3_Person","256525_1_Person",
	//					"137132_1_Person","19536_1_Person","170945_1_Person","61512_3_Person","264697_2_Person","20296_1_Person",
	//					"323233_1_Person","15611_1_Person","126333_5_Person","46256_1_Person","12304_1_Person");
	//
	public static void main(String[] args) throws NumberFormatException, IOException{


		ConstructSpeedFlowsFromCopenhagen.run(ScenarioUtils.createScenario(ConfigUtils.createConfig()),
				"full", -1, Arrays.asList(""), new HashSet<String>(Arrays.asList(TransportMode.bike,TransportMode.car)));
	}

	public static void run(Scenario scenario, String type, int it, 
			List<String> ignoredModes, Set<String> analysedModes) throws IOException {

		Config config = scenario.getConfig();
		if(config.controler().isLinkToLinkRoutingEnabled()) {
			roW = true;
		}

		String outDir = config.controler().getOutputDirectory();
		Network network = scenario.getNetwork();
		File networkFile = new File(outDir + "/output_network.xml.gz");
		if(networkFile.exists()) {
			Iterator<? extends Node> ite = network.getNodes().values().iterator();
			while(ite.hasNext()) {
				Node node = ite.next();
				network.removeNode(node.getId());
			}
			Iterator<? extends Link> itel = network.getLinks().values().iterator();
			while(itel.hasNext()) {
				Link link = itel.next();
				network.removeLink(link.getId());
			}
			MatsimNetworkReader  mnr = new MatsimNetworkReader(network);
			mnr.readFile(networkFile.getAbsolutePath());
		} 
		boolean populationAlreadyLoaded = false;
		for(Person person : scenario.getPopulation().getPersons().values()) {
			if(person.getPlans().size()>1) {
				populationAlreadyLoaded = true;
				break;
			} else {
				for(PlanElement pe : person.getPlans().get(0).getPlanElements()) {
					if(pe instanceof Leg) {
						Leg leg = (Leg) pe;
						if(leg.getRoute() != null) {
							populationAlreadyLoaded = true;
							break;
						}
					}
				}
			}
		}

		Population population = scenario.getPopulation();
		Scenario tempScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		if(it == 0){
			PopulationReader pr = new PopulationReader(tempScenario);
			pr.readFile(outDir + "/ITERS/it." + it + "/" + it + ".plans.xml.gz");
			population = tempScenario.getPopulation();
		} else if(!populationAlreadyLoaded) {
			PopulationReader pr = new PopulationReader(tempScenario);
			pr.readFile(outDir + "/output_plans.xml.gz");
			population = tempScenario.getPopulation();			
		}



		MultiModalBicycleDoorToDoorHandler eventsHandler = new 
				MultiModalBicycleDoorToDoorHandler();
		eventsHandler.setNetwork(network);
		eventsHandler.setFetchHourlyLinkFlows(true);
		eventsHandler.setFetchHourlyLinkStucks(true);
		eventsHandler.setFetchHourlyLinkSpeeds(true);

		eventsHandler.setPopulation(population);
		eventsHandler.setAnalysedModes(analysedModes);
		eventsHandler.setRoW(scenario.getConfig().controler().isLinkToLinkRoutingEnabled());
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(eventsHandler);


		String outDirWithIt = it >= 0 ? outDir += "/ITERS/it." + it : outDir;

		MyEventWriterXML myWriterXML = null;
		if(interestingPersonsList != null && !interestingPersonsList.isEmpty()){
			myWriterXML = new MyEventWriterXML(outDirWithIt + "/filteredEvents.xml.gz");
			HashSet<String> interestingPersons = new HashSet<String>();
			for(String s : interestingPersonsList ){
				interestingPersons.add(s);
			}
			myWriterXML.setInterestingPersons(interestingPersons);
			eventsManager.addHandler(myWriterXML);
		}


		MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);


		if(it >= 0){
			eventsReader.readFile(outDirWithIt + "/" + it + ".events.xml.gz");
		} else {
			eventsReader.readFile(outDirWithIt + "/output_events.xml.gz");
		}
		//eventsHandler.writeFlowSpeeds(outDir + "/speedFlows.csv");
		eventsHandler.writeFlowSpeeds(outDirWithIt + "/AllFlowSpeeds.csv");
		eventsHandler.writeAllStucks(outDirWithIt + "/");
		eventsHandler.writeDetailedOutput(outDirWithIt +  "/detailedOutput.csv");
		if(interestingPersonsList != null && !interestingPersonsList.isEmpty()){
			myWriterXML.closeFile();
		}




		createAndWriteTravelTimeTable(outDirWithIt + "/TravelTimesTable.csv", population, eventsHandler);


		FileWriter writer = new FileWriter(outDirWithIt + "/variousMeasures.txt");

		for(String mode : eventsHandler.totalTravelTimes.keySet()){

			double[] measures = calculateCongestedTravelTimeAndTotalDistance(population, eventsHandler, mode);
			int nTrips = 0;
			for(Person person : scenario.getPopulation().getPersons().values()){
				for(PlanElement planElement : person.getSelectedPlan().getPlanElements()){
					if(planElement instanceof Leg && mode.equals(((Leg) planElement).getMode())){
						nTrips++;
					}
				}
			}
			int highlightTrips = eventsHandler.highlightTrips.get(mode).get();

			String s = "\n" + nTrips + " " + mode + " trips in this study";
			s += "\nTotal " + mode + " travel time is: " + measures[0]/60. + " minutes\n";
			s += "That is " + (measures[0]/nTrips/60.) + " minutes per trip\n";
			s += "Total congested " + mode + " travel time is: " + measures[1]/60. + " minutes\n";
			s += "That is " + (measures[1]/nTrips/60.) + " minutes per trip\n";
			s += "Total " + mode + " distance is: " + measures[2]/1000. + " kilometres\n";
			s += "That is " + (measures[2]/nTrips/1000.) + " kilometres per trip\n";
			System.out.println(s);
			String s2 = "\n" + highlightTrips + " " + mode + " highlight trips in this study\n";
			s2 += "Total highlight " + mode + " travel time is: " + measures[3]/60. + " minutes\n";
			s2 += "That is " + (measures[3]/highlightTrips/60.) + " minutes per trip\n";
			s2 += "Total " + mode + " congested travel time is: " + measures[4]/60. + " minutes\n";
			s2 += "That is " + (measures[4]/highlightTrips/60.) + " minutes per trip\n";
			s2 += "Total " + mode + " distance is: " + measures[5]/1000. + " kilometres\n";
			s2 += "That is " + (measures[5]/highlightTrips/1000.) + " kilometres per trip\n";
			System.out.println(s2);

			writer.append(s);
			writer.append(s2);
		}
		writer.flush();
		writer.close();
	}

	private static void createAndWriteTravelTimeTable(String outputFilename, Population pop, MultiModalBicycleDoorToDoorHandler eventsHandler){
		HashMap<Id<Person>, HashMap<String, LinkedList<Double>>> everyTravelTime = eventsHandler.everyTravelTime;
		Network network = eventsHandler.getNetwork();
		FileWriter writer;
		try {
			writer = new FileWriter(outputFilename);
			writer.append("PersonId;Mode;TripNumber;ActualTravelTime;FreeFlowTravelTime;Distance;"
					+ "OriginTime;DestinationTime;OriginWithinCC;DestinationWithinCC\n");
			long outOfTrips = 0;
			
			for(Person person : pop.getPersons().values()){
				Id<Person> personId = person.getId();
				if(everyTravelTime.containsKey(personId)){
					Set<String> relevantModes = everyTravelTime.get(personId).keySet();
					TreeMap<Integer,String> relevantLegs = new TreeMap<Integer,String>();
					HashSet<Integer> relevantHighlightLegs = new HashSet<Integer>();
					HashMap<String,AtomicInteger> tripCounter = new HashMap<String,AtomicInteger>();

					//Extracting relevant sublegs.
					int i = 0;
					for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
						if(pe instanceof Leg){
							String mode = ((Leg) pe).getMode();
							if(relevantModes.contains(mode)){
								if(!tripCounter.containsKey(mode)){
									tripCounter.put(mode, new AtomicInteger(0));
								}
								relevantLegs.put(i,mode);
								Leg leg = (Leg) pe;			
								if(eventsHandler.isHighlightTrip(leg)){
									relevantHighlightLegs.add(i);
								}
							}
						}
						i++;
					}

					Object[] planElements = person.getSelectedPlan().getPlanElements().toArray();
					for(int j  : relevantLegs.keySet()){

						String mode = relevantLegs.get(j);

						Leg mainLeg = (Leg) planElements[j];
						if(mainLeg.getRoute().getDistance() == 0) {// !everyTravelTime.get(personId).get(mode).isEmpty() || ){
							continue;
						}
						Leg accessLeg = (Leg) planElements[j-2];
						Leg egressLeg = (Leg) planElements[j+2];

						double distance = 0;
						double freeFlowTravelTime = 0;

						for(Leg leg : Arrays.asList(accessLeg,mainLeg,egressLeg)){
							double legDistance = leg.getRoute().getDistance();
							double legFreeFlowTravelTime = leg.getTravelTime().seconds();

							// The router bases distance of entire trip, but travel time without the last link!
							// This means, that when comparing leg by leg, the numbers differ. But since we have
							// taken the actual travel time from the events file, the aggregated numbers are in
							// fact correct without subtract the distance of the last link.. 'Mads.

							//For cars the distance does NOT include the last link...

							// So it makes no sense to compare the free flow travel time with the (route-)travel times,
							// only with the travel times extracted from the events file.

							if(mode.equals(leg.getMode()) && legDistance > 0){
								if(leg.getMode().equals(TransportMode.bike)){
									double freeSpeed = (double) person.getAttributes().getAttribute("v_0");
									legFreeFlowTravelTime = Math.ceil(legDistance / freeSpeed) ;
								} else if(leg.getMode().equals(TransportMode.car)) {
									NetworkRoute route = (NetworkRoute) leg.getRoute();
									legFreeFlowTravelTime = 0;
									Link link;
									for(Id<Link> id : route.getLinkIds()){
										link = network.getLinks().get(id);
										legFreeFlowTravelTime += Math.ceil(link.getLength() / link.getFreespeed());
									}
									link = network.getLinks().get(leg.getRoute().getEndLinkId());
									legFreeFlowTravelTime += Math.ceil(link.getLength() / link.getFreespeed());
								} else if(leg.getMode().equals(TransportMode.truck)) {
									NetworkRoute route = (NetworkRoute) leg.getRoute();
									legFreeFlowTravelTime = 0;
									Link link;
									for(Id<Link> id : route.getLinkIds()){
										link = network.getLinks().get(id);
										double freeSpeed = Math.min(link.getFreespeed(), 80/3.6);
										legFreeFlowTravelTime += Math.ceil(link.getLength() / freeSpeed);
									}
									link = network.getLinks().get(leg.getRoute().getEndLinkId());
									double freeSpeed = Math.min(link.getFreespeed(), 80/3.6);
									legFreeFlowTravelTime += Math.ceil(link.getLength() / freeSpeed);
								}
							}
							freeFlowTravelTime += legFreeFlowTravelTime;
							distance += legDistance;			
						}
						int tripNumber = tripCounter.get(mode).incrementAndGet();
						
						if(!everyTravelTime.get(personId).get(mode).isEmpty()){
						double actualTravelTime = everyTravelTime.get(personId).get(mode).pollFirst();
						double originTime = mainLeg.getDepartureTime().seconds();
						double destinationTime = mainLeg.getDepartureTime().seconds() + actualTravelTime - 
								accessLeg.getTravelTime().seconds() - egressLeg.getTravelTime().seconds();
						Coord originCoord = network.getLinks().get(mainLeg.getRoute().getStartLinkId()).getCoord();
						Coord destinationCoord = network.getLinks().get(mainLeg.getRoute().getEndLinkId()).getCoord();
						int originGeo = MultiModalBicycleDoorToDoorHandler.isInHighlightArea(originCoord) ? 1 : 0;
						int destinationGeo = MultiModalBicycleDoorToDoorHandler.isInHighlightArea(destinationCoord) ? 1 : 0;

						writer.append(personId + ";" + mode + ";" + tripNumber + ";" +  actualTravelTime + ";" +
								freeFlowTravelTime + ";" + distance + ";" + originTime + ";" + destinationTime + ";" +
								originGeo + ";" + destinationGeo + "\n");
						} else {
							outOfTrips++;
						}
					}

				}

			}
			System.out.println( outOfTrips + " occurences of running out of trips");
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static double[] calculateCongestedTravelTimeAndTotalDistance(Population pop, MultiModalBicycleDoorToDoorHandler eventsHandler, String analysedMode) throws IOException{
		double totalFreeFlowTime= 0.;
		double totalDistance = 0.;
		double totalHighlightFreeFlowTime= 0.;
		double totalHighlightDistance = 0.;

		FileWriter writer = new FileWriter("/work1/s103232/DTA2020/withNodeModelling/SmallRoW50/detailedOutputAlt.csv");
		writer.append("Person;Trip;FreeT\n");

		double totalTravelTime = eventsHandler.totalTravelTimes.get(analysedMode).get();
		double totalHighlightTravelTime = eventsHandler.totalHighlightTravelTimes.get(analysedMode).get();
		Network network = eventsHandler.getNetwork();

		int highlightCounter = 0;

		double totalTravelTime2 = 0;

		for(Person person : pop.getPersons().values()){
			HashSet<Integer> relevantLegs = new HashSet<Integer>();
			HashSet<Integer> relevantHighlightLegs = new HashSet<Integer>();

			//Extracting relevant sublegs.
			int i = 0;
			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
				if(pe instanceof Leg && analysedMode.equals(((Leg) pe).getMode()) ){
					relevantLegs.add(i);
					Leg leg = (Leg) pe;			
					if(eventsHandler.isHighlightTrip(leg)){
						relevantHighlightLegs.add(i);
					}
				}
				i++;
			}

			i = 0;
			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
				if(relevantLegs.contains(i)){
					Leg leg = (Leg) pe;
					double legTravelTime = leg.getTravelTime().seconds();
					double legDistance = leg.getRoute().getDistance();
					double freeFlowTravelTime = legTravelTime;

					// The router bases distance of entire trip, but travel time without the last link!
					// This means, that when comparing leg by leg, the numbers differ. But since we have
					// taken the actual travel time from the events file, the aggregated numbers are in
					// fact correct without subtract the distance of the last link.. 'Mads.

					//For cars the distance does NOT include the last link...

					// So it makes no sense to compare the free flow travel time with the (route-)travel times,
					// only with the travel times extracted from the events file.

					if(analysedMode.equals(leg.getMode()) && legDistance > 0){
						if(leg.getMode().equals(TransportMode.bike)){
							double freeSpeed = (double) person.getAttributes().getAttribute("v_0");
							//legDistance -= network.getLinks().get(leg.getRoute().getEndLinkId()).getLength();
							freeFlowTravelTime = Math.floor(legDistance / freeSpeed);		
						} else if(leg.getMode().equals(TransportMode.car)) {
							NetworkRoute route = (NetworkRoute) leg.getRoute();
							freeFlowTravelTime = 0;
							Link link;
							for(Id<Link> id : route.getLinkIds()){
								link = network.getLinks().get(id);
								freeFlowTravelTime += Math.ceil(link.getLength() / link.getFreespeed());
							}
							link = network.getLinks().get(leg.getRoute().getEndLinkId());
							freeFlowTravelTime += Math.ceil(link.getLength() / link.getFreespeed());
						} else if(leg.getMode().equals(TransportMode.truck)) {
							NetworkRoute route = (NetworkRoute) leg.getRoute();
							freeFlowTravelTime = 0;
							Link link;
							for(Id<Link> id : route.getLinkIds()){
								link = network.getLinks().get(id);
								double freeSpeed = Math.min(link.getFreespeed(), 80/3.6);
								freeFlowTravelTime += Math.ceil(link.getLength() / freeSpeed);
							}
							link = network.getLinks().get(leg.getRoute().getEndLinkId());
							double freeSpeed = Math.min(link.getFreespeed(), 80/3.6);
							freeFlowTravelTime += Math.ceil(link.getLength() / freeSpeed);
						}




						//	double surplusDist = network.getLinks().get(leg.getRoute().getEndLinkId()).getLength();			
						//	System.out.println("Actual congested time (excl. last link) for " + 
						//		leg.getMode()+": " + 
						//		(travelTime - (Math.ceil((distance - surplusDist)/freeSpeed) - 1) ) );
						// System.out.println(travelTime - freeFlowTravelTime);
						if(relevantHighlightLegs.contains(i)){
							totalHighlightFreeFlowTime += freeFlowTravelTime;
							totalHighlightDistance += legDistance;
							highlightCounter++;
							writer.append(person.getId() + ";" + i + ";" + freeFlowTravelTime + "\n");
						}
					}
					totalFreeFlowTime += freeFlowTravelTime;
					totalDistance += legDistance;
					totalTravelTime2 += legTravelTime;

				}
				i++;
			}
		}
		double totalCongestedTravelTime = totalTravelTime - totalFreeFlowTime;	
		double totalHighlightCongestedTravelTime = totalHighlightTravelTime - totalHighlightFreeFlowTime;
		writer.flush();
		writer.close();
		System.out.println("\n\n\n\n\n\n\n\n" + (totalTravelTime2 - totalTravelTime));

		System.out.println("Highlight counter " + highlightCounter);
		return new double[]{totalTravelTime, totalCongestedTravelTime, totalDistance,
				totalHighlightTravelTime, totalHighlightCongestedTravelTime, totalHighlightDistance};
	}

}
