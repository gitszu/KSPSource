/**
 * 
 */
package query;

import invertedindex.InvertedIndexHash;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import neustore.base.LRUBuffer;
import kSP.kSP;
import kSP.candidate.KSPCandidateVisitor;
import multiThreadTask.SPMultipleThreadTask;
import multiThreadTask.SPMultipleThreadTask2;
import multiThreadTask.SimpleThreadpool;
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
 * Main class invoking SP algorithm
 * @author jmshi
 * 
 * 多线程的KSP算法，在搜索semantic tree时，也使用多线程。
 * 
 *
 */
public class MTSP3 {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		if (args.length != 6) {
			throw new Exception("Usage: runnable configFile alphaRadius alphaPageSize poolsize maxPoolSize keepAlivetime");
		}
		//Global configurations are provided by file args[0]
		Utility.loadInitialConfig(args[0]);	
		
		//alpha radius Word Neighborhood (WN) of a place/node
		double alphaRadius = Double.parseDouble(args[1]);
		//alpha radius WN is organized by inverted index with the alphaPageSize
		int alphaPageSize = Integer.parseInt(args[2]);
		int poolsize= Integer.parseInt(args[3]);
		int maxPoolSize = Integer.parseInt(args[4]);
		long keepAlivetime = Integer.parseInt(args[5]);
	
		//buffer for alpha WN inverted index 
		LRUBuffer buffer = new LRUBuffer(Global.alphaIindexRTNodeBufferSize, alphaPageSize);
		
		//alpha WN inverted index 
		/*
		 * Places are identified by their id in alpha WN inverted index
		 * R-tree nodes are identified by (-id-1) in alpha WN inverted index
		 */
		String alphaIindexFile = Global.outputDirectoryPath + Global.alphaIindexFile + "." + Global.rtreeFanout
						+ "." + alphaRadius + Global.diskFlag + alphaPageSize + Global.dataVersion;
		
		InvertedIndexHash alphaIindex = new InvertedIndexHash(buffer, alphaIindexFile + ".iindex", false,
				alphaPageSize, Global.alphaIindexRTNodeBufferSize);
		
		//the file stores the String Connected Components (SCCs) of RDF graph. 
		//Used for reachable test (Unqualified place pruning)
		String vertexSCCFile = Global.outputDirectoryPath + Global.edgeFile + Global.sccFlag
				+ Global.dataVersion;
		Map<Integer, Integer> vertexSCCMap = TFlabelUtility.loadVertexSCCMap(vertexSCCFile);
//		String reachableIndexFile = Global.tfindexDirectoryPath + Global.dagFile 
//				+ Global.keywordFlag + Global.edgeFile + Global.dataVersionWithoutExtension + Global.sccFlag;	
		
		String reachableIndexFile = Global.tfindexDirectoryPath + Global.dataVersionWithoutExtension + Global.sccFlag;	
		//System.out.println(reachableIndexFile);

		//load the C++ TFlabel library for unqualified place pruning (applicable in Linux)
		//System.loadLibrary("ReachableQuery");
		//The tester for unqualified place pruning
		//ReachableQuery reachableTester = new ReachableQuery();
		//reachableTester.initQuery(Global.numSCCs, reachableIndexFile);

		ReachableQuery reachableTester = new ReachableQuery(Global.numSCCs, reachableIndexFile);
		
		//the data index structure of RDF data with R-tree, RDF Graph, and Inverted index of keywords
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
		//String querySettingFile = "/home/yuanshuai/YagoLib/query/QS_Yago_All_3.txt";
		System.out.println("read query file :"+ querySettingFile);
		BufferedReader qsettingReader = Utility.getBufferedReader(querySettingFile);
		String lineSetting;
		while ((lineSetting = qsettingReader.readLine()) != null) {
			if (lineSetting.contains(Global.delimiterPound)) {
				/*if the lineSetting contains pound, it means it is commented out*/
				continue;
			}
			String[] qsetting = lineSetting.split(Global.delimiterSpace);
			int k = Integer.parseInt(qsetting[0]);
			String queryfile = qsetting[1];
                          
			// Construct the result and runtime output files.
			String resultfile = Global.outputDirectoryPath
					+ queryfile.substring(queryfile.lastIndexOf('/') + 1, queryfile.indexOf(".txt"))
					+ "_resultSPPRPlaceAlpha_" + k + "_" + alphaRadius + Global.dataVersion;
			String runtimefile = Global.outputDirectoryPath
					+ queryfile.substring(queryfile.lastIndexOf('/') + 1, queryfile.indexOf(".txt"))
					+ "_runtimeSPPRPlaceAlpha_" + k + "_" + alphaRadius + Global.dataVersion;

			PrintWriter rewriter = new PrintWriter(resultfile);
			PrintWriter runwriter = new PrintWriter(runtimefile);

			/*each line contains a query*/
			String line;
			BufferedReader queryreader = Utility.getBufferedReader(queryfile);
			int cntline = 0;
			
			/*得到多有关键字对应的PostList*/
			System.out.println("开始读取所有关键字位置信息");
			long start_inital = System.currentTimeMillis();
			AlphaPostinglistsOperator apo = new AlphaPostinglistsOperator();
			Map<Integer, Map<Integer, Float>> allAlphaPostinglists = apo.getAlphaPostinglists(queryfile, alphaIindex);
			long end_inital = System.currentTimeMillis();
			long time_inital = (end_inital - start_inital);
			System.out.println("读取所有的关键字位置信息需要的时间："+time_inital);
			
			//queryreader.lines().count();
			
		    ThreadPoolExecutor executor = new ThreadPoolExecutor(poolsize, maxPoolSize, keepAlivetime, TimeUnit.MILLISECONDS,
		    		new LinkedBlockingQueue<Runnable>());
		    
		  //  SimpleThreadpool executor = SimpleThreadpool.getInstance(2);
		    		    		    
		    long startotal = System.currentTimeMillis();		    
			while ((line = queryreader.readLine()) != null) {
				
				//the alpha-WN posting lists of query keywords for the alpha-WN precomputed index
				Map<Integer, Map<Integer, Float>> alphaPostinglists = new HashMap<Integer, Map<Integer, Float>>();
				//the posting lists of query keywords for the RDF data
				Map<Integer, ArrayList> postinglists = new HashMap<Integer, ArrayList>();
				
				cntline++;
				if (line.contains(Global.delimiterPound)) {
					//if the line contains pound, the query is commented out
					continue;
				}
				
				//initialize the statistic variables
				for (int i = 0; i < Global.count.length; i++) {
					Global.count[i] = 0;
					Global.runtime[i] = 0;
				}
				String[] queryelements = line.split(Global.delimiterSpace);
				if (queryelements.length <= 2) {
					rewriter.close();
					runwriter.close();
					throw new Exception("invalid query with no query keyword " + line);
				}
								
				/*get the query coordinates*/
				double[] qcoords = { Double.parseDouble(queryelements[0]),
						Double.parseDouble(queryelements[1]) };
				Point qpoint = new Point(qcoords);

				//get the query keywords in int
				Integer[] qwords = new Integer[queryelements.length - 2];
				for (int i = 0; i < qwords.length; i++) {
					qwords[i] = Integer.parseInt(queryelements[i + 2]);
				}
				
				int leastFrequency = Integer.MAX_VALUE;
				int leastFrequentQword = -1;
				for (int i = 0; i < qwords.length; i++) {
					ArrayList postinglist = rgi.readPostingList(qwords[i]);
					postinglists.put(qwords[i], postinglist);

					if (postinglist.size() < leastFrequency) {
						leastFrequentQword = qwords[i];
					}
				}
				
				// load the alpha-WN posting lists of query keywords
				long start1 = System.currentTimeMillis();
				for (int i = 0; i < qwords.length; i++) {				
					Map<Integer, Float> alphaPostinglist = allAlphaPostinglists.get(qwords[i]);
					alphaPostinglists.put(qwords[i], alphaPostinglist);
				}
				
				VertexQwordsMap<Integer> vertexQwordsMap = new VertexQwordsMap<Integer>(qwords, postinglists, false);
				IVisitor v = new KSPCandidateVisitor(k);
				
				//ThreadPoolExecutor subExecutor = new ThreadPoolExecutor(3, 3, 200, TimeUnit.MILLISECONDS,new ArrayBlockingQueue<Runnable>(3));
				
				SPMultipleThreadTask2 task = new SPMultipleThreadTask2(rgi, vertexQwordsMap,alphaPostinglists,alphaRadius, reachableTester,
			    											vertexSCCMap, k, qpoint, qwords,postinglists, v,leastFrequentQword);					
				executor.execute(task);			
			}
//			executor.stop();
//			executor.awaitTermination();
			executor.shutdown();
			
			while(true){
				if(executor.isTerminated()){
					break;
				}
			}
			long endtotal = System.currentTimeMillis();	
			System.out.println("总的时间：" + ( endtotal- startotal));
			
			System.out.println("主线程池执行完毕");
			rewriter.close();
			runwriter.close();
		}
		//reachableTester.freeQuery(Global.numSCCs);
		inputParam.close();
	}

}
