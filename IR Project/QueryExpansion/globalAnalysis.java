
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


public class globalAnalysis {

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
	public static LinkedHashMap<String,Double> RankingExpanded = new LinkedHashMap<String,Double>();

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {

		readStopWords();
		readStemWords();
		readQueries();
		readIndex();
		calcDices();
		AverageDocLength();
		Double Rank = 0.0;

		System.out.print("\n Original Query");
		for(Entry e : queryTerms.entrySet()) {

			String Result = "";
			Result += "\nQuery : "+e.getKey()+"\n";

			System.out.print("\nFor Query"+e.getKey());
			String query = (String) e.getKey();

			for(String doc :documentCollection) {	
				Rank = 0.0;
				for(String word : queryTerms.get(query)) {
					if(invertedIndex.containsKey(word)){
						System.out.println(word);
					if(invertedIndex.get(word).containsKey(doc))
						Rank += (double)(calcPart1(word)*calcPart2(word,doc)*calcPart3(query,word));
					else
						Rank += 0.0;
					}
				}

				Ranking.put(doc, Rank);

			}	

			List list = new LinkedList(Ranking.entrySet());

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

			String Top10 = "\n";
			Set sortedPRSet = sortedPR.entrySet();
			Iterator iterator = sortedPRSet.iterator();
			int i = 0;
			while(iterator.hasNext()) {
				Map.Entry me = (Map.Entry)iterator.next();
				if((double)me.getValue()!=0)
					Result += me.getKey()+"="+me.getValue()+"\n";
			}
			i = 0;
			while(iterator.hasNext()) {
				Map.Entry me = (Map.Entry)iterator.next();
				Top10 += me.getKey()+"="+me.getValue()+"\n";
				if(i==100)break;
				i++;
			}

			System.out.print(Top10);
			File ResultFile = new File(file_path+"\\output\\","Ranking_original_"+query+".out");

			BufferedWriter out = new BufferedWriter(new FileWriter(ResultFile));

			out.append(Result);
			out.close();

			System.out.print("\n Ranked documents list created");

		}
		
		System.out.print("\n Expanded Query");
		for(Entry e : queryTermsExpandedDices.entrySet()) {

			String Result1 = "";
			Result1 += "\nQuery : "+e.getKey()+"\n";

			System.out.print("\nFor Query"+e.getKey());
			String query = e.getKey();
			
			for(String doc : documentCollection) {	
				Rank = 0.0;
				for(String word : queryTermsExpandedDices.get(query)) {
					if(invertedIndex.containsKey(word)) {
						System.out.println(word);
						if(invertedIndex.get(word).containsKey(doc))
					//if(invertedIndex.get(word).containsValue(doc))
						Rank += (double)(calcPart1(word)*calcPart2(word,doc)*calcPart3Expanded(query,word));
					else
						Rank += 0.0;
					}
				}

				RankingExpanded.put(doc, Rank);

			}	

			List list1 = new LinkedList(RankingExpanded.entrySet());

			Collections.sort(list1, new Comparator() {
				public int compare(Object o1, Object o2) {
					return ((Comparable) ((Map.Entry) (o1)).getValue())
							.compareTo(((Map.Entry) (o2)).getValue());
				}	
			});

			Collections.reverse(list1);

			LinkedHashMap sortedPR = new LinkedHashMap();
			for (Iterator i = list1.iterator(); i.hasNext();) {
				Map.Entry entry = (Map.Entry) i.next();
				sortedPR.put(entry.getKey(), entry.getValue());
			} 

			String Top10 = "\n";
			Set sortedPRSet = sortedPR.entrySet();
			Iterator iterator = sortedPRSet.iterator();
			int i = 0;
			while(iterator.hasNext()) {
				Map.Entry me = (Map.Entry)iterator.next();
				if((double)me.getValue()!=0)
					Result1 += me.getKey()+"="+me.getValue()+"\n";
				//					Result += me.getKey()+"\n";
			}
			i = 0;
			while(iterator.hasNext()) {
				Map.Entry me = (Map.Entry)iterator.next();
				Top10 += me.getKey()+"="+me.getValue()+"\n";
				if(i==100)break;
				i++;
			}

			System.out.print(Top10);
			File ResultFile = new File(file_path+"\\output\\",e.getKey()+"_resultsBM25_.eval");
			BufferedWriter out = new BufferedWriter(new FileWriter(ResultFile));

			out.append(Result1);
			out.close();

			System.out.print("\n Ranked documents list created");
			
			break;

		}


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
		System.out.print("\n Number of docs"+documentCollection.size());
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
	public static double calcPart3Expanded(String query,String word){

		Double tf = (double)Collections.frequency(queryTermsExpandedDices.get(query),word);
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
	public static void calcDices() throws IOException {

		for(Entry<String, LinkedList<String>> qEntry : queryTerms.entrySet()) {
			String qid = qEntry.getKey();
			
			System.out.println("query "+qid);
			LinkedList<String> qTerms = new LinkedList<String>(queryTerms.get(qid));

			for(String qt : qTerms){
			
				LinkedHashMap<String,Double> d = new LinkedHashMap<String,Double>();
				LinkedHashMap<String,Double> m = new LinkedHashMap<String,Double>();

				if(invertedIndex.keySet().contains(qt)) {
					int na = invertedIndex.get(qt).size();
					for(Entry e : invertedIndex.entrySet()) {
						String b = (String) e.getKey();
						int nb = invertedIndex.get(b).size();
						int nab = findnab(b,qt);	
						double dc = ((double)((double)nab)/((double) na+nb));
						
						d.put(b, dc);
						
					}
				}

				//Dices term sorting
				List list = new LinkedList(d.entrySet());

				Collections.sort(list, new Comparator() {
					public int compare(Object o1, Object o2) {
						return ((Comparable) ((Map.Entry) (o2)).getValue())
								.compareTo(((Map.Entry) (o1)).getValue());
					}	
				});
				LinkedHashMap sortedDices = new LinkedHashMap();
				for (Iterator i = list.iterator(); i.hasNext();) {
					Map.Entry entry = (Map.Entry) i.next();
					sortedDices.put(entry.getKey(), entry.getValue());
				} 
				Set sortedDicesSet = sortedDices.entrySet();
				Iterator iterator = sortedDicesSet.iterator();
				int i = 0;
				while(iterator.hasNext()) {
					Map.Entry me = (Map.Entry)iterator.next();
					if(i==15)break;
					String newQTerm = (String) me.getKey();
					if((double)me.getValue()>0.0 && (!qTerms.contains(newQTerm))) {
						queryTermsExpandedDices.get(qid).add(newQTerm);
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
			for(String qt1 : queryTermsExpandedDices.get(id)){
				queryExpandedDices += "\n"+qt1;
			}
		}

		FileWriter out = new FileWriter(file_path + "\\output\\query_Dices.out");
		out.append(queryExpandedDices);
		out.close();

	}

}

