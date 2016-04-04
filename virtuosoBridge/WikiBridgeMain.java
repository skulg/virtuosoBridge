package virtuosoBridge;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import virtuoso.jena.driver.*;

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
		// get Mean Relation Distribution
		HashMap<String, Double> normalRelationDistributionMap=querier.generateNormalRelationProfile();
		HashMap<String, Double> resultSet=new HashMap<String,Double>();

		
		

		//querier.generateAllSingleTermRelationProfiles();
		
		//Queries
		
		//querier.selectAll();
		//resultSet=querier.findEntityRelationProfile("term:foo1");
		//HashMap<String, Double> resultSet2=querier.compare2RelationProfile(normalRelationDistributionMap,resultSet);

		//querier.printSortedRelationProfile(resultSet);
		//System.out.println("");
		//querier.printSortedRelationProfile(normalRelationDistributionMap);
		//System.out.println("");
		//querier.printSortedRelationProfile(resultSet2);
		//resultSet=querier.calcRelProfileFromCat("term:une_ville");
		//querier.printSortedRelationProfile(resultSet);
		
		

		//resultSet=querier.calcRelProfileFromCat("term:une_ville");
		//querier.printSortedRelationProfile(resultSet);
		
		querier.clearRelProfileGraph();
		long startTime2 = System.nanoTime();
		querier.generateCatRelationProfileGraph("term:une_ville");
		
		long endTime2 = System.nanoTime();

		long duration2 = (endTime2 - startTime2);  //divide by 1000000 to get milliseconds.
		System.out.println(duration2);
		
		
		
		
		
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
