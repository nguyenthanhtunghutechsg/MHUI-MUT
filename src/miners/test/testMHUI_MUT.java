package miners.test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import miners.algorithms.frequentpatterns.efim.MHUI_MUT;
import miners.algorithms.frequentpatterns.efim.pMHUI_MUT;
import miners.MHUI.AlgoFHM;
import miners.algorithms.frequentpatterns.efim.Itemsets;

// dEFIM TESTER, OUTPUT TO SCREEN
public class testMHUI_MUT {

	public static void main(String[] arg) throws IOException {
		String input = fileToPath("DetailExample.txt"); // the input and output file paths
		String MMUPath = fileToPath("DetailExampleMMU.txt");

		long GLMU = 1;
		int Beta = 1;
		int dbSize = Integer.MAX_VALUE;
		int number = 1;
		switch (number) {
		case 1:
			MHUI_MUT algo = new MHUI_MUT(); // Create the dEFIM algorithm object
			algo.runAlgorithm(input, MMUPath, dbSize, Beta, GLMU);
			algo.printStats();
			break;
		case 2:
			AlgoFHM algoFHM = new AlgoFHM(); // Create the dEFIM algorithm object
			algoFHM.runAlgorithm(input, MMUPath, null,Beta,GLMU);
			algoFHM.printStats();
			break;
		default:
			break;
		}

	}

	public static String fileToPath(String filename) throws UnsupportedEncodingException {
		URL url = testMHUI_MUT.class.getResource(filename);
		return java.net.URLDecoder.decode(url.getPath(), "UTF-8");
	}
}
