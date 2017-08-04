package multiThreadTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.DoubleAccumulator;

import kSP.candidate.KSPCandidate;
import kSP.candidate.KSPCandidateVisitor;
import mygraph.GraphAttributes;
import mygraph.GraphByArray;
import queryindex.VertexQwordsMap;
import rdfindex.memory.RTreeWithGI;
import spatialindex.rtree.Data;
import spatialindex.rtree.NNEntry;
import spatialindex.spatialindex.IVisitor;
import utility.Global;

/**
 * SP 算法, SemanticPlace多线程任务
 * 
 * @author YS
 *
 */
public class SemanticPlaceMT2 implements Runnable{
	
	private Data placeData;
	private Integer[] qwords;
	private double loosenessThreshold ;
	private VertexQwordsMap<Integer> vertexQwordsMap;
	private List<List<Integer>> semanticTree;
	private RTreeWithGI rgi;
	private IVisitor result;
	private int k;
	private DoubleAccumulator kthScore;
	private double m_minDist;
	private String uuid;
	
	public SemanticPlaceMT2(Data placeData,Integer[] qwords,double loosenessThreshold,VertexQwordsMap<Integer> vertexQwordsMap
			,List<List<Integer>> semanticTree,RTreeWithGI rgi,IVisitor result,int k ,DoubleAccumulator kthScore,double m_minDist,String uuid){
		this.placeData = placeData;
		this.qwords = qwords;
		this.loosenessThreshold = loosenessThreshold;
		this.vertexQwordsMap = vertexQwordsMap;
		this.semanticTree = semanticTree;
		this.rgi = rgi;
		this.result = result;
		this.k = k;
		this.kthScore = kthScore;
		this.m_minDist = m_minDist;
		this.uuid = uuid;
	}

	@Override
	public void run() {
		long start_w_semantic = System.currentTimeMillis();
		// TODO Auto-generated method stub
		double looseness = 0.0;
		try {		
			long start_w_r_l = System.currentTimeMillis();
			looseness = rgi.getGraph().getSemanticPlaceP3(placeData.getIdentifier(),
			qwords, vertexQwordsMap, semanticTree,m_minDist,placeData.getWeight(),kthScore);
			long end_w_r_l = System.currentTimeMillis();
			//System.out.println(uuid+":      1--->2--->3--->4  kSPComputation  --->  while ---> semantic --> looseness time: "+(end_w_r_l-start_w_r_l));
			//System.out.println("节点: "+placeData.getIdentifier()+" looseness:"+looseness+" 的结束时间： "+new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss:SSS").format(new Date()));
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		try {
			if (looseness < 1) {
			throw new Exception("semantic score " + looseness + " < 1, for place"
					+ placeData.getIdentifier());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			if (looseness != Double.POSITIVE_INFINITY) {				
				synchronized(SemanticPlaceMT2.class){					
					  Global.count[2]++;// number of valid place candidate
					double rankingScore = placeData.getWeight() * looseness;
					KSPCandidate candidate = new KSPCandidate(new NNEntry(placeData, rankingScore),semanticTree);
					((KSPCandidateVisitor) result).addPlaceCandidate(candidate);
					double score = ((KSPCandidateVisitor) result).size() >= k ? ((KSPCandidateVisitor) result).getWorstRankingScore() : Double.POSITIVE_INFINITY; 
					kthScore.accumulate(score);
					//System.out.println("节点: "+placeData.getIdentifier()+" 的结束时间： "+new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss:SSS").format(new Date()));
				}				
				Global.endTime = System.currentTimeMillis();						
			}
			
			long end_w_semantic = System.currentTimeMillis();
			//System.out.println(uuid+":      1--->2--->3  kSPComputation  --->  while ---> semantic time: "+(end_w_semantic-start_w_semantic));
}

}
