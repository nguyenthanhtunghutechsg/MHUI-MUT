package miners.MHUI;

import java.io.IOException;

/**
 * Example of how to use the FHM algorithm 
 * from the source code.
 * @author Philippe Fournier-Viger, 2014
 */
public class MainTestFHM {

	public static void main(String [] arg) throws IOException{
		
		String input = "BMS.txt";
		String output = ".//output.txt";
		String MMU = "BMSMMU.txt";
		//int min_utility = 2000;  // 
		
		// Applying the HUIMiner algorithm
		AlgoFHM fhm = new AlgoFHM();
		//fhm.runAlgorithm(input,MMU, output);
		fhm.printStats();

	}
}
