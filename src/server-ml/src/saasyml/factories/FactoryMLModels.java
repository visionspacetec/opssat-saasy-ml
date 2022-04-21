package saasyml.factories;

import jsat.classifiers.Classifier;
import jsat.classifiers.linear.LogisticRegressionDCD;

public class FactoryMLModels {
    public static Classifier build(String modelName) {

        switch (modelName){
            case "LogisticRegressionDCD" : return new LogisticRegressionDCD();
            default:
                return new LogisticRegressionDCD();
        }
    }
}
