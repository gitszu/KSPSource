package reachable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class ReachableQuery {
	
	private int sz_int;  // size of int
	private int sz_ptr;  // depends on System x86 or x64
	private long blk_sz; // 1024*1024*4
	private int sz_vertex; //size of int
	private int vertex_per_blk;// blk_sz / sz_vertex
	
	private int sccN; // sccN number
	
	private int [] dag; // conserve dag file
	private int [] topo; // conserve topo file
	private int [] tlsize; // conserve tlsize file
	private ArrayList<int[]> TL; // conserve TL file		
	
	/**
	 * constructor function 
	 * 
	 * @param sccN   sccN number 
	 * @param index_filename  index of file
	 * @throws IOException
	 */
	public ReachableQuery(int sccN,String index_filename) throws IOException{
		this.sccN = sccN;
		initial();
		initial_query(index_filename);
	}
	
	// initial 
	public void initial(){
		sz_int = Integer.SIZE/8;
		if(System.getProperties().getProperty("os.arch")=="adm64"){
			sz_ptr = 8;
		}else{
			sz_ptr = 4;
		}
		blk_sz = 4194304;  //1024*1024*4 sizeof blk for I/O
		sz_vertex = Integer.SIZE/8;
		vertex_per_blk = (int)(blk_sz / sz_vertex);
	}
	
	/**
	 * load all files 
	 * @param index_filename   index of file.
	 * @throws IOException
	 */
	public void initial_query(String index_filename) throws IOException{
		
		// get files name
		String dag_filename = index_filename+"_dag_label";
		String topo_filename = index_filename+"_topo_label";
		String tlstart_filename = index_filename+"_tlstart";
		String tl_filename = index_filename+"_TL";
		
		// load files
		dag = load(dag_filename);
		topo = load(topo_filename);
		tlsize = load(tlstart_filename);
		TL = load2(tl_filename,tlsize);
		
	}
	
	/**
	 * load file 
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public int[] load(String filename) throws IOException{
		File file= new File(filename);    //filename为 文件目录，请自行设置
		InputStream in= null;
		byte[] bytes= null;
		in = new FileInputStream(file);    //真正要用到的是FileInputStream类的read()方法
		bytes= new byte[in.available()];    //in.available()是得到文件的字节数
		in.read(bytes);    //把文件的字节一个一个地填到bytes数组中
		in.close();    //关闭
		return toInt(bytes); // return integer array
	}
	
	/**
	 * load file 
	 * @param filename  file name
	 * @param tlsize    tlsize Array
	 * @return
	 */
	public ArrayList<int[]> load2(String filename,int [] tlsize){
		 ArrayList<int[]> al = new ArrayList<int[]>();
		 int count = 0;
		 long sum = 0;
	      try {   
	          FileInputStream is = new FileInputStream(filename);  
	         
	            // 设定读取的字节数   
	            int len = vertex_per_blk*4;   
	            byte buffer[] = new byte[vertex_per_blk*4];   
	            int ptr1 = 0;  // row
	            int ptr2 = 0;  // column
	            int ptr_buffer = 0; // buffer 
	            
	            // 读取输入流
	            int num_read = is.read(buffer, 0, len);  
	            int [] buff_int = toInt(buffer);
	            int [] temp =new int[tlsize[ptr1]];

	            while (num_read != -1) {  	            			            		
	            		  		
	            	if(ptr2 == 0){
	            		temp = new int [tlsize[ptr1]];
	            	}
	            	temp[ptr2] = buff_int[ptr_buffer];	            		
	            	ptr2++;
	            	ptr_buffer++;

	            	if(ptr2 == tlsize[ptr1]){
	            		ptr1++;
	            		ptr2 = 0;           		
	            		al.add(temp);						            			
	            		}

	            	//attention: do not use buff_int.length as the condition.
		            if(ptr_buffer == num_read/4){
		            		num_read = is.read(buffer, 0, len);
		            		buff_int = toInt(buffer);
		            		ptr_buffer = 0;
		            	}
		            		
	            }
	            // 关闭输入流   
	            is.close();       
	            } catch (IOException ioe) {   
	            System.out.println(ioe);   
	            } catch (Exception e) {   
	            System.out.println(e);   
	            }
	      return al;
	}
	
	/**
	 * convert byte Array to Integer Array 
	 * @param bRefArr  byte Array
	 * @return
	 */
    public  int[] toInt(byte[] bRefArr) {  
    	
    	int len = bRefArr.length/4;
    	
    	int [] ret = new int[len];
    	
    	byte bLoop; 
    	
    	for(int i = 0; i < len; i++){
    		int begin = i*4;
    		for(int j = 0; j < 4; j++){  			
    			bLoop = bRefArr[begin+j];
    			ret[i] += (bLoop& 0xFF) << (8 * j);
    		}
    	}       
     return ret;  
    }
	
    /**
     * query reachable
     * 
     * @param p source 
     * @param q target
     * @return is Reachable
     */
	public boolean queryReachable(int p, int q,int sccN) {
		boolean isReachable = false;
		
		int i2,j2,size1,size2;
		int[] intArray1,intArray2;
		
		if(p == q){
			return true;
		}
		
		if(dag[p] != dag[q]){
			return false;
		}else{
			if(topo[p] >= topo[q]){
				return false;
			}else{
				
				i2 = j2 = 0;
				intArray1 = TL.get(p);
				intArray2 = TL.get(q+sccN);
				
				size1 = tlsize[p];
				size2 = tlsize[q + sccN];
				
				while((i2 < size1) && (j2 <size2)){
					if(intArray1[i2] < intArray2[j2]){
						++i2;
					}else if(intArray1[i2] > intArray2[j2]){
						++j2;
					}else{
						return true;
					}
				}
				
				if((i2 == size1) || (j2 == size2)){
					return false;
				}				
			}
		}
		
		return isReachable;
	}
	
}
