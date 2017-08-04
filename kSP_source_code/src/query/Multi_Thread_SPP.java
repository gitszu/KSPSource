/**
 * 
 */
package query;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import kSP.kSPP;
import kSP.candidate.KSPCandidateVisitor;
import mygraph.GraphAttributes;
import queryindex.VertexQwordsMap;
import rdfindex.memory.RTreeWithGI;
import reachable.ReachableQuery;
import spatialindex.spatialindex.IVisitor;
import spatialindex.spatialindex.Point;
import utility.Global;
import utility.RGIUtility;
import utility.TFlabelUtility;
import utility.Utility;

/**
 * Main class invoking SPP algorithm
 * @author jmshi
 *
 */
public class Multi_Thread_SPP {
	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		if (args.length != 1) {
			throw new Exception("Usage: runnable configFile");
		}
		
		Utility.loadInitialConfig(args[0]);
		RTreeWithGI rgi = RGIUtility.buildRGI();
		// the file containing SCCs of RDF graph, used for unqualified place pruning
		String vertexSCCFile = Global.outputDirectoryPath + Global.edgeFile + Global.sccFlag
				+ Global.dataVersion;

		Map<Integer, Integer> vertexSCCMap = TFlabelUtility.loadVertexSCCMap(vertexSCCFile);

		// the file of the pre-built TF-label reachable index  
//		String reachableIndexFile = Global.tfindexDirectoryPath + Global.dagFile + Global.sccFlag
//				+ Global.keywordFlag + Global.edgeFile + Global.dataVersionWithoutExtension;
		String reachableIndexFile = Global.tfindexDirectoryPath + Global.dataVersionWithoutExtension + Global.sccFlag;	
		System.out.println(reachableIndexFile);

//		System.out.println(reachableIndexFile);
//		System.out.println(Global.numSCCs);

		/**
		 * TF-label source code is in C++. It is available online.
		 * We wrap its C++ code as a library using JNI, therefore, it can be called in Java under linux os.
		 * There are plenty tutorials online about how to do this.
		 */
		//System.loadLibrary("ReachableQuery");
		//ReachableQuery reachableTester = new ReachableQuery();
		//reachableTester.initQuery(Global.numSCCs, reachableIndexFile);
		
		ReachableQuery reachableTester = new ReachableQuery(Global.numSCCs, reachableIndexFile);

		/**
		 * There are two nested while loops to read and process queries in different query settings in one batch.
		 * The outer loop loads your query setting file, each line in which is a query setting in format:
		 * k queryfile
		 * , where queryfile is the file containing all the queries to be run under the query setting.
		 * 
		 * The inner loop loads and process each query in queryfile.
		 * Each line of queryfile is a query in format:
		 * lat lng qword1 qword2 ...
		 */
		System.out.println("please input query setting file path");
		Scanner inputParam = new Scanner(System.in);
		String querySettingFile = inputParam.nextLine();
		BufferedReader qsettingReader = Utility.getBufferedReader(querySettingFile);
		String lineSetting;
		while ((lineSetting = qsettingReader.readLine()) != null) {
			if (lineSetting.contains(Global.delimiterPound)) {
				continue;
			}
			String[] qsetting = lineSetting.split(Global.delimiterSpace);
			int k = Integer.parseInt(qsetting[0]);
			String queryfile = qsetting[1];

			String resultfile = Global.outputDirectoryPath
					+ queryfile.substring(queryfile.lastIndexOf('/') + 1, queryfile.indexOf(".txt"))
					+ "_resultSPP_" + k + "_" + Global.dataVersion;
			String runtimefile = Global.outputDirectoryPath
					+ queryfile.substring(queryfile.lastIndexOf('/') + 1, queryfile.indexOf(".txt"))
					+ "_runtimeSPP_" + k + "_" + Global.dataVersion;

			PrintWriter rewriter = new PrintWriter(resultfile);
			PrintWriter runwriter = new PrintWriter(runtimefile);

			
			/*built thread pool*/
			int poolsize = 4;
			int maxPoolSize = 12;
			long keepAlivetime = 200;
		    ThreadPoolExecutor executor = new ThreadPoolExecutor(poolsize, maxPoolSize, keepAlivetime, TimeUnit.MILLISECONDS,
		    		new LinkedBlockingQueue<Runnable>());
		    Map<Integer, String> concurrentHashMap = new ConcurrentHashMap<Integer, String>();
			
			
			String line;
			BufferedReader queryreader = Utility.getBufferedReader(queryfile);
			int cntline = 0;
			while ((line = queryreader.readLine()) != null) {
				cntline++;
				if (line.contains(Global.delimiterPound)) {
					continue;
				}
				for (int i = 0; i < Global.count.length; i++) {
					Global.count[i] = 0;
					Global.runtime[i] = 0;
				}
				String[] queryelements = line.split(Global.delimiterSpace);
				if (queryelements.length <= 2) {
					// by default, there is at least one query keyword.
					throw new Exception("invalid query with no query keyword " + line);
				}
				
				double[] qcoords = { Double.parseDouble(queryelements[0]), Double.parseDouble(queryelements[1]) };
				Point qpoint = new Point(qcoords);

				Integer[] qwords = new Integer[queryelements.length - 2];
				for (int i = 0; i < qwords.length; i++) {
					qwords[i] = Integer.parseInt(queryelements[i + 2]);
				}

				Map<Integer, ArrayList> postinglists = new HashMap<Integer, ArrayList>();

				/*
				 * For unqualified place pruning, based on the observation that infrequent query keywords have a high chance to make a place unqualified, 
				 * we prioritize them when issuing reachability queries.
				 * Furthermore, the least Frequent query keyword is powerful enough for pruning.
				 * 
				 * Here get the query keyword with the least frequency for unqualified place pruning.
				 * */
				int leastFrequency = Integer.MAX_VALUE;
				for (int i = 0; i < qwords.length; i++) {
					ArrayList postinglist = rgi.readPostingList(qwords[i]);
					postinglists.put(qwords[i], postinglist);
					if (postinglist.size() < leastFrequency) {
						Global.leastFrequentQword = qwords[i];
					}
				}

				VertexQwordsMap<Integer> vertexQwordsMap = new VertexQwordsMap<Integer>(qwords, postinglists, false);

				IVisitor v = new KSPCandidateVisitor(k);
				
				
				/*multiply thread */			
				SPP_Task task = new SPP_Task(k, qpoint, qwords, postinglists, v, rgi, vertexQwordsMap,Global.numNodes,reachableTester, vertexSCCMap);
				Future<String> r = executor.submit(task);
				concurrentHashMap.put(cntline, r.get());
			}
			executor.shutdown();
		    int m = 0;
		    while(m < concurrentHashMap.size()){
		    	rewriter.println(concurrentHashMap.get(m));
		    	m++;
		    }
		   rewriter.close();
		}
		inputParam.close();
	}
}
