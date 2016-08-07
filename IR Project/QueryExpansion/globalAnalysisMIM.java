
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;


public class globalAnalysisMIM {

	static String file_path = (System.getProperty("user.dir") + File.separator);
	public static LinkedHashMap<String,LinkedList<String>> queryTerms = new LinkedHashMap<String,LinkedList<String>>();
	public static LinkedHashMap<String,Set<String>> queryTermsExpandedDices = new LinkedHashMap<String,Set<String>>();
	public static LinkedHashMap<String,Set<String>> queryTermsExpandedMIM = new LinkedHashMap<String,Set<String>>();
	public static LinkedList<String> stopWords = new LinkedList<String>();
	public static Double AvgDocLength;
	public static LinkedHashMap<String,LinkedList<String>> stemClass = new LinkedHashMap<String,LinkedList<String>>();
	public static LinkedHashMap<String,LinkedHashMap<String,Integer>> invertedIndex = 
			new LinkedHashMap<String,LinkedHashMap<String,Integer>>();
	public static LinkedHashMap<String,LinkedHashMap<String,Double>> dicesCoefficient = 
			new LinkedHashMap<String,LinkedHashMap<String,Double>>();
	public static LinkedHashMap<String,LinkedHashMap<String,Double>> MIM = 
			new LinkedHashMap<String,LinkedHashMap<String,Double>>();
	public static HashSet<String> queryOriginal = new HashSet<String>();
	public static Set<String> uniqueterms = new HashSet<String>();
	public static LinkedList<String> vocabulary = new LinkedList<String>();
	public static LinkedList<String> documentCollection = new LinkedList<String>();
	public static LinkedHashMap<String,Double> Ranking = new LinkedHashMap<String,Double>();

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {

		readStopWords();
		readStemWords();
		readQueries();
		readIndex();
		calcMIM();
		AverageDocLength();
		Double Rank = 0.0;

	}

	public static void readStopWords() throws IOException {

		FileReader file = new FileReader(file_path + "input\\stoplist.txt");
		BufferedReader in = new BufferedReader(file);
		String inData = " ";

		while ((inData = in.readLine()) != null) {
			stopWords.add(inData.trim());
		}
		in.close();
	}

	public static void readStemWords() throws IOException {

		FileReader file = new FileReader(file_path + "input\\stem-classes.lst");
		BufferedReader in = new BufferedReader(file);
		String inData = " ";
		String[] valueList;
		String stem;

		while ((inData = in.readLine()) != null) {
			inData = inData.replace("|","=");
			valueList = inData.split("=");
			stem = valueList[0].trim();
			LinkedList<String> stemWords = new LinkedList<String>();
			for(String stemWord : valueList[1].split(" ")) {
				if(!stemWord.matches(""))
					stemWords.add(stemWord);
			}
			stemClass.put(stem,stemWords);
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
			System.out.print("\n"+q+" | ");
			queryTerms.put(q, new LinkedList<String>());
			Set<String> value = new HashSet<String>();
			String term = "";
			for(int i = 1; i<valueList.length;i++) {
				term = valueList[i].toLowerCase();
				if((!term.equals(""))&&(!stopWords.contains(term))) {
					value.add(term);
					queryOriginal.add(term);	
				}
			}
			LinkedList<String> qTerms = new LinkedList<String>(value);
			queryTerms.put(q,qTerms);
			queryTermsExpandedDices.put(q, value);
			queryTermsExpandedMIM.put(q, value);
			
		}
		in.close();
	}

	public static void readIndex() throws IOException {

		
		FileReader file = new FileReader(file_path + "output\\index_ap89.out");
		
		BufferedReader in = new BufferedReader(file);
		String inData = " ";
		String [] valueList;
		String term = "";
		String docList = "";
		HashSet<String> docColl = new HashSet<String>();
		while (( inData = in.readLine()) != null) {
			valueList = inData.split("->"); 
			term = valueList[0];
			System.out.print("\n"+term+" ");
			docList = valueList[1];
			invertedIndex.put(term,new LinkedHashMap<String,Integer>());
			LinkedHashMap<String,Integer> docMap = new LinkedHashMap<String,Integer>();
			String [] doc;
		
			doc = docList.split(" ");
			for(String docValPair : doc) {
				docValPair = docValPair.replace("(", "");
				docValPair = docValPair.replace(")", "");
				docMap.put(docValPair.split(",")[0],Integer.parseInt(docValPair.split(",")[1]));
				docColl.add(docValPair.split(",")[0]);
				System.out.print(" "+docValPair.split(",")[0]+" = "+docValPair.split(",")[1]);
			}
			invertedIndex.put(term, docMap);
		}

		for(String doc : docColl) {
			documentCollection.add(doc);
		}

		System.out.println("\ninverted index size "+invertedIndex.size());
		in.close();

	}

