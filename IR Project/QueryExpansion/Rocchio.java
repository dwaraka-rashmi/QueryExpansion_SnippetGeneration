import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public class Rocchio {

	static String file_path = (System.getProperty("user.dir") + File.separator);
	
	static class Posting 
	{
		public String doc_id;
		public int tf; 

		public Posting()
		{
			doc_id = "0";
			tf = 0;
		}
	}
	
	public static LinkedHashMap<String,ArrayList<Posting>> invertedIndex = 
			new LinkedHashMap<String,ArrayList<Posting>>();

	public static LinkedList<String> vocabulary = new LinkedList<String>();
	public static HashSet<String> docColl = new HashSet<String>();
	public static LinkedList<String> documentCollection = new LinkedList<String>();
	
	public static LinkedHashMap<String,LinkedList<String>> queryTerms = new LinkedHashMap<String,LinkedList<String>>();
	public static LinkedHashMap<String,LinkedList<String>> queryTermsExpanded = new LinkedHashMap<String,LinkedList<String>>();
	
	public static LinkedHashMap<String,Double> Ranking = new LinkedHashMap<String,Double>();
	
	public static LinkedHashMap<String,LinkedList<String>> RelevantDocuments = new LinkedHashMap<String,LinkedList<String>>();
	public static LinkedHashMap<String,LinkedList<String>> nonRelevantDocuments = new LinkedHashMap<String,LinkedList<String>>();
	
	public static LinkedHashMap<String,Double> reldijVector = new LinkedHashMap<String,Double> ();
	public static LinkedHashMap<String,Double> nreldijVector = new LinkedHashMap<String,Double> ();
	public static LinkedHashMap<String,Double> qjVector = new LinkedHashMap<String,Double>();
	public static LinkedHashMap<String,Double> qjVectorModified = new LinkedHashMap<String,Double>();
	
	public static LinkedList<String> stopWords = new LinkedList<String>();
	public static Double AvgDocLength;
	public static double n_doc= 0.0;
	
	public static LinkedHashMap<String, Integer> doc_length = new LinkedHashMap<String, Integer>();

	public static void readStopWords() throws IOException {

		FileReader file = new FileReader(file_path + "input\\stoplist.txt");
		BufferedReader in = new BufferedReader(file);
		String inData = " ";

		while ((inData = in.readLine()) != null) {
			stopWords.add(inData.trim());
		}
		in.close();
	}

	public static void readQueries() throws IOException {

		FileReader file = new FileReader(file_path + "input\\query_desc.51-100.short.txt");

		BufferedReader in = new BufferedReader(file);
		String inData = " ";
		String [] valueList;

		String q = "";
		while ((inData = in.readLine()) != null) {
			
			inData = inData.replaceAll("[\\.\\,\\?\\!\\;\\:]","");
			valueList = inData.split(" ");
			q = valueList[0];
			System.out.println("\n"+q);
			queryTerms.put(q, new LinkedList<String>());
			queryTermsExpanded.put(q, new LinkedList<String>());

			LinkedList<String> value = new LinkedList<String>();
			String term = "";
			for(int i = 1; i<valueList.length;i++) {
				term = valueList[i];
				if((!term.equals(""))&&(!stopWords.contains(term))) {
					value.add(term);
				}
			}
			queryTerms.put(q,(value));
			queryTermsExpanded.put(q, value);

		}
		in.close();
		
	}

	public static void readIndex(String index_file) throws IOException {
		
		FileReader file = new FileReader(file_path + index_file);
		BufferedReader in = new BufferedReader(file);
		String inData = " ";
		String [] valueList;
		String term = "";
		String docList = "";
		
		while (( inData = in.readLine()) != null) {
			valueList = inData.split("->"); 
			term = valueList[0];
			vocabulary.add(term);
			System.out.print("\n"+term+" ");
			docList = valueList[1];
			valueList = null; 
			ArrayList<Posting> docMap = new ArrayList<Posting>();

			for(String docValPair : docList.split(" ")) 
			{
				docValPair = docValPair.replaceAll("[()]", "");
				String dv[]=docValPair.split(",");
				Posting pst = new Posting();
				pst.doc_id=dv[0];
				pst.tf=Integer.parseInt(dv[1]);
				docMap.add(pst);
				docColl.add(dv[0]);
				System.out.print(" "+pst.doc_id+" = "+pst.tf);
			}
			invertedIndex.put(term, docMap);	
			
			System.out.println("\ninverted index size "+invertedIndex.size());
			docMap = null;
			docList = null;
		}			
		System.out.println("\ninverted index size "+invertedIndex.size());
		in.close();
	}
	
	public static Boolean contains(ArrayList<Posting> pst_lst , String doc) throws Exception
	{
		Boolean contain = false;
		for(Posting pst : pst_lst)
			contain = ((pst.doc_id.equals(doc)) || contain); 
		
		return contain;
	}

	public static void AverageDocLength() throws Exception
	{
		AvgDocLength = 0.0;
		System.out.print("\n Average doc length calculating ..... ");
		for (Entry<String, ArrayList<Posting>> i_l : (invertedIndex.entrySet()))
		{
			ArrayList<Posting> pst_list = new ArrayList<Posting>();
			pst_list = i_l.getValue();
			for (Posting pst : pst_list)
			{
				if(doc_length.keySet().contains(pst.doc_id))
				{
					int len = doc_length.get(pst.doc_id);
					len = len + pst.tf;
					doc_length.put(pst.doc_id,len);
				}
				else
				{
					doc_length.put(pst.doc_id,pst.tf);
				}
			}
		}
		for (Entry<String, Integer> doc : (doc_length.entrySet()))
		{
			AvgDocLength = AvgDocLength + doc.getValue();
		}
		AvgDocLength = ((double) AvgDocLength / doc_length.size());
		
		System.out.print("\n Average doc length "+AvgDocLength);
		
	}
	

	public static Integer QueryLength(String query) throws Exception {

		return queryTerms.get(query).size();

	}

	public static void readRankingOriginal() throws IOException{

		File folder = new File(file_path + "output\\");

		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles)
			if (file.isFile()) {
				if(file.getName().startsWith("Ranking_original_")) {
					FileReader fileReader = new FileReader(file);
					BufferedReader br = new BufferedReader(fileReader);
					String inData = "";
					LinkedList<String> docList = new LinkedList<String>();
					String q = "";
					while ((inData = br.readLine()) != null) {
						if(inData.matches("\n")) continue;
						if(inData.startsWith("Query")){
							q = inData.split(":")[1].trim();
							RelevantDocuments.put(q, new LinkedList<String>());
							continue;
						}
						if(inData.startsWith("AP890")){
							docList.add(inData.split("=")[0]);
							continue;
						}
					}
					RelevantDocuments.put(q, docList);
					System.out.print("\nRelevantDocuments size "+q+" = "+RelevantDocuments.get(q).size());
					br.close();
				}
			}


		for(String qid : RelevantDocuments.keySet()) {
			nonRelevantDocuments.put(qid, new LinkedList<String>());
			LinkedList<String> docs = new LinkedList<String>();
			for(String doc : documentCollection){
				if(!RelevantDocuments.get(qid).contains(doc)){
					docs.add(doc);
				}
			}
			nonRelevantDocuments.put(qid, docs);
			System.out.print("\nnonRelevantDocuments size "+qid+" = "+nonRelevantDocuments.get(qid).size());
		}

	}

	public static double okapi_tf(String word, String doc) throws Exception {
		for(Posting pst : invertedIndex.get(word))
			if(pst.doc_id.equals(doc))
			{
				return ((double)pst.tf/((double) pst.tf+ 0.5 + ((double) 1.5* ((double)doc_length.get(doc)/AvgDocLength))));
			}
		return 0.0;
	}

	public static Integer docFrequency(String word) throws Exception {

		ArrayList<Posting> posting = invertedIndex.get(word);
		return posting.size();	
	}

	public static void calcRank(String query) throws Exception {
		
		for(String term : vocabulary){
			System.out.println("\n"+term);
			Double tfidf = (double)0;
			Double ntfidf = (double)0;
			int pst_size=invertedIndex.get(term).size();
			double logPart = ((double)(Math.log((double)((double)n_doc)/((double)pst_size))));
			for(String rdoc : RelevantDocuments.get(query)){
				tfidf += ((double)okapi_tf(term,rdoc)*logPart);
			}
			reldijVector.put(term, tfidf);
			System.out.println("\nreldij created");
			for(String nrdoc : nonRelevantDocuments.get(query)){
				ntfidf += ((double)okapi_tf(term,nrdoc)*logPart);
			}
			System.out.println("\nnonreldij created");
			nreldijVector.put(term, ntfidf);
			qjVector.put(term,((double)calcQueryTfIdf(term,query) * logPart));		
		}
		
	}

	public static void calcQjModifiedVector(String qid) throws Exception {
		
		//qjVectorModified
		Double alpha = (double)8;
		Double beta = (double)16;
		Double gamma = (double)4;
		
		for(String term : vocabulary){
			
			double part1 = (double)alpha*qjVector.get(term);
			double relPart = (double)beta*((double)1/RelevantDocuments.get(qid).size());
			double reldij = reldijVector.get(term);
			double part2 = (double)relPart*reldij;
			
			double nrelPart = (double)gamma*((double)1/nonRelevantDocuments.get(qid).size());		
			double nreldij = nreldijVector.get(term);
			double part3 = (double)nrelPart*nreldij;
			
			qjVectorModified.put(term,((double)part1 + part2 - part3));
			
		}
		
	}

	public static Double calcQueryTfIdf(String word, String query) throws Exception {

		double tf = (double)0.0;

		if(queryTerms.get(query).contains(word))
			tf = ((double) Collections.frequency(queryTerms.get(query),word)/(queryTerms.get(query).size()));
		
		return ((double)tf);

	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {

		readStopWords();
		readIndex("output\\index_new.txt");
		
		for(String doc : docColl) {
			documentCollection.add(doc);
		}
		n_doc = documentCollection.size();
		
		readQueries();
		AverageDocLength();
		readRankingOriginal();

		for(Entry e : queryTerms.entrySet())
		{
			String Result = "";
			String query = (String) e.getKey();

			Result += "\nQuery : "+query+"\n";
			System.out.print("\nFor Query"+ query);

			System.out.println("Calculating Rank " + query );
			calcRank(query);
			System.out.println("Calculating Qj Modified Vector " + query);
			calcQjModifiedVector(query);	
			System.out.println("Calculated Qj Modified Vector " + query);
			
			System.out.println("\nQjvectorModified");
				
			List list = new LinkedList(qjVectorModified.entrySet());

			Collections.sort(list, new Comparator() {
				public int compare(Object o1, Object o2) {
					return ((Comparable) ((Map.Entry) (o1)).getValue())
							.compareTo(((Map.Entry) (o2)).getValue());
				}	
			});

			Collections.reverse(list);

			LinkedHashMap sortedPR = new LinkedHashMap();
			for (Iterator i = list.iterator(); i.hasNext();) {
				Map.Entry entry = (Map.Entry) i.next();
				sortedPR.put(entry.getKey(), entry.getValue());
			} 

			Set sortedPRSet = sortedPR.entrySet();
			Iterator iterator = sortedPRSet.iterator();
			int i = 0;
			
			String Top50 = "";
			while(iterator.hasNext()) {
				Map.Entry me = (Map.Entry)iterator.next();
				String term = (String) me.getKey();
				Double val = (Double) me.getValue();
				if(val>0 && !queryTerms.get(query).contains(term)){
					queryTermsExpanded.get(query).add(term);
					System.out.println("\n"+term+" = "+val);
					Top50 += "\n"+term+" = "+val;
					
				}
				if(i==50)
					break;
				i++;
			}
			
			System.out.print(Top50);
			File ResultFile = new File(file_path+"\\output\\","Expanded_" + query + ".out");

			BufferedWriter out = new BufferedWriter(new FileWriter(ResultFile));

			out.append(Top50);
			out.close();

		}

		System.out.print("\nexpanded terms");
		for(Entry<String, LinkedList<String>> e : queryTermsExpanded.entrySet()) {
			System.out.print("\n"+e.getKey()+" | ");
			for(String qterms : e.getValue()){
				System.out.print(qterms+" ");
			}
		}
		System.out.print("\n Vector created");
	}
}
