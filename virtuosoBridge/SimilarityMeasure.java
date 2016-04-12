package virtuosoBridge;

import java.util.HashMap;

public abstract class SimilarityMeasure {


	public SimilarityMeasure() {

	}
	
	public abstract Double calcSimilarity(RelationProfile profile1,RelationProfile profile2,RelationProfile normalProfile);

}