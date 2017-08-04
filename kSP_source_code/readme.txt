Steps to build all the indexes:
1. Disk-resident R-tree (Disk version is used for preprocessing):  spatialindex.rtree.RTree.main(), e.g., 
    java -Xmx18000m -jar jar/rtree.jar your/path/to/configfile
2. Build the Inverted index of the documents of all the RDF graph vertices (We split this index building into two steps, mainly due to the constraint of memory):
    (i) Build the inverted index that is stored in plain text: precomputation.InvertedIndexPlainTextBuilder.main(), e.g., 
        java -Xmx18000m -jar jar/InvertedIndexPlainTextBuilder.jar your/path/to/configfile input/path/to/documentFileOfRDFVertices output/path/to/invertedIndexPlainTextfile n
    (ii) Store the plain text inverted index on disk for fast lookup:  invertedindex.InvertedIndexHash.main(), e.g., 
        java -Xmx18000m -jar jar/InvertedIndexHash.jar your/path/to/configfile  buffersize pagesize iidxNameFlag n input/path/to/invertedIndexPlainTextfile
3. Compute the Strong Connected Components of RDF-graph for unqualified place pruning: precomputation.graph.StrongConnectedComponentComp.main(), e.g., 
    java -Xmx18000m -jar jar/StrongConnectedComponentComp.jar your/path/to/configfile
4. Data preparation for TF-label algorithm that prunes unqualified places: precomputation.graph.TFlabelDataFormatter.main(), e.g., 
    java -Xmx18000m -jar jar/TFlabelDataFormatter.jar your/path/to/configfile
5. Build the index for TF-label algorithm as instructed by TF-label source code (Also attached in the library folder for your ease.)
6. Compute the Alpha Word Neighbourhoods (WN) of all the places and R-tree nodes: precomputation.alpha.AlphaWNPrecomputation.main(), e.g., 
    java -Xmx18000m -jar jar/AlphaWNPrecomputation your/path/to/configfile alphaRadius input/path/to/documentFileOfRDFVertices
7. Build the Alpha WN inverted index stored in plain text file (built part by part due to memory constraint): precomputation.AlphaWNInvertedIndexBuilderPartByPart.main(), e.g.,
    java -Xmx18000m -jar AlphaWNInvertedIndexBuilderPartByPart.jar your/path/to/configfile input/path/to/alphaWNfile output/path/to/alphaWNInvertedIndexPlainTextFile 500000
8. Store the plain text Alpha WN inverted index on disk for fast lookup: invertedindex.InvertedIndexHash.main(), e.g., 
    java -Xmx18000m -jar jar/InvertedIndexHash.jar your/path/to/configfile alphaBuffersize alphaPagesize alphaIidxNameFlag y input/path/to/alphaWNInvertedIndexPlainTextFile

Steps to process queries:
1. BSP: query.BSP.main(), e.g., 
    java -Xmx16000m -jar jar/BSP.jar your/path/to/configfile
2. SPP: query.SPP.main(), e.g., 
    java -Xmx16000m -Djava.library.path=your/path/to/tf-label-library -jar jar/SPP.jar your/path/to/configfile alphaRadius alphaPageSize
3. SP: query.SP.main(), e.g., 
    java -Xmx16000m -Djava.library.path=your/path/to/tf-label-library -jar jar/SP.jar your/path/to/configfile alphaRadius alphaPageSize

Notes:
1. There are some predefined input and output directory structure for the programs. They may report exceptions if you do not have such directory structure created. Please feel free to custom your own directory structure. I believe it is easy to fix.