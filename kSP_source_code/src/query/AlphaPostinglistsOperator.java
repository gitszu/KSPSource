package query;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import invertedindex.InvertedIndexHash;
import kSP.candidate.KSPCandidateVisitor;
import spatialindex.spatialindex.IVisitor;
import spatialindex.spatialindex.Point;
import utility.Global;
import utility.Utility;

public class AlphaPostinglistsOperator {

	//Map<Integer, Map<Integer, Float>> alphaPostinglists = new HashMap<Integer, Map<Integer, Float>>();
	public Map<Integer, Map<Integer, Float>> getAlphaPostinglists(String queryfile,InvertedIndexHash alphaIindex) 
			throws NumberFormatException, IOException, Exception{
		Map<Integer, Map<Integer, Float>> alphaPostinglists = new HashMap<Integer, Map<Integer, Float>>();
		
		long start1 = System.currentTimeMillis();
		
		String line;
		BufferedReader queryreader = Utility.getBufferedReader(queryfile);
		ArrayList<Integer> al = new ArrayList<Integer>();	

		while ((line = queryreader.readLine()) != null) {			
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
				throw new Exception("invalid query with no query keyword " + line);
			}
			
			//get the query keywords in int
			Integer[] qwords = new Integer[queryelements.length - 2];
			for (int i = 0; i < qwords.length; i++) {				
				al.add(Integer.parseInt(queryelements[i + 2]));
			}					
		}
		
		long end1 = System.currentTimeMillis();
		long time1 = (end1 - start1);
		System.out.println("time of while loop"+ time1);
		
				
		long start = System.currentTimeMillis();
		for (int i = 0; i < al.size();i++) {
			Map<Integer, Float> alphaPostinglist = alphaIindex.readPostingListMap(al.get(i),true);
			alphaPostinglists.put(al.get(i), alphaPostinglist);
		}
		long end = System.currentTimeMillis();
		long time = (end - start);
		System.out.println("time of read all alhpaPostinglists"+ time);
		
		return alphaPostinglists;
	}
}
