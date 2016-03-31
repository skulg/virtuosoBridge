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
		String graph="http://wikiDataReduced2";

		String testingGraph="http://testingStuff";
		
		//INIT QUERIER
		readServerInfo();
		WikiQuerier querier= new WikiQuerier(user,pass,server,graph);


		// get Mean Relation Distribution
		//HashMap<String, Double> normalRelationDistributionMap=querier.generateNormalRelationProfile();
		HashMap<String, Double> resultSet=new HashMap<String,Double>();

		//Queries

		//resultSet=querier.calcRelProfileFromCat("term:un_film");
		//querier.compare2RelationProfile(normalRelationDistributionMap,resultSet);
		
		//querier.printSortedRelationProfile(resultSet);
		resultSet=querier.calcRelProfileFromCat("term:une_ville");
		querier.printSortedRelationProfile(resultSet);
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
