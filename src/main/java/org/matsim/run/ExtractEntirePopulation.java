package org.matsim.run;

import java.util.HashMap;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.MatsimFacilitiesReader;

public class ExtractEntirePopulation {
	
	static double bicycleProbability = 1.0;
	static double carProbability = 0.1; 
	
	
	
	public static void main(String[] args) {
		
		
		HashMap<String, Double> modeProbabilities = new HashMap<String, Double>();
		modeProbabilities.put(TransportMode.car, carProbability);
		modeProbabilities.put(TransportMode.bike, bicycleProbability);
		
		
		
		
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Population population = scenario.getPopulation();
		ActivityFacilities facilities = scenario.getActivityFacilities();
		PopulationReader pr = new PopulationReader(scenario);
		MatsimFacilitiesReader fr = new MatsimFacilitiesReader(scenario);
		
		Config newConfig = ConfigUtils.createConfig();
		Scenario newScenario = ScenarioUtils.createScenario(newConfig);
		Population newPopulation = newScenario.getPopulation();
		PopulationFactory pf = newPopulation.getFactory();
		PopulationWriter pw = new PopulationWriter(newPopulation);

		
		Config newUnevenConfig = ConfigUtils.createConfig();
		Scenario newUnevenScenario = ScenarioUtils.createScenario(newUnevenConfig);
		Population newUnevenPopulation = newUnevenScenario.getPopulation();
		PopulationWriter unevenPW = new PopulationWriter(newUnevenPopulation);

		
		Config newSmallConfig = ConfigUtils.createConfig();
		Scenario newSmallScenario = ScenarioUtils.createScenario(newSmallConfig);
		Population newSmallPopulation = newSmallScenario.getPopulation();
		PopulationWriter smallPW = new PopulationWriter(newSmallPopulation);
		
		
		Random randomSmall = new Random(1);
		Random randomUneven = new Random(1);
		
		
		
		
		fr.readFile("C:/workAtHome/Berlin/Data/facilities_CPH.xml.gz");
		pr.readFile("C:/workAtHome/Berlin/Data/plans_CPH.xml.gz");
		
		int totalNumberOfTrips = 0;
		int unevenNumberOfTrips = 0;
		
		
		for(Person person : population.getPersons().values()){
			Plan plan = person.getSelectedPlan();
			int i = 0;
			boolean[] keptElement = new boolean[plan.getPlanElements().size()];
			boolean[] keptElementUneven = new boolean[plan.getPlanElements().size()];
			
			for(PlanElement pe : plan.getPlanElements()){
				if(pe instanceof Leg){
					Leg leg = (Leg) pe;
					if(modeProbabilities.containsKey(leg.getMode())){
						keptElement[i-1] = true;
						keptElement[i] = true;
						keptElement[i+1] = true;
						totalNumberOfTrips++;
						double prob = modeProbabilities.get(leg.getMode());
						if(prob >= 1 || randomUneven.nextDouble() > prob){
							keptElementUneven[i-1] = true;
							keptElementUneven[i] = true;
							keptElementUneven[i+1] = true;
							unevenNumberOfTrips++;
						}
					} 
				}
				i++;
			}
			i = 0;
			Plan newPlan = PopulationUtils.createPlan();
			Plan newPlanUneven = PopulationUtils.createPlan();
			newPlan = addElementsToPlan(facilities, plan, i, keptElement, newPlan);
			newPlanUneven = addElementsToPlan(facilities, plan, i, keptElementUneven, newPlanUneven);
			
			Person newPerson = addPlanToPersonAndPopulation(newPopulation, pf, person, newPlan);
			addPlanToPersonAndPopulation(newUnevenPopulation, pf, person, newPlanUneven);
			if(newPerson != null && randomSmall.nextDouble() < 0.0001){
				newSmallPopulation.addPerson(newPerson);
			}
		}
		
		
		smallPW.write("./input/AllPlans_CPH_small.xml.gz");
		unevenPW.write("./input/AllPlans_CPH_uneven.xml.gz");
		pw.write("./input/AllPlans_CPH_full.xml.gz");

		
		System.out.println("Uneven number of agents: " + newUnevenPopulation.getPersons().size());
		System.out.println("Uneven number of trips: " + unevenNumberOfTrips);
	
		System.out.println("Total number of agents: " + newPopulation.getPersons().size());
		System.out.println("Total number of trips: " + totalNumberOfTrips);
		
	
	}



	private static Person addPlanToPersonAndPopulation(Population newPopulation, PopulationFactory pf, Person person,
			Plan newPlan) {
		if(newPlan.getPlanElements().size() == 0){
			//do nothing - the person has no valid trips, and thus not qualified for the new population.
			return null;
		} else {
			
			Person newPerson = pf.createPerson(person.getId());
			newPlan.setPerson(newPerson);
			newPerson.addPlan(newPlan);
			newPerson.setSelectedPlan(newPlan);
			newPopulation.addPerson(newPerson);
			return newPerson;
		}
	}



	private static Plan addElementsToPlan(ActivityFacilities facilities, Plan plan, int i, boolean[] keptElement,
			Plan newPlan) {
		for(PlanElement pe : plan.getPlanElements()){
			if(keptElement[i]){
				if(pe instanceof Leg){
					Leg leg = (Leg) pe;
					leg.setRoute(null);
					newPlan.addLeg(leg);
				} else {
					Activity activity = (Activity) pe;
					ActivityFacility facility = facilities.getFacilities().get(activity.getFacilityId());
					Coord coord = facility.getCoord();
					activity.setFacilityId(null);
					activity.setLinkId(null);
					activity.setCoord(coord);
					newPlan.addActivity((Activity) pe);
				}
			}
			i++;
		}

		return plan;
	}
}
