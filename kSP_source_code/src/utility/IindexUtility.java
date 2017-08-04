/**
 * 
 */
package utility;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import invertedindex.InvertedIndexHash;
import neustore.base.LRUBuffer;

/**
 * @author jieming
 *
 */
public class IindexUtility {

	public static void InvertedIndexWeigthedBuilder(String inputDocFile, String outputIindexFile)
			throws Exception {
		Map<Integer, Set<String>> invertedIndex = new HashMap<Integer, Set<String>>();
		
		String[][] memoryTest = new String[Global.numNodes][];
		BufferedReader reader = Utility.getBufferedReader(inputDocFile);
		String line;

//		int cntlines = 0;
		while ((line = reader.readLine()) != null) {
//			cntlines++;
			if (line.contains(Global.delimiterPound)) {
				continue;
			}

//			if (cntlines % 10 == 0) {
//				System.out.println(cntlines + ",");
//			}

			String[] idKwordids = line.split(Global.delimiterSpace);
			int id = Integer.parseInt(idKwordids[0]);
			memoryTest[id] = idKwordids;
		}
		outputInvertedIndexWeigthed(invertedIndex, outputIindexFile);
	}

	private static void outputInvertedIndexWeigthed(Map<Integer, Set<String>> invertedIndex, String outputFile)
			throws Exception {

		PrintWriter writer = new PrintWriter(outputFile);

		int totalCount = 0;

		for (Entry<Integer, Set<String>> entry : invertedIndex.entrySet()) {
			totalCount += entry.getValue().size();
		}

		writer.println(invertedIndex.size() + Global.delimiterPound + totalCount + Global.delimiterPound);

		for (int kwordidx = 0; kwordidx < Global.numKeywords; kwordidx++) {
			int kid = kwordidx + Global.numNodes;
			Set<String> idsOfKid = invertedIndex.get(kid);

			if (idsOfKid == null || idsOfKid.isEmpty()) {
				continue;
			}
			String output = "" + kid + Global.delimiterSpace;
			for (String idWeight : idsOfKid) {
				output += idWeight + Global.delimiterSpace;
			}
			writer.println(output);
		}
		writer.close();
	}

	public static void InvertedIndexBuilder(String inputDocFile, String outputIindexFile) throws Exception {
		Map<Integer, Set<Integer>> invertedIndex = new HashMap<Integer, Set<Integer>>();
		// Map<Integer, Set<String>> invertedIndex = new HashMap<Integer,
		// Set<String>>();
		// read nidKeywordListMap line by line to build inverted index
		BufferedReader reader = Utility.getBufferedReader(inputDocFile);
		String line;

//		int cntlines = 0;
		while ((line = reader.readLine()) != null) {
//			cntlines++;
			if (line.contains(Global.delimiterPound)) {
				continue;
			}

//			if (cntlines % 10 == 0) {
//				System.out.println(cntlines + ",");
//			}

			String[] idWords = line.split(Global.delimiterLevel1);
			int id = Integer.parseInt(idWords[0]);
			String[] words = idWords[1].split(Global.delimiterLevel2);
			for (int j = 0; j < words.length; j++) {
				int word = Integer.parseInt(words[j]);
				Set<Integer> postinglist = invertedIndex.get(word);
				if (postinglist == null) {
					postinglist = new HashSet<Integer>();
					postinglist.add(id);
					invertedIndex.put(word, postinglist);
				}
				else
				{
					postinglist.add(id);
				}
			}
		}

		outputInvertedIndex(invertedIndex, outputIindexFile);
	}

	private static void outputInvertedIndex(Map<Integer, Set<Integer>> invertedIndex, String outputFile)
			throws Exception {

		PrintWriter writer = new PrintWriter(outputFile);

		int totalCount = 0;

		for (Entry<Integer, Set<Integer>> entry : invertedIndex.entrySet()) {
			totalCount += entry.getValue().size();
		}

		writer.println(invertedIndex.size() + Global.delimiterPound + totalCount + Global.delimiterPound);

		for (int kwordidx = 0; kwordidx < Global.numKeywords; kwordidx++) {
			int kid = kwordidx + Global.numNodes;
			Set<Integer> postinglist = invertedIndex.get(kid);
			if (postinglist==null) {
//				System.out.print(kid + " is not contained in any vertex");
				continue;
			}
			writer.print(kid + Global.delimiterLevel1);
			for (Integer id : postinglist) {
				writer.print(id + Global.delimiterLevel2);
			}
			writer.println();
		}
		writer.close();
	}
}
