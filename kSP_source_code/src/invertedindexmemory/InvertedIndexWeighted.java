/**
 * 
 */
package invertedindexmemory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jmshi
 *
 */
public class InvertedIndexWeighted {

	private Map<Integer, Map<Integer, Float>> invertedIndex = null;

	public InvertedIndexWeighted() {
		invertedIndex = new HashMap<Integer, Map<Integer, Float>>();
	}

	public int size() {
		return invertedIndex.size();
	}

	public Map<Integer, Float> readPostingListMap(int keyword) {
		return this.invertedIndex.get(keyword);
	}
	public void putPostingListMap(int keyword, Map<Integer, Float> postinglist) {
		this.invertedIndex.put(keyword, postinglist);
	}
	
	public Map<Integer, Map<Integer, Float>> get(){
		return this.invertedIndex;
	}
	
	public void clear(){
		this.invertedIndex.clear();
	}
}
