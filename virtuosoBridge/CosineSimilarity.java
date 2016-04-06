package virtuosoBridge;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class CosineSimilarity extends SimilarityMeasure {

	public CosineSimilarity() {
		super();
	}

	@Override
	public Double calcSimilarity(HashMap<String,Double> set1,HashMap<String,Double> set2) {

		Set<String> terms=virtuosoBridgeTools.getAllKeysFrom2HashMap(set1, set2);

		set1=virtuosoBridgeTools.smoothRelProfile(set1,terms);
		set2=virtuosoBridgeTools.smoothRelProfile(set2,terms);


		Iterator<String> iter=terms.iterator();
		Double numerator=0.0;
		Double aSquared=0.0;
		Double bSquared=0.0;
		while (iter.hasNext()){
			String currentEntry=iter.next();
			Double val1=set1.get(currentEntry);
			Double val2=set2.get(currentEntry);
			numerator+=(val1*val2);
			aSquared+=val1*val1;
			bSquared+=val2*val2;
		}
		Double denominator= Math.sqrt(aSquared)*Math.sqrt(bSquared);

		Double cosSimilarity=numerator/denominator;

		return cosSimilarity;

	}

}
