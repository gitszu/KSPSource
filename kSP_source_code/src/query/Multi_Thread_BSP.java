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

import kSP.candidate.KSPCandidateVisitor;
import queryindex.VertexQwordsMap;
import rdfindex.memory.RTreeWithGI;
import spatialindex.spatialindex.IVisitor;
import spatialindex.spatialindex.Point;
import utility.Global;
import utility.RGIUtility;
import utility.Utility;

public class Multi_Thread_BSP {
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
				/*if the lineSetting contains pound, it means it is commented out*/
				continue;
			}
			// A query setting is split by a single space.
			String[] qsetting = lineSetting.split(Global.delimiterSpace);
			int k = Integer.parseInt(qsetting[0]);
			// the file contains all the queries, each of which occupies one line.
			String queryfile = qsetting[1];
			// Construct the result and runtime output files.
			String resultfile = Global.outputDirectoryPath
					+ queryfile.substring(queryfile.lastIndexOf('/') + 1, queryfile.indexOf(".txt"))
					+ "_resultSPB_" + k + Global.dataVersion;
			String runtimefile = Global.outputDirectoryPath
					+ queryfile.substring(queryfile.lastIndexOf('/') + 1, queryfile.indexOf(".txt"))
					+ "_runtimeSPB_" + k + Global.dataVersion;

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
					// If a line of query contains pound symbol, it is as commented out.
					continue;
				}
				for (int i = 0; i < Global.count.length; i++) {
					Global.count[i] = 0;
					Global.runtime[i] = 0;
				}
				// A line is a query in format: "lat lon keyword1 keyword2 ... keywordn"
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

				// read the posting lists of each query keywords into postinglists
				Map<Integer, ArrayList> postinglists = new HashMap<Integer, ArrayList>();
				for (int i = 0; i < qwords.length; i++) {
					ArrayList postinglist = rgi.readPostingList(qwords[i]);
					postinglists.put(qwords[i], postinglist);
				}
			
				// VertexQwordsMap is the M_{q.\psi}, a map structure where keys are the vertices in these posting lists of query keywords
				VertexQwordsMap<Integer> vertexQwordsMap = new VertexQwordsMap<Integer>(qwords, postinglists, false);

				IVisitor v = new KSPCandidateVisitor(k);

				/**/
				BSP_Task task = new BSP_Task(k, qpoint, qwords, postinglists, v, rgi, vertexQwordsMap,Global.numNodes);
				Future<String> r = executor.submit(task);
				concurrentHashMap.put(cntline, r.get());
				
				//ATTENTION: MUST reset graph after each query
				//rgi.getGraph().reset();
			}
			executor.shutdown();
		    int m = 1;
		    while(m <= concurrentHashMap.size()){
		    	rewriter.println(concurrentHashMap.get(m));
		    	m++;
		    }
			rewriter.close();
		}
		inputParam.close();
	}
}
