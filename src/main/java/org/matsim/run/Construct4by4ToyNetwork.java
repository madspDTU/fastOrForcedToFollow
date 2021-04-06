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

public class Construct4by4ToyNetwork {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Config configNew = ConfigUtils.createConfig();
		Scenario scenarioNew = ScenarioUtils.loadScenario(configNew);

		Network network = scenario.getNetwork();
		Network networkNew = scenarioNew.getNetwork();
		NetworkFactory nf = networkNew.getFactory();

		MatsimNetworkReader nr =  new MatsimNetworkReader(network);
		nr.readFile("/zhome/81/e/64390/MATSim/input/Berlin/berlin-v5-network.xml.gz");

		HashMap<String, Id<Link>> nodePairs = new HashMap<String, Id<Link>>();

		for(Link link : network.getLinks().values()){

			if(link.getAllowedModes().contains(TransportMode.car)){		
				Node fromNode = link.getFromNode();
				Node toNode = link.getToNode();
				if(!networkNew.getNodes().containsKey(fromNode.getId())){
					Node tempNode = nf.createNode(fromNode.getId(), fromNode.getCoord());
					networkNew.addNode(tempNode);
				}
				if(!networkNew.getNodes().containsKey(toNode.getId())){
					Node tempNode = nf.createNode(toNode.getId(), toNode.getCoord());
					networkNew.addNode(tempNode);
				}
				String nodePair = fromNode.getId().toString() + "_" + toNode.getId().toString();
				if(!nodePairs.containsKey(nodePair)){

					Link newLink = nf.createLink(link.getId(), fromNode, toNode);
					HashSet<String> modes = new HashSet<String>();
					modes.add(TransportMode.car);
					newLink.setAllowedModes(modes);
					newLink.setFreespeed(link.getFreespeed());
					newLink.setCapacity(link.getCapacity());
					newLink.setNumberOfLanes(link.getNumberOfLanes());
					newLink.setLength(link.getLength());

					networkNew.addLink(newLink);
					nodePairs.put(nodePair, newLink.getId());

				} else {
					Link oldLink = networkNew.getLinks().get(nodePairs.get(nodePair));
					oldLink.setNumberOfLanes(Double.max(oldLink.getNumberOfLanes(), link.getNumberOfLanes()));
					oldLink.setCapacity(Double.max(oldLink.getCapacity(), link.getCapacity()));
					oldLink.setFreespeed(Double.max(oldLink.getFreespeed(), link.getFreespeed()));
				}
			}
		}

		NetworkWriter nw = new NetworkWriter(networkNew);
		nw.write("/zhome/81/e/64390/MATSim/input/Berlin/simplifiedNetwork.xml.gz");
	}

}
