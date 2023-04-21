package miners.algorithms.frequentpatterns.efim;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
//import java.util.concurrent.LinkedBlockingQueue;
//import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import miners.tools.MemoryLogger;

// ==================================
// pdEFIM ALGORITHM, PARALLEL VERSION
// ==================================
public class pMHUI_MUT {

	// HUIList huis; // the set of high-utility itemsets
	int patternCount; // the number of high-utility itemsets found (for statistics)
	int newItemCount; // the number of new items
	int coreCount; // total processors
	boolean multithread = true; // parallel execution flag

	long startTimestamp; // the start time and end time of the last algorithm execution
	long endTimestamp;

	long transactionReadingCount; // number of times a transaction was read
	long mergeCount; // number of merges
	long candidateCount; // number of itemsets from the search tree that were considered

	final boolean DEBUG = false; // Set to TRUE to show some debugging information, for good sake
	final int MAXIMUM_SIZE_MERGING = 1000; // A parameter for transaction merging

	int[] oldNameToNewNames; // an array that map an old item name to its new name
	int[] newNamesToOldNames; // an array that map a new item name to its old name
	int[] utilityBinArraySU; // utility bin array for sub-tree utility
	int[] utilityBinArrayLU; // utility bin array for local utility
	Map<Integer, Long> mapItemToThreshhold;

	// === [ PARALLEL SEARCH ]
	// ====================================================================================================
	class ParallelSearch implements Callable<ParallelSearch> {

		int[] arrSU; // utility bin array for sub-tree utility
		int[] arrLU; // utility bin array for local utility
		int[] nameMapping;
		int[] temp;

		List<Transaction> dataset;
		ArrayList<Integer> itemsToKeep;
		ArrayList<Integer> itemsToExplore;
		HUIList huiList; // list of found HUIs in the search space

		long patternCount; // number of found HUIs in the search space
		long mergeCount; // number of transaction merged
		long transactionReadingCount; // number of times a transaction was read
		long candidateCount; // number of itemsets from the search tree that were considered
		int newItemCount;

		public ParallelSearch(List<Transaction> trans, // Data set
				ArrayList<Integer> itemsToKeep, // itemsToKeep = Secondary set
				Integer itemToExplore, // itemToExplore = Primary item
				int itemCount, int[] arrLU, // local utility
				int[] arrSU, // sub-tree utility
				int[] nameMapping) {
			// allocations
			this.dataset = trans;
			this.itemsToExplore = new ArrayList<Integer>();
			this.itemsToExplore.add(itemToExplore);
			this.itemsToKeep = itemsToKeep;
			this.temp = new int[500];
			this.newItemCount = itemCount;

			// copies
			this.nameMapping = nameMapping; // please copy array before giving to a thread
			this.arrLU = arrLU;
			this.arrSU = arrSU;

			// output
			 this.huiList = new HUIList();

			// statistics
			this.patternCount = 0;
		}

		@Override
		public ParallelSearch call() throws Exception {
			this.dfsSearch(0, this.dataset, this.itemsToKeep, this.itemsToExplore, 0);
			return this;
		}

