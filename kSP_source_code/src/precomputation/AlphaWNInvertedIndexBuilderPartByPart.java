/**
 * 
 */
package precomputation;

import invertedindexmemory.InvertedIndexWeighted;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import utility.Global;
import utility.Utility;

/**
 * Build the alpha wn inverted index part by part.
 * @author jmshi
 *
 */
public class AlphaWNInvertedIndexBuilderPartByPart {

	public static void main(String[] args) throws Exception {
		if (args.length != 4) {
			throw new Exception("\n Usage: runnable configFile inputAlphaWNfile outputAlphaIindexFile keywordInterval");
		}
		Utility.loadInitialConfig(args[0]);
		String inputDocFile = args[1];
		String outputIindexFile = args[2];
		int interval = Integer.parseInt(args[3]);
		int startKeyword = Global.numNodes;
		int endKeyword = Global.numNodes + Global.numKeywords - 1;

		PrintWriter writer = new PrintWriter(outputIindexFile);
		PrintWriter writerstat = new PrintWriter(outputIindexFile + ".stat.txt");
		int iindexSize = 0;
		int iindexTotalLength = 0;
		writer.println(Global.delimiterPound);// just output #
		long start = System.currentTimeMillis();
		
		InvertedIndexWeighted invertedIndexPartial = new InvertedIndexWeighted();
		while (startKeyword < endKeyword) {
			System.out
					.println("processing keywords [" + startKeyword + "," + (startKeyword + interval) + "]");
			// build partial inverted index
			invertedIndexPartial.clear();
			InvertedIndexWeigthedBuilderPartial(startKeyword,
					startKeyword + interval, inputDocFile, invertedIndexPartial);
			// output partial inverted index
			for (int kid = startKeyword; kid <= startKeyword + interval; kid++) {
				Map<Integer, Float> postinglist = invertedIndexPartial.readPostingListMap(kid);
				if (postinglist == null || postinglist.size() == 0) {
					continue;
				}
				//a new keyword with nonempty posting list.
				iindexSize++;
				iindexTotalLength += postinglist.size();
				writer.print(kid + Global.delimiterSpace);
				for (Entry<Integer, Float> docWeight : postinglist.entrySet()) {
					writer.print(docWeight.getKey() + Global.delimiterSpace + docWeight.getValue()
							+ Global.delimiterSpace);
				}
				writer.println();
				writer.flush();
				postinglist.clear();
			}
			// clear and go to next batch
			invertedIndexPartial.clear();
			startKeyword += interval + 1;
		}
		long end = System.currentTimeMillis();
		System.out.println("Revision Minutes: " + ((end - start) / 1000.0f) / 60.0f);
		
		writer.flush();
		writer.close();
		
		writerstat.println(iindexSize + Global.delimiterPound + iindexTotalLength + Global.delimiterPound);
		writerstat.close();
	}

	private static void InvertedIndexWeigthedBuilderPartial(int startKeyword,
			int endKeyword, String inputDocFile, InvertedIndexWeighted invertedIndex) throws Exception {
		
		// Map<Integer, Set<String>> invertedIndex = new HashMap<Integer,
		// Set<String>>();
		// read nidKeywordListMap line by line to build inverted index
		BufferedReader reader = Utility.getBufferedReader(inputDocFile);
		String line;

		int cntlines = 0;
		String[] idKwordids;
		while ((line = reader.readLine()) != null) {
			cntlines++;
			if (line.contains(Global.delimiterPound)) {
				continue;
			}

//			if (cntlines % 10000 == 0) {
//				System.out.print(cntlines + ",");
//			}

			idKwordids = line.split(Global.delimiterSpace);
			if (idKwordids.length < 3) {
				continue;
			}
			int id = Integer.parseInt(idKwordids[0]);
			for (int i = 1; i < idKwordids.length; i += 2) {
				int kword;
				try {
					kword = Integer.parseInt(idKwordids[i]);
				} catch (Exception e) {
					throw e;
				}
				
				if (kword < startKeyword || kword > endKeyword) {
					continue;
				}
				float weight = Float.parseFloat(idKwordids[i + 1]);
				Map<Integer, Float> postinglist = invertedIndex.readPostingListMap(kword);
				if (postinglist == null) {
					postinglist = new HashMap<Integer, Float>();
					postinglist.put(id, weight);
					invertedIndex.putPostingListMap(kword, postinglist);
				} else {
					postinglist.put(id, weight);
				}
			}
		}
	}
}
