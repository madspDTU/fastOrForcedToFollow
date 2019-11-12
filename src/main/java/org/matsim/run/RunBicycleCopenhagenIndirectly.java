package org.matsim.run;

import java.io.IOException;

public class RunBicycleCopenhagenIndirectly {

	public static void main(String[] args) throws IOException{
		
		//Run fullRowBothResume50
		//Run fullRowBothDasAutoResume50
		
		if(args.length==0){
			args = new String[]{
					"fullRoWBoth50", "50", "bike,car",  "true", "30"}; 
		}
		RunBicycleCopenhagen.main(args);




		//		 	ConstructSpeedFlowsFromCopenhagen.run(
		//	"/work1/s103232/ABMTRANS2019/withNodeModelling/smallRoWMixedQSimEndsAt100/", 
		//			"small", 0, Arrays.asList(),Arrays.asList(TransportMode.bike, TransportMode.car));
	}
}
