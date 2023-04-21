package miners.test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import miners.algorithms.frequentpatterns.efim.MHUI_MUT;
import miners.algorithms.frequentpatterns.efim.pMHUI_MUT;
import miners.algorithms.frequentpatterns.efim.Itemsets;

// dEFIM TESTER, OUTPUT TO SCREEN
public class testpMHUI_MUT {

	public static void main(String[] arg) throws IOException {
		String input = fileToPath("accidentsUtility.txt"); // the input and output file paths
		String MMUPath = fileToPath("accidentsMMU.txt");

		long GLMU = 2000000;
		int Beta = (int) GLMU / 100;
		int dbSize = Integer.MAX_VALUE;
		int number = 2;
		switch (number) {
		case 1:
			MHUI_MUT algo = new MHUI_MUT(); // Create the dEFIM algorithm object
			algo.runAlgorithm(input, MMUPath, dbSize, Beta, GLMU);
			algo.printStats();
			break;
		case 2:
			pMHUI_MUT algo4 = new pMHUI_MUT(); // Create the dEFIM algorithm object
			algo4.runAlgorithm(input, MMUPath, dbSize, Beta, GLMU, 2);
			algo4.printStats();
			break;
		case 3:
			pMHUI_MUT algo8 = new pMHUI_MUT(); // Create the dEFIM algorithm object
			algo8.runAlgorithm(input, MMUPath, dbSize, Beta, GLMU, 8);
			algo8.printStats();
			break;
		default:
			break;
		}

	}

	public static String fileToPath(String filename) throws UnsupportedEncodingException {
		URL url = testpMHUI_MUT.class.getResource(filename);
		return java.net.URLDecoder.decode(url.getPath(), "UTF-8");
	}
}
