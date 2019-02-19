package fastOrForcedToFollow.scoring;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.run.FFFScoringConfigGroup;

import com.google.inject.Inject;

public class ScoringFunctionPenalisingCongestedTimeFactory implements ScoringFunctionFactory {

	private Network network;
	private final MyScoringParameters params;

	
	public ScoringFunctionPenalisingCongestedTimeFactory( final Scenario sc ) {
		this( sc.getConfig() , sc.getNetwork() );
	}

	@Inject
	ScoringFunctionPenalisingCongestedTimeFactory(Config config, Network network) {
		FFFScoringConfigGroup fffScoringConfig =
				ConfigUtils.addOrGetModule(config, FFFScoringConfigGroup.class);
		this.params = fffScoringConfig.getMyScoringParameters();
		this.network = network;
	}

	@Override
	public ScoringFunction createNewScoringFunction(final Person person) {
		//Not person-specific at the moment.
		return new MyScoringFunction(params, network, person);
	}

}
