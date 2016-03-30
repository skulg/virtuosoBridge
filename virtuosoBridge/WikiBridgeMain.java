package virtuosoBridge;


import java.util.HashMap;

import virtuoso.jena.driver.*;

public class WikiBridgeMain {

	public static void main(String[] args) {
		
		
		WikiQuerier querier= new WikiQuerier();
		//querier.selectAll();
		//querier.selectMostFreqRelations(100);	
		//querier.findEntityRelationProfile("term:Obama");

		HashMap<String, Double> normalRelationDistributionMap=querier.generateNormalRelationProfile();
		//Double value =relationDistributionMap.get("http://test/relation/passer");
		//System.out.println(value);		
		//HashMap<String, Double> queryRelationDistributionMap=querier.findEntityRelationProfile("term:la_ville");

		//querier.compare2RelationProfile(normalRelationDistributionMap, queryRelationDistributionMap);

		//System.out.println(querier.fetchAllEntitiesBelongingToCat("term:un_film"));
		HashMap<String, Double> resultSet=new HashMap<String,Double>();
		resultSet=querier.calcRelProfileFromCat("term:un_film");
		
		querier.compare2RelationProfile(normalRelationDistributionMap,resultSet);
		
		
	}
}
