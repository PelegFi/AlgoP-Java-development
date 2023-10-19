import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Indicators {
    public static ArrayList<Double> calculateExponentialMovingAverage(ArrayList<Double> prices, int windowSize) {
        ArrayList<Double> ema = new ArrayList<>(Collections.nCopies(prices.size(), 0.0));
        double smoothing = 2.0 / (windowSize + 1);

        // Start from the first price
        ema.set(0, prices.get(0));

        for (int i = 1; i < prices.size(); i++) {
            ema.set(i, smoothing * prices.get(i) + (1 - smoothing) * ema.get(i - 1));
        }

        return ema;
    }
    public static ArrayList<Double> calculateRSI(ArrayList<Double> prices, int windowSize) {
        ArrayList<Double> gains = new ArrayList<>();
        gains.add(0.0);
        ArrayList<Double> losses = new ArrayList<>();
        losses.add(0.0);
        ArrayList<Double> avgGains = new ArrayList<>(Collections.nCopies(prices.size(), 0.0));
        ArrayList<Double> avgLosses = new ArrayList<>(Collections.nCopies(prices.size(), 0.0));

        for (int i = 1; i < prices.size(); i++) {
            double delta = prices.get(i) - prices.get(i - 1);
            if (delta >= 0) {
                gains.add(delta);
                losses.add(0.0);
            } else {
                gains.add(0.0);
                losses.add(Math.abs(delta));
            }
        }

        // calculate average gain and average loss
        for (int i = windowSize; i < prices.size(); i++) {
            if (i == windowSize) {
                avgGains.set(i, calculateAverage(gains.subList(0, windowSize)));
                avgLosses.set(i, calculateAverage(losses.subList(0, windowSize)));
            } else {
                avgGains.set(i, ((avgGains.get(i - 1) * (windowSize - 1)) + gains.get(i)) / windowSize);
                avgLosses.set(i, ((avgLosses.get(i - 1) * (windowSize - 1)) + losses.get(i)) / windowSize);
            }
        }


        ArrayList<Double> RS = new ArrayList<>(Collections.nCopies(prices.size(), 0.0));
        // calculate average gain / average loss
        for (int i = windowSize; i < avgGains.size(); i++) {
            RS.set(i, avgGains.get(i) / avgLosses.get(i));
        }

        ArrayList<Double> RSI = new ArrayList<>(Collections.nCopies(prices.size(), 0.0));
        // calculate RSI
        for (int i = windowSize; i < RS.size(); i++) {
            RSI.set(i, 100 - (100 / (1 + RS.get(i))));
        }
        return RSI;
    }
    public static double calculateAverage(List<Double> list) {
        double average , sum = 0;
        for(int i=0 ; i< list.size() ; i++){
            sum+= list.get(i);
        }
        average=sum/ list.size();
        return average;
    }
    public static ArrayList<Double> calculateMACD(ArrayList<Double> closingPrices , int movingAverageSlow, int movingAverageFast,boolean isExponential){
        ArrayList<Double> macd = new ArrayList<>();
        if(isExponential) {
        	ArrayList<Double> EMAFast= calculateExponentialMovingAverage(closingPrices,movingAverageFast);
        	ArrayList<Double> EMASlow= calculateExponentialMovingAverage(closingPrices,movingAverageSlow);
        	for (int i=0 ; i<EMASlow.size() ; i++){
                macd.add(EMAFast.get(i)-EMASlow.get(i));
        	}
        }
        else {
        	ArrayList<Double> MAFast= calculateMovingAverage(closingPrices,movingAverageFast);
        	ArrayList<Double> MASlow= calculateMovingAverage(closingPrices,movingAverageSlow);
        	for (int i=0 ; i<MASlow.size() ; i++){
                macd.add(MAFast.get(i)-MASlow.get(i));
        	}
        }
        return macd;
    }
    public static ArrayList<Double> calculateMovingAverage (ArrayList<Double> prices, int windowSize){
        ArrayList<Double> movingAverage = new ArrayList<>(Collections.nCopies(windowSize-1, 0.0));
        for (int i = windowSize - 1; i < prices.size(); i++){
            double sum = 0;
            for(int j = i - windowSize + 1; j <= i; j++){
                sum += prices.get(j);
            }
            movingAverage.add(sum/windowSize);
        }
        return movingAverage;
    }
    public static double calculateATR(ArrayList<Double> highs, ArrayList<Double> lows, ArrayList<Double> closes, int period) {
            if (highs.size() != lows.size() || highs.size() != closes.size() || highs.size() < period) {
                throw new IllegalArgumentException("Input lists must have the same size and at least as long as the period.");
            }

            double atr = 0.0;
            double previousClose = closes.get(0);

            for (int i = 1; i < period; i++) {
                double tr =  Math.max(Math.max(highs.get(i) - lows.get(i), Math.abs(highs.get(i) - previousClose)), Math.abs(lows.get(i) - previousClose));
                atr += tr;
                previousClose = closes.get(i);
            }

            atr /= period;

            for (int i = period; i < highs.size(); i++) {
                double tr = Math.max(Math.max(highs.get(i) - lows.get(i), Math.abs(highs.get(i) - previousClose)), Math.abs(lows.get(i) - previousClose));
                atr = ((atr * (period - 1)) + tr) / period;
                previousClose = closes.get(i);
            }

            return atr;
    }
    public static Map<String, Double> calculateFibonacciLevels(double high, double low) {
        Map<String, Double> fibonacciLevels = new HashMap<>();

        double difference = high - low;

        fibonacciLevels.put("23.6%", high - difference * 0.236);
        fibonacciLevels.put("38.2%", high - difference * 0.382);
        fibonacciLevels.put("50.0%", high - difference * 0.5);
        fibonacciLevels.put("61.8%", high - difference * 0.618);
        fibonacciLevels.put("78.6%", high - difference * 0.786);

        return fibonacciLevels;
    }
    public static Map<String, ArrayList<Double>> calculateBollingerBands(ArrayList<Double> closingPrices, int movingAveragePeriod, double k) {
        if (closingPrices.size() < movingAveragePeriod) {
            throw new IllegalArgumentException("Input list must have at least as many elements as the specified period.");
        }

        // Calculate standard deviation
        double standardDeviation = Indicators.calculateStandardDeviation(closingPrices);

        // Calculate MIDDLE BANDS
        ArrayList<Double> middleBand = Indicators.calculateMovingAverage(closingPrices, movingAveragePeriod);

        // Calculate UPPER BAND
        ArrayList<Double> upperBand = new ArrayList<>();
        for (int i = 0; i < closingPrices.size(); i++) {
            if (i < movingAveragePeriod) {
                upperBand.add(null);
            } else {
                upperBand.add(middleBand.get(i) + k * standardDeviation);
            }
        }

        // Calculate LOWER BAND
        ArrayList<Double> lowerBand = new ArrayList<>();
        for (int i = 0; i < closingPrices.size(); i++) {
            if (i < movingAveragePeriod) {
                lowerBand.add(null);
            } else {
                lowerBand.add(middleBand.get(i) - k * standardDeviation);
            }
        }

        Map<String, ArrayList<Double>> bollingerBands = new HashMap<>();
        bollingerBands.put("upper", upperBand);
        bollingerBands.put("middle", middleBand);
        bollingerBands.put("lower", lowerBand);

        return bollingerBands;
    }
    public static double calculateStandardDeviation(ArrayList <Double> closingPrices) {
    	double sum = 0.0;
        for (Double closePrice : closingPrices) {
            sum += closePrice;
        }
        double average = sum / closingPrices.size();

        double sumOfSquares = 0.0;
        for (Double closePrice : closingPrices) {
            sumOfSquares += Math.pow(closePrice - average, 2);
        }
        double standardDeviation = Math.sqrt(sumOfSquares / closingPrices.size());
        return standardDeviation;
    }
    public static Double calculateMean(ArrayList<Double> data) {
        double sum = 0.0;
        for (double num : data) {
            sum += num;
        }
        return sum / data.size();
    }
    public static ArrayList<Double> calculateMovingZscore(ArrayList<Double> closingPrices, int ZscoreWindow){
        ArrayList<Double> Zscore = new ArrayList<>(Collections.nCopies(ZscoreWindow-1, 0.0));
        Double average;
        Double standardDeviation;

        for(int i = ZscoreWindow-1, j = 0; i < closingPrices.size(); i++, j++){
            List<Double> sublist = closingPrices.subList(j, i+1);
            ArrayList<Double> subArrayList = new ArrayList<>(sublist); // Convert sublist to ArrayList

            average = Indicators.calculateMean(subArrayList);
            standardDeviation = Indicators.calculateStandardDeviation(subArrayList);

            Zscore.add((closingPrices.get(i) - average) / (standardDeviation + 0.0000000007));
        }

        return Zscore;
    }
}