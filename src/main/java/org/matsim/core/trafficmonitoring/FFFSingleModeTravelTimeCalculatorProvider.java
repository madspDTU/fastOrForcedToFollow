package org.matsim.core.trafficmonitoring;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup.TravelTimeCalculatorType;

import fastOrForcedToFollow.configgroups.FFFNodeConfigGroup;

public class FFFSingleModeTravelTimeCalculatorProvider implements Provider<FFFTravelTimeCalculator> {

	@Inject TravelTimeCalculatorConfigGroup config;
	@Inject EventsManager eventsManager;
	@Inject Network network;

	private boolean useFFFTimeDataArray;

	public FFFSingleModeTravelTimeCalculatorProvider(FFFNodeConfigGroup fffNodeConfigGroup) {
		this.useFFFTimeDataArray = fffNodeConfigGroup.getApproximateNullLinkToLinkTravelTimes();
	}

	@Override
	public FFFTravelTimeCalculator get() {
//		TravelTimeCalculator calculator = new TravelTimeCalculator(network, config.getTraveltimeBinSize(), config.getMaxTime(),
//				config.isCalculateLinkTravelTimes(), config.isCalculateLinkToLinkTravelTimes(), true, CollectionUtils.stringToSet(mode));
//		eventsManager.addHandler(calculator);
//		return TravelTimeCalculator.configure(calculator, config, network);
		FFFTravelTimeCalculator.Builder builder = new FFFTravelTimeCalculator.Builder( network );
		builder.setTimeslice( config.getTraveltimeBinSize() );
		builder.setMaxTime( config.getMaxTime() );
		builder.setCalculateLinkTravelTimes( config.isCalculateLinkTravelTimes() );
		builder.setCalculateLinkToLinkTravelTimes( config.isCalculateLinkToLinkTravelTimes() );
		builder.setFilterModes( true ); // no point asking the config since we are in "separateModes" anyways.
		builder.setAnalyzedModes( config.getAnalyzedModes() );
		builder.configure( config );
		builder.setUseFFFTimeDataArray(config.getTravelTimeCalculatorType() == TravelTimeCalculatorType.TravelTimeCalculatorArray && useFFFTimeDataArray);
		FFFTravelTimeCalculator calculator = builder.build();
		eventsManager.addHandler( calculator );
		return calculator ;
	}
}

