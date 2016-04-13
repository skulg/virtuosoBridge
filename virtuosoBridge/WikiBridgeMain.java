package virtuosoBridge;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;

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
		catToCalcRelProfile.add("term:OTHERcATS");
		
		// get Mean Relation Distribution
		//HashMap<String, Double> normalRelationDistributionMap=querier.generateNormalRelationProfile();
		
		RelationProfile termProfile=new RelationProfile();
		RelationProfile termProfile2=new RelationProfile();

		//RelationProfile normalProfile= querier.generateNormalRelationProfile();
		
		//Queries
		String termToCompare ="term:James_Bond";
		String termToCompare2 ="term:Madonna";
		
		HashMap<String, Double>  similarityMap=querier.findTermSimilarityToCats(termToCompare, catToCalcRelProfile,new MyMeasure());
		//HashMap<String, Double>  similarityMap=querier.findTermSimilarityToCats(termToCompare2, catToCalcRelProfile,new TestingStuffSimilarity());
		//HashMap<String, Double>  similarityMap=querier.findTermSimilarityToCats(termToCompare2, catToCalcRelProfile,new CosineSimilarity());
		
		
		//Entry<String, Double> catAssigned = querier.assignCatToTerm(termToCompare, catToCalcRelProfile, new MyMeasure());
		
		
		//System.out.println(catAssigned);
		
		new RelationProfile(similarityMap).printSortedProfile();
		
		
		
		
		
		//querier.assignAllTermsACat(catToCalcRelProfile, new MyMeasure());
	
	
		/*
		termProfile=querier.findEntityRelationProfile2(termToCompare);
		termProfile2=querier.findEntityRelationProfile2(termToCompare2);
	
		termProfile.removeRelationFromProfile("relation:être");
		Set<String> commonRelations =virtuosoBridgeTools.getIntersectionFrom2HashMap(termProfile.getProfile(), termProfile2.getProfile());
		
		
		termProfile=termProfile.createSubsetProfile(commonRelations);
		termProfile2=termProfile2.createSubsetProfile(commonRelations);
		
		
		termProfile.normalize();
		termProfile2.normalize();
		
		
		Double symLevel=termProfile.findSimilarityLevel(termProfile2, normalProfile, new CosineSimilarity());
		System.out.println(symLevel);
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