		private void dfsSearch(long MMU, List<Transaction> transactionsOfP, List<Integer> itemsToKeep,
				List<Integer> itemsToExplore, int prefixLength) throws IOException {

			// update the number of candidates explored so far
			candidateCount += itemsToExplore.size();

			// ======== for each frequent item e =============
			for (int j = 0; j < itemsToExplore.size(); j++) {
				Integer e = itemsToExplore.get(j);
				long valueMIUofPe = MMU;
				if (prefixLength == 0) {
					valueMIUofPe = mapItemToThreshhold.get(newNamesToOldNames[e]);
				} else {
					if (valueMIUofPe > mapItemToThreshhold.get(newNamesToOldNames[e])) {
						valueMIUofPe = mapItemToThreshhold.get(newNamesToOldNames[e]);
					}
				}
				// ========== PERFORM INTERSECTION =====================
				// Calculate transactions containing P U {e}
				// At the same time project transactions to keep what appears after "e"
				List<Transaction> transactionsPe = new ArrayList<Transaction>();

				// variable to calculate the utility of P U {e}
				int utilityPe = 0;

				// For merging transactions, we will keep track of the last transaction read
				// and the number of identical consecutive transactions
				Transaction previousTransaction = null;
				int consecutiveMergeCount = 0;

				// For each transaction
				for (Transaction transaction : transactionsOfP) {
					// Increase the number of transaction read
					transactionReadingCount++;

					// we remember the position where e appears.
					// we will call this position an "offset"
					int positionE = -1;
					// Variables low and high for binary search
					int low = transaction.offset;
					int high = transaction.items.length - 1;

					// perform binary search to find e in the transaction
					while (high >= low) {
						int middle = (low + high) >>> 1; // divide by 2
						if (transaction.items[middle] < e) {
							low = middle + 1;
						} else if (transaction.items[middle] == e) {
							positionE = middle;
							break;
						} else {
							high = middle - 1;
						}
					}

					// if 'e' was found in the transaction
					if (positionE > -1) {

						// optimization: if the 'e' is the last one in this transaction,
						// we don't keep the transaction
						if (transaction.getLastPosition() == positionE) {
							// but we still update the sum of the utility of P U {e}
							utilityPe += transaction.utilities[positionE] + transaction.prefixUtility;
						} else {
							// otherwise
							if (MAXIMUM_SIZE_MERGING >= (transaction.items.length - positionE)) {
								// we cut the transaction starting from position 'e'
								Transaction projectedTransaction = new Transaction(transaction, positionE);
								utilityPe += projectedTransaction.prefixUtility;

								// if it is the first transaction that we read
								if (previousTransaction == null) {
									// we keep the transaction in memory
									previousTransaction = projectedTransaction;
								} else if (isEqualTo(projectedTransaction, previousTransaction)) {
									// If it is not the first transaction of the database and
									// if the transaction is equal to the previously read transaction,
									// we will merge the transaction with the previous one

									// increase the number of consecutive transactions merged
									mergeCount++;

									// if the first consecutive merge
									if (consecutiveMergeCount == 0) {
										// copy items and their profit from the previous transaction
										int itemsCount = previousTransaction.items.length - previousTransaction.offset;
										int[] items = new int[itemsCount];
										System.arraycopy(previousTransaction.items, previousTransaction.offset, items,
												0, itemsCount);
										int[] utilities = new int[itemsCount];
										System.arraycopy(previousTransaction.utilities, previousTransaction.offset,
												utilities, 0, itemsCount);

										// make the sum of utilities from the previous transaction
										int positionPrevious = 0;
										int positionProjection = projectedTransaction.offset;
										while (positionPrevious < itemsCount) {
											utilities[positionPrevious] += projectedTransaction.utilities[positionProjection];
											positionPrevious++;
											positionProjection++;
										}

										// make the sum of prefix utilities
										int sumUtilities = previousTransaction.prefixUtility += projectedTransaction.prefixUtility;

										// create the new transaction replacing the two merged transactions
										previousTransaction = new Transaction(items, utilities,
												previousTransaction.transactionUtility
														+ projectedTransaction.transactionUtility);
										previousTransaction.prefixUtility = sumUtilities;

									} else {
										// if not the first consecutive merge

										// add the utilities in the projected transaction to the previously
										// merged transaction
										int positionPrevious = 0;
										int positionProjected = projectedTransaction.offset;
										int itemsCount = previousTransaction.items.length;
										while (positionPrevious < itemsCount) {
											previousTransaction.utilities[positionPrevious] += projectedTransaction.utilities[positionProjected];
											positionPrevious++;
											positionProjected++;
										}

										// make also the sum of transaction utility and prefix utility
										previousTransaction.transactionUtility += projectedTransaction.transactionUtility;
										previousTransaction.prefixUtility += projectedTransaction.prefixUtility;
									}
									// increment the number of consecutive transaction merged
									consecutiveMergeCount++;
								} else {
									// if the transaction is not equal to the preceding transaction
									// we cannot merge it so we just add it to the database
									transactionsPe.add(previousTransaction);
									// the transaction becomes the previous transaction
									previousTransaction = projectedTransaction;
									// and we reset the number of consecutive transactions merged
									consecutiveMergeCount = 0;
								}
							} else {
								// Otherwise, if merging has been deactivated
								// then we just create the projected transaction
								Transaction projectedTransaction = new Transaction(transaction, positionE);
								// we add the utility of Pe in that transaction to the total utility of Pe
								utilityPe += projectedTransaction.prefixUtility;
								// we put the projected transaction in the projected database of Pe
								transactionsPe.add(projectedTransaction);
							}
						}
						// This is an optimization for binary search:
						// we remember the position of E so that for the next item, we will not search
						// before "e" in the transaction since items are visited in lexicographical
						// order
						transaction.offset = positionE;
					} else {
						// This is an optimization for binary search:
						// we remember the position of E so that for the next item, we will not search
						// before "e" in the transaction since items are visited in lexicographical
						// order
						transaction.offset = low;
					}
				}

				// Add the last read transaction to the database if there is one
				if (previousTransaction != null) {
					transactionsPe.add(previousTransaction);
				}

				// Append item "e" to P to obtain P U {e}
				// but at the same time translate from new name of "e" to its old name
				temp[prefixLength] = this.nameMapping[e];

				// if the utility of PU{e} is enough to be a high utility itemset
				if (utilityPe >= valueMIUofPe) {
					// output PU{e}
					output(prefixLength, utilityPe);
				}

				// ==== Next, we will calculate the Local Utility and Sub-tree utility of
				// all items that could be appended to PU{e} ====
				calculateUpperBounds(transactionsPe, j, itemsToKeep);

				// We will create the new list of secondary items
				List<Integer> newItemsToKeep = new ArrayList<Integer>();
				// We will create the new list of primary items
				List<Integer> newItemsToExplore = new ArrayList<Integer>();

				// for each item
				long valueMinUtility = valueMIUofPe;
				for (int k = itemsToKeep.size() - 1; k > j; k--) {

					Integer itemk = itemsToKeep.get(k);
					if (valueMinUtility > mapItemToThreshhold.get(newNamesToOldNames[itemk])) {
						valueMinUtility = mapItemToThreshhold.get(newNamesToOldNames[itemk]);
					}
					// if the sub-tree utility is no less than min util
					if (this.arrSU[itemk] >= valueMinUtility) {
						// and if sub-tree utility pruning is activated
						newItemsToExplore.add(0, itemk);
						// consider that item as a secondary item
						newItemsToKeep.add(0, itemk);
					} else if (this.arrLU[itemk] >= valueMinUtility) {
						newItemsToKeep.add(0, itemk);
					}
				}

				// === recursive call to explore larger itemsets
				dfsSearch(valueMIUofPe, transactionsPe, newItemsToKeep, newItemsToExplore, prefixLength + 1);
			}
			MemoryLogger.getInstance().checkMemory();
		}

		private void calculateUpperBounds(List<Transaction> transactionsPe, int j, List<Integer> itemsToKeep) {

			// For each promising item > e according to the total order
			for (int i = j + 1; i < itemsToKeep.size(); i++) {
				Integer item = itemsToKeep.get(i);
				// We reset the utility bins of that item for computing the sub-tree utility and
				// local utility
				this.arrSU[item] = 0;
				this.arrLU[item] = 0;
			}

			int sumRemainingUtility;
			// for each transaction
			for (Transaction transaction : transactionsPe) {
				// count the number of transactions read
				transactionReadingCount++;

				// We reset the sum of reamining utility to 0;
				sumRemainingUtility = 0;
				// we set high to the last promising item for doing the binary search
				int high = itemsToKeep.size() - 1;

				// for each item in the transaction that is greater than i when reading the
				// transaction backward
				// Note: >= is correct here. It should not be >.
				for (int i = transaction.getItems().length - 1; i >= transaction.offset; i--) {
					// get the item
					int item = transaction.getItems()[i];

					// We will check if this item is promising using a binary search over promising
					// items.

					// This variable will be used as a flag to indicate that we found the item or
					// not using the binary search
					boolean contains = false;
					// we set "low" for the binary search to the first promising item position
					int low = 0;

					// do the binary search
					while (high >= low) {
						int middle = (low + high) >>> 1; // divide by 2
						int itemMiddle = itemsToKeep.get(middle);
						if (itemMiddle == item) {
							// if we found the item, then we stop
							contains = true;
							break;
						} else if (itemMiddle < item) {
							low = middle + 1;
						} else {
							high = middle - 1;
						}
					}
					// if the item is promising
					if (contains) {
						// We add the utility of this item to the sum of remaining utility
						sumRemainingUtility += transaction.getUtilities()[i];
						// We update the sub-tree utility of that item in its utility-bin
						this.arrSU[item] += sumRemainingUtility + transaction.prefixUtility;
						// We update the local utility of that item in its utility-bin
						this.arrLU[item] += transaction.transactionUtility + transaction.prefixUtility;
					}
				}
			}
		}

