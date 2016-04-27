package virtuosoBridge;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;

import javax.management.relation.Relation;

public class WikiBridgeMain {
	static String user="";
	static String pass="";
	static String server="";
	public static void main(String[] args) {
		String productionGraph="http://wikiDataReduced2"; //MAIN GRAPH
		String testingGraph="http://testingStuff"; //TESTING GRAPH 


		//SELECT which Graph to use
		String graph=productionGraph;

		//INIT QUERIER
		readServerInfo();
		WikiQuerier querier= new WikiQuerier(user,pass,server,graph);

		//NEVER RUN THIS PART ON PRODUCTION GRAPH
		if(graph==testingGraph){

			querier.testingOnTestGraph();
		}

		LinkedList<String> catToCalcRelProfile= querier.findTopCats(1000);


		//Regenerate Graph of category RelationGraph
		//querier.generateListCatRelationProfileGraph(catToCalcRelProfile);

		//RelationProfile otherCatProfile=querier.genOtherCatRelProfilev2(catToCalcRelProfile);
		//otherCatProfile.printSortedProfile();
		//catToCalcRelProfile.add("term:OTHERcATS");

		// get Mean Relation Distribution
		//HashMap<String, Double> normalRelationDistributionMap=querier.generateNormalRelationProfile();

		RelationProfile termProfile=new RelationProfile();
		RelationProfile termProfile2=new RelationProfile();

		//RelationProfile normalProfile= querier.generateNormalRelationProfile();

		//Queries
		String termToCompare ="term:James_Bond";
		String termToCompare2 ="term:Madonna";

		//HashMap<String, Double>  similarityMap=querier.findTermSimilarityToCats(termToCompare, catToCalcRelProfile,new MyMeasure());
		//new RelationProfile(similarityMap).printSortedProfile();


		//querier.isCatAssigned(termToCompare2);

		LinkedList<String> termsToClassify=new LinkedList<String>();

				//termsToClassify=querier.fetchDistinctArg1TermsInFreqOrder(100000);

		//querier.resumeTermAssignementLinkedList(termsToClassify,catToCalcRelProfile, new MyMeasure());


				//querier.resumeTermCatSimilarityLinkedList(termsToClassify, catToCalcRelProfile, new MyMeasure(), 10);


		querier.dumpCatAssignementGraphToTextFiles();

	}

	/*
	 * Read server info from file to avoid making info public through file versioning(Git).
	 */
	static void readServerInfo(){
		try{
			File f=new File("serverInfo.txt");
			FileReader fileReader = new FileReader(f);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			String line;
			int lineNumber=0;
			while ((line = bufferedReader.readLine()) != null) {
				switch (lineNumber){
				case 0: user=line;
				lineNumber++;
				break;
				case 1: pass=line;
				lineNumber++;
				break;
				case 2: server=line;
				lineNumber++;
				break;
				}
			}
			fileReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
