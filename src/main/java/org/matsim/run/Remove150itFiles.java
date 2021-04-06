package org.matsim.run;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Remove150itFiles {

	public static void main(String[] args) throws IOException {

		doStuff("/work1/s103232/DTA2020");
		System.out.println("<Done>");
	}


	private static void doStuff(String directoryName) throws IOException {
		File directory = new File(directoryName);

		// Get all files from a directory.
		File[] fList = directory.listFiles();
		if(fList != null) {
			for (File file : fList) {      
				if (file.isFile()) {
					// Deleting 150 iteration files
					if( file.toString().endsWith("150.events.xml.gz") || file.toString().endsWith("150.plans.xml.gz") ) {
						System.out.println("Removing " + file.toString());
						file.delete();
					} else if(file.toString().endsWith("logfile.log")){
						System.out.println("Processing logfile: " + file.toString());
						FileWriter writer = new FileWriter(directoryName + "/StuckEventsPerIteration.csv");
						writer.append("Iteration;StuckEvents\n");
						BufferedReader br = new BufferedReader(new FileReader(file.getAbsolutePath()));
						String nextLine;
						String s1 = "A total of ";
						String s2 =  " stuck events in this iteration.";
						
						int iteration = 0;
						while((nextLine = br.readLine()) != null) {
							if( nextLine.contains(" stuck events in this iteration.")) {
								int i2 = nextLine.indexOf(s2);
								int i1 = nextLine.indexOf(s1) + s1.length();
								long stuckEvents = Long.parseLong(nextLine.substring(i1, i2));
								writer.append(iteration + ";" + stuckEvents + "\n");
								iteration++;
							}
						}
						br.close();
						writer.flush();
						writer.close();
					}
				} else if (file.isDirectory()) {
					doStuff(file.getAbsolutePath());
				}
			}
		}
	}
}


