import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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


public class queryProcessing {

	static String file_path = (System.getProperty("user.dir") + File.separator);
	public static LinkedHashMap<String,LinkedList<String>> queryTerms = new LinkedHashMap<String,LinkedList<String>>();
	public static LinkedList<String> stopWords = new LinkedList<String>();
	public static Double AvgDocLength;
	
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
	
	public static LinkedHashMap<String,LinkedList<String>> stemClass = new LinkedHashMap<String,LinkedList<String>>();
	public static LinkedHashMap<String,ArrayList<Posting>> invertedIndex = 
			new LinkedHashMap<String,ArrayList<Posting>>();
	public static Set<String> uniqueterms = new HashSet<String>();
	public static LinkedList<String> vocabulary = new LinkedList<String>();
	public static LinkedList<String> documentCollection = new LinkedList<String>();
	public static LinkedHashMap<String,Double> Ranking = new LinkedHashMap<String,Double>();
	public static HashSet<String> docColl = new HashSet<String>();

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {

		readStopWords();
		readStemWords();
		readQueries();
		filterIndex("output\\index.txt");
		readIndex("output\\index_new.txt");
		
		for(String doc : docColl) {
			documentCollection.add(doc);
		}
		
		AverageDocLength();
		Double Rank = 0.0;
		
		for(Entry e : queryTerms.entrySet()) {

			String Result = "";
			Result += "\nQuery : "+e.getKey()+"\n";

			System.out.print("\nFor Query"+e.getKey());
			String query = (String) e.getKey();
			
			for(String doc :documentCollection) {	
				Rank = 0.0;
				for(String word : queryTerms.get(query)) {
					if(contains ((invertedIndex.get(word)) , doc)) 
						Rank += (double)(calcPart1(word)*calcPart2(word,doc)*calcPart3(query,word));
					else
						Rank += 0.0;
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
			File ResultFile = new File(file_path+"\\output\\","Ranking_" + query + ".out");
			
			BufferedWriter out = new BufferedWriter(new FileWriter(ResultFile));

			out.append(Result);
			out.close();

			System.out.print("\n Ranked documents list created");
			
		}
	}
	
	
	public static Boolean contains(ArrayList<Posting> pst_lst , String doc)
	{
		Boolean contain = false;
		for(Posting pst : pst_lst)
			contain = ((pst.doc_id == doc) || contain); 
		
		return contain;
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
			valueList = inData.split("|");
			stem = valueList[0].trim();
			LinkedList<String> stemWords = new LinkedList<String>();
			for(String stemWord : valueList[1].split(" ")) {
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
			LinkedList<String> value = new LinkedList<String>();
			String term = "";
			for(int i = 1; i<valueList.length;i++) {
				term = valueList[i];
				if((!term.equals(""))&&(!stopWords.contains(term))){
					if(stemClass.containsValue(term))
						for(Entry<String, LinkedList<String>> e : stemClass.entrySet()) {
							if(e.getValue().contains(term))
								for(String stemWord : stemClass.get(e.getKey())){
									value.add(stemWord.toLowerCase());
								}
						}
					else
						value.add(term.toLowerCase());
					System.out.print(" "+term.toLowerCase());
				}
			}
			queryTerms.put(q,(value));
		}
		in.close();
	}
	
	public static void filterIndex(String index_file) throws IOException {
		FileReader file = new FileReader(file_path + index_file);
		BufferedReader in = new BufferedReader(file);
		FileWriter file_out = new FileWriter(file_path + index_file.replace(".txt", "_new.txt"));
		BufferedWriter out = new BufferedWriter(file_out);
		
		String inData = " ";
		int count =0;
		while (( inData = in.readLine()) != null) {
			String s[] = inData.replace("->"," ").trim().split(" ");
			if(((s.length == 2) && (s[1].endsWith(",1)") || s[1].endsWith(",2)"))) || ((s.length == 3) && s[1].endsWith(",1)") && s[2].endsWith(",1)"))) 
				continue;
			else
			{
				out.write(inData + "\n");
				count++;
				System.out.println("Count = " + count);
			}
		}
		System.out.println("Final Count = " + count);
		out.close();
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
		
				System.out.print(" "+pst.doc_id+" = "+pst.tf);
			}
			invertedIndex.put(term, docMap);	
			
			System.out.println("\ninverted index size "+invertedIndex.size());
			docMap=null;
			docList = null;
		}			
		System.out.println("\ninverted index size "+invertedIndex.size());
		in.close();
	}

	public static Integer docLength(String doc) {

		Integer length = 0;
		for (Entry<String, ArrayList<Posting>> pos : invertedIndex.entrySet()) {
			ArrayList<Posting> posting = pos.getValue();
			for(Posting e : posting)
			{
				if(doc.contentEquals(e.doc_id)) {
					length += e.tf;
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
		ArrayList<Posting> posting = invertedIndex.get(word);
		Double tf = 0.0;
		for(Posting pst : posting)
			if(pst.doc_id.equals(doc))
				tf = (double)pst.tf;
		
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


}
