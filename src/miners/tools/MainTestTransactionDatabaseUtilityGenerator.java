package miners.tools;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;



/**
 * Example of how to generate utility values randomly for a transaction database
 * in SPMF format.
 */
public class MainTestTransactionDatabaseUtilityGenerator {
	
	public static void main(String [] arg) throws IOException{
		String name = "USCensus";
		String inputFile = name+".txt";
		String outputFile = name+"Utility.txt";
		
		// The maximum quantity of an item in a transaction will be 10
		int maxQuantityOfItemInTransaction = 10;
		// the external utility of items will be generate by Random.nextGaussian() 
		// and will be multiplied by this value
		double externalUtilityMultiplicativeFactor = 1d;
		
		// generat the utility values
		TransactionDatasetUtilityGenerator converter = new TransactionDatasetUtilityGenerator();
		converter.convert(inputFile, outputFile, maxQuantityOfItemInTransaction,
				externalUtilityMultiplicativeFactor);
	}

	

	public static String fileToPath(String filename) throws UnsupportedEncodingException{
		URL url = MainTestTransactionDatabaseUtilityGenerator.class.getResource(filename);
		 return java.net.URLDecoder.decode(url.getPath(),"UTF-8");
	}
}