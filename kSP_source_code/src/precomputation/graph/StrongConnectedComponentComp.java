/**
 * 
 */
package precomputation.graph;


import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.StrongConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;

import utility.Global;
import utility.GraphUtility;
import utility.Utility;

/**
 * Compute the Strong Connected Components in RDF graphs by utilizing jgrapht library.
 * @author jmshi
 *
 */
public class StrongConnectedComponentComp {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			throw new Exception("Usage: runnable configFile");
		}
		Utility.loadInitialConfig(args[0]);
		String edgeFile = Global.inputDirectoryPath + Global.edgeFile + Global.dataVersion;
		long start = System.currentTimeMillis();
		DirectedGraph<Integer, DefaultEdge> graph = GraphUtility.buildSimpleDirectedGraph(edgeFile);
		StrongConnectivityInspector<Integer, DefaultEdge> scc = 
				new StrongConnectivityInspector<Integer, DefaultEdge>(graph);
		List<Set<Integer>> sccs = scc.stronglyConnectedSets();
		long end = System.currentTimeMillis();
		System.out.println("Revision Minutes: " + ((end - start) / 1000.0f) / 60.0f);
		Utility<Integer, Integer> uti = new Utility<Integer, Integer>();
		String outputFile = Global.outputDirectoryPath + Global.edgeFile + ".SCC." + Global.dataVersion;
		uti.outputListOfSets(outputFile, sccs);
	}

}