		private boolean isEqualTo(Transaction t1, Transaction t2) {
			int length1 = t1.items.length - t1.offset; // we first compare the transaction lengths
			int length2 = t2.items.length - t2.offset;
			if (length1 != length2) { // if not same length, then transactions are not identical
				return false;
			}

			// if same length, we need to compare each element position by position, to see
			// if they are the same
			int position1 = t1.offset;
			int position2 = t2.offset;
			while (position1 < t1.items.length) { // for each position in the first transaction
				// if different from corresponding position in transaction 2 return false
				// because they are not identical
				if (t1.items[position1] != t2.items[position2]) {
					return false;
				}
				position1++; // if the same, then move to next position
				position2++;
			}
			return true; // if all items are identical, then return to true
		}

		private void output(int tempPosition, int utility) {
			patternCount++; // atm, we only record the number of HUIs found

//			int[] copy = new int[tempPosition + 1];
//			System.arraycopy(temp, 0, copy, 0, tempPosition + 1);
//			Itemset items = new Itemset(copy, utility);
//			this.huiList.add(items);
			//System.out.println(items.toString());

		}

	};
	// === [ END OF PARALLEL SEARCH ]
	// ====================================================================================================

	// CONSTRUCTOR
	public pMHUI_MUT() {
		candidateCount = 0;
	}

