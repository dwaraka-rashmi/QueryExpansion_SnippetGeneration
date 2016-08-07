import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;
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

public class corpusCleanup {

	static LinkedHashMap<Integer, String> file = new LinkedHashMap<Integer, String>();
	static String file_path = (System.getProperty("user.dir") + File.separator);
	static LinkedList<String> stop_words = new LinkedList<String>();

	public static void main(String[] args) throws NumberFormatException, IOException {
		intialize_stopwords();
		intialize_docs();
	}

	public static String removeTrailingDot(String term){
		term = term.replaceAll("^\\.+", "");
		term = term.replaceAll("\\.+$", "");
		return term.toLowerCase();
	}

	@SuppressWarnings("null")
	public static void intialize_docs() throws NumberFormatException, IOException
	{

		File folder = new File(file_path + "input\\AP_DATA\\ap89_collection\\");

		File[] listOfFiles = folder.listFiles();
		String line = null;
		String doc_id=null;

		Pattern p = Pattern.compile("\\w+(\\.?\\w+)*");

		for (File file : listOfFiles)
			if (file.isFile()) {

				FileReader fileReader = new FileReader(file_path + "input\\AP_DATA\\ap89_collection\\" + file.getName());
				System.out.println("filename "+file.getName());
				BufferedReader br = new BufferedReader(fileReader);
				LinkedList<String> words = new LinkedList<String>();
				String path = file_path + "\\output\\rawCorpus\\";
				File customDir = new File(path,file.getName());
				if (!customDir.exists()) {
					customDir.mkdirs(); 
				}
				path += File.separator + customDir.getName();
				line = null;
				doc_id = null;
				String[] s;

				FileWriter out = null;
				while ((line = br.readLine()) != null) {
					// System.out.println(line); // for testing
					s = line.trim().split(" "); // for breaking up the line where there are spaces

					if(line.startsWith("<DOCNO>")){ 
						doc_id = s[1];
						out = new FileWriter(path+"\\"+doc_id+".txt");  
				    continue;
				    }
					if(line.startsWith("<TEXT>")){ 
						String[] terms = null;
						String FilesText = " ";
						while (!((line = br.readLine()).contains("</TEXT>")))
						{
							FilesText += line+"\n";		
						}

						out.append(FilesText);
						continue;
					}
					if(line.startsWith("</DOC>")){ 
						out.close();
						out = null;
						System.out.println(doc_id + "tokenized");
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

}
