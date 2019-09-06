package org.matsim.run;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class TestCopenhagenNetwork {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
	
		Network network = scenario.getNetwork();
	
		MatsimNetworkReader nr =  new MatsimNetworkReader(network);
		nr.readFile("/zhome/81/e/64390/MATSim/ABMTRANS2019/input/MATSimCopenhagenNetwork_WithBicycleInfrastructure.xml.gz");

/*	
		for(Link link : network.getLinks().values()){

			if(link.getAllowedModes().contains(TransportMode.car)){		
				if(link.getNumberOfLanes()> 4){
					System.out.println(link.getCapacity() + "  " + link.getCapacity() / link.getNumberOfLanes() +
							" " + link.getNumberOfLanes());
				}
			}
		}
		*/
		for(Node node : network.getNodes().values()){
			int inCar = 0;
			int inBicycle = 0;
			int outCar = 0;
			int outBicycle = 0;
			for(Link link : node.getInLinks().values()){
				if(link.getAllowedModes().contains(TransportMode.car)){
					inCar++;
				}
				if(link.getAllowedModes().contains(TransportMode.bike)){
					inBicycle++;
				}
			}
			for(Link link : node.getOutLinks().values()){
				if(link.getAllowedModes().contains(TransportMode.car)){
					outCar++;
				}
				if(link.getAllowedModes().contains(TransportMode.bike)){
					outBicycle++;
				}
			}
			if(inCar == 2 && outCar == 1){
				System.out.println("In: " + inBicycle + "   out: " + outBicycle);
			}
		}
	}

}