	// MAIN ALGORITHM INVOKER
	public HUIList runAlgorithm(String inputPath, String threshHoldPath, int maximumTransactionCount, int Beta,
			long GLMU, int corenumber) throws IOException {

		int dbTranMiningLen = 0; // size of database after remove transaction

		// INITIALIZATION PHASE
		this.candidateCount = 0;
		this.mergeCount = 0; // counters reset
		this.transactionReadingCount = 0;
		this.patternCount = 0; // reset the number of itemset found
		// this.huis = new HUIList();

		Dataset dataset = new Dataset(inputPath, maximumTransactionCount); // read the input file
		MinimumUtility minimumUtility = new MinimumUtility(threshHoldPath, Beta, GLMU); // save minUtil value selected
																						// by the user
		mapItemToThreshhold = minimumUtility.getMapItemToThreshhold();
		startTimestamp = System.currentTimeMillis(); // record the start time
		MemoryLogger.getInstance().reset();

		// PRE-COMPUTE PHASE
		System.out.println("Parallel          : " + (this.multithread ? "Yes" : "No"));
		if (this.multithread) {
			// obtains number of available processors
			Runtime runtime = Runtime.getRuntime();
			this.coreCount = runtime.availableProcessors();
			System.out.println("Number of threads : " + this.coreCount);
		}

		calculateLocalUtilityFirstTime(dataset);
		List<Integer> AllItemList = new ArrayList<Integer>();
		for (int j = 1; j < utilityBinArrayLU.length; j++) {
			if (utilityBinArrayLU[j] > 0) {
				AllItemList.add(j);
			}

		}
		insertionSort(AllItemList, utilityBinArrayLU);
		Map<Integer, Long> mapItemWithSMUofItem = new HashMap<Integer, Long>();
		for (int i = 0; i < AllItemList.size(); i++) {
			int MinItem = AllItemList.get(i);
			Long MinSMU = mapItemToThreshhold.get(MinItem);
			for (int j = i + 1; j < AllItemList.size(); j++) {
				int currentItem = AllItemList.get(j);
				long currentSMU = mapItemToThreshhold.get(currentItem);
				if (MinSMU > currentSMU) {
					MinSMU = currentSMU;
				}
			}
			mapItemWithSMUofItem.put(MinItem, MinSMU);
		}
		ArrayList<Integer> itemsToKeep = new ArrayList<Integer>(); // Only keep the promising items (those having TWU >=
																	// minutil)
		for (int j = 0; j < AllItemList.size(); j++) {
			int item = AllItemList.get(j);
			Long MinU = mapItemWithSMUofItem.get(item);
			if (MinU != null) {
				if (utilityBinArrayLU[item] >= MinU) {
					itemsToKeep.add(item);
				}
			}

		}

		// Rename promising items according to the increasing order of TWU. This will
		// allow very fast comparison between items later by the algorithm
		oldNameToNewNames = new int[dataset.getMaxItem() + 1]; // This structure will store the new name corresponding
																// to each old name
		newNamesToOldNames = new int[dataset.getMaxItem() + 1]; // This structure will store the old name corresponding
																// to each new name
		int currentName = 1; // We will now give the new names starting from the name "1"
		for (int j = 0; j < itemsToKeep.size(); j++) { // For each item in increasing order of TWU
			int item = itemsToKeep.get(j); // get the item old name
			oldNameToNewNames[item] = currentName; // give it the new name
			newNamesToOldNames[currentName] = item; // remember its old name
			itemsToKeep.set(j, currentName); // replace its old name by the new name in the list of promising items
			currentName++; // increment by one
		}

		newItemCount = itemsToKeep.size(); // remember the number of promising item
		int dataSize = dataset.getTransactions().size();
		for (int i = 0; i < dataSize /* dataset.getTransactions().size() */; i++) { // We now loop over each transaction
																					// from the dataset to remove
																					// unpromising items
			Transaction transaction = dataset.getTransactions().get(i); // Get the transaction
			// Remove unpromising items from the transaction and at the same time, rename
			// the items in the transaction according to their new names
			// and sort the transaction by increasing TWU order
			transaction.removeUnpromisingItems(oldNameToNewNames);
		}

		// Now we will sort transactions in the database according to the proposed total
		// order on transaction
		// (the lexicographical order when transactions are read backward)
		Collections.sort(dataset.getTransactions(), new Comparator<Transaction>() { // Sort the dataset using a new
																					// comparator
			@Override
			public int compare(Transaction t1, Transaction t2) {
				// we will compare the two transaction items by items starting from the last
				// items.
				int pos1 = t1.items.length - 1;
				int pos2 = t2.items.length - 1;

				// if the first transaction is smaller than the second one
				if (t1.items.length < t2.items.length) {
					while (pos1 >= 0) { // while the current position in the first transaction is >0
						int subtraction = t2.items[pos2] - t1.items[pos1];
						if (subtraction != 0) {
							return subtraction;
						}
						pos1--;
						pos2--;
					}
					return -1; // if they ware the same, they we compare based on length
					// else if the second transaction is smaller than the first one
				} else if (t1.items.length > t2.items.length) {
					while (pos2 >= 0) { // while the current position in the second transaction is >0
						int subtraction = t2.items[pos2] - t1.items[pos1];
						if (subtraction != 0) {
							return subtraction;
						}
						pos1--;
						pos2--;
					}
					return 1; // if they ware the same, they we compare based on length
				} else {
					while (pos2 >= 0) { // else if both transactions have the same size
						int subtraction = t2.items[pos2] - t1.items[pos1];
						if (subtraction != 0) {
							return subtraction;
						}
						pos1--;
						pos2--;
					}
					return 0; // if they ware the same, they we compare based on length
				}
			}
		});

		// =======================REMOVE EMPTY TRANSACTIONS==========================
		// After removing unpromising items, it may be possible that some transactions
		// are empty. We will now remove these transactions from the database.
		int emptyTransactionCount = 0;
		dataSize = dataset.getTransactions().size();
		for (int i = 0; i < dataSize; i++) { // for each transaction
			Transaction transaction = dataset.getTransactions().get(i); // if the transaction length is 0, increase the
																		// number of empty transactions
			if (transaction.items.length == 0) {
				emptyTransactionCount++;
			}
		}

		// To remove empty transactions, we just ignore the first transactions from the
		// dataset
		// The reason is that empty transactions are always at the begining of the
		// dataset since transactions are sorted by size
		dataset.transactions = dataset.transactions.subList(emptyTransactionCount,
				/* dataset.transactions.size() */dataSize);
		dbTranMiningLen = dataset.getTransactions().size(); // initialize the utility-bin array for counting the subtree
															// utility

		if (dbTranMiningLen != 0) {
			this.utilityBinArraySU = new int[newItemCount + 1];
			calculateSubtreeUtilityFirstTime(dataset);

			List<Integer> itemsToExplore = new ArrayList<Integer>(); // Calculate the set of items that pass the
																		// sub-tree utility pruning condition
			for (Integer item : itemsToKeep) { // for each item
				if (utilityBinArraySU[item] >= mapItemWithSMUofItem.get(newNamesToOldNames[item])) { // if the subtree
																										// utility is
																										// higher or
																										// equal to
																										// minutil, then
					// keep it
					itemsToExplore.add(item);
				}
			}

			System.out.println("Explore size: " + itemsToExplore.size());
			System.out.println("Keep size: " + itemsToKeep.size());

			// creates a thread pool limited at the number of available processor cores
			// CORE COUNT
			ExecutorService executor = Executors.newFixedThreadPool(corenumber);
			List<Future<ParallelSearch>> list = new ArrayList<Future<ParallelSearch>>();

			// initializes a list of objects for paralleling first level in search tree
			for (int idx = 0; idx < itemsToExplore.size(); idx++) {
				Integer item = itemsToExplore.get(idx);
				ParallelSearch obj = new ParallelSearch(dataset.getTransactions(), itemsToKeep, item, this.newItemCount,
						utilityBinArrayLU.clone(), utilityBinArraySU.clone(), newNamesToOldNames);
				Future<ParallelSearch> future = executor.submit(obj);
				list.add(future);
			}

			// waiting for all threads to terminate
			executor.shutdown();
			try {
				if (!executor.awaitTermination(500, TimeUnit.SECONDS)) {
					executor.shutdownNow();
				}
			} catch (InterruptedException ex) {
				executor.shutdownNow();
				// Thread.currentThread().interrupt();
			}

			endTimestamp = System.currentTimeMillis(); // record the end time

			// collect data
			for (Future<ParallelSearch> fut : list) {
				try {
					collectData(fut.get());
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
		}

		// CLEAN-UP PHASE
		MemoryLogger.getInstance().checkMemory();
		
		// printPeakHeapUsage();
		return null; // return the set of high-utility itemsets
	}

	// GLOBAL FUNC
	public static void insertionSort(List<Integer> items, int[] utilityBinArrayTWU) {
		for (int j = 1; j < items.size(); j++) {
			Integer itemJ = items.get(j);
			int i = j - 1;
			Integer itemI = items.get(i);

			int comparison = utilityBinArrayTWU[itemI] - utilityBinArrayTWU[itemJ];
			// if the twu is equal, we use the lexicographical order to decide whether i is
			// greater than j or not.
			if (comparison == 0) {
				comparison = itemI - itemJ;
			}

			while (comparison > 0) {
				items.set(i + 1, itemI);
				i--;
				if (i < 0) {
					break;
				}
				itemI = items.get(i);
				comparison = utilityBinArrayTWU[itemI] - utilityBinArrayTWU[itemJ];
				// if the twu is equal, we use the lexicographical order to decide whether i is
				// greater than j or not.
				if (comparison == 0) {
					comparison = itemI - itemJ;
				}
			}
			items.set(i + 1, itemJ);
		}
	}

	// GLOBAL FUNC
	public void calculateLocalUtilityFirstTime(Dataset dataset) {

		// Initialize utility bins for all items
		utilityBinArrayLU = new int[dataset.getMaxItem() + 1];

		// Scan the database to fill the utility bins
		// For each transaction
		for (Transaction transaction : dataset.getTransactions()) {
			// for each item
			for (Integer item : transaction.getItems()) {
				// we add the transaction utility to the utility bin of the item
				utilityBinArrayLU[item] += transaction.transactionUtility;
			}
		}
	}

	public void calculateSubtreeUtilityFirstTime(Dataset dataset) {
		int sumSU;
		// Scan the database to fill the utility-bins of each item
		// For each transaction
		for (Transaction transaction : dataset.getTransactions()) {
			// We will scan the transaction backward. Thus,
			// the current sub-tree utility in that transaction is zero
			// for the last item of the transaction.
			sumSU = 0;

			// For each item when reading the transaction backward
			for (int i = transaction.getItems().length - 1; i >= 0; i--) {
				// get the item
				Integer item = transaction.getItems()[i];

				// we add the utility of the current item to its sub-tree utility
				sumSU += transaction.getUtilities()[i];
				// we add the current sub-tree utility to the utility-bin of the item
				utilityBinArraySU[item] += sumSU;
			}
		}
	}

	public void collectData(ParallelSearch obj) {
		if (obj != null) {
			// collect statistics
			this.patternCount += obj.patternCount;
			this.mergeCount += obj.mergeCount;
			this.candidateCount += obj.candidateCount;
			this.transactionReadingCount += obj.transactionReadingCount;
			// append the HUI lists found in each thread into the global one
			// this.huis.addAll(obj.huiList);
		}
	}

	public static void printPeakHeapUsage() {
		try {
			List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
			// we print the result in the console
			double total = 0;
			for (MemoryPoolMXBean memoryPoolMXBean : pools) {
				if (memoryPoolMXBean.getType() == MemoryType.HEAP) {
					long peakUsed = memoryPoolMXBean.getPeakUsage().getUsed();
					// System.out.println(String.format("Peak used for: %s is %.2f",
					// memoryPoolMXBean.getName(), (double)peakUsed/1024/1024));
					total = total + peakUsed;
				}
			}
			System.out.println(String.format("Total heap peak used: %f MB", total / 1024 / 1024));

		} catch (Throwable t) {
			System.err.println("Exception in agent: " + t);
		}
	}

	// GLOBAL
	public void printStats() {
		System.out.println("================ [pEFIM STATISTICS] ================");
		System.out.println(" High utility itemsets count: " + patternCount + " itemsets");
		System.out.println("                Total time ~: " + (endTimestamp - startTimestamp) + " ms");
		// System.out.println(" Transaction merge count ~: " + mergeCount + "
		// transactions");
		System.out.println("    Transaction read count ~: " + transactionReadingCount + " transactions");
		System.out.println(" Max memory:" + MemoryLogger.getInstance().getMaxMemory());
		System.out.println(" Candidate count: " + candidateCount + " itemsets");
		System.out.println("=====================================================");
	}

}
