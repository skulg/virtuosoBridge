package virtuosoBridge;

import java.util.HashMap;
import java.util.Set;


/*
 * Class that describe a relationProfile and its operations
 */
public class RelationProfile {

	private HashMap<String,Double> profile;


	public RelationProfile(HashMap<String,Double> relProfile) {
		this.profile=relProfile;
	}

	public void normalize(){
		//TODO
	}
	
	public RelationProfile sumOtherProfile(RelationProfile profileToAdd){
		//TODO
		
		return null;
	}
	
	public Double getTotalProbMass(){
		//TODO
		return null;
	}

	public Double findSimilarityLevel(RelationProfile otherProfile){
		//TODO
		return null;
	}

	public void printSortedProfile(){
		//TODO
	}

	public void smooth(Set<String> terms){
		//TODO
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
