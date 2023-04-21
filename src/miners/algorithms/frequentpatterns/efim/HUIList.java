package miners.algorithms.frequentpatterns.efim;

import java.util.ArrayList;

/*
 * High Utility Itemsets List class
 * Coded by Trinh Nguyen (dzutrinh@gmail.com), 2017
 */
public class HUIList {
	
	private ArrayList<Itemset>	chuis;
	
	// constructor
	HUIList() {
		this.chuis = new ArrayList<Itemset>();
	}

	// returns number of CHUIs
	public int size() {
		return this.chuis.size();
	}
	
	// add a new CHUI
	public void add(Itemset item) {
		this.chuis.add(item);
	}
	
	// add a collection of new CHUIs
	public void addAll(HUIList items) {
		this.chuis.addAll(items.getCHUIs());
	}
	
	// returns the list of all CHUIs found
	public ArrayList<Itemset> getCHUIs() {
		return this.chuis;
	}
	
	// return the CHUI at a given index
	public Itemset get(int i) {
		return this.chuis.get(i);
	}
	
	// for displaying to console or writing into file
	public String toString() {
		String	result = "";
		
		for (int i = 0; i < this.chuis.size(); i++) {
			Itemset item = this.chuis.get(i);
			result += (i+1) + ": " + item.toString() + "#UTIL: " + 
					  (long) (item.utility) + 
					  "\n";
		}
		return result;
	}
}
