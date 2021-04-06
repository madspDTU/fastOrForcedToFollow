package org.matsim.run;

import java.util.Arrays;
import java.util.HashSet;
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

public class ExtractCertainModesPlansFile {

	public static void main(String[] args){

		HashSet<String> validModes = new HashSet<String>(Arrays.asList(TransportMode.car, TransportMode.truck));

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Population population = scenario.getPopulation();
		PopulationReader pr = new PopulationReader(scenario);

		Config newConfig = ConfigUtils.createConfig();
		Scenario newScenario = ScenarioUtils.createScenario(newConfig);
		Population newPopulation = newScenario.getPopulation();
		PopulationFactory pf = newPopulation.getFactory();
		PopulationWriter pw = new PopulationWriter(newPopulation);


		pr.readFile("/zhome/81/e/64390/MATSim/ABMTRANS2019/input/AllPlans_CPH_uneven.xml.gz");


		for(Person person : population.getPersons().values()){
			Plan plan = person.getSelectedPlan();
			int i = 0;
			boolean[] keptElement = new boolean[plan.getPlanElements().size()];

			for(PlanElement pe : plan.getPlanElements()){
				if(pe instanceof Leg){
					Leg leg = (Leg) pe;
					if(validModes.contains(leg.getMode())){
						keptElement[i-1] = true;
						keptElement[i] = true;
						keptElement[i+1] = true;
					} 
				}
				i++;
			}
			i = 0;
			Plan newPlan = PopulationUtils.createPlan();
			newPlan = addElementsToPlan(plan, keptElement, newPlan);
		
			addPlanToPersonAndPopulation(newPopulation, pf, person, newPlan);
		}


		pw.write("/zhome/81/e/64390/MATSim/ABMTRANS2019/input/AllPlans_CPH_carsOnly.xml.gz");


		System.out.println("Total number of agents before: " + population.getPersons().size());
		System.out.println("Total number of agents now: " + newPopulation.getPersons().size());


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



	private static Plan addElementsToPlan(Plan plan, boolean[] keptElement, Plan newPlan) {
		int i = 0;
		for(PlanElement pe : plan.getPlanElements()){
			if(keptElement[i]){
				if(pe instanceof Leg){
					Leg leg = (Leg) pe;
					newPlan.addLeg(leg);
				} else {
					Activity activity = (Activity) pe;
					newPlan.addActivity(activity);
				}
			}
			i++;
		}

		return newPlan;
	}
}
