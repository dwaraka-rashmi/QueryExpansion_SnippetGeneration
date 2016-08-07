
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class LanguageModelWithPRF {

	static String file_path = (System.getProperty("user.dir") + File.separator);
	public static LinkedHashMap<String,LinkedList<String>> queryTerms = new LinkedHashMap<String,LinkedList<String>>();
	public static LinkedHashMap<String,LinkedList<String>> queryTermsExpanded = new LinkedHashMap<String,LinkedList<String>>();
	public static LinkedList<String> stopWords = new LinkedList<String>();
	public static Double AvgDocLength;
	public static LinkedHashMap<String,LinkedList<String>> stemClass = new LinkedHashMap<String,LinkedList<String>>();
	
	static LinkedHashMap<String, ArrayList<Posting>> invertedIndex = new LinkedHashMap<String, ArrayList<Posting>>();

	static class Posting 
	{
		public String doc_id;
		public int tf; 

		public Posting()
		{
			doc_id = "0";
			tf = 0;
		}
		
		public Posting(String s , int i)
		{
			doc_id = s;
			tf = i;
		}
	}
	
	public static HashSet<String> queryOriginal = new HashSet<String>();
	public static Set<String> uniqueterms = new HashSet<String>();
	public static LinkedList<String> vocabulary = new LinkedList<String>();
	public static TreeMap<String,Integer> documentCollection = new TreeMap<String,Integer>();
	public static LinkedList<String> Topdoc = new LinkedList<String>();
	public static LinkedHashMap<String,Double> Ranking = new LinkedHashMap<String,Double>();
	public static LinkedHashMap<String,Double> PreComputedJM = new LinkedHashMap<String,Double>();
	public static LinkedHashMap<String,Double> RankingTerm = new LinkedHashMap<String,Double>();
	public static LinkedHashMap<String,Double> RankingKL = new LinkedHashMap<String,Double>(); 
	static HashSet<String> vocabC = new HashSet<String>();

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {

		readStopWords();
		readIndex();
		readQueries();
		calcDocLength();


		for(Entry e : queryTerms.entrySet()) {

			String Result = "";
			Result += "\nQuery : "+e.getKey()+"\n";

			System.out.print("\nFor Query"+e.getKey());
			String query = (String) e.getKey();

			
			for(String doc :documentCollection.keySet()) {	
				calcDirichlet(query, doc);
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
				if(i==15)break;
				if((double)me.getValue()!=0) {
					Result += me.getKey()+"="+me.getValue()+"\n";

					Topdoc.add((String) me.getKey());	
				}
				i++;
			}
			
			System.out.print("\n Set C computed");
			
			for(String doc : Topdoc){
			for(Entry<String, ArrayList<Posting>> entry : invertedIndex.entrySet()){
				if(contains(entry.getValue() , doc)) vocabC.add(entry.getKey());
				}
			}
			
			System.out.print("\n size pof the vocab in set C "+vocabC.size());

			System.out.print("\nprecomputing JM for set C");
			for(String doc : Topdoc){
				calcJM(query,doc);
			}
			
			for(String word : vocabC){
				Double value = p_w_r(word,query);
				if(value!=-1)
					RankingTerm.put(word, value);
				System.out.print("\n"+word+" = "+value);
			}
			
			addqueryTermsExapanded(query);
			System.out.print("\nexpanded terms "+query+"\n");
			for(String eqTerm : queryTermsExpanded.get(query)){
				System.out.print(eqTerm+" ");
			}
			
			System.out.println("\nkl ranking");
			Result = "";
			for(String doc :documentCollection.keySet()){
				Double kl_score = (double)0;
				for(String qterm : queryTermsExpanded.get(query)) {

					kl_score += RankingTerm.get(qterm)*Dirichlet_score(qterm,doc);
				}
				Result += "\n"+doc+" = "+kl_score;
				System.out.print("\n"+doc+" = "+kl_score);
				RankingKL.put(doc, kl_score);
			}
			RankingTerm = new LinkedHashMap<String,Double>();
			System.out.print(Top10);
			File ResultFile = new File(file_path+"\\output\\KL_Ranking\\"+query+"_Ranking.out");

			BufferedWriter out = new BufferedWriter(new FileWriter(ResultFile));

			out.append(Result);
			out.close();

		}		
	}
	
	public static Boolean contains(ArrayList<Posting> pst_lst , String doc) throws Exception
	{
		Boolean contain = false;
		for(Posting pst : pst_lst)
			contain = ((pst.doc_id.equals(doc)) || contain); 
		
		return contain;
	}
	
	@SuppressWarnings("unchecked")
	public static void addqueryTermsExapanded(String query) {		
		
		List list = new LinkedList(RankingTerm.entrySet());

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
		while(iterator.hasNext()) {
			Map.Entry me = (Map.Entry)iterator.next();
			if(i==15)break;
			if((double)me.getValue()!=0 && !queryTerms.get(query).contains(me.getKey())) {
				queryTermsExpanded.get(query).add((String) me.getKey());
			}
			i++;
		}
		
	}
	
	public static Double p_w_r(String word, String query) throws Exception{
		
		Double numerator = p_w_q(word, query);
		Double denominator = (double)0;
		for(String term : vocabC){
			denominator += p_w_q(term, query);
		}
		
		if(denominator==0) return (double)-1;
		else return ((double)numerator/denominator);
		
	}
	
	public static Double p_w_q(String word, String query) throws Exception{
		
		Double score = (double)0;
		Double p_w_d = (double)0;
		Double p_q_d = (double)0;
		
		for(String doc : Topdoc) {

			p_w_d = calcJMval(word,doc);
			if(p_w_d==0) continue;
			p_q_d = (double)1;
			for(String qterm : queryTerms.get(query)) {

				p_q_d *= calcJMval(qterm, doc);
			}
			score +=p_w_d*p_q_d;
		}
		return score;
		
	}
	
	public static Double Dirichlet_score(String qterm, String doc) throws Exception {
		
		Double C = (double)21478667;
		Double D =   (double)documentCollection.get(doc);
		Double mu = (double)2000;
		Double score = (double)0;
		if(invertedIndex.containsKey(qterm)) {
		Double tf = (double)0;
		if(contains(invertedIndex.get(qterm) , doc)){
			for(Posting pst : invertedIndex.get(qterm))
				if(pst.doc_id.equals(doc))
					tf = (double)pst.tf;
		}
		ArrayList<Posting> posting = invertedIndex.get(qterm);
		Double cqi =(double)0;
		for(Posting e : posting) {
			cqi +=(double)e.tf;
		}
		
		score = (double)(tf+mu*cqi/C)/(D+mu);
		}

		return score;
		
	}
	public static double calcJMval(String word, String doc) throws Exception {
		
		Double C = (double)21478667;
		Double D =   (double)documentCollection.get(doc);
		Double lambda = (double)0.5;
		Double tf = (double)0;
		Double cqi =(double)0;
		if(invertedIndex.containsKey(word)){
			ArrayList<Posting> posting = invertedIndex.get(word);
			
			if(contains (invertedIndex.get(word) , doc)){
				for(Posting pst : invertedIndex.get(word))
					if(pst.doc_id.equals(doc))
						tf = (double)pst.tf;
			}
			for(Posting e : posting) {
					cqi +=(double)e.tf;
			}
		}		
		
		return Math.log((double)(((1-lambda)*(tf/D))+(lambda*(cqi/C))));
		
	}

	public static Double queryLikelihood(String word, String doc) throws Exception {
		
		Double D =   (double)documentCollection.get(doc);
		Double tf = (double)0;
		if(invertedIndex.containsKey(word))
		{   
			ArrayList<Posting> tflist = invertedIndex.get(word);
			if(contains ( tflist , doc)) 
			for(Posting pst : tflist)
				if(pst.doc_id.equals(doc))
					tf = (double)pst.tf;
		}
		return (double)tf/D;
		
	}
	
	public static void calcDirichlet(String query, String doc) throws Exception {
		
		Double C = (double)21478667;
		Double D =   (double)documentCollection.get(doc);
		Double mu = (double)2000;
		Double score = (double)0;
		for(String qterm : queryTerms.get(query)) {
			if(!invertedIndex.containsKey(qterm)) continue;
			Double tf = (double)0;
			if(contains (invertedIndex.get(qterm) , doc)){
				for(Posting pst : invertedIndex.get(qterm))
					if(pst.doc_id.equals(doc))
						tf = (double)pst.tf;
			}
			if(tf>0)
			System.out.print("\n"+doc+"="+tf);
			ArrayList<Posting> posting = invertedIndex.get(qterm);
			Double cqi =(double)0;
			for(Posting e : posting) {
				cqi +=(double)e.tf;
			}
			System.out.print("\ndoc="+doc+" query "+qterm+" cqi="+cqi);
			
			score += Math.log((double)(tf+mu*cqi/C)/(D+mu));
			
		}
		
		Ranking.put(doc, score);
		
	}
	
	public static void calcJM(String query, String doc) throws Exception {
		
		Double C = (double)21478667;
		Double D =   (double)documentCollection.get(doc);
		Double lambda = (double)0.5;
		Double score = (double)0;
		for(String qterm : queryTerms.get(query)) {
			if(!invertedIndex.containsKey(qterm)) continue;
			Double tf = (double)0;
			if(contains (invertedIndex.get(qterm),doc)){
				for(Posting pst : invertedIndex.get(qterm))
					if(pst.doc_id.equals(doc))
						tf = (double)pst.tf;
			}
			if(tf>0)
			System.out.print("\n"+doc+"="+tf);
			ArrayList<Posting> posting = invertedIndex.get(qterm);
			Double cqi =(double)0;
			for(Posting e : posting) {
				cqi +=(double)e.tf;
			}
			System.out.print("\ndoc="+doc+" query "+qterm+" cqi="+cqi);
			
			score += (double)(((1-lambda)*(tf/D))+(lambda*(cqi/C)));
			
		}
		System.out.print("\n"+doc+" = "+score);
		PreComputedJM.put(doc, score);
		
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
				if((!term.equals(""))&&(!stopWords.contains(term))&& invertedIndex.keySet().contains(term)) {
					value.add(term);
					queryOriginal.add(term);	
				}
			}
			
			queryTerms.put(q,new LinkedList<String>(value));
			queryTermsExpanded.put(q,new LinkedList<String>());			
		}
		in.close();
	}

	public static void readIndex() throws IOException {

		
		FileReader file = new FileReader(file_path + "output\\index_new.txt");
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
			invertedIndex.put(term,new ArrayList<Posting>());
			ArrayList<Posting> docMap = new ArrayList<Posting>();
			String [] doc;

			doc = docList.split(" ");
			for(String docValPair : doc) {
				docValPair = docValPair.replace("(", "");
				docValPair = docValPair.replace(")", "");
				docMap.add(new Posting(docValPair.split(",")[0],Integer.parseInt(docValPair.split(",")[1])));
				docColl.add(docValPair.split(",")[0]);
				System.out.print(" "+docValPair.split(",")[0]+" = "+docValPair.split(",")[1]);
			}
			invertedIndex.put(term, docMap);
		}

		System.out.println("\ninverted index size "+invertedIndex.size());
		in.close();

	}

	
	public static void calcDocLength() {
		
		for (Entry<String, ArrayList<Posting>> pos : invertedIndex.entrySet()) {
			ArrayList<Posting> posting = pos.getValue();
			for(Posting e : posting)
			{
				String doc = e.doc_id;
				if(documentCollection.containsKey(doc))
					documentCollection.put(doc, documentCollection.get(doc)+1);
				else
					documentCollection.put(doc, 1);
			}
		}
	}	


}

