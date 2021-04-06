package org.matsim.core.population;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.utils.objectattributes.attributable.Attributes;

public class FFFPlan implements Plan {

	private Plan delegate;
	private int latestUsedIteration = -1;


	public FFFPlan(Plan delegate){
		this.delegate = delegate;
	}

	public FFFPlan(Plan delegate, int latestUsedIteration){
		this.delegate = delegate;
		this.latestUsedIteration = latestUsedIteration;
	}


	public int getLatestUsedIteration() {
		return latestUsedIteration;
	}

	public void setLatestUsedIteration(int latestUsedIteration) {
		this.latestUsedIteration = latestUsedIteration;
	}



	@Override
	public Map<String, Object> getCustomAttributes() {
		return this.delegate.getCustomAttributes();
	}

	@Override
	public void setScore(Double score) {
		this.delegate.setScore(score);
	}

	@Override
	public Double getScore() {
		return this.delegate.getScore();
	}

	@Override
	public Attributes getAttributes() {
		return this.delegate.getAttributes();
	}

	@Override
	public List<PlanElement> getPlanElements() {
		return this.delegate.getPlanElements();
	}

	@Override
	public void addLeg(Leg leg) {
		this.delegate.addLeg(leg);
	}

	@Override
	public void addActivity(Activity act) {
		this.delegate.addActivity(act);
	}

	@Override
	public String getType() {
		return this.delegate.getType();
	}

	@Override
	public void setType(String type) {
		this.delegate.setType(type);
	}

	@Override
	public Person getPerson() {
		return this.delegate.getPerson();
	}

	@Override
	public void setPerson(Person person) {
		this.delegate.setPerson(person);

	}


	public static void convertToFFFPlans(Population population) {
		for(Person person : population.getPersons().values()) {
			int i = 0;
			int n = person.getPlans().size();
			while( i < n) {
				if(!(person.getPlans().get(i) instanceof FFFPlan)) {
					Plan originalPlan = person.getPlans().get(i);
					Plan fffPlan = new FFFPlan(originalPlan,0);
					person.getPlans().remove(i);
					person.addPlan(fffPlan);
					person.setSelectedPlan(fffPlan);
					n--;
				} else {
					i++;
				}
			}
		}

	}

}