	public static Integer docLength(String doc) {

		Integer length = 0;
		for (Entry<String, LinkedHashMap<String, Integer>> pos : invertedIndex.entrySet()) {
			LinkedHashMap<String, Integer> posting = pos.getValue();
			for(Entry<String, Integer> e : posting.entrySet())
			{
				if(doc.contentEquals(e.getKey())) {
					length += e.getValue();
				}
			}
		}
		return length;
	}

	public static void AverageDocLength()
	{
		Integer TotalDocLength = 0;

		for(String doc : documentCollection) {
			TotalDocLength += docLength(doc);
		}
		AvgDocLength = (double) (((double)TotalDocLength)/(double)documentCollection.size());
		System.out.print("\n Average doc length "+AvgDocLength);

	}

	public static double calcPart1(String word){

		Double D = (double)documentCollection.size();
		Double logNumerator = (double)D+0.5;
		Double Dfw = (double)invertedIndex.get(word).size();
		Double logDenominator = (double)Dfw+0.5;
		
		return ((double)(Math.log(logNumerator/logDenominator)));

	}

	public static double calcPart2(String word,String doc){

		Double k1 = (double)1.2;
		Double b = (double)0.75;
		Double lenDAvgD = (double)docLength(doc)/AvgDocLength;
		LinkedHashMap<String, Integer> posting = invertedIndex.get(word);
		Double tf = (double)posting.get(doc);
		Double numerator = ((double)tf+k1*tf);
		Double denominator = ((double)tf+(k1*((1-b)+(b*lenDAvgD))));
		
		return (double)numerator/denominator;

	}

	public static double calcPart3(String query,String word){

		Double tf = (double)Collections.frequency(queryTerms.get(query),word);
		Double k2 = (double)100;
		Double numerator = (double)tf+(k2*tf);
		Double denominator = (double)tf+k2;
		
		return (double)numerator/denominator;

	}

	public static int findnab(String indexkey,String qterm) {

		LinkedList<String> indexTermDoclist = new LinkedList(invertedIndex.get(indexkey).keySet());
		LinkedList<String> qTermDoclist = new LinkedList(invertedIndex.get(qterm).keySet());

		int n = 0;
		for(String indexDocs : indexTermDoclist) {
			if(qTermDoclist.contains(indexDocs)){
				n++;
			}
		}
		return n;

	}

	@SuppressWarnings("unchecked")
	public static void calcMIM() throws IOException {

		for(Entry<String, LinkedList<String>> qEntry : queryTerms.entrySet()) {
			String qid = qEntry.getKey();
		
			System.out.println("query "+qid);
			LinkedList<String> qTerms = new LinkedList<String>(queryTerms.get(qid));

			for(String qt : qTerms){
				LinkedHashMap<String,Double> m = new LinkedHashMap<String,Double>();

				if(invertedIndex.keySet().contains(qt)) {
					int na = invertedIndex.get(qt).size();
					for(Entry e : invertedIndex.entrySet()) {
						String b = (String) e.getKey();
						int nb = invertedIndex.get(b).size();
						int nab = findnab(b,qt);	

						double mim = ((double)((double)nab)/((double) na*nb));

						m.put(b, mim);
					}
				}

				//MIM term sorting

				int i = 0;
				LinkedList<String> list1 = new LinkedList(m.entrySet());

				Collections.sort(list1, new Comparator() {
					public int compare(Object o1, Object o2) {
						return ((Comparable) ((Map.Entry) (o2)).getValue())
								.compareTo(((Map.Entry) (o1)).getValue());
					}	
				});
				LinkedHashMap sortedMIM = new LinkedHashMap();
				for (Iterator it = list1.iterator(); it.hasNext();) {
					Map.Entry entry = (Map.Entry) it.next();
					sortedMIM.put(entry.getKey(), entry.getValue());
				} 
				Set sortedMIMSet = sortedMIM.entrySet();
				Iterator iterator1 = sortedMIMSet.iterator();
				i = 0;
				while(iterator1.hasNext()) {
					Map.Entry me = (Map.Entry)iterator1.next();
					if(i==15)break;
					String newQTerm = (String) me.getKey();

					if((double)me.getValue()>0.0) {
						queryTermsExpandedMIM.get(qid).add(newQTerm);

						i++;
					}
				}

			}

		}

		String queryExpandedDices ="";
		String queryExpandedMIM ="";
		for(String id :queryTerms.keySet()){
			queryExpandedDices += "\n"+id+"-------------";
			queryExpandedMIM += "\n"+id+"---------------";
			for(String qt2 : queryTermsExpandedMIM.get(id)){
				queryExpandedMIM += "\n"+qt2;
			}

		}

		FileWriter out = new FileWriter(file_path + "\\output\\query_MIM.out");
		out.append(queryExpandedMIM);
		out.close();

	}



}

