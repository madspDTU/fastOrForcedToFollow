package org.matsim.run;

import java.io.IOException;
import java.util.Arrays;

import org.matsim.api.core.v01.TransportMode;

public class RunBicycleCopenhagenIndirectly {

	public static void main(String[] args) throws IOException{
		String[] arguments = new String[]{"smallRoWMixed", "1"};
		RunBicycleCopenhagen.main(arguments);
	
		//		ConstructSpeedFlowsFromCopenhagen.run(
		//"/work1/s103232/ABMTRANS2019/withNodeModelling/smallRoWMixed/", 
			//	"small", -1, Arrays.asList(TransportMode.car),Arrays.asList(TransportMode.bike));
	}
}
