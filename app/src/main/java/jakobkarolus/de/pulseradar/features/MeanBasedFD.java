package jakobkarolus.de.pulseradar.features;

import jakobkarolus.de.pulseradar.algorithm.AlgoHelper;

/**
 * Created by Jakob on 02.07.2015.
 */
public class MeanBasedFD extends FeatureDetector{

    private int fftLength;
    private int hopSize;
    private int carrierIdx;
    private double halfCarrierWidth;
    private double magnitudeThreshold;
    private double featHighThreshold;
    private double featLowThreshold;
    private int featSlackWidth;
    private int currentSlack;

    private double[] carryOver;

    public MeanBasedFD(int fftLength, int hopSize, int carrierIdx, double halfCarrierWidth, double magnitudeThreshold, double featHighThreshold, double featLowThreshold, int featSlackWidth) {
        super();
        this.fftLength = fftLength;
        this.hopSize = hopSize;
        this.carrierIdx = carrierIdx;
        this.halfCarrierWidth = halfCarrierWidth;
        this.magnitudeThreshold = magnitudeThreshold;
        this.featHighThreshold = featHighThreshold;
        this.featLowThreshold = featLowThreshold;
        this.featSlackWidth = featSlackWidth;
        this.currentSlack = 0;
        this.carryOver = new double[hopSize];
    }

    @Override
    public void checkForFeatures(double[] audioBuffer) {

        //buffer is assumed to be a multiple of 4096, plus added hopSize from the previous buffer
        double[] tempBuffer = new double[audioBuffer.length+hopSize];
        System.arraycopy(carryOver, 0, tempBuffer, 0, hopSize);
        System.arraycopy(audioBuffer, 0, tempBuffer, hopSize, audioBuffer.length);
        //save the carry-over for the next buffer
        System.arraycopy(audioBuffer, audioBuffer.length-hopSize, carryOver, 0, hopSize);

        double[] buffer = new double[fftLength];
        for(int i=0; i <= tempBuffer.length - fftLength; i+=hopSize){
            System.arraycopy(tempBuffer, i, buffer, 0, fftLength);
            double valueForTimeStep = meanExtraction(AlgoHelper.fftMagnitude(buffer), carrierIdx, halfCarrierWidth);
            processFeatureValue(valueForTimeStep);
        }


    }

    private void processFeatureValue(double valueForTimeStep) {
        if(valueForTimeStep >= featHighThreshold){
            if(!getCurrentFeature().hasStarted()){
                //start a new feature
                getCurrentFeature().setHasStarted(true);
                currentSlack = 0;
                getCurrentFeature().addTimeStep(valueForTimeStep);
            }
            else{
                //reset slack
                currentSlack = 0;
                getCurrentFeature().addTimeStep(valueForTimeStep);
            }
        }
        else{
            if(getCurrentFeature().hasStarted()){
                //is it also below the low threshold
                if(valueForTimeStep < featLowThreshold){
                    //check for slack
                    if(currentSlack < featSlackWidth){
                        //ignore this one and go on
                        currentSlack++;
                        getCurrentFeature().addTimeStep(valueForTimeStep);
                    }
                    else{
                        //if it below low_threshold and no slack left -> finish it
                        //these means we have used all our slack to no avail -> remove them to get the correct feature size
                        if(getCurrentFeature().removeSlackedSteps(featSlackWidth))
                            notifyFeatureDetected();
                    }
                }
                else{
                    //below threshold is fine for already started features
                    getCurrentFeature().addTimeStep(valueForTimeStep);
                }
            }
        }
    }

    private double meanExtraction(double[] values, int carrierIdx, double halfCarrierWidth) {
        double meanWeights = 0.0;
        double mean = 0.0;
        for(int i=carrierIdx + (int) Math.ceil(halfCarrierWidth); i < values.length; i++){
            if(values[i] > magnitudeThreshold){
                mean += i*values[i];
                meanWeights += values[i];
            }
        }
        if(Math.abs(0.0 - meanWeights) > 1e-6)
            mean /= meanWeights;

        return mean;
    }
}