package virtuosoBridge;

import java.util.HashMap;

public abstract class SimilarityMeasure {


	public SimilarityMeasure() {

	}
	
	public abstract Double calcSimilarity(HashMap<String,Double> set1,HashMap<String,Double> set2);

}