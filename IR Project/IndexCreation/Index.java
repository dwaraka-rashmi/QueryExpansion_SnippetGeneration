import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public class Index {

	static LinkedHashMap<Integer, String> file = new LinkedHashMap<Integer, String>();
	static String file_path = (System.getProperty("user.dir") + File.separator);
	static LinkedList<String> stop_words = new LinkedList<String>();
	static LinkedHashSet<String> vocab = new LinkedHashSet<String>();

	static LinkedHashMap<String, ArrayList<String>> docs_text = new LinkedHashMap<String, ArrayList<String>>();

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

	static LinkedHashMap<String, ArrayList<Posting>> index = new LinkedHashMap<String, ArrayList<Posting>>();

	public static void main(String[] args) throws NumberFormatException, IOException {

		intialize_stopwords();
		intialize_docs();

		build_index();

		printfile();

	}

	public static String removeTrailingDot(String term){
		
		term = term.replaceAll("^\\.+", "");
		term = term.replaceAll("\\.+$", "");
		return term.toLowerCase();
				
	}
	
	public static void intialize_docs() throws NumberFormatException, IOException
	{
		File folder = new File(file_path + "input\\AP_DATA\\ap89_collection\\");

		File[] listOfFiles = folder.listFiles();
		ArrayList<String> words = new ArrayList<String>();
		Pattern p = Pattern.compile("\\w+(\\.?\\w+)*");

		for (File file : listOfFiles)
			if (file.isFile()) {

				FileReader fileReader = new FileReader(file_path + "input\\AP_DATA\\ap89_collection\\" + file.getName());
				System.out.println("filename "+file.getName());
				BufferedReader br = new BufferedReader(fileReader);
				String line = null;
				String doc_id = null;
				String[] s;
				while ((line = br.readLine()) != null) {
					// System.out.println(line); // for testing
					s = line.trim().split(" "); // for breaking up the line where there are spaces
					switch (s[0]) {

					case "<DOCNO>": doc_id = s[1];  
					break;
					case "<TEXT>":
						String[] terms = null;
						while (!((line = br.readLine()).contains("</TEXT>")))
						{
							//	String s_text[] = line.trim().replace("."," ").replace(",", " ").replace("?", " ").replace("!", " ").replace(";", " ").replace(":", " ").replace("\t", " ").replace("\r", " ").split(" ");
							terms = line.split("[^\\w.]");
							for(String each_t : terms)
							{
								each_t = removeTrailingDot(each_t);
								if(p.matcher(each_t).matches()&&(!stop_words.contains(each_t)))
								{
									words.add(each_t);
									vocab.add(each_t);
								}
							}		
							
						}
						break;
					case "</DOC>":
						//String s_text[] = text.replace("."," ").replace(",", " ").replace("?", " ").replace("!", " ").replace(";", " ").replace(":", " ").replace("\t", " ").replace("\r", " ").split(" ");
						docs_text.put(doc_id,words);
						System.out.println(doc_id + "tokenized");
						words = new ArrayList<String>();
						break;
					default: break;

					}

				}

				br.close();
			}
	}


	public static void intialize_stopwords() throws IOException
	{		
		FileReader fileReader = new FileReader(file_path + "input\\stoplist.txt");
		BufferedReader br = new BufferedReader(fileReader);

		String line = null;

		while ((line = br.readLine()) != null)
			stop_words.add(line);

		br.close();
	}

	public static void stop_docs()
	{
		LinkedHashMap<String, ArrayList<String>> docs_text_stopped = new LinkedHashMap<String, ArrayList<String>>();
		for(Entry<String, ArrayList<String>> doc : docs_text.entrySet())
		{
			ArrayList<String> words = new ArrayList<String>();
			for(String word : doc.getValue())
			{
				if((stop_words.contains(word)) || word.equals(" "))
					continue;
				else
					words.add(word);
			}
			docs_text_stopped.put(doc.getKey(), words);
			System.out.println(doc.getKey() + "stopped");
		}
		docs_text = new LinkedHashMap<String, ArrayList<String>>();
		docs_text = docs_text_stopped;
	}

	public static void build_index()
	{
		System.out.println("Free Memory - " +Runtime.getRuntime().freeMemory() + "Max Memory - " + Runtime.getRuntime().maxMemory());
		System.out.println("Building Index ...");

		System.out.println("Vocab Ready 1 ...");
		
		LinkedList<String> result_values = new LinkedList<String>(vocab);

		Collections.sort(result_values);
		
		System.out.println("Free Memory - " +Runtime.getRuntime().freeMemory() + "Max Memory - " + Runtime.getRuntime().maxMemory());		

		LinkedHashMap<String, Integer> index_doc = new LinkedHashMap<String, Integer>();

		ArrayList<Posting> empty_lst = new ArrayList<Posting>();
			
		for (String word : result_values)
			index.put(word, empty_lst);
		
		result_values = null;
		vocab = null;
		
		System.out.println("Vocab Ready 2 ...");
		System.out.println("Free Memory - " +Runtime.getRuntime().freeMemory() + "Max Memory - " + Runtime.getRuntime().maxMemory());
		
		for(Entry<String, ArrayList<String>> doc : docs_text.entrySet())
		{
			index_doc = new LinkedHashMap<String, Integer>();

			for(String word : doc.getValue())
			{
				int count = 0; 
				if(index_doc.keySet().contains(word))
					index_doc.put(word, (index_doc.get(word) + 1));
				else
					index_doc.put(word, 1);
			}
			System.out.println("Document - " + doc.getKey() + "indexed ");
			for (String word_tf : index_doc.keySet())
			{
				if(!(index.get(word_tf).isEmpty()))
				{
					index.get(word_tf).add(new Posting(doc.getKey(),index_doc.get(word_tf)));
				}
				else
				{
					ArrayList<Posting> pst_list = new ArrayList<Posting>();
					Posting pst = new Posting();
					pst.doc_id = doc.getKey();
					pst.tf = index_doc.get(word_tf);
					pst_list.add(pst);
					index.put(word_tf, pst_list);
					pst=null;
					pst_list=null;
				}
			}
			System.out.println(doc.getKey() + " indexed");
		}
	}
		
		
	public static void sort_docs_text()
	{
		LinkedHashMap<String, ArrayList<String>> sorted_docs = new LinkedHashMap<String, ArrayList<String>>();
		
		List result_values = new LinkedList(docs_text.keySet());
		
		Collections.sort(result_values);

		Iterator valueIt = result_values.iterator();

		while (valueIt.hasNext()) {

			Object val = valueIt.next();

			ArrayList<String> list = new ArrayList<String>();
			for (Entry<String, ArrayList<String>> ind : (docs_text.entrySet()))
			{
				if(val.equals(ind.getKey()))
					list= ind.getValue();
			}
			if(!(list.isEmpty()))
				sorted_docs.put((String) val, list);
		}

		docs_text = new LinkedHashMap<String, ArrayList<String>>();

		docs_text = sorted_docs;

	}

	public static void printfile() throws IOException
	{
		
		System.out.println("Docs Text - " + docs_text.size());
		System.out.println("Index Size - " + index.size());
		
		FileWriter out = new FileWriter(file_path + "\\output\\index.out");
		for (Entry<String, ArrayList<Posting>> doc : (index.entrySet()))
		{
			out.append(doc.getKey() + "->");
			ArrayList<Posting> pst_list = new ArrayList<Posting>();
			pst_list = doc.getValue();
			for (Posting pst : pst_list)
			{
				out.append("(" + pst.doc_id + ","+pst.tf + "), ");
				System.out.print("\n(" + pst.doc_id + " "+pst.tf + ") ");
			}
			out.append("\n");
		}
		out.close();
	}

}
