package virtuosoBridge;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.jena.base.Sys;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;


public class virtuosoBridgeTools {

	static class MyListComparator implements Comparator<Entry<String,Double>>{

		@Override
		public int compare(Entry<String,Double> e1, Entry<String,Double> e2) {
			if(e1.getValue() < e2.getValue()){
				return 1;
			} else if(e1.getValue() > e2.getValue()) {
				return -1;
			}else{
				return 0;
			}
		}
	}

	/*
	 * Calculate similarity between two relationProfile
	 */

	static public double calcHashMapSimilirity(HashMap<String, Double> set1 ,HashMap<String, Double> set2){

		
		
		
		Set<String> terms=getAllKeysFrom2HashMap(set1, set2);

		set1=smoothRelProfile(set1,terms);
		set2=smoothRelProfile(set2,terms);
		
		//set1=compare2RelationProfile(set1, set2);
		

		Iterator<Entry<String, Double>> iter=set1.entrySet().iterator();
		HashMap<String, Double> resultMap= new HashMap<String, Double>();
		Double numerator=0.0;
		Double aSquared=0.0;
		Double bSquared=0.0;
		while (iter.hasNext()){
			Entry<String,Double> currentEntry=iter.next();
			String key=currentEntry.getKey();
			Double val1=currentEntry.getValue();
			Double val2=set2.get(key);
			//TODO
			
			numerator+=(val1*val2);
			aSquared+=val1*val1;
			bSquared+=val2*val2;
		}
		Double denominator= Math.sqrt(aSquared)*Math.sqrt(bSquared);
		
		Double cosSimilarity=numerator/denominator;
		
		return cosSimilarity;

	}



	public static Set<String> getAllKeysFrom2HashMap(HashMap<String,Double> map1,HashMap<String,Double> map2) {

		Set<String>  resultSet= new HashSet<String>();
		resultSet.addAll(map1.keySet());
		resultSet.addAll(map2.keySet());
		return resultSet;
	}



	public static HashMap<String, Double> smoothRelProfile(HashMap<String, Double> setToSmooth,Set<String> terms) {
		Double smoothingMass=0.0;
		Double totalMassAdded=0.0;
		HashMap<String, Double> resultSet=new HashMap<String,Double>();
		Iterator<String> iter=terms.iterator();
		while (iter.hasNext()){
			String currentTerm=iter.next();
			Double currentValue=0.0;
			
			if(setToSmooth.containsKey(currentTerm)){
				currentValue=setToSmooth.get(currentTerm);
			}
			
			resultSet.put(currentTerm, currentValue+smoothingMass);
			totalMassAdded+=smoothingMass;
		}

		resultSet=normalizeRelationProfile(resultSet, 1+totalMassAdded);
		return resultSet;

	}



	static public void printHashMap(HashMap<String, Double> map){

		System.out.println("----------------");
		System.out.println("Printing HASHMAP");
		System.out.println("----------------");

		for (String name: map.keySet()){

			String key =name.toString();
			String value = map.get(name).toString();  
			System.out.println(key + " " + value);  


		} 
	}

	static public LinkedList<Entry<String,Double>> hashMapToSortedLinkedList(HashMap<String, Double> map){

		Iterator<Entry<String, Double>> iter=map.entrySet().iterator();
		LinkedList<Entry<String,Double>> resultList=new LinkedList<Entry<String,Double>>();
		while (iter.hasNext()){
			Entry<String,Double> currentEntry=iter.next();
			String key=currentEntry.getKey();
			Double val=currentEntry.getValue();
			resultList.add(new AbstractMap.SimpleEntry<String, Double>(key,val));
		}
		Collections.sort(resultList, new virtuosoBridgeTools.MyListComparator());
		return resultList;



	}

	/*
	 * Clean URI of prefix and datatype before printing
	 */
	static String entityCleaner(String s){
		String result="";
		String[] temp=s.split("\\^\\^");
		String[] parts=temp[0].split("/");
		result=parts[parts.length-1];
		result=result.replace("_", " ");
		return result;
	}

	/*
	 *  Pretty up a QuerySolution and print it accordind to list of vars
	 */
	static void somewhatPrettyPrint(ArrayList<String> vars , ResultSet results){
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
			Iterator<String> iter=vars.iterator();
			String prettyString="  |||  ";
			RDFNode graph_name = result.get("graph");
			//prettyString+=graph_name+ "{ ";
			while (iter.hasNext()){
				String currentElem=iter.next();
				prettyString=prettyString+entityCleaner(result.get(currentElem).toString())+"  |||  ";


			}			
			System.out.println(prettyString);
		}

	}

	static public void printSortedRelationProfile(HashMap<String, Double> map){
		LinkedList<Entry<String,Double>> resultList=new LinkedList<Entry<String,Double>>();

		resultList=hashMapToSortedLinkedList(map);

		for(Entry<String,Double> e:resultList){
			System.out.println(e.getKey()+" "+e.getValue());
		}
	}

	static public HashMap<String, Double> compare2RelationProfile(HashMap<String, Double> set1 ,HashMap<String, Double> set2){

		Iterator<Entry<String, Double>> iter=set1.entrySet().iterator();
		HashMap<String, Double> resultMap= new HashMap<String, Double>();
		while (iter.hasNext()){
			Entry<String,Double> currentEntry=iter.next();
			String key=currentEntry.getKey();
			Double val1=currentEntry.getValue();
			Double val2=(double) 0;
			if(set2.containsKey(key)){
				val2=set2.get(key);
			}
			Double resultVal=val2-val1;
			resultMap.put(key,resultVal);

		}

		return resultMap;

	}

	static public HashMap<String, Double> sum2RelationProfile(HashMap<String, Double> map1 , HashMap<String, Double> map2){
		HashMap<String, Double> resultMap=map1;

		Iterator<Entry<String, Double>> iter = map2.entrySet().iterator();

		while(iter.hasNext()){
			Entry<String, Double> currentEntry = iter.next();
			String key = currentEntry.getKey();
			Double val1 = currentEntry.getValue();
			if (resultMap.containsKey(key)){
				Double val2 = resultMap.get(key);
				resultMap.put(key, (val1+val2));
			}else{
				resultMap.put(key, val1);
			}

		}



		return resultMap;
	}

	static HashMap<String, Double> normalizeRelationProfile(HashMap<String, Double> map , Double normalFactor){
		Iterator<Entry<String, Double>> iter=map.entrySet().iterator();
		HashMap<String, Double> resultMap = new HashMap<String , Double>();
		while(iter.hasNext()){
			Entry<String, Double> currentEntry = iter.next();
			String key = currentEntry.getKey();
			Double val1 = currentEntry.getValue();
			resultMap.put(key, val1/normalFactor);

		}
		return resultMap;
	}

	static void resultSetToRelationMap(HashMap<String, Double> relationMap, ResultSet results) {
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
			String relation=result.get("p").toString();
			Double prob=Double.valueOf(entityCleaner(result.get("o").toString()));
			relationMap.put(relation,prob);

		}
	}

}
