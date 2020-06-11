package fastOrForcedToFollow.scoring;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.misc.Time;

import com.google.inject.Inject;

import fastOrForcedToFollow.configgroups.FFFScoringConfigGroup;

public class FFFScoringFunctionFactory implements ScoringFunctionFactory {

	private Network network;
	private final FFFScoringParameters params;
	private double simulationEndTime;
	private double simulationStartTime;

	
	public FFFScoringFunctionFactory( final Scenario sc ) {
		this( sc.getConfig() , sc.getNetwork() );
	}

	@Inject
	FFFScoringFunctionFactory(Config config, Network network) {
		FFFScoringConfigGroup fffScoringConfig =
				ConfigUtils.addOrGetModule(config, FFFScoringConfigGroup.class);
		this.params = fffScoringConfig.getMyScoringParameters();
		this.network = network;
		
		if(Time.isUndefinedTime(config.qsim().getStartTime()) || Double.isInfinite(config.qsim().getStartTime())) {
			this.simulationStartTime = 0.;
		} else {
			this.simulationStartTime = config.qsim().getStartTime();
		}
		
		if(Time.isUndefinedTime(config.qsim().getEndTime()) || Double.isInfinite(config.qsim().getEndTime())) {
			this.simulationEndTime = 30*3600.;
		} else {
			this.simulationEndTime = config.qsim().getEndTime();
		}
	}

	@Override
	public ScoringFunction createNewScoringFunction(final Person person) {
		//Not person-specific at the moment.
		return new FFFScoringFunction(params, network, person, simulationStartTime, simulationEndTime);
	}

}
