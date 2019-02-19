/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.test;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.run.RunMatsim;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator.Result;

import fastOrForcedToFollow.configgroups.FFFConfigGroup;


public class FFFTest {

	private static final Logger log = Logger.getLogger( FFFTest.class);

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	
	
	@Test
	public final void berlinTest_Max50_2() {
		int lanesPerLink = 2;
				
		Config config = RunMatsim.createConfigFromExampleName("berlin");
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		
		//Possible changes to config
		FFFConfigGroup fffConfig = ConfigUtils.addOrGetModule(config, FFFConfigGroup.class);
		fffConfig.setLMax(50.);
		
		Scenario scenario = RunMatsim.createScenario(config, lanesPerLink, false, 1.);
		Controler controler = RunMatsim.createControler(scenario);
		try {		
			controler.run();
		} catch ( Exception ee ) {
			ee.printStackTrace();
		}

		testAux(scenario);

	}

	
	
	@Test @Ignore
	public final void test_2() {
		int lanesPerLink = 2;
		

		Config config = RunMatsim.createConfigFromExampleName("equil");
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		
		Scenario scenario = RunMatsim.createScenario(config, lanesPerLink, true, 1.);
		Controler controler = RunMatsim.createControler(scenario);
		try {		
			controler.run();
		} catch ( Exception ee ) {
			ee.printStackTrace();
		}

		testAux(scenario);
	
	}

	@Test @Ignore
	public final void berlinTest_2() {
		int lanesPerLink = 2;

		Config config = RunMatsim.createConfigFromExampleName("berlin");
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		
		Scenario scenario = RunMatsim.createScenario(config, lanesPerLink, false, 1.);
		Controler controler = RunMatsim.createControler(scenario);
		try {		
			controler.run();
		} catch ( Exception ee ) {
			ee.printStackTrace();
		}

		testAux(scenario);

	}
	

	private void testAux(Scenario scenario){
		String newEventsFile = utils.getOutputDirectory() + "/output_events.xml.gz";
		String referenceEventsFile = utils.getInputDirectory() + "/output_events.xml.gz";
		String referencePopulationFile = utils.getInputDirectory() + "/output_plans.xml.gz";

		Config refConfig = ConfigUtils.createConfig();
		Scenario refScenario = ScenarioUtils.createScenario(refConfig);
		PopulationReader pr = new PopulationReader(refScenario);
		pr.readFile(referencePopulationFile);
		boolean booleanPopulation = PopulationUtils.equalPopulation(refScenario.getPopulation(), scenario.getPopulation());

		Result resultEvents = EventsFileComparator.compare(referenceEventsFile, newEventsFile);
		if(booleanPopulation){ log.info("Populations files are semantic equivalent"); 			} else {
			log.warn("Populations files are not semantic equivalent"); 	}

		Assert.assertEquals(resultEvents, Result.FILES_ARE_EQUAL);
		Assert.assertTrue("Different plans file", booleanPopulation);
	}
	
}