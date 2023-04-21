package miners.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

public class GenMU {

	static Map<Integer,Integer>  mapItemToThreshhold;
	public static void main(String[] args) throws NumberFormatException, IOException {
		String pathOutput = "C:\\Users\\Admin\\OneDrive - hutech.edu.vn\\Desktop\\HUTECH\\NCKH\\SOURCE\\MEHUI\\MTiMEFIM_Package\\src\\miners\\tools\\";
		String pathInPut = "C:\\Users\\Admin\\OneDrive - hutech.edu.vn\\Desktop\\HUTECH\\NCKH\\SOURCE\\MEHUI\\MTiMEFIM_Package\\src\\miners\\test\\";
		String FileName = "USCensus";
		TransactionDBUtilityStatsGenerator tdbg = new TransactionDBUtilityStatsGenerator();
		tdbg.runAlgorithm(pathInPut+FileName+"Utility.txt");
		// TODO Auto-generated method stub
//		mapItemToThreshhold = new HashMap<Integer, Integer>();
//		BufferedReader in = new BufferedReader(new FileReader(pathInPut+FileName+"Utility.txt"));
//		String line = null;
//		String[] pair = null;
//		Random generator = new Random();
//		while ( (line = in.readLine())!=null){
//			pair = line.split(":");
//			String[] itemsString = pair[0].split(" ");
//			int[] items = new  int[itemsString.length];
//			for (int i = 0; i < items.length; i++) {
//				items[i] = Integer.parseInt(itemsString[i]);
//				if (mapItemToThreshhold.get(items[i])==null) {
//					mapItemToThreshhold.put(items[i],generator.nextInt(200)+1);
//				}
//				
//			}
//			
//		}
//		in.close();
//		BufferedWriter writer = new BufferedWriter(new FileWriter(pathOutput+FileName+"MMU.txt"));;
//		StringBuffer buffer = new StringBuffer();
//		// append each item from the itemset to the stringbuffer, separated by spaces
//		for (Entry<Integer, Integer> entry : mapItemToThreshhold.entrySet()) {
//			buffer.append(entry.getKey()+","+entry.getValue());
//			buffer.append(System.lineSeparator());
//		}
//		writer.write(buffer.toString());
//		writer.close();
	}
	

}
