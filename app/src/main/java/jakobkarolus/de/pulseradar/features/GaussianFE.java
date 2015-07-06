package jakobkarolus.de.pulseradar.features;

import android.util.Log;

import java.util.List;

/**
 * Created by Jakob on 02.07.2015.
 */
public class GaussianFE extends FeatureExtractor{


    public GaussianFE(FeatureProcessor featProc) {
        super(featProc);
    }

    @Override
    public void onHighFeatureDetected(UnrefinedFeature uF) {

        GaussianFeature gf = fitGaussian(uF);
        if(gf != null)
            getFeatProc().processFeature(fitGaussian(uF));

    }

    private GaussianFeature fitGaussian(UnrefinedFeature uF){

        if(uF.getEndTime() -uF.getStartTime() <= 0)
            return null;

        double[] y = toArray(uF.getUnrefinedFeature());

        double[] x = getRange(uF.getStartTime(), uF.getEndTime());
        double mu = (x[x.length-1] + x[0]) / 2.0;

        double variance = 0.0;
        for(double d: x){
            variance += ((d-mu)*(d-mu));
        }
        variance /= uF.getUnrefinedFeature().size();
        double sigma = Math.sqrt(variance);

        x = normpdf(x, mu, sigma);
        double innerProd1 = innerProduct(x, x) + 1e-6;
        double innerProd2 = innerProduct(x, y);
        double weight = (1.0/innerProd1)*innerProd2;

        if(Double.isNaN(weight)){
            Log.e("GFE", "NaN");
        }

        return new GaussianFeature(mu, sigma, weight);
    }

    @Override
    public void onLowFeatureDetected(UnrefinedFeature uF) {

        GaussianFeature gf = fitGaussian(uF);
        if(gf != null)
            getFeatProc().processFeature(new GaussianFeature(gf.getMu(), gf.getSigma(), gf.getWeight()*(-1)));

    }


    private double[] getRange(long start, long end) {
        int length = (int) (end-start+1);
        double[] range = new double[length];
        for(int i=0; i < length; i++)
            range[i] = (double) start+i;
        return range;
    }

    private double[] toArray(List<Double> list) {
        double[] array = new double[list.size()];
        for(int i=0; i < array.length; i++)
            array[i] = list.get(i);
        return array;
    }


    private double[] normpdf(double[] x, double mu, double sigma){
        double[] y = new double[x.length];
        for(int i=0; i < x.length; i++){
            y[i] = Math.exp(-0.5*Math.pow((x[i]-mu)/sigma, 2)) / (Math.sqrt(2*Math.PI)*sigma);
        }
        return y;
    }

    private double innerProduct(double[] vec1, double[] vec2){
        double innerProdut = 0.0;
        for(int i=0; i < vec1.length; i++){
            innerProdut += vec1[i]*vec2[i];
        }
        return innerProdut;
    }
}
