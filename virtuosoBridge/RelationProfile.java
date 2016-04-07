package virtuosoBridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;


/*
 * Class that describe a relationProfile and its operations
 */
public class RelationProfile {

	private HashMap<String,Double> profile;


	public RelationProfile(HashMap<String,Double> relProfile) {
		this.profile=relProfile;
	}

	
	public RelationProfile() {
		this.profile=new HashMap<String,Double>();
	}


	//Normalize the relationProfile to a proper Distribution
	public void normalize(){
		//TOTEST
		
		Double normalFactor=this.getTotalProbMass();

		Iterator<Entry<String, Double>> iter=profile.entrySet().iterator();
		HashMap<String, Double> normalizedMap = new HashMap<String , Double>();
		while(iter.hasNext()){
			Entry<String, Double> currentEntry = iter.next();
			String key = currentEntry.getKey();
			Double val1 = currentEntry.getValue();
			normalizedMap.put(key, val1/normalFactor);

		}
		
		
		this.profile= normalizedMap;
		
		
		
	}
	
	public void sumOtherProfile(RelationProfile profileToAdd){
		//TODO
		
		
		HashMap<String, Double> resultMap=this.profile;
		HashMap<String, Double> mapToAdd=profileToAdd.profile;
		
		Iterator<Entry<String, Double>> iter = mapToAdd.entrySet().iterator();

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
		this.profile=resultMap;
		
		
	}
	
	
	
	public Double getTotalProbMass(){
	
		Iterator<Entry<String, Double>> iter = this.profile.entrySet().iterator();
		Double totalMass=0.0;
		while(iter.hasNext()){
			Entry<String, Double> currentEntry = iter.next();

			totalMass+=currentEntry.getValue();
		}
		
		return totalMass;
		
	}

	public Double findSimilarityLevel(RelationProfile otherProfile,SimilarityMeasure measure){
		return measure.calcSimilarity(this.profile,otherProfile.profile);
		
	}

	public void printSortedProfile(){
		//TODO
		LinkedList<Entry<String,Double>> resultList=new LinkedList<Entry<String,Double>>();
		resultList=virtuosoBridgeTools.hashMapToSortedLinkedList(this.profile);
		for(Entry<String,Double> e:resultList){
			System.out.println(e.getKey()+" "+e.getValue());
		}
		
	}

	public void smooth(Set<String> terms){
		//TODO
		Double smoothingMass=0.0;
		Double totalMassAdded=0.0;
		
		Iterator<String> iter=terms.iterator();
		while (iter.hasNext()){
			String currentTerm=iter.next();
			Double currentValue=0.0;

			if(this.profile.containsKey(currentTerm)){
				currentValue=this.profile.get(currentTerm);
			}

			this.profile.put(currentTerm, currentValue+smoothingMass);
			totalMassAdded+=smoothingMass;
		}

		
		
		
	}


	public String toString(){
		String lineSeparator= System.getProperty("line.separator");		
		String result="";		
		for (String name: this.profile.keySet()){			
			String value = this.profile.get(name).toString();  
			String line=name+" "+value;
			result+=line+lineSeparator;
		} 		
		return result;
	}

	
	
	public HashMap<String,Double> getProfile(){
		return this.profile;
	}
	
}
