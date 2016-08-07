All the java files need to have the following folder structure in place in order to run them :

The working directory should consisit of these two folders :

1. "input"
It should consist of the AP89 eextracted collection

2. "output"
This is the folder where all the files will be generated and will be used by subsequent java programs

About java files :

corpusCleanup.java -
Used to cleanup the AP89 corpus to be read by Lucene. Output of the new index file is in the output folder.

Evaluation.java
Used at the end for calculating different evaluation measures using the top ranked document result sets for different query ecpansion techniques.
Place the Ranking folder for each expansion technique and run the source code to get the measure

globalAnalysis.java
Used to calculate expanded words for the query using DICE's coefficient. The expanded queries are then run through BM25 to get the top documents.

globalAnalysisMIM.java
Used to calculate expanded words for the query using MIM's coefficient. The expanded queries are then run through BM25 to get the top documents.

Snippet_Generation.java
Have the code for the snippet generation using Lucene Highlighter class . The input to this program is a cleaned up corpus.

Rocchio.java (localAnalysisRocchio is an extra file)
Used to calculate expanded words for the query using Rocchio coefficient. The expanded queries are then run through BM25 to get the top documents.

LanguageModelWithPRF.java
Used to calculate expanded words for the query using Language model using JM and Direlect smoothing. The expanded queries are then run through BM25 to get the top documents.

queryProcessing.java
Used to calculate expanded words for the query using StemClasses . The expanded queries are then run through BM25 to get the top documents.

Index.java
Used to get the index built for applying several query expansion techniques.
