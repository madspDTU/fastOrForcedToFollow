package org.matsim.run;

import java.util.LinkedList;
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

public class ExtractBicyclePopulation {

	public static void main(String[] args) {
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
		
		Config newSmallConfig = ConfigUtils.createConfig();
		Scenario newSmallScenario = ScenarioUtils.createScenario(newSmallConfig);
		Population newSmallPopulation = newSmallScenario.getPopulation();
		PopulationWriter smallPW = new PopulationWriter(newSmallPopulation);
		
		
		Random random = new Random(1);
		
		
		fr.readFile("C:/workAtHome/Berlin/Data/facilities_CPH.xml.gz");
		pr.readFile("C:/workAtHome/Berlin/Data/plans_CPH.xml.gz");
		
		
		for(Person person : population.getPersons().values()){
			Plan plan = person.getSelectedPlan();
			int i = 0;
			boolean[] keptElement = new boolean[plan.getPlanElements().size()];
			for(PlanElement pe : plan.getPlanElements()){
				if(pe instanceof Leg){
					Leg leg = (Leg) pe;
					if(leg.getMode().equals(TransportMode.bike)){
						keptElement[i-1] = true;
						keptElement[i] = true;
						keptElement[i+1] = true;
					} 
				}
				i++;
			}
			i = 0;
			Plan newPlan = PopulationUtils.createPlan();
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
			if(newPlan.getPlanElements().size() == 0){
				//do nothing
			} else {
				
				Person newPerson = pf.createPerson(person.getId());
				newPlan.setPerson(newPerson);
				newPerson.addPlan(newPlan);
				newPerson.setSelectedPlan(newPlan);
				newPopulation.addPerson(newPerson);
				if(random.nextDouble() < 0.001){
					newSmallPopulation.addPerson(newPerson);
				}
			}
		}
		
		smallPW.write("C:/workAtHome/Berlin/Data/BicyclePlans_CPH_1percent.xml.gz");
		pw.write("C:/workAtHome/Berlin/Data/BicyclePlans_CPH.xml.gz");
		
	}
	
}
