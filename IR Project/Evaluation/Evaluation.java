
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class Evaluation {

	static String file_path = (System.getProperty("user.dir") + File.separator);
	public static LinkedHashMap<String,LinkedList<String>> RelDoc = new LinkedHashMap<String,LinkedList<String>>();
	public static LinkedList<Integer> relLevel = new LinkedList<Integer>();
	public static LinkedList<Integer> IdealRelLevel = new LinkedList<Integer>();
	public static LinkedHashMap<String,LinkedList<String>> documentCollectionRetrieved = new LinkedHashMap<String,LinkedList<String>>();

	public static void readRelJudgments() throws IOException {

		FileReader file = new FileReader(file_path + "input\\qrels.adhoc.51-100.AP89.txt");
		BufferedReader in = new BufferedReader(file);
		String inData = " ";
		String [] valueList;

		String q = "";
		while ((inData = in.readLine()) != null) {
			if(!inData.endsWith("0")){
				valueList = inData.split(" ");
				q = valueList[0];
				//LinkedList<String> value = new LinkedList<String>();
				if(!(RelDoc.keySet().contains(q)))
				{
					System.out.print("\n"+valueList[0]);
					RelDoc.put(q, new LinkedList<String>());
					for(String val : valueList) {
						if(val.startsWith("AP89")) {
							RelDoc.get(q).add(val);
							System.out.print(" "+val);
						}
					}
				}
				else 
					for(String val : valueList) {
						if(val.startsWith("AP89")) {
							RelDoc.get(q).add(val);
							System.out.print(" "+val);
						}
					}
			}
		}
	}

	public static void readdocumentRetrieved() throws IOException {

		String path = file_path+ "output"+File.separator+"LuceneRanking"+File.separator+"RankingExpandedQuery1c_Language_Model"+File.separator;
		final File files = new File(path);


		for (File fileEntry : files.listFiles()) {

			FileReader file = new FileReader(fileEntry);
			BufferedReader in = new BufferedReader(file);
			String inData = " ";
			String query ="";

			LinkedList<String> docList = new LinkedList<String>();

			String filename = fileEntry.getName();
			query = filename.replace("_Ranking.out","");

			documentCollectionRetrieved.put(query,new LinkedList<String>());

			while (( inData = in.readLine()) != null) {
				docList.add(inData);
			}

			documentCollectionRetrieved.put(query, docList);
			docList = new LinkedList<String>();

		}
	}

	public static double precision_at_n(int n, String q){

		int count = 0;
		LinkedList<String> rel = new LinkedList<String>();
		for(String retdocs : documentCollectionRetrieved.get(q)) {
			if(RelDoc.get(q).contains(retdocs)){
				rel.add(retdocs);
			}
			count++;
			if(count==n) break;
		}

		Double Precision = ((double)rel.size())/n;
		return ((double)Precision);
	}
	
	public static double dcg_at_n(int n,String q){
		
		int count = 0;
		Double DCGpart2 = (double)0;
		for(int i = 2;i<=n;i++){
			DCGpart2 += (double)relLevel.get(i-1)/(Math.log((double)i)/Math.log(2));
		}
		
		return DCGpart2;
	}
	
	public static double idcg_at_n(int n,String q){
		int count = 0;
		Double DCGpart2 = (double)0;
		for(int i = 2;i<=n;i++){
			DCGpart2 += (double)IdealRelLevel.get(i-1)/(Math.log((double)i)/Math.log(2));
		}
		
		return DCGpart2;
	}


	public static void main(String[] args) throws Exception {

		readRelJudgments();
		readdocumentRetrieved();
		Double Precision;
		Double Recall;
		Double F1;
		Double Reciprocal_Rank = (double)0;

		double TotalPrecision_at_n = 0;
		
		for(String q : documentCollectionRetrieved.keySet())
		{

			Double RRank = (double)0;
			 
			LinkedList<String> rel = new LinkedList<String>();
			
			int rank = 1;
			for(String reldocs : RelDoc.get(q)) {
				if(documentCollectionRetrieved.get(q).contains(reldocs)){
					rel.add(reldocs);
					if(RRank==(double)0)
						RRank = (double)1/rank;
				}
				else rank++;
			}
			
			Reciprocal_Rank += RRank; 

			System.out.print("\nquery"+q+"\n");

			Precision = ((double)rel.size())/((double)documentCollectionRetrieved.get(q).size());
			Recall = ((double)rel.size())/((double)RelDoc.get(q).size());
			F1 = ((double)2*Precision*Recall)/((double)Precision+Recall);
			System.out.print("\nPrecision "+Precision);
			System.out.print("\nRecall "+Recall);
			System.out.print("\nF1 "+F1+"\n");
			int[] nList = {5, 10, 20, 30, 50, 70, 100};

			double AvgPrecision_at_n = 0;
			for(int i : nList) {
				AvgPrecision_at_n += precision_at_n(i,q);
				System.out.println("prec@n "+i+" "+precision_at_n(i,q));
			}
			TotalPrecision_at_n += AvgPrecision_at_n/nList.length;

			System.out.print("\nAvgPrecision_at_n "+AvgPrecision_at_n/nList.length);
			
			Double DCGpart2 = (double)0;
			Double DCGpart1 = (double)0;
			
			if(RelDoc.get(q).contains(documentCollectionRetrieved.get(q).get(0)))
				DCGpart1 = (double)1;
			
			for(String doc : documentCollectionRetrieved.get(q)){
				if(RelDoc.get(q).contains(doc)){
					relLevel.add(1);
				}
				else{
					relLevel.add(0);	
				}
			}
			
			IdealRelLevel = new LinkedList<Integer>(relLevel);
			Collections.sort(IdealRelLevel);
			Collections.reverse(IdealRelLevel);
			
			Double DCG = (double)0;
			Double IDCG = (double)0;
			Double NDCG = (double)0;
			Double TotalNDCG = (double)0;
			for(int i : nList) {
				DCG =  DCGpart1+dcg_at_n(i,q);
				IDCG =  1+idcg_at_n(i,q);
				NDCG = (double)DCG/IDCG;
				System.out.println("NDCG@n "+i+" "+NDCG);
				TotalNDCG += NDCG;
			}
			System.out.print("\nAvgNDCG_at_n "+TotalNDCG/nList.length);
			
			IdealRelLevel = new LinkedList<Integer>();
			relLevel = new LinkedList<Integer>();
			
		}

		Double MRR = Reciprocal_Rank/documentCollectionRetrieved.keySet().size();
		System.out.print("\nMRR "+MRR);
		Double MAP = TotalPrecision_at_n/documentCollectionRetrieved.keySet().size();
		System.out.print("\nMAP "+MAP);

	} 
}
