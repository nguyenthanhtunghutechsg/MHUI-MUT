package miners.MHUI;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MinimumUtility {
	Map<Integer,Long> mapItemToThreshhold;
	public int getBeta() {
		return Beta;
	}

	public void setBeta(int beta) {
		Beta = beta;
	}

	public long getGLMU() {
		return GLMU;
	}

	public void setGLMU(long gLMU) {
		GLMU = gLMU;
	}

	int Beta;
	long GLMU;
	
	public MinimumUtility(String PathTheshHold,int Beta,long GLMU) throws IOException {
		mapItemToThreshhold = new HashMap<Integer, Long>();
		BufferedReader in = new BufferedReader(new FileReader(PathTheshHold));
		String line = null;
		String[] pair = null;
		while ( (line = in.readLine())!=null){
			pair = line.split(",");
			mapItemToThreshhold.put(Integer.parseInt(pair[0].trim()),
				 Math.max(Beta*Long.parseLong(pair[1].trim()),GLMU ));
		}
		in.close();
	}

	public Map<Integer, Long> getMapItemToThreshhold() {
		return mapItemToThreshhold;
	}

	public void setMapItemToThreshhold(Map<Integer, Long> mapItemToThreshhold) {
		this.mapItemToThreshhold = mapItemToThreshhold;
	}
	
	
	
}
