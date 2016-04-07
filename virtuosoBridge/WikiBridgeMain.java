package virtuosoBridge;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

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
		
		LinkedList<String> catToCalcRelProfile= new LinkedList<String>();
		catToCalcRelProfile.add("term:une_ville");
		catToCalcRelProfile.add("term:un_acteur");
		catToCalcRelProfile.add("term:un_village");
		catToCalcRelProfile.add("term:un_film_américain");
		catToCalcRelProfile.add("term:un_écrivain");
		catToCalcRelProfile.add("term:un_acteur_américain");
		catToCalcRelProfile.add("term:un_joueur_professionel_de_hockey");
		catToCalcRelProfile.add("term:un_journaliste");
		catToCalcRelProfile.add("term:un_peintre");
		catToCalcRelProfile.add("term:un_film");
		catToCalcRelProfile.add("term:un_chanteur");
		catToCalcRelProfile.add("term:une_chanson");
		catToCalcRelProfile.add("term:un_joueur");
		catToCalcRelProfile.add("term:un_musicien");
		catToCalcRelProfile.add("term:un_groupe_de_musique");
		
		//Regenerate Graph of category RelationGraph
		//querier.generateListCatRelationProfileGraph(catToCalcRelProfile);
		
		
		// get Mean Relation Distribution
		//HashMap<String, Double> normalRelationDistributionMap=querier.generateNormalRelationProfile();
		
		RelationProfile termProfile=new RelationProfile();
		
		
		//Queries
		String termToCompare ="term:Obama";
		
		//querier.findTermSimilarityToCats(termToCompare, catToCalcRelProfile,new CosineSimilarity());
		
		
	
		termProfile=querier.findEntityRelationProfile2(termToCompare);
		termProfile.normalize();
		termProfile.printSortedProfile();
		

		
		
		
		
		
		/*
		HashMap<String, Double>testingSimilarity1= new HashMap<String, Double>();
		HashMap<String, Double>testingSimilarity2= new HashMap<String, Double>();
		testingSimilarity1.put("a", 0.005);
		testingSimilarity2.put("a", 0.0002);

		testingSimilarity1.put("c", 0.000000000005);
		testingSimilarity2.put("c", 0.000000000027);
		testingSimilarity1.put("d", 0.0000000000025);
		testingSimilarity2.put("d", 0.000000000047);
		testingSimilarity1.put("e", 0.0000000000011);
		testingSimilarity2.put("e", 0.000000000127);
		
		
		
	
		testingSimilarity1=virtuosoBridgeTools.normalizeRelationProfile(testingSimilarity1);
		testingSimilarity2=virtuosoBridgeTools.normalizeRelationProfile(testingSimilarity2);
		Double sim=virtuosoBridgeTools.calcHashMapSimilirity(testingSimilarity1, testingSimilarity2);
		System.out.println(sim);
		*/
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
