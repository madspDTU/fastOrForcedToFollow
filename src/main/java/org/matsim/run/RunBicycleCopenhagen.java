package org.matsim.run;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scoring.ScoringFunctionPenalisingCongestedTimeFactory;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;

public class RunBicycleCopenhagen {

	public static void main(String[] args){
		Config config = RunMatsim.createConfigFromExampleName("berlin");
		config.controler().setOutputDirectory("C:/workAtHome/Berlin/Data/BicycleCopenhagen_1pct");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(1);
		final List<String> networkModes = Arrays.asList( new String[]{TransportMode.bike} );
		config.qsim().setMainModes( networkModes );
		config.plansCalcRoute().setNetworkModes(networkModes);
		config.travelTimeCalculator().setAnalyzedModes( TransportMode.bike);
		config.controler().setWritePlansInterval(5);
		config.controler().setWriteEventsInterval(5);
		config.controler().setCreateGraphs(true);
		config.controler().setDumpDataAtEnd(true);
		
		config.global().setCoordinateSystem("EPSG:32632");   ///EPSG:32632 is WGS84 UTM32N

		{
			Map<String, String> defaultParams = config.planCalcScore().getActivityParams("work").getParams();
			for(String actType : Arrays.asList("other","missing","shopping")){
				ActivityParams ap = new ActivityParams(actType);
				for(String key : defaultParams.keySet()){
					if(!key.equals("activityType")){
						ap.addParam(key, defaultParams.get(key));
					}
				}
				config.planCalcScore().addActivityParams(ap);
			}
			config.planCalcScore().getActivityParams("edu").setActivityType("school");
		}



	
		config.network().setInputFile("C:/workAtHome/Berlin/Data/MATSimCopenhagenNetwork_BicyclesOnly.xml.gz");
		config.plans().setInputFile("C:/workAtHome/Berlin/Data/BicyclePlans_CPH_1percent.xml.gz");

		//Possible changes to config
		FFFConfigGroup fffConfig = ConfigUtils.addOrGetModule(config, FFFConfigGroup.class);
		fffConfig.setLMax(60.);

		Scenario scenario = RunMatsim.addCyclistAttributes(config);



		Controler controler = RunMatsim.createControler(scenario);
		
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install() {
				this.bindScoringFunctionFactory().to( ScoringFunctionPenalisingCongestedTimeFactory.class ) ;
			}
		} );

		try {			
			controler.run();
		} catch ( Exception ee ) {
			ee.printStackTrace();
		}

	}
}
