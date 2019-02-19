package fastOrForcedToFollow.scoring;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.SumScoringFunction.AgentStuckScoring;
import org.matsim.core.scoring.SumScoringFunction.LegScoring;

public class FFFScoringFunctionFactory implements ScoringFunction {

	private final LegScoring legScoring;
	private final AgentStuckScoring stuckScoring;


	public FFFScoringFunctionFactory(final FFFScoringParameters params, Network network, Person person) {
		this.legScoring = new FFFLegScoring(params, network, person);
		this.stuckScoring = new FFFStuckScoring(params);
	}

	
	@Override
	public void handleActivity(Activity activity) {
		// Do nothing
	}

	@Override
	public void handleLeg(Leg leg) {
		legScoring.handleLeg(leg);
	}

	@Override
	public void agentStuck(double time) {
		stuckScoring.agentStuck(time);	
	}

	@Override
	public void addMoney(double amount) {
		// Do nothing
	}

	@Override
	public void finish() {
		// Do nothing
	}

	@Override
	public double getScore() {
		return stuckScoring.getScore() + legScoring.getScore();
	}

	@Override
	public void handleEvent(Event event) {
		//Do nothing
	}
	
}