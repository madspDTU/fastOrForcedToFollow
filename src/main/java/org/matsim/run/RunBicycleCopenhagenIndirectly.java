package org.matsim.run;

import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

public class RunBicycleCopenhagenIndirectly {

	public static void main(String[] args) throws IOException{

		//Run fullRowBothResume50
		//Run fullRowBothDasAutoResume50
/*
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Person person = scenario.getPopulation().getFactory().createPerson(Id.create("TheOne",Person.class));
		Plan plan = scenario.getPopulation().getFactory().createPlan();
		plan.setPerson(person);
		Activity act1 = PopulationUtils.createActivityFromCoord("home", CoordUtils.createCoord(718117.32, 6174085.13));
		act1.setEndTime(8*3600);
		Leg leg = PopulationUtils.createLeg(TransportMode.bike);
		Activity act2 = PopulationUtils.createActivityFromCoord("work", CoordUtils.createCoord(722050.02, 6178807.76));
		act2.setEndTime(Double.POSITIVE_INFINITY);

		plan.addActivity(act1);
		plan.addLeg(leg);
		plan.addActivity(act2);

		person.addPlan(plan);
		person.setSelectedPlan(plan);
		scenario.getPopulation().addPerson(person);

		PopulationWriter writer = new PopulationWriter(scenario.getPopulation());
		writer.write("/zhome/81/e/64390/MATSim/DTA2020/input/dadadadada.xml");


*/ 

		/*
		 *
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader pr = new PopulationReader(scenario);
		pr.readFile("/zhome/81/e/64390/MATSim/DTA2020/input/COMPASBicycle100_COMPASSCarOTM100.xml.gz");
		
		long bCounter = 0;
		long cCounter = 0;
		long tCounter = 0;
		for(Person person : scenario.getPopulation().getPersons().values()) {
			for(PlanElement pe : person.getPlans().get(0).getPlanElements()) {
				if(pe instanceof Leg) {
					if(((Leg) pe).getMode().equals(TransportMode.bike)){
						bCounter++;
					} else if(((Leg) pe).getMode().equals(TransportMode.car)){
						cCounter++;
					} else if(((Leg) pe).getMode().equals(TransportMode.truck)){
						tCounter++;
					}
				}
			}
		}
		
		System.out.println("#Bicycle trips: " + bCounter);
		System.out.println("#Car trips: " + cCounter);
		System.out.println("#Truck trips: " + tCounter);
		System.exit(-1);
	*/	
		
		
		//	if(args.length==0){
		args = new String[]{
				"Full150_10n_qKKos", "1", "bicycle", "true", "8" ,"0.05", "Bounded", "Best", "true", "1.00", "queue", "-1", "1000"}; 
		//	};
		RunBicycleCopenhagen.main(args);
		
		//Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		//		PopulationReader reader = new PopulationReader(scenario);
		//		reader.readFile("/zhome/81/e/64390/MATSim/DTA2020/input/AllPlans_CPH_Full.xml.gz");
		//		System.out.println(scenario.getPopulation().getPersons().size() + " agents");


		//		 	ConstructSpeedFlowsFromCopenhagen.run(
		//	"/work1/s103232/ABMTRANS2019/withNodeModelling/smallRoWMixedQSimEndsAt100/", 
		//			"small", 0, Arrays.asList(),Arrays.asList(TransportMode.bike, TransportMode.car));
	}
}
