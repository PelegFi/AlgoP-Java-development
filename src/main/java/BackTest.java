import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import com.ib.client.Bar;
import com.ib.client.Contract;
import com.ib.client.EClient;


public class BackTest {
	public static Double calculateProfitPercentage(double startPrice, double endPrice, String tradeAction) {
		Double percentage;
		Double PnL;
		if (tradeAction.equals("BUY")) {
			if (startPrice < endPrice) {
				PnL=endPrice-startPrice;
				percentage = (PnL / startPrice ) * 100;
			} else if (startPrice > endPrice) {
				PnL=startPrice-endPrice;
				percentage = -((PnL / startPrice) * 100);
			} else {
				percentage = 0.0;
			}
		} else if (tradeAction.equals("SELL")) {
			if (startPrice > endPrice) {
				PnL=startPrice-endPrice;
				percentage = (PnL / startPrice) * 100;
			} else if (startPrice < endPrice) {
				PnL=endPrice-startPrice;
				percentage = -((PnL / startPrice) * 100);
			} else {
				percentage = 0.0;
			}
		} else {
			// Invalid trade action, return a special value to indicate an error.
			percentage = Double.NaN;
		}

		return percentage;
	}
	public static Double calculateCAGR(ArrayList<Double> closingPrices, Double amountOfTimeFrame){
		Double startPrice= closingPrices.get(0);
		Double endPrice= closingPrices.get(closingPrices.size() - 1);
		return (Math.pow((endPrice/startPrice),(1/amountOfTimeFrame))-1);
	}
	public static void backTestStrategy2(Ewrapperimpl myWrapper, EClient m_client,boolean longShort ,boolean inverseStrategy, HashMap <Integer,ArrayList<Bar>> barsDict, Double initialRiskPrecent, int MACDfast, int MACDslow, int signalLineWindow, int RSIwindow,int minimumCandles,String excelFilePath,String backTestingTime,String backTestStartDate,String loadEsitindExcelFilePath) throws IOException {
		//EXYTRACTING PRICES + DATES FROM BARS +implent
		ArrayList<Double> prices = new ArrayList<>();
		ArrayList<String> dates = new ArrayList<>();
		ExcelWriter excelWriter = new ExcelWriter(excelFilePath,true,2,loadEsitindExcelFilePath);
		for(int i=0;i<barsDict.get(999).size();i++) {
			prices.add(barsDict.get(999).get(i).close());
			dates.add(barsDict.get(999).get(i).time());
		}
		int rowCounter=1;
		//INITIALIZE INDICATORZ
		ArrayList<Double> returns = new ArrayList<>();
		ArrayList<Double> macd = new ArrayList<>(Indicators.calculateMACD(prices, MACDslow, MACDfast, false));
		ArrayList<Double> signalLine=new ArrayList<>(Indicators.calculateMovingAverage(macd, signalLineWindow));
		ArrayList<Double> rsi = new ArrayList<>(Indicators.calculateRSI(prices, RSIwindow));

		//printing results
		System.out.println("SIZE: " + prices.size() + " || " + prices);
		System.out.println("SIZE: " + macd.size() + " || " + macd);
		System.out.println("SIZE: " + signalLine.size() + " || " + signalLine);
		System.out.println("SIZE: " + rsi.size() + " || " + rsi);

		//initial variables for the loop
		boolean isInDeal = false;
		Double totalReturn = 0.0;
		Double currentReturn = 0.0;
		int dealCount = 0;
		double startPrice = 0.0;
		double endPrice;
		double currentPrice;
		double currentMacd;
		double currentSignalLine;
		double currentRsi;
		String currentPositionType = "";
		String currentDate = "";

		//back testing loop -> strategy  1 :
		for (int i = minimumCandles; i < prices.size(); i++) {
			currentPrice=prices.get(i);;
			currentMacd=macd.get(i);
			currentSignalLine= signalLine.get(i);
			currentRsi=rsi.get(i);
			currentDate=dates.get(i);

			if(longShort) {
				if(!isInDeal) {
					if(Strategy.buyStrategy2(currentMacd,currentSignalLine,currentRsi,inverseStrategy)) {	
						startPrice=currentPrice;
						isInDeal = true;
						currentPositionType="BUY";
						dealCount++;
						System.out.println("FIRST TRADE TYPE : "+currentPositionType+" || CURRENT PRICE : " + currentPrice+" || CURRENT DATE : "+currentDate + " || MACD : " +currentMacd + " || SIGNALLINE : " + currentSignalLine + " || RSI : " + currentRsi);
					}
					else{
						startPrice=currentPrice;
						isInDeal = true;
						currentPositionType="SELL";
						System.out.println("FIRST TRADE TYPE : "+currentPositionType+" || CURRENT PRICE : " + currentPrice +" || CURRENT DATE : "+currentDate+ " || MACD : " +currentMacd + " || SIGNALLINE : " + currentSignalLine + " || RSI : " + currentRsi);
					}
				}
				else {
					if(currentPositionType.equals("BUY")) {
						if(!Strategy.buyStrategy2(currentMacd, currentSignalLine, currentRsi,inverseStrategy)){
							currentReturn=BackTest.calculateProfitPercentage(startPrice,currentPrice,currentPositionType);
							totalReturn+=currentReturn;
							returns.add(currentReturn);
							System.out.print("CURRENT POSITION : "+currentPositionType+" || CURRENT DEAL RETURN : "+currentReturn+" %"+" || CURRENT DATE : "+currentDate);
							currentPositionType="SELL";
							startPrice=currentPrice;
							dealCount++;
							System.out.println(" || NEW POSITION : "+currentPositionType+" || CURRENT PRICE : " + currentPrice + " || MACD : " +currentMacd + " || SIGNALLINE : " + currentSignalLine + " || RSI : " + currentRsi);
							if(currentReturn<-initialRiskPrecent){
								System.out.println("!!!! STOP LOSS JUMPED !!!!");
							}
						}
					}
					else {
						if(Strategy.buyStrategy2(currentMacd, currentSignalLine, currentRsi,inverseStrategy)){
							currentReturn=BackTest.calculateProfitPercentage(startPrice,currentPrice,currentPositionType);
							totalReturn+=currentReturn;
							returns.add(currentReturn);
							System.out.print("CURRENT POSITION : "+currentPositionType+" || CURRENT DEAL RETURN : "+currentReturn+" %"+" || CURRENT DATE : "+currentDate);
							currentPositionType="BUY";
							startPrice=currentPrice;
							dealCount++;
							System.out.println(" || NEW POSITION: "+currentPositionType+" || CURRENT PRICE : " + currentPrice + " || MACD : " +currentMacd + " || SIGNALLINE : " + currentSignalLine + " || RSI : " + currentRsi);
							if(currentReturn<-initialRiskPrecent){
								System.out.println("!!!! STOP LOSS JUMPED !!!!");
							}
						}
					}
				}
			}
			else {
				if(!isInDeal) {
					if(Strategy.buyStrategy2(currentMacd, currentSignalLine, currentRsi,inverseStrategy)) {
						currentPositionType="BUY";
						startPrice=currentPrice;
						isInDeal = true;
						dealCount++;
						System.out.println("CURRENT ACTION : BUY"+"|| CURRENT PRICE : " + currentPrice +" || CURRENT DATE : "+currentDate+ " || MACD : " +currentMacd + " || SIGNALLINE : " + currentSignalLine + " || RSI : " + currentRsi);
					}
				}
				else{
					if(!Strategy.buyStrategy2(currentMacd, currentSignalLine, currentRsi,inverseStrategy)) {
						currentReturn=BackTest.calculateProfitPercentage(startPrice,currentPrice,currentPositionType);
						returns.add(currentReturn);
						totalReturn+=currentReturn;
						isInDeal=false;
						dealCount++;
						System.out.println("CURRENT ACTION : SELL"+"|| CURRENT PRICE : " + currentPrice+" || CURRENT DATE : "+currentDate +" || CURRENT DEAL RETURN : "+currentReturn+" %"+ " || MACD : " +currentMacd + " || SIGNALLINE : " + currentSignalLine + " || RSI : " + currentRsi);
						if(currentReturn<-initialRiskPrecent){
							System.out.println("!!!! STOP LOSS JUMPED !!!!");
						}
					}
				}
			}
		}
		System.out.println("real pnl : "+totalReturn);
		System.out.println("my compund return is " + Actions.calculateCompoundPnL(returns) + " %");
		System.out.println("deal count is: " + dealCount);
		//write data in to excel
		ExcelWriter.writeBacktestPnLStrategy2(excelWriter,rowCounter,MACDslow,MACDfast,signalLineWindow,RSIwindow,Actions.calculateCompoundPnL(returns),totalReturn,backTestingTime,backTestStartDate);
	}
	public static double roundToDecimalPlaces(double value, int decimalPlaces) {
		double scale = Math.pow(10, decimalPlaces);
		return Math.round(value * scale) / scale;
	}
	public static void backtestStrategy1(Ewrapperimpl myWrapper, EClient m_client, String excelFilePath , Contract myContract,String backTestStartDate,String backTestingTime,String candlesTimeFrame,int ZscroeWindow,int smaZwindow,String loadEsitindExcelFilePath) throws IOException, InterruptedException {
		//variabels
		HashMap<Integer, ArrayList<Bar>> barsDict;
		ArrayList <Double> closingPrices;
		ArrayList<Double> Zscore;
		ArrayList <Double> smaZ;
		ArrayList <String> dates = new ArrayList<>();
		ExcelWriter excelWriter = new ExcelWriter(excelFilePath,false,1,loadEsitindExcelFilePath);
		int rowCounter=excelWriter.getCurrentSheet().getLastRowNum()+1;
		ArrayList <Double> returns = loadEsitindExcelFilePath.equals("") ? new ArrayList<>() : new ArrayList<>(Collections.singleton(Double.valueOf(excelWriter.getCellValue(0, rowCounter - 1, 7))));
		int minimumCandles = Actions.calculateMinimumCandlesStrategy1(ZscroeWindow,smaZwindow);

		//reuqesting historical data + watiing for data
		m_client.reqHistoricalData(7,myContract,backTestStartDate,backTestingTime,candlesTimeFrame,"MIDPOINT",1,1,false,null);
		Actions.waitingForHistData(myWrapper);

		//apllying data into lists
		barsDict=myWrapper.getBarsDict();
		closingPrices=myWrapper.getClosingPrices(7);
		if (closingPrices.size()<minimumCandles){
			System.out.println("!!!!! need more data - not enough candles ");
		} else{
			System.out.println("PRICES || SIZE: " + closingPrices.size() + " || " + closingPrices);
		}
		for(int i=0;i<barsDict.get(7).size();i++) {
			dates.add(barsDict.get(7).get(i).time());
		}

		//calculating Zscore+smaZ
		Zscore = Indicators.calculateMovingZscore(closingPrices,ZscroeWindow);
		System.out.println("ZSCORE || SIZE: " + Zscore.size() + " || " + Zscore);
		smaZ = Indicators.calculateExponentialMovingAverage(Zscore,smaZwindow);
		System.out.println("SMAZ || SIZE: " + smaZ.size() + " || " + smaZ);
		
		//LOOP VARIABELS
		boolean isInDeal=false;
		Double currentZscore;
		Double currentSmaZ;
		double startPrice = 0.0;
		int dealCount = 0;
		double currentPrice;
		String currentBuyStrategy;
		String currentPositionType = "";
		String currentDate;

		//THE BACKTEST LOOP PART
		for (int i=minimumCandles;i< closingPrices.size();i++){
			//current loop variabels
			currentDate = dates.get(i);
			currentPrice= closingPrices.get(i);
			currentZscore=Zscore.get(i);
			currentSmaZ=smaZ.get(i);
			currentBuyStrategy=Strategy.buyStrategy1(currentZscore,currentSmaZ);

			//the strategy part
			if (!isInDeal){
				if (currentBuyStrategy.equals("BUY")){
					isInDeal=true;
					startPrice=currentPrice;
					dealCount++;
					currentPositionType="BUY";
					System.out.println("ACTION : "+currentPositionType+" || Current price : "+currentPrice+" || Current Zscore : "+currentZscore+" || Current SMAZ : "+currentSmaZ +" || DATE : "+currentDate);
					//WRITE DATA TO EXCEL
					ExcelWriter.writeStrategy1PnL(excelWriter,rowCounter,true,currentPositionType,String.valueOf(currentPrice),currentBuyStrategy,String.valueOf(currentZscore),currentSmaZ.toString(),"0","0",currentDate);
					rowCounter++;
				} else if (currentBuyStrategy.equals("SELL")) {
					isInDeal=true;
					startPrice=currentPrice;
					dealCount++;
					currentPositionType="SELL";
					System.out.println("ACTION : "+currentPositionType+" || Current price : "+currentPrice+" || Current Zscore : "+currentZscore+" || Current SMAZ : "+currentSmaZ+" || DATE : "+currentDate);
					//WRITE DATA TO EXCEL
					ExcelWriter.writeStrategy1PnL(excelWriter,rowCounter,true,currentPositionType,String.valueOf(currentPrice),currentBuyStrategy,String.valueOf(currentZscore),currentSmaZ.toString(),"0","0",currentDate);
					rowCounter++;
				}
			}
			else{
				if (currentPositionType.equals("BUY")){
					if (!currentBuyStrategy.equals("BUY")){
						isInDeal=false;
						dealCount++;
						returns.add(BackTest.calculateProfitPercentage(startPrice,currentPrice,currentPositionType));
						currentPositionType="";
						System.out.println("ACTION : SELL"+" || Current price : "+currentPrice+" || Current Zscore : "+currentZscore+" || Current SMAZ : "+currentSmaZ+" || DATE : "+currentDate+"\n ************************************************************");
						//WRITE DATA TO EXCEL
						ExcelWriter.writeStrategy1PnL(excelWriter,rowCounter,true,"SELL",String.valueOf(currentPrice),currentBuyStrategy,String.valueOf(currentZscore),currentSmaZ.toString(), String.valueOf(returns.stream().mapToDouble(Double::doubleValue).sum()),Actions.calculateCompoundPnL(returns).toString(),currentDate);
						rowCounter++;
					}
				} else if (currentPositionType.equals("SELL")) {
					if (!currentBuyStrategy.equals("SELL")){
						isInDeal=false;
						dealCount++;
						returns.add(BackTest.calculateProfitPercentage(startPrice,currentPrice,currentPositionType));
						currentPositionType="";
						System.out.println("ACTION : BUY"+" || Current price : "+currentPrice+" || Current Zscore : "+currentZscore+" || Current SMAZ : "+currentSmaZ+" || DATE : "+currentDate+"\n ************************************************************");
						ExcelWriter.writeStrategy1PnL(excelWriter,rowCounter,true,"BUY",String.valueOf(currentPrice),currentBuyStrategy,String.valueOf(currentZscore),currentSmaZ.toString(), String.valueOf(returns.stream().mapToDouble(Double::doubleValue).sum()),Actions.calculateCompoundPnL(returns).toString(),currentDate);
						rowCounter++;
					}
				}
			}
		}
		//calculate end results
		double totalReturn =returns.stream().mapToDouble(Double::doubleValue).sum();
		Double compoundReturn = Actions.calculateCompoundPnL(returns);
		System.out.println("pnl : "+totalReturn);
		System.out.println("my compund return is " + compoundReturn + " %");
		System.out.println("deal count is: " + dealCount);

		//calculate winRate
		int win = 0,lose = 0;
		for (Double currentReturn : returns){
			if (currentReturn>0)
			{
				win++;
			}
			else if (currentReturn<0){
				lose++;
			}
		}
		System.out.println("WINS  : "+win);
		System.out.println("LOSE  : "+lose);

	}
	public static void backtestStrategy1Reversed(Ewrapperimpl myWrapper, EClient m_client, String excelFilePath , Contract myContract,String backTestStartDate,String backTestingTime,String candlesTimeFrame,int ZscroeWindow,int smaZwindow,String loadEsitindExcelFilePath) throws IOException, InterruptedException {
		//variabels
		HashMap<Integer, ArrayList<Bar>> barsDict;
		ArrayList <Double> closingPrices;
		ArrayList<Double> Zscore;
		ArrayList <Double> smaZ;
		ArrayList <String> dates = new ArrayList<>();
		ArrayList <Double> returns = new ArrayList<>();
		ExcelWriter excelWriter = new ExcelWriter(excelFilePath,true,1,loadEsitindExcelFilePath);
		int minimumCandles = Actions.calculateMinimumCandlesStrategy1(ZscroeWindow,smaZwindow);

		//reuqesting historical data + watiing for data
		m_client.reqHistoricalData(7,myContract,backTestStartDate,backTestingTime,candlesTimeFrame,"MIDPOINT",1,1,false,null);
		Actions.waitingForHistData(myWrapper);

		//apllying data into lists
		barsDict=myWrapper.getBarsDict();
		closingPrices=myWrapper.getClosingPrices(7);
		if (closingPrices.size()<minimumCandles){
			System.out.println("!!!!! need more data - not enough candles ");
		} else{
			System.out.println("PRICES || SIZE: " + closingPrices.size() + " || " + closingPrices);
		}
		for(int i=0;i<barsDict.get(7).size();i++) {
			dates.add(barsDict.get(7).get(i).time());
		}

		//calculating Zscore+smaZ
		Zscore = Indicators.calculateMovingZscore(closingPrices,ZscroeWindow);
		System.out.println("ZSCORE || SIZE: " + Zscore.size() + " || " + Zscore);
		smaZ = Indicators.calculateExponentialMovingAverage(Zscore,smaZwindow);
		System.out.println("SMAZ || SIZE: " + smaZ.size() + " || " + smaZ);

		//LOOP VARIABELS
		boolean isInDeal=false;
		Double currentZscore;
		Double currentSmaZ;
		double startPrice = 0.0;
		int dealCount = 0;
		double currentPrice;
		String currentBuyStrategy;
		String currentPositionType = "";
		String currentDate;

		//THE BACKTEST LOOP PART
		for (int i=minimumCandles;i< closingPrices.size();i++){
			//current loop variabels
			currentDate = dates.get(i);
			currentPrice= closingPrices.get(i);
			currentZscore=Zscore.get(i);
			currentSmaZ=smaZ.get(i);
			currentBuyStrategy=Strategy.buyStrategy1(currentZscore,currentSmaZ);

			//the strategy part
			if (!isInDeal){
				if (currentBuyStrategy.equals("BUY")){
					isInDeal=true;
					startPrice=currentPrice;
					dealCount++;
					currentPositionType="SELL";
					System.out.println("ACTION : "+currentPositionType+" || Current price : "+currentPrice+" || Current Zscore : "+currentZscore+" || Current SMAZ : "+currentSmaZ +" || DATE : "+currentDate);
				} else if (currentBuyStrategy.equals("SELL")) {
					isInDeal=true;
					startPrice=currentPrice;
					dealCount++;
					currentPositionType="BUY";
					System.out.println("ACTION : "+currentPositionType+" || Current price : "+currentPrice+" || Current Zscore : "+currentZscore+" || Current SMAZ : "+currentSmaZ+" || DATE : "+currentDate);
				}
			}
			else{
				if (currentPositionType.equals("BUY")){
					if (currentBuyStrategy.equals("BUY")){
						isInDeal=false;
						dealCount++;
						returns.add(BackTest.calculateProfitPercentage(startPrice,currentPrice,currentPositionType));
						System.out.println("ACTION : SELL"+" || Current price : "+currentPrice+" || Current Zscore : "+currentZscore+" || Current SMAZ : "+currentSmaZ+" || DATE : "+currentDate+"\n ************************************************************");
						currentPositionType="";
					}
				} else if (currentPositionType.equals("SELL")) {
					if (currentBuyStrategy.equals("SELL")){
						isInDeal=false;
						dealCount++;
						returns.add(BackTest.calculateProfitPercentage(startPrice,currentPrice,currentPositionType));
						System.out.println("ACTION : BUY"+" || Current price : "+currentPrice+" || Current Zscore : "+currentZscore+" || Current SMAZ : "+currentSmaZ+" || DATE : "+currentDate+"\n ************************************************************");
						currentPositionType="";
					}
				}
			}
		}
		//calculate end results
		double totalReturn =returns.stream().mapToDouble(Double::doubleValue).sum();
		Double compoundReturn = Actions.calculateCompoundPnL(returns);
		System.out.println("pnl : "+totalReturn);
		System.out.println("my compund return is " + compoundReturn + " %");
		System.out.println("deal count is: " + dealCount);
		//calculate winRate
		int win = 0,lose = 0;
		for (Double currentReturn : returns){
			if (currentReturn>0)
			{
				win++;
			}
			else if (currentReturn<0){
				lose++;
			}
		}
		System.out.println("WINS  : "+win);
		System.out.println("LOSE  : "+lose);
	}
}
