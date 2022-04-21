import jsat.classifiers.ClassificationDataSet;
import jsat.classifiers.DataPointPair;
import jsat.classifiers.linear.LogisticRegressionDCD;
import jsat.utils.random.RandomUtil;

public class ServerML{

    public ServerML(){

    }

    public static void main(String[] args){
        System.out.println("trainC");
        ClassificationDataSet train = FixedProblems.get2ClassLinear(200, RandomUtil.getRandom());

        LogisticRegressionDCD lr = new LogisticRegressionDCD();
        lr.train(train, true);

        ClassificationDataSet test = FixedProblems.get2ClassLinear(200, RandomUtil.getRandom());

        for(DataPointPair<Integer> dpp : test.getAsDPPList())
            assertEquals(dpp.getPair().longValue(), lr.classify(dpp.getDataPoint()).mostLikely());
    }

}