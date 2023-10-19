import java.util.ArrayList;

import com.ib.client.*;

public class Strategy {
	public static String buyStrategy1(Double currentZscore , Double currentSmaZ){
		if(currentZscore<0&&currentZscore<currentSmaZ){
			return "SELL";
		} else if(currentZscore>0&&currentZscore>currentSmaZ){
			return "BUY";
		}
		else {
			return "";
		}
	}
	public static boolean buyStrategy2(Double macd , Double signalLine , Double rsi ,boolean inverse){
		if(!inverse) {
			if (macd>signalLine && rsi>40){
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (macd>signalLine && rsi>40){
				return false;
			}
			else {
				return true;
			}
		}
	}
	public static boolean seconderyStrategy(Ewrapperimpl myWrapper,EClient m_client,int capital,Double initialRiskPrecent, Contract contract,String CandlesTimeFrame,int macdSlow,int macdFast,int SIGNALLINE,int RSI,boolean inverseStrategy,boolean longShort,String seconderyCandlesTimeFrame,int amountOfSeconderyCandles) throws InterruptedException {
		//VARIABLES : 
		ArrayList<Double> prices = new ArrayList<>();
		ArrayList<Double> macd = new ArrayList<>();
		ArrayList<Double> signalLine = new ArrayList<>();
		ArrayList<Double> rsi = new ArrayList<>();
		ArrayList<Double> seconderyPrices = new ArrayList<>();
		int minimumCandles = Actions.calculateMinimumCandlesStrategy2(macdFast, macdSlow, SIGNALLINE, RSI);
		String minimumDuration = Actions.calculateMinimumDuration(CandlesTimeFrame, minimumCandles);
		String minimumDurationSeconderyCandles=Actions.calculateMinimumDuration(seconderyCandlesTimeFrame, amountOfSeconderyCandles);
		Double currentMacd;
		Double currentSignalLine;
		Double currentRsi;
		
		//PRICE LIST EXTABLISHE : requesting histprical data + implanting prices list (primary + secondery)
		m_client.reqHistoricalData(1004, contract,"",minimumDuration,CandlesTimeFrame, "MIDPOINT", 1, 1, false, null);
		Actions.waitingForHistData(myWrapper);
		prices=myWrapper.getClosingPrices(1004);
		m_client.reqHistoricalData(1005, contract,"",minimumDurationSeconderyCandles,seconderyCandlesTimeFrame, "MIDPOINT", 1, 1, false, null);
		Actions.waitingForHistData(myWrapper);
		
		//adding scondery timefraems bars
//		System.out.println("SIZE : "+prices.size() +"|| prices before : "+prices);
		seconderyPrices=myWrapper.getClosingPrices(1005);
		int counter=0;
		for (int i=(prices.size()-seconderyPrices.size());i<prices.size();i++) {
			prices.set(i,seconderyPrices.get(counter));
			counter++;
		}
//		System.out.println("SIZE : "+prices.size() +"|| prices after : "+prices);
		
		//CALCULATING INDICATORS 
		macd=Indicators.calculateMACD(prices,macdSlow,macdFast,false);
		signalLine=Indicators.calculateMovingAverage(macd,SIGNALLINE);
		rsi=Indicators.calculateRSI(prices,RSI);

		
		//SETTING UP VARIABELS
		currentMacd=macd.get(macd.size()-1);
		currentSignalLine= signalLine.get(signalLine.size()-1);
		currentRsi=rsi.get(rsi.size()-1);
		
		//THE STRATEGY PART 
		if(Strategy.buyStrategy2(currentMacd, currentSignalLine, currentRsi, inverseStrategy)) {
			System.out.println("SECOND STRATEGY : TRUE"+"||CURRENT MACD :"+currentMacd+"|| CURRENT SIGNLALINE : "+currentSignalLine+"CURRENT RSI"+currentRsi);
			myWrapper.resetBarsDict();
			return true;
		}
		else {
			System.out.println("SECOND STRATEGY : FALSE"+"||CURRENT MACD :"+currentMacd+"|| CURRENT SIGNLALINE : "+currentSignalLine+"CURRENT RSI"+currentRsi);
			myWrapper.resetBarsDict();
			return false;
		}
	}
}