/**
 * 
 */
package precomputation.graph;

import java.util.Map;
import java.util.Set;

import utility.Global;
import utility.TFlabelUtility;
import utility.Utility;

/**
 * @author jmshi 
 * 
 */
public class TFlabelDataFormatter {
	public static void main(String[] args) throws Exception {

		if (args.length != 1) {
			throw new Exception("Usage: runnable configFile");
		}
		Utility.loadInitialConfig(args[0]);
		// output
		String DAGedgeFile = Global.outputDirectoryPath + Global.dagFile + Global.sccFlag
				+ Global.keywordFlag + Global.edgeFile + Global.dataVersion;
		
		// input scc file
		String sccFile = Global.outputDirectoryPath + Global.edgeFile + Global.sccFlag + Global.dataVersion;
		Map<Integer, Integer> vertexSCCMap = TFlabelUtility.loadVertexSCCMap(sccFile);

		// input edge file and converted it to DAG with scc as vertex
		String edgeFile = Global.inputDirectoryPath + Global.edgeFile + Global.dataVersion;
		Map<Integer, Set<Integer>> DAGedges = TFlabelUtility.convertToDAG(edgeFile, vertexSCCMap);

		// input: documents of vertices. Augment each keyword as a vertex into the DAG graph
		long start = System.currentTimeMillis();
		String nidDocFile = Global.inputDirectoryPath + Global.nidKeywordsListMapFile + Global.dataVersion;
		TFlabelUtility.augmentKeywordsToDAG(nidDocFile, vertexSCCMap, DAGedges);
		long end = System.currentTimeMillis();
		System.out.println("Revision Minutes: " + ((end - start) / 1000.0f) / 60.0f);
		
		Utility<Integer, Integer> uti = new Utility<Integer, Integer>();
		uti.outputMapOfSetsTFLabelFormat(DAGedgeFile, DAGedges, (Global.numNodes + Global.numKeywords));
	}
}
