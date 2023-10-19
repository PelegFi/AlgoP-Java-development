import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.ib.client.*;


public class Actions {
	//	public static  void placeStockOrder(Ewrapperimpl myWrapper, EClientSocket m_client , String action , String ticker , double amount , int stopPrice, int takePrice) throws InterruptedException {
//		//        //create new contract -> stocks
//		//        Contract contract = new Contract();
//		//        contract.symbol(ticker);
//		//        contract.secType("STK");
//		//        contract.currency("USD");
//		//        contract.exchange("SMART");
////		Contract contract = createContract(ticker,"STK","USD","SMART");
//
//		//create new ORDER
//		int currentOrderId= myWrapper.getCurrentOrderId();
//		Order myOrder= new Order();
//		myOrder.orderId(currentOrderId);
//		myOrder.totalQuantity(Decimal.get(amount));
//		myOrder.orderType("MKT");
//		myOrder.action(action);
//
//		// Take profit order
//		Order takeProfit = new Order();
//		takeProfit.orderId(currentOrderId + 1);
//		takeProfit.action(action.equals("BUY") ? "SELL" : "BUY");
//		takeProfit.orderType("LMT");
//		takeProfit.totalQuantity(Decimal.get(amount));
//		takeProfit.lmtPrice(takePrice);
//		takeProfit.parentId(currentOrderId); // Important: The parent order ID
//
//		// Stop loss order
//		Order stopLoss = new Order();
//		stopLoss.orderId(currentOrderId + 2);
//		stopLoss.action(action.equals("BUY") ? "SELL" : "BUY");
//		stopLoss.orderType("STP");
//		stopLoss.totalQuantity(Decimal.get(amount));
//		stopLoss.auxPrice(stopPrice);
//		stopLoss.parentId(currentOrderId);
//
//
//		//place order
//		m_client.placeOrder(myOrder.orderId(),contract,myOrder);
//		Thread.sleep(1000);
//		m_client.placeOrder(takeProfit.orderId(),contract,takeProfit);
//		Thread.sleep(1000);
//		m_client.placeOrder(stopLoss.orderId(),contract,stopLoss);
//		System.out.println("your order has been created succesfully!!");
//
//
	public static void placeOrder(Ewrapperimpl myWrapper, EClient m_client,Contract contract,String action,String orderType,Double LimitOrStopPrice ,Double initialRiskPrecent,Double currentPrice,int quantity,boolean withStopLoss,boolean withTrailing){
		int currentOrderId=myWrapper.getCurrentOrderId();

		//create order
		Order mainOrder = new Order();
		mainOrder.orderId(currentOrderId);
		mainOrder.action(action);
		mainOrder.orderType(orderType);
		if (orderType.equals("LMT")){
			mainOrder.lmtPrice(LimitOrStopPrice);
		} else if (orderType.equals("STP")) {
			mainOrder.auxPrice(LimitOrStopPrice);
		}
		mainOrder.totalQuantity(Decimal.get(quantity));
		m_client.placeOrder(mainOrder.orderId(), contract, mainOrder);

		if(withStopLoss) {
			// Stop loss order
			Order stopLoss = new Order();
			stopLoss.orderId(currentOrderId + 1);
			stopLoss.action(action.equals("BUY") ? "SELL" : "BUY");
			stopLoss.orderType("STP");
			stopLoss.totalQuantity(Decimal.get(quantity));
			stopLoss.auxPrice(currentPrice*(1-initialRiskPrecent/100.0));
			stopLoss.parentId(mainOrder.orderId());
			m_client.placeOrder(stopLoss.orderId(), contract, stopLoss);
		}

		if(withTrailing) {
			//        	//trailing stop loss order + initial stoploss
			Order trailOrder = new Order();
			trailOrder.orderId(currentOrderId+2);
			trailOrder.action(action);
			trailOrder.orderType("TRAIL"); // Trailing stop order
			trailOrder.trailingPercent(5.0);
			trailOrder.trailStopPrice(action.equals("BUY") ? currentPrice*(1-initialRiskPrecent) : currentPrice*(1+initialRiskPrecent));// initial stop loss price 
			trailOrder.totalQuantity(Decimal.get(quantity));
			trailOrder.parentId(mainOrder.orderId());
			m_client.placeOrder(trailOrder.orderId(), contract, trailOrder);
		}
	}
	public static void shutDown(EClientSocket m_client, Scanner scan1, ExecutorService executorService){
		//shutdown
		m_client.reqAccountUpdates(false,null);
		m_client.eDisconnect();
		scan1.close();
		executorService.shutdownNow();
	}
	public static Contract createContract(String botMode, String symbol ,String secType, String currency ,String exchange,String LastTradeDateOrContractMonth){
		if(botMode.equals("STK")||botMode.equals("FX")){
			//create new contract
			Contract contract = new Contract();
			contract.symbol(symbol);
			contract.secType(secType);
			contract.currency(currency);
			contract.exchange(exchange);
			return contract;
		} else if (botMode.equals("CFD")||botMode.equals("FUT")) {
			//create new contract
			Contract contract = new Contract();
			contract.symbol(symbol);
			contract.secType(secType);
			contract.currency(currency);
			contract.exchange(exchange);
			contract.lastTradeDateOrContractMonth(LastTradeDateOrContractMonth);
			return contract;
		}else {
			System.out.println("!!!!!!! INVALID BOTMODE !!!!!!!");
			return null;
		}
	}
	public static int calculateCapital(Ewrapperimpl myWrapper , EClientSocket m_client , Double precentFromCapital) throws InterruptedException {
		// requesting account data stream
		m_client.reqAccountUpdates(true,null);
		while(!myWrapper.getisAccountAvailableFundsRecived()) {
			Thread.sleep(1000);
		}
		myWrapper.setisAccountAvailableFundsRecived(false);

		//shutdown reqAccountUpdates data stream
		m_client.reqAccountUpdates(false, null);

		//setting the amount to be 5%
		int amount= (int) (myWrapper.getAvailableFunds()*(precentFromCapital/100));
		return amount;
	}
	public static int calculateMinimumCandlesStrategy2(int macdFast , int macdSlow , int signalLine , int rsi) {
		int minCandles ;

		//calculate the bigger number to set the minimum amount of candle needed for the calculations to run
		int a = Math.max(macdSlow, macdFast);
		int b = Math.max(signalLine, rsi);
		minCandles = Math.max(a, b) + signalLine;

		return minCandles;
	}
	public static Double calculateCompoundPnL(ArrayList<Double> pnlValues) {
		ArrayList <Double> decimalPnL = new ArrayList<>();

		for(Double number : pnlValues){
			decimalPnL.add(1+(number/100));
		}

		Double sum=1.0;
		for (Double number : decimalPnL){
			sum=sum*number;
		}

		return (sum-1)*100;
	}
	public static String calculateMinimumDuration (String CandlesTimeFrame , int minCandles){
		String minDuration ;
		String[] parts = CandlesTimeFrame.split(" "); // Split timeframe string on space
		int amountOfTimeFrame = Integer.parseInt(parts[0]);
		String timeFrame = parts[1];

		if(timeFrame.equals("secs")){
			if(amountOfTimeFrame==1){
				minDuration = Integer.toString(minCandles) + " S";
				return minDuration;
			}
			else if(amountOfTimeFrame==5){
				minCandles = minCandles*5;
				minDuration = Integer.toString(minCandles) + " S";
				return minDuration;
			}
			else if(amountOfTimeFrame==10){
				minCandles = minCandles*10;
				minDuration = Integer.toString(minCandles) + " S";
				return minDuration;
			}
			else if(amountOfTimeFrame==15){
				minCandles = minCandles*15;
				minDuration = Integer.toString(minCandles) + " S";
				return minDuration;
			}
			else if(amountOfTimeFrame==30){
				minCandles = minCandles*30;
				minDuration = Integer.toString(minCandles) + " S";
				return minDuration;
			}
		}
		else if(timeFrame.equals("min")||timeFrame.equals("mins")){
			if(amountOfTimeFrame==1){
				minCandles = minCandles*1*60;
				minDuration = Integer.toString(minCandles) + " S";
				return minDuration;
			}
			else if(amountOfTimeFrame==2){
				minCandles = minCandles*2*60;
				minDuration = Integer.toString(minCandles) + " S";
				return minDuration;
			}
			else if(amountOfTimeFrame==3){
				minCandles = minCandles*3*60;
				minDuration = Integer.toString(minCandles) + " S";
				return minDuration;
			}
			else if(amountOfTimeFrame==5){
				minCandles = minCandles*5*60;
				minDuration = Integer.toString(minCandles) + " S";
				return minDuration;
			}
			else if(amountOfTimeFrame==10){
				minCandles = minCandles*10*60;
				minDuration = Integer.toString(minCandles) + " S";
				return minDuration;
			}
			else if(amountOfTimeFrame==15){
//				minCandles = minCandles*15*60;
//				minDuration = Integer.toString(minCandles) + " S";
				minDuration="1 D";
				return minDuration;
			}
			else if(amountOfTimeFrame==20){
				minCandles = minCandles*20*60;
				minDuration = Integer.toString(minCandles) + " S";
				return minDuration;
			}
			else if(amountOfTimeFrame==30){
				minCandles = minCandles*30*60;
				minDuration = Integer.toString(minCandles) + " S";
				return minDuration;
			}
		}
		else if(timeFrame.equals("hour")||timeFrame.equals("hours")){
			if(amountOfTimeFrame==1){
				minCandles = minCandles*1*60*60;
				minDuration = Integer.toString(minCandles) + " S";
				return minDuration;
			}
			else if(amountOfTimeFrame==2){
				minCandles = minCandles*2*60*60;
				minDuration = Integer.toString(minCandles) + " S";
				return minDuration;
			}
			else if(amountOfTimeFrame==3){
				minCandles = minCandles*3*60*60;
				minDuration = Integer.toString(minCandles) + " S";
				return minDuration;
			}
			else if(amountOfTimeFrame==4){
				minCandles = minCandles*4*60*60;
				minDuration = Integer.toString(minCandles) + " S";
				return minDuration;
			}
			else if(amountOfTimeFrame==8){
				minCandles = minCandles*8*60*60;
				minDuration = Integer.toString(minCandles) + " S";
				return minDuration;
			}
		}
		else if(timeFrame.equals("day")){
			if(amountOfTimeFrame==1){
				minDuration = Integer.toString(minCandles) + " D";
				return minDuration;
			}
		}
		else if(timeFrame.equals("week")){
			if(amountOfTimeFrame==1){
				minDuration = Integer.toString(minCandles) + " W";
				return minDuration;
			}
		}
		else if(timeFrame.equals("month")){
			if(amountOfTimeFrame==1){
				minDuration = Integer.toString(minCandles) + " M";
				return minDuration;
			}
		}
		return "error calculating minimum amount of candels";
	}
	public static boolean timeInLoop (String CandlesTimeFrame ,long CurrentLoopStartTime){
		String[] parts = CandlesTimeFrame.split(" "); // Split timeframe string on space
		int amountOfTimeFrame = Integer.parseInt(parts[0]);
		String timeFrame = parts[1];

		if(timeFrame.equals("secs")){
			if(amountOfTimeFrame==1){
				if( System.currentTimeMillis() - CurrentLoopStartTime < TimeUnit.SECONDS.toMillis(1)) {
					return true;
				} else {
					return false;
				}
			}
			else if(amountOfTimeFrame==5){
				if( System.currentTimeMillis() - CurrentLoopStartTime < TimeUnit.SECONDS.toMillis(5)) {
					return true;
				} else {
					return false;
				}
			}
			else if(amountOfTimeFrame==10){
				if( System.currentTimeMillis() - CurrentLoopStartTime < TimeUnit.SECONDS.toMillis(10)) {
					return true;
				} else {
					return false;
				}
			}
			else if(amountOfTimeFrame==15){
				if( System.currentTimeMillis() - CurrentLoopStartTime < TimeUnit.SECONDS.toMillis(15)) {
					return true;
				} else {
					return false;
				}
			}
			else if(amountOfTimeFrame==30){
				if( System.currentTimeMillis() - CurrentLoopStartTime < TimeUnit.SECONDS.toMillis(30)) {
					return true;
				} else {
					return false;
				}
			}
		}
		else if(timeFrame.equals("min")||timeFrame.equals("mins")){
			if(amountOfTimeFrame==1){
				if( System.currentTimeMillis() - CurrentLoopStartTime < TimeUnit.MINUTES.toMillis(1)) {
					return true;
				} else {
					return false;
				}
			}
			else if(amountOfTimeFrame==2){
				if( System.currentTimeMillis() - CurrentLoopStartTime < TimeUnit.MINUTES.toMillis(2)) {
					return true;
				} else {
					return false;
				}
			}
			else if(amountOfTimeFrame==3){
				if( System.currentTimeMillis() - CurrentLoopStartTime < TimeUnit.MINUTES.toMillis(3)) {
					return true;
				} else {
					return false;
				}
			}
			else if(amountOfTimeFrame==5){
				if( System.currentTimeMillis() - CurrentLoopStartTime < TimeUnit.MINUTES.toMillis(5)) {
					return true;
				} else {
					return false;
				}
			}
			else if(amountOfTimeFrame==10){
				if( System.currentTimeMillis() - CurrentLoopStartTime < TimeUnit.MINUTES.toMillis(10)) {
					return true;
				} else {
					return false;
				}
			}
			else if(amountOfTimeFrame==15){
				if( System.currentTimeMillis() - CurrentLoopStartTime < TimeUnit.MINUTES.toMillis(15)) {
					return true;
				} else {
					return false;
				}
			}
			else if(amountOfTimeFrame==20){
				if( System.currentTimeMillis() - CurrentLoopStartTime < TimeUnit.MINUTES.toMillis(20)) {
					return true;
				} else {
					return false;
				}
			}
			else if(amountOfTimeFrame==30){
				if( System.currentTimeMillis() - CurrentLoopStartTime < TimeUnit.MINUTES.toMillis(30)) {
					return true;
				} else {
					return false;
				}
			}
		}
		else if(timeFrame.equals("hour")||timeFrame.equals("hours")){
			if(amountOfTimeFrame==1){
				if( System.currentTimeMillis() - CurrentLoopStartTime < TimeUnit.HOURS.toMillis(1)) {
					return true;
				} else {
					return false;
				}
			}
			else if(amountOfTimeFrame==2){
				if( System.currentTimeMillis() - CurrentLoopStartTime < TimeUnit.HOURS.toMillis(2)) {
					return true;
				} else {
					return false;
				}
			}
			else if(amountOfTimeFrame==3){
				if( System.currentTimeMillis() - CurrentLoopStartTime < TimeUnit.HOURS.toMillis(3)) {
					return true;
				} else {
					return false;
				}
			}
			else if(amountOfTimeFrame==4){
				if( System.currentTimeMillis() - CurrentLoopStartTime < TimeUnit.HOURS.toMillis(4)) {
					return true;
				} else {
					return false;
				}
			}
			else if(amountOfTimeFrame==8){
				if( System.currentTimeMillis() - CurrentLoopStartTime < TimeUnit.HOURS.toMillis(8)) {
					return true;
				} else {
					return false;
				}
			}
		}
		else if(timeFrame.equals("day")){
			if(amountOfTimeFrame==1){
				if( System.currentTimeMillis() - CurrentLoopStartTime < TimeUnit.DAYS.toMillis(1)) {
					return true;
				} else {
					return false;
				}
			}
		}
		else if(timeFrame.equals("week")){
			if(amountOfTimeFrame==1){
				if( System.currentTimeMillis() - CurrentLoopStartTime < TimeUnit.DAYS.toMillis(7)) {
					return true;
				} else {
					return false;
				}
			}
		}
		else if(timeFrame.equals("month")){
			if(amountOfTimeFrame==1){
				if( System.currentTimeMillis() - CurrentLoopStartTime < TimeUnit.DAYS.toMillis(30)) {
					return true;
				} else {
					return false;
				}
			}
		}
		System.out.println("error in function timeInLoop ");
		return false;
	}
	public static void placeOrderStrategy2(Ewrapperimpl myWrapper,EClient m_client,int capital,Double initialRiskPrecent, Contract contract,Contract contract2,String CandlesTimeFrame,int macdSlow,int macdFast,int SIGNALLINE,int RSI,boolean inverseStrategy,boolean longShort,boolean demoMode,boolean isCurrenlyInDeal,String currentPositionType1,String seconderyCandlesTimeFrame,int amountOfSeconderyCandles,String andOrSwitch,String excelFilePath,String loadEsitindExcelFilePath) throws InterruptedException, IOException {
		//VARIABLES : 
		boolean isInDeal = isCurrenlyInDeal;
//		ArrayList<Bar> reset1 = new ArrayList<>();
		ArrayList<Double> prices = new ArrayList<>();
		ArrayList<Double> macd = new ArrayList<>();
		ArrayList<Double> signalLine = new ArrayList<>();
		ArrayList<Double> rsi = new ArrayList<>();
		ArrayList<Double> seconderyPrices = new ArrayList<>();
		ArrayList<Double> returns = new ArrayList<>();
		int minimumCandles = Actions.calculateMinimumCandlesStrategy2(macdFast, macdSlow, SIGNALLINE, RSI);
		String minimumDuration = Actions.calculateMinimumDuration(CandlesTimeFrame, minimumCandles);
		String minimumDurationSeconderyCandles=Actions.calculateMinimumDuration(seconderyCandlesTimeFrame, amountOfSeconderyCandles);
		long CurrentLoopStartTime ;
		int initialTradeQuantity = (isInDeal) ? Math.abs(Integer.parseInt(myWrapper.getCurrentOpenPositionSize().toString()))  : 0 ;
		Double currentPrice = 0.0;
		Double currentPnL=0.0;
		Double enterPrice = (isInDeal) ? myWrapper.getCurrentOpenPositionCost() : 0.0 ;
		String currentPositionType = currentPositionType1;
		Double lastTickPrice ;
		Double compoundPnL;
		Double realPnl=0.0;
		int quantity;
		Date date;
		SimpleDateFormat sdf;
		String formattedDateTime;
		Double currentMacd;
		Double currentSignalLine;
		Double currentRsi;
		boolean buyStrategy;
		boolean seconderyStrategy;
		int rowCounter=1;

		//ESTABLISHE SOCKET CONNECTION
		m_client.reqMktData(1,contract,"",false,false,null);
		Thread.sleep(1000);

		//start excel file for data
		ExcelWriter excelWriter = new ExcelWriter(excelFilePath,false,2,loadEsitindExcelFilePath);

		while (true) {
			System.out.print("LOOP  LOOP  LOOP  LOOP LOOP  LOOP  LOOP  LOOP  LOOP  LOOP  LOOP  LOOP -->  ");
			//SETUP STRAT TIME
			CurrentLoopStartTime = System.currentTimeMillis();

			//PRINT LOOP DATE END TIME
			date = new Date(CurrentLoopStartTime);
			sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			formattedDateTime = sdf.format(date);
			System.out.println(formattedDateTime);


			//requesting histprical data + implanting prices list (primary + secondery)
			m_client.reqHistoricalData(1002, contract,"",minimumDuration,CandlesTimeFrame, "MIDPOINT", 1, 1, false, null);
			Actions.waitingForHistData(myWrapper);
			prices=myWrapper.getClosingPrices(1002);
			m_client.reqHistoricalData(1003, contract,"",minimumDurationSeconderyCandles,seconderyCandlesTimeFrame, "MIDPOINT", 1, 1, false, null);
			Actions.waitingForHistData(myWrapper);
			//adding scondery timefraems bars
			System.out.println("SIZE : "+prices.size() +"|| prices before : "+prices);// can be remove -> just for checking
			seconderyPrices=myWrapper.getClosingPrices(1003);
			int counter=0;
			for (int i=(prices.size()-seconderyPrices.size());i<prices.size();i++) {
				prices.set(i,seconderyPrices.get(counter));
				counter++;
			}
			System.out.println("SIZE : "+prices.size() +"|| prices after : "+prices);// can be remove -> just for checking


			//CALCULATING INDICATORS 
			macd=Indicators.calculateMACD(prices,macdSlow,macdFast,false);
			signalLine=Indicators.calculateMovingAverage(macd,SIGNALLINE);
			rsi=Indicators.calculateRSI(prices,RSI);
			System.out.println("CURRENT POSITION : " +currentPositionType);
			System.out.println("SIZE : "+prices.size() + " || last PRICES : "+ prices.get(prices.size()-1));
			System.out.println("SIZE : "+macd.size() + " || last MACD : "+ macd.get(macd.size()-1));
			System.out.println("SIZE : "+signalLine.size() +" || last  signalLine : "+ signalLine.get(signalLine.size()-1));
			System.out.println("SIZE : "+rsi.size() +" ||last RSI : "+ rsi.get(rsi.size()-1));
			System.out.println("************************************************************");

			//SETTING UP VARIABELS
			currentPrice = prices.get(prices.size()-1);
			quantity = (int) (capital / currentPrice);
			if (quantity <= 0) {
				System.out.println("!!!!!!!!!!!!!!!!!!!!!low capital -> quantitiy is zero!!!!!!!!!!!!!!!!!!!!!!!!!!");
				break;
			}
			currentMacd=macd.get(macd.size()-1);
			currentSignalLine= signalLine.get(signalLine.size()-1);
			currentRsi=rsi.get(rsi.size()-1);
			buyStrategy=Strategy.buyStrategy2(currentMacd,currentSignalLine,currentRsi,inverseStrategy);
			seconderyStrategy=Strategy.seconderyStrategy(myWrapper, m_client, capital, initialRiskPrecent, contract2, CandlesTimeFrame, macdSlow, macdFast, SIGNALLINE, RSI, inverseStrategy, longShort, seconderyCandlesTimeFrame, amountOfSeconderyCandles);
			System.out.println("MAIN CONTRACT STRATEGY : "+buyStrategy);
			System.out.println("SECOND  CONTRACT STRATEGY : "+seconderyStrategy);

			//THE STRATEGY PART
			if(longShort) {
				if(!isInDeal) {
					if(buyStrategy&&seconderyStrategy) {
						initialTradeQuantity=quantity;
						isInDeal = true;
						if(!demoMode) {
							currentPositionType="BUY";
							enterPrice=currentPrice;
							Actions.placeOrder(myWrapper,m_client,contract,currentPositionType,"MKT",null,initialRiskPrecent,currentPrice,initialTradeQuantity,false,false);
							System.out.print("ACTION TYPE : "+currentPositionType+" || CURRENT PRICE : " + currentPrice +"|| current buytrstagey : "+buyStrategy+" || cuurent secondery strategy "+seconderyStrategy+ " || MACD : " + currentMacd + " || SIGNALLINE : " + currentSignalLine + " || RSI : " + currentRsi);
						}
						else {
							currentPositionType="BUY";
							enterPrice=currentPrice;
							System.out.print("!! DEMO MODE !!"+"ACTION TYPE : "+currentPositionType+" || CURRENT PRICE : " + currentPrice + " || MACD : " + currentMacd + " || SIGNALLINE : " + currentSignalLine + " || RSI : " + currentRsi);
						}
					}
					else {
						initialTradeQuantity=quantity;
						isInDeal = true;
						if(!demoMode) {
							currentPositionType="SELL";
							enterPrice=currentPrice;
							Actions.placeOrder(myWrapper,m_client,contract,currentPositionType,"MKT",null,initialRiskPrecent,prices.get(prices.size()-1),initialTradeQuantity,false,false);
							System.out.print("ACTION TYPE : "+currentPositionType+" || CURRENT PRICE : " + currentPrice +"|| current buytrstagey : "+buyStrategy+" || cuurent secondery strategy "+seconderyStrategy+ " || MACD : " + currentMacd + " || SIGNALLINE : " + currentSignalLine + " || RSI : " + currentRsi);
						}
						else {
							currentPositionType="SELL";
							enterPrice=currentPrice;
							System.out.print("!! DEMO MODE !!"+" ||| ACTION TYPE : BUY"+" || CURRENT PRICE : " + currentPrice + " || MACD : " + currentMacd + " || SIGNALLINE : " + currentSignalLine + " || RSI : " + currentRsi);
						}
					}
				}
				else {
					if(currentPositionType.equals("BUY")) {
						if((andOrSwitch.equals("OR")) ? !buyStrategy||!seconderyStrategy : !buyStrategy&&!seconderyStrategy){
							//SWITCH POSITION + GO SHORT 
							if(!demoMode) {
								currentPositionType="SELL";
								enterPrice=currentPrice;
								Actions.placeOrder(myWrapper,m_client,contract,currentPositionType,"MKT",null,initialRiskPrecent,prices.get(prices.size()-1),initialTradeQuantity+quantity,false,false);
								initialTradeQuantity=quantity;
								System.out.print("ACTION TYPE : "+currentPositionType+" || CURRENT PRICE : " + currentPrice + " || MACD : " + currentMacd + " || SIGNALLINE : " + currentSignalLine + " || RSI : " + currentRsi);
							}
							else {
								currentPositionType="SELL";
								enterPrice=currentPrice;
								System.out.print("!! DEMO MODE !!"+" ||| ACTION TYPE : SELL"+" || CURRENT PRICE : " + currentPrice + " || MACD : " + currentMacd + " || SIGNALLINE : " + currentSignalLine + " || RSI : " + currentRsi);
							}
						}
					}
					else {
						if((andOrSwitch.equals("OR")) ? buyStrategy||seconderyStrategy : buyStrategy&&seconderyStrategy){
							if(!demoMode) {
								//SWITCH POSITION + GO LONG
								currentPositionType="BUY";
								enterPrice=currentPrice;
								Actions.placeOrder(myWrapper,m_client,contract,currentPositionType,"MKT",null,initialRiskPrecent,prices.get(prices.size()-1),initialTradeQuantity+quantity,false,false);
								initialTradeQuantity=quantity;
								System.out.print("ACTION TYPE : "+currentPositionType+" || CURRENT PRICE : " + currentPrice + " || MACD : " + currentMacd + " || SIGNALLINE : " + currentSignalLine + " || RSI : " + currentRsi);
							}
							else {
								currentPositionType="BUY";
								enterPrice=currentPrice;
								System.out.print("!! DEMO MODE !!"+"ACTION TYPE : "+currentPositionType+" || CURRENT PRICE : " + currentPrice + " || MACD : " + currentMacd + " || SIGNALLINE : " + currentSignalLine + " || RSI : " + currentRsi);
							}
						}
					}
				}
			}
			else {
				if(!isInDeal) {
					if(buyStrategy&&seconderyStrategy) {
						initialTradeQuantity = quantity;
						isInDeal = true;

						if(!demoMode) {
							currentPositionType="BUY";
							enterPrice=currentPrice;
							Actions.placeOrder(myWrapper,m_client,contract,currentPositionType,"MKT",null,initialRiskPrecent,currentPrice,quantity,false,false);
							System.out.print("ACTION TYPE : "+currentPositionType+" || CURRENT PRICE : " + currentPrice + " || MACD : " + currentMacd + " || SIGNALLINE : " + currentSignalLine + " || RSI : " + currentRsi);
						}
						else {
							currentPositionType="BUY";
							enterPrice=currentPrice;
							System.out.print("!! DEMO MODE !!"+"ACTION TYPE : "+currentPositionType+" || CURRENT PRICE : " + currentPrice + " || MACD : " + currentMacd + " || SIGNALLINE : " + currentSignalLine + " || RSI : " + currentRsi);
						}
					}
				}
				else {
					if((andOrSwitch.equals("OR") ? buyStrategy||seconderyStrategy : buyStrategy&&seconderyStrategy)) {
						currentPrice = prices.get(prices.size()-1);
						isInDeal = false;
						if(!demoMode) {
							currentPositionType="SELL";
							Actions.placeOrder(myWrapper,m_client,contract,currentPositionType,"MKT",null,initialRiskPrecent,currentPrice,initialTradeQuantity,false,false);
							System.out.print("ACTION TYPE : "+currentPositionType+" || CURRENT PRICE : " + currentPrice + " || MACD : " + currentMacd + " || SIGNALLINE : " + currentSignalLine + " || RSI : " + currentRsi);
						}
						else {
							currentPositionType="SELL";
							System.out.print("!! DEMO MODE !!"+"ACTION TYPE : "+currentPositionType+" || CURRENT PRICE : " + currentPrice + " || MACD : " + currentMacd + " || SIGNALLINE : " + currentSignalLine + " || RSI : " + currentRsi);
						}
					}
				}
			}

			//CHECK SITUATION
			while(Actions.timeInLoop(CandlesTimeFrame, CurrentLoopStartTime)) {
				lastTickPrice=myWrapper.getCurrentTickPrice(1);
				if(isInDeal) {
					currentPnL = BackTest.calculateProfitPercentage(enterPrice, lastTickPrice, currentPositionType);
					System.out.println("CURRENT P&L : "+currentPnL+" % ");
					if(currentPnL<=-initialRiskPrecent) {
						System.out.print("!!!! STOP LOSS JUMPED !!!!!!");
						if(currentPositionType.equals("BUY")) {
							if(!demoMode) {
								isInDeal=false;
								currentPositionType="SELL";
								Actions.placeOrder(myWrapper,m_client,contract,currentPositionType,"MKT",null,initialRiskPrecent,currentPrice,initialTradeQuantity,false,false);
								System.out.print("ACTION TYPE : "+currentPositionType+" || CURRENT PRICE : " + currentPrice + " || MACD : " + currentMacd + " || SIGNALLINE : " + currentSignalLine + " || RSI : " + currentRsi);
							}
							else {
								isInDeal=false;
								currentPositionType="SELL";
								System.out.print("!! DEMO MODE !!"+"ACTION TYPE : "+currentPositionType+" || CURRENT PRICE : " + currentPrice + " || MACD : " + currentMacd + " || SIGNALLINE : " + currentSignalLine + " || RSI : " + currentRsi);
							}
						}
						else {
							if(!demoMode) {
								isInDeal=false;
								currentPositionType="BUY";
								Actions.placeOrder(myWrapper,m_client,contract,currentPositionType,"MKT",null,initialRiskPrecent,currentPrice,initialTradeQuantity,false,false);
								System.out.print("ACTION TYPE : "+currentPositionType+" || CURRENT PRICE : " + currentPrice + " || MACD : " + currentMacd + " || SIGNALLINE : " + currentSignalLine + " || RSI : " + currentRsi);
							}
							else {
								isInDeal=false;
								currentPositionType="BUY";
								System.out.print("!! DEMO MODE !!"+"ACTION TYPE : "+currentPositionType+" || CURRENT PRICE : " + currentPrice + " || MACD : " + currentMacd + " || SIGNALLINE : " + currentSignalLine + " || RSI : " + currentRsi);
							}
						}
					}
				}
				else {
					System.out.print(".");
				}
				Thread.sleep(1000);
			}

			//CALCULATING REAL P&L FOR CURRENT CANDEL
			returns.add(currentPnL);
			compoundPnL=Actions.calculateCompoundPnL(returns);
			System.out.println(" CURRENT COMPUOND P&L : "+compoundPnL);
			realPnl=0.0;
			for (Double currentReturn : returns) {
				realPnl+=currentReturn;
			}
			System.out.println(" real p&l : "+realPnl);

			//write data to excel
			ExcelWriter.writeStrategy2PnL(excelWriter,rowCounter,contract.symbol(),compoundPnL,realPnl);
			rowCounter++;

			//RESET CLOSING PRICES LIST + BRAS LIST
			myWrapper.resetBarsDict();
			prices.clear();
			seconderyPrices.clear();
		}
	}
	public static boolean waitingForHistData(Ewrapperimpl myWrapper) throws InterruptedException {
		int wastedTimeFromLifeCounter = 0;
		System.out.print("WAITING FOR HISTORICAL DATA ..."+"\n"+"time you are wasting from life counter :");

		while (!myWrapper.getIsHistoricalDataRecived()) {
			wastedTimeFromLifeCounter++;
			Thread.sleep(1000);
			System.out.println(wastedTimeFromLifeCounter + " seconds");
		}

		myWrapper.setIsHistoricalDataRecived(false);
		return true;
	}
	public static int calculateMinimumCandlesStrategy1(int ZscoreWindow , int smaZwindow){
		int sum=ZscoreWindow+smaZwindow+1;
		return sum;
	}
	public static void placeOrderStrategy1(Ewrapperimpl myWrapper, EClient m_client, String excelFilePath , int ZscroeWindow , int smaZwindow, String candlesTimeFrame,Contract myContract,Double initialRiskPrecent,Double precentageFromCapital,boolean currentlyInDeal,String currentPositionType2,int currentQuantity,Double currentStartPrice,String loadEsitindExcelFilePath) throws IOException, InterruptedException {
		//variabels
		HashMap<Integer, ArrayList<Bar>> barsDict;
		ArrayList<Double> closingPrices ;
		ArrayList<Double> Zscore;
		ArrayList <Double> smaZ;
		ArrayList <String> dates = new ArrayList<>();
		ExcelWriter excelWriter = new ExcelWriter(excelFilePath,false,1,loadEsitindExcelFilePath);
		int rowCounter=excelWriter.getCurrentSheet().getLastRowNum()+1;
		ArrayList <Double> realReturns = loadEsitindExcelFilePath.equals("") ? new ArrayList<>() : new ArrayList<>(Collections.singleton(Double.valueOf(excelWriter.getCellValue(0, rowCounter - 1, 7))));
		boolean isInDeal = false;
		String currentPositionType = "";
		Double currentPrice = null;
		Double currentZscore;
		Double currentSmaz;
		Double startPrice = null;
		long currentLoopStartTime;
		String currentBuyStrategy;
		String stopLossTriger = "";
		int quantitiy;
		int initialTradeQuantity = 0;
		Double lastTickPrice;
		double currentPnL;
		if(currentlyInDeal){
			currentPositionType=currentPositionType2;
			initialTradeQuantity=currentQuantity;
			startPrice=currentStartPrice;
			isInDeal=currentlyInDeal;
		}

		System.out.println("!*&^&^#R&*# real returns real return sreal return sreal return sreal returns real returns real returns !@#&*^#(#@!" + realReturns);

		//open socket connection
		m_client.reqMktData(98,myContract,"",false,false,null);

		//the LOOP
		while (true) {
			currentLoopStartTime = System.currentTimeMillis();
			System.out.println("----------------------------LOOP LOOP LOOP LOOP LOOP LOOP LOOP LOOP LOOP LOOP ---------------------------------");

			m_client.reqHistoricalData(54, myContract, "", "10 D", candlesTimeFrame, "MIDPOINT", 1, 1, false, null);
			Actions.waitingForHistData(myWrapper);
			closingPrices = myWrapper.getClosingPrices(54);

			//calculating Z score + Smaz
			Zscore = Indicators.calculateMovingZscore(closingPrices, ZscroeWindow);
			smaZ = Indicators.calculateExponentialMovingAverage(Zscore, smaZwindow);

			//seting up variabels for strategy
			currentPrice = closingPrices.get(closingPrices.size() - 1);
			currentSmaz = smaZ.get(smaZ.size() - 1);
			currentZscore = Zscore.get(Zscore.size() - 1);
			currentBuyStrategy = Strategy.buyStrategy1(currentZscore, currentSmaz);
			quantitiy = (int) (Actions.calculateCapital(myWrapper, (EClientSocket) m_client, precentageFromCapital) / currentPrice);
			System.out.println("-->Current Price : " + currentPrice);
			System.out.println("-->Current SMA-Z : " + currentSmaz);
			System.out.println("-->Current Zscore : " + currentZscore);
			System.out.println("-->Current Buy Strategy : " + currentBuyStrategy);
			System.out.println("-->Current Position Type : " + currentPositionType);
			System.out.println("-->Quantity : " + quantitiy);


			//the strategy part
			if (!isInDeal) {
				if (currentBuyStrategy.equals("BUY") && !stopLossTriger.equals("SELL")) {
					isInDeal = true;
					currentPositionType = "BUY";
					startPrice = currentPrice;
					initialTradeQuantity = quantitiy;
					Actions.placeOrder(myWrapper, m_client, myContract, currentPositionType,"LMT",currentPrice, null, currentPrice, initialTradeQuantity, false, false);
					System.out.println("ACTION : " + currentPositionType + "|| CURRENT BUYSTRATEGY : " + currentBuyStrategy + " || Current Zscore : " + currentZscore + " || CurentSmaz : " + currentSmaz);
					//write data to excel
					ExcelWriter.writeStrategy1PnL(excelWriter,rowCounter,false,currentPositionType,currentPrice.toString(),currentBuyStrategy,currentZscore.toString(),currentSmaz.toString(),String.valueOf(realReturns.stream().mapToDouble(Double::doubleValue).sum()),String.valueOf(Actions.calculateCompoundPnL(realReturns)),null);
					rowCounter++;
				} else if (currentBuyStrategy.equals("SELL") && !stopLossTriger.equals("BUY")) {
					isInDeal = true;
					currentPositionType = "SELL";
					startPrice = currentPrice;
					initialTradeQuantity = quantitiy;
					Actions.placeOrder(myWrapper, m_client, myContract, currentPositionType,"LMT",currentPrice,null, currentPrice, initialTradeQuantity, false, false);
					System.out.println("ACTION : " + currentPositionType + "|| CURRENT BUYSTRATEGY : " + currentBuyStrategy + " || Current Zscore : " + currentZscore + " || CurentSmaz : " + currentSmaz);
					//write data to excel
					ExcelWriter.writeStrategy1PnL(excelWriter,rowCounter,false,currentPositionType,currentPrice.toString(),currentBuyStrategy,currentZscore.toString(),currentSmaz.toString(),String.valueOf(realReturns.stream().mapToDouble(Double::doubleValue).sum()),String.valueOf(Actions.calculateCompoundPnL(realReturns)),null);
					rowCounter++;
				}
			} else {
				if (currentPositionType.equals("BUY")) {
					if (!currentBuyStrategy.equals("BUY")) {
						//calculating real PnL
						realReturns.add(BackTest.calculateProfitPercentage(startPrice, currentPrice, currentPositionType));
						isInDeal = false;
						currentPositionType = "";
						Actions.placeOrder(myWrapper, m_client, myContract, "SELL","MKT",null, null, currentPrice, initialTradeQuantity, false, false);
						System.out.println("ACTION : SELL " + "|| CURRENT BUYSTRATEGY : " + currentBuyStrategy + " || Current Zscore : " + currentZscore + " || CurentSmaz : " + currentSmaz);
						//write data to excel
						ExcelWriter.writeStrategy1PnL(excelWriter,rowCounter,false,"SELL",currentPrice.toString(),currentBuyStrategy,currentZscore.toString(),currentSmaz.toString(), String.valueOf(realReturns.stream().mapToDouble(Double::doubleValue).sum()),String.valueOf(Actions.calculateCompoundPnL(realReturns)),null);
						rowCounter++;
					}
				} else if (currentPositionType.equals("SELL")) {
					if (!currentBuyStrategy.equals("SELL")) {
						//calculating real PnL
						realReturns.add(BackTest.calculateProfitPercentage(startPrice, currentPrice, currentPositionType));
						isInDeal = false;
						currentPositionType = "";
						Actions.placeOrder(myWrapper, m_client, myContract, "BUY","MKT",null, null, currentPrice, Math.abs(initialTradeQuantity), false, false);
						System.out.println("ACTION : BUY " + "|| CURRENT BUYSTRATEGY : " + currentBuyStrategy + " || Current Zscore : " + currentZscore + " || CurentSmaz : " + currentSmaz);
						//write data to excel
						ExcelWriter.writeStrategy1PnL(excelWriter,rowCounter,false,"BUY",currentPrice.toString(),currentBuyStrategy,currentZscore.toString(),currentSmaz.toString(), String.valueOf(realReturns.stream().mapToDouble(Double::doubleValue).sum()),String.valueOf(Actions.calculateCompoundPnL(realReturns)),null);
						rowCounter++;
					}
				}
			}

			//checkSituation
			while (timeInLoop(candlesTimeFrame, currentLoopStartTime)) {
				Thread.sleep(5000);
				if(isInDeal){
					lastTickPrice = myWrapper.getCurrentTickPrice(98);
					currentPnL = BackTest.calculateProfitPercentage(startPrice, lastTickPrice, currentPositionType);
					System.out.println("CURRENT P&L : " + currentPnL + " % ");

					if (currentPnL <= -initialRiskPrecent) {
						System.out.print("!!!! STOP LOSS JUMPED !!!!!!");
						if (currentPositionType.equals("BUY")) {
							isInDeal = false;
							currentPositionType = "SELL";
							stopLossTriger="SELL";
							Actions.placeOrder(myWrapper, m_client, myContract, currentPositionType,"MKT",null, initialRiskPrecent, lastTickPrice, initialTradeQuantity, false, false);
						} else {
							isInDeal = false;
							currentPositionType = "BUY";
							stopLossTriger="BUY";
							Actions.placeOrder(myWrapper, m_client, myContract, currentPositionType,"MKT",null, initialRiskPrecent, lastTickPrice, initialTradeQuantity, false, false);
						}
					}
				}
				else {
					System.out.print(".");
				}
			}

			//reset barsDict
			myWrapper.resetBarsDict();
			closingPrices.clear();

			//calculatePn;
			if (realReturns.size()>0){
				System.out.println("MY REAL PNL : "+realReturns.stream().mapToDouble(Double::doubleValue).sum());
				System.out.println(" MY compound PNL :"+Actions.calculateCompoundPnL(realReturns));
			}

		}
	}
	public static void placeOrderStrategy12(Ewrapperimpl myWrapper, EClient m_client, String excelFilePath , int ZscroeWindow , int smaZwindow, String candlesTimeFrame,Contract myContract,Double initialRiskPrecent,Double precentageFromCapital,boolean currentlyInDeal,String currentPositionType2,int currentQuantity,Double currentStartPrice,String loadEsitindExcelFilePath) throws IOException, InterruptedException {
		//variabels
		HashMap<Integer, ArrayList<Bar>> barsDict;
		ArrayList<Double> closingPrices ;
		ArrayList<Double> Zscore;
		ArrayList <Double> smaZ;
		ArrayList <String> dates = new ArrayList<>();
		ExcelWriter excelWriter = new ExcelWriter(excelFilePath,false,1,loadEsitindExcelFilePath);
		int rowCounter=excelWriter.getCurrentSheet().getLastRowNum()+1;
		ArrayList <Double> realReturns = loadEsitindExcelFilePath.equals("") ? new ArrayList<>() : new ArrayList<>(Collections.singleton(Double.valueOf(excelWriter.getCellValue(0, rowCounter - 1, 7))));
		boolean isInDeal = false;
		String currentPositionType = "";
		Double currentPrice = null;
		Double currentZscore;
		Double currentSmaz;
		Double startPrice = null;
		long currentLoopStartTime;
		String currentBuyStrategy;
		String stopLossTriger = "";
		int quantitiy;
		int initialTradeQuantity = 0;
		Double lastTickPrice;
		double currentPnL;
		if(currentlyInDeal){
			currentPositionType=currentPositionType2;
			initialTradeQuantity=currentQuantity;
			startPrice=currentStartPrice;
			isInDeal=currentlyInDeal;
		}

		System.out.println("!*&^&^#R&*# real returns real return sreal return sreal return sreal returns real returns real returns !@#&*^#(#@!" + realReturns);

		//open socket connection
		m_client.reqMktData(98,myContract,"",false,false,null);
		Thread.sleep(1000);

		//the LOOP
		while (true) {
			currentLoopStartTime = System.currentTimeMillis();
			System.out.println("----------------------------LOOP LOOP LOOP LOOP LOOP LOOP LOOP LOOP LOOP LOOP ---------------------------------");

			m_client.reqHistoricalData(54, myContract, "", "10 D", candlesTimeFrame, "MIDPOINT", 1, 1, false, null);
			Actions.waitingForHistData(myWrapper);
			closingPrices = myWrapper.getClosingPrices(54);

			while (timeInLoop(candlesTimeFrame, currentLoopStartTime)) {
				//get current tick price + initial
				lastTickPrice = myWrapper.getCurrentTickPrice(98);
				closingPrices.set(closingPrices.size()-1, lastTickPrice);

				//calculating Z score + Smaz for current price
				Zscore = Indicators.calculateMovingZscore(closingPrices, ZscroeWindow);
				smaZ = Indicators.calculateExponentialMovingAverage(Zscore, smaZwindow);

				//seting up variabels for strategy
				currentPrice = closingPrices.get(closingPrices.size() - 1);
				currentSmaz = smaZ.get(smaZ.size() - 1);
				currentZscore = Zscore.get(Zscore.size() - 1);
				currentBuyStrategy = Strategy.buyStrategy1(currentZscore, currentSmaz);
				quantitiy = (int) (Actions.calculateCapital(myWrapper, (EClientSocket) m_client, precentageFromCapital) / currentPrice);
				System.out.println("-->Current Price : " + currentPrice);
				System.out.println("-->Current SMA-Z : " + currentSmaz);
				System.out.println("-->Current Zscore : " + currentZscore);
				System.out.println("-->Current Buy Strategy : " + currentBuyStrategy);
				System.out.println("-->Current Position Type : " + currentPositionType);
				System.out.println("-->Quantity : " + quantitiy);

				//the strategy part
				if (!isInDeal) {
					if (currentBuyStrategy.equals("BUY") && !stopLossTriger.equals("SELL")) {
						isInDeal = true;
						currentPositionType = "BUY";
						startPrice = currentPrice;
						initialTradeQuantity = quantitiy;
						Actions.placeOrder(myWrapper, m_client, myContract, currentPositionType,"LMT",currentPrice, null, currentPrice, initialTradeQuantity, false, false);
						System.out.println("ACTION : " + currentPositionType + "|| CURRENT BUYSTRATEGY : " + currentBuyStrategy + " || Current Zscore : " + currentZscore + " || CurentSmaz : " + currentSmaz);
						//write data to excel
						ExcelWriter.writeStrategy1PnL(excelWriter,rowCounter,false,currentPositionType,currentPrice.toString(),currentBuyStrategy,currentZscore.toString(),currentSmaz.toString(),String.valueOf(realReturns.stream().mapToDouble(Double::doubleValue).sum()),String.valueOf(Actions.calculateCompoundPnL(realReturns)),null);
						rowCounter++;
						//reset stopLossTrriger :
						stopLossTriger = "";
					} else if (currentBuyStrategy.equals("SELL") && !stopLossTriger.equals("BUY")) {
						isInDeal = true;
						currentPositionType = "SELL";
						startPrice = currentPrice;
						initialTradeQuantity = quantitiy;
						Actions.placeOrder(myWrapper, m_client, myContract, currentPositionType,"LMT",currentPrice,null, currentPrice, initialTradeQuantity, false, false);
						System.out.println("ACTION : " + currentPositionType + "|| CURRENT BUYSTRATEGY : " + currentBuyStrategy + " || Current Zscore : " + currentZscore + " || CurentSmaz : " + currentSmaz);
						//write data to excel
						ExcelWriter.writeStrategy1PnL(excelWriter,rowCounter,false,currentPositionType,currentPrice.toString(),currentBuyStrategy,currentZscore.toString(),currentSmaz.toString(),String.valueOf(realReturns.stream().mapToDouble(Double::doubleValue).sum()),String.valueOf(Actions.calculateCompoundPnL(realReturns)),null);
						rowCounter++;
						//reset stopLossTrriger :
						stopLossTriger = "";
					}
				} else {
					if (currentPositionType.equals("BUY")) {
						if (!currentBuyStrategy.equals("BUY")) {
							//calculating real PnL
							realReturns.add(BackTest.calculateProfitPercentage(startPrice, currentPrice, currentPositionType));
							isInDeal = false;
							currentPositionType = "";
							Actions.placeOrder(myWrapper, m_client, myContract, "SELL","MKT",null, null, currentPrice, initialTradeQuantity, false, false);
							System.out.println("ACTION : SELL " + "|| CURRENT BUYSTRATEGY : " + currentBuyStrategy + " || Current Zscore : " + currentZscore + " || CurentSmaz : " + currentSmaz);
							//write data to excel
							ExcelWriter.writeStrategy1PnL(excelWriter,rowCounter,false,"SELL",currentPrice.toString(),currentBuyStrategy,currentZscore.toString(),currentSmaz.toString(), String.valueOf(realReturns.stream().mapToDouble(Double::doubleValue).sum()),String.valueOf(Actions.calculateCompoundPnL(realReturns)),null);
							rowCounter++;
						}
					} else if (currentPositionType.equals("SELL")) {
						if (!currentBuyStrategy.equals("SELL")) {
							//calculating real PnL
							realReturns.add(BackTest.calculateProfitPercentage(startPrice, currentPrice, currentPositionType));
							isInDeal = false;
							currentPositionType = "";
							Actions.placeOrder(myWrapper, m_client, myContract, "BUY","MKT",null, null, currentPrice, Math.abs(initialTradeQuantity), false, false);
							System.out.println("ACTION : BUY " + "|| CURRENT BUYSTRATEGY : " + currentBuyStrategy + " || Current Zscore : " + currentZscore + " || CurentSmaz : " + currentSmaz);
							//write data to excel
							ExcelWriter.writeStrategy1PnL(excelWriter,rowCounter,false,"BUY",currentPrice.toString(),currentBuyStrategy,currentZscore.toString(),currentSmaz.toString(), String.valueOf(realReturns.stream().mapToDouble(Double::doubleValue).sum()),String.valueOf(Actions.calculateCompoundPnL(realReturns)),null);
							rowCounter++;
						}
					}
				}

				// check stopLoss!
				if(isInDeal){
					currentPnL = BackTest.calculateProfitPercentage(startPrice, lastTickPrice, currentPositionType);
					System.out.println("CURRENT P&L : " + currentPnL + " % ");
					if (currentPnL <= -initialRiskPrecent) {
						System.out.print("!!!! STOP LOSS JUMPED !!!!!!");
						if (currentPositionType.equals("BUY")) {
							isInDeal = false;
							currentPositionType = "";
							stopLossTriger="SELL";
							Actions.placeOrder(myWrapper, m_client, myContract,"SELL","MKT",null, initialRiskPrecent, lastTickPrice, initialTradeQuantity, false, false);
						} else {
							isInDeal = false;
							currentPositionType = "";
							stopLossTriger="BUY";
							Actions.placeOrder(myWrapper, m_client, myContract, "BUY","MKT",null, initialRiskPrecent, lastTickPrice, initialTradeQuantity, false, false);
						}
					}
				}

				Thread.sleep(5000);
			}

			//reset barsDict
			myWrapper.resetBarsDict();
			closingPrices.clear();

			//calculatePn;
			if (realReturns.size()>0){
				System.out.println("MY REAL PNL : "+realReturns.stream().mapToDouble(Double::doubleValue).sum());
				System.out.println(" MY compound PNL :"+Actions.calculateCompoundPnL(realReturns));
			}

		}
	}
	public static void placeOrderStrategy1Reversed(Ewrapperimpl myWrapper, EClient m_client, String excelFilePath , int ZscroeWindow , int smaZwindow, String candlesTimeFrame,Contract myContract,Double initialRiskPrecent,Double precentageFromCapital,String loadEsitindExcelFilePath) throws IOException, InterruptedException {
		//variabels
		HashMap<Integer, ArrayList<Bar>> barsDict;
		ArrayList<Double> closingPrices ;
		ArrayList<Double> Zscore;
		ArrayList <Double> smaZ;
		ArrayList <Double> realReturns = null;
		ArrayList <String> dates = new ArrayList<>();
		ExcelWriter excelWriter = new ExcelWriter(excelFilePath,false,1,loadEsitindExcelFilePath);
		boolean isInDeal = false;
		String currentPositionType = "";
		Double currentPrice = null;
		Double currentZscore;
		Double currentSmaz;
		Double startPrice = null;
		long currentLoopStartTime;
		String currentBuyStrategy;
		String stopLossTriger = "";
		int quantitiy;
		int initialTradeQuantity = 0;
		Double lastTickPrice;
		double currentPnL;


		//open socket connection
		m_client.reqMktData(98,myContract,"",false,false,null);

		//the LOOP
		while (true) {
			currentLoopStartTime = System.currentTimeMillis();
			System.out.println("----------------------------LOOP LOOP LOOP LOOP LOOP LOOP LOOP LOOP LOOP LOOP ---------------------------------");

			m_client.reqHistoricalData(54, myContract, "", "7 D", candlesTimeFrame, "MIDPOINT", 1, 1, false, null);
			Actions.waitingForHistData(myWrapper);
			closingPrices = myWrapper.getClosingPrices(54);

			//calculating Z score + Smaz
			Zscore = Indicators.calculateMovingZscore(closingPrices, ZscroeWindow);
			smaZ = Indicators.calculateExponentialMovingAverage(Zscore, smaZwindow);

			//seting up variabels for strategy
			currentPrice = closingPrices.get(closingPrices.size() - 1);
			currentSmaz = smaZ.get(smaZ.size() - 1);
			currentZscore = Zscore.get(Zscore.size() - 1);
			currentBuyStrategy = Strategy.buyStrategy1(currentZscore, currentSmaz);
			quantitiy = (int) (Actions.calculateCapital(myWrapper, (EClientSocket) m_client, precentageFromCapital) / currentPrice);
			System.out.println("Current Price: " + currentPrice);
			System.out.println("Current SMA-Z: " + currentSmaz);
			System.out.println("Current Zscore: " + currentZscore);
			System.out.println("Current Buy Strategy: " + currentBuyStrategy);
			System.out.println("Quantity: " + quantitiy);


			//the strategy part
			if (!isInDeal) {
				if (currentBuyStrategy.equals("BUY") && !stopLossTriger.equals("BUY")) {
					isInDeal = true;
					currentPositionType = "SELL";
					startPrice = currentPrice;
					initialTradeQuantity = quantitiy;
					Actions.placeOrder(myWrapper, m_client, myContract, currentPositionType,"MKT",null, null, currentPrice, initialTradeQuantity, false, false);
					System.out.println("ACTION : " + currentPositionType + "|| CURRENT BUYSTRATEGY : " + currentBuyStrategy + " || Current Zscore : " + currentZscore + " || CurentSmaz : " + currentSmaz);
				} else if (currentBuyStrategy.equals("SELL") && !stopLossTriger.equals("SELL")) {
					isInDeal = true;
					currentPositionType = "BUY";
					startPrice = currentPrice;
					initialTradeQuantity = quantitiy;
					Actions.placeOrder(myWrapper, m_client, myContract, currentPositionType,"MKT",null, null, currentPrice, initialTradeQuantity, false, false);
					System.out.println("ACTION : " + currentPositionType + "|| CURRENT BUYSTRATEGY : " + currentBuyStrategy + " || Current Zscore : " + currentZscore + " || CurentSmaz : " + currentSmaz);
				}
			} else {
				if (currentPositionType.equals("BUY")) {
					if (currentBuyStrategy.equals("BUY")) {
						//calculating real PnL
						realReturns.add(BackTest.calculateProfitPercentage(startPrice, currentPrice, currentPositionType));
						isInDeal = false;
						currentPositionType = "";
						Actions.placeOrder(myWrapper, m_client, myContract, "SELL","MKT",null, null, currentPrice, initialTradeQuantity, false, false);
						System.out.println("ACTION : SELL " + "|| CURRENT BUYSTRATEGY : " + currentBuyStrategy + " || Current Zscore : " + currentZscore + " || CurentSmaz : " + currentSmaz);
					}
				} else if (currentPositionType.equals("SELL")) {
					if (currentBuyStrategy.equals("SELL")) {
						//calculating real PnL
						realReturns.add(BackTest.calculateProfitPercentage(startPrice, currentPrice, currentPositionType));
						isInDeal = false;
						currentPositionType = "";
						Actions.placeOrder(myWrapper, m_client, myContract, "BUY","MKT",null, null, currentPrice, initialTradeQuantity, false, false);
						System.out.println("ACTION : SELL " + "|| CURRENT BUYSTRATEGY : " + currentBuyStrategy + " || Current Zscore : " + currentZscore + " || CurentSmaz : " + currentSmaz);
					}
				}
			}

			//checkSituation

			while (timeInLoop(candlesTimeFrame, currentLoopStartTime)) {
				Thread.sleep(5000);
				lastTickPrice = myWrapper.getCurrentTickPrice(98);
				currentPnL = BackTest.calculateProfitPercentage(startPrice, lastTickPrice, currentPositionType);
				System.out.println("CURRENT P&L : " + currentPnL + " % ");

				if (currentPnL <= -initialRiskPrecent) {
					System.out.print("!!!! STOP LOSS JUMPED !!!!!!");
					if (currentPositionType.equals("BUY")) {
						isInDeal = false;
						currentPositionType = "SELL";
						stopLossTriger="SELL";
						Actions.placeOrder(myWrapper, m_client, myContract, currentPositionType,"MKT",null, initialRiskPrecent, lastTickPrice, initialTradeQuantity, false, false);
					} else {
						isInDeal = false;
						currentPositionType = "BUY";
						stopLossTriger="BUY";
						Actions.placeOrder(myWrapper, m_client, myContract, currentPositionType,"MKT",null, initialRiskPrecent, lastTickPrice, initialTradeQuantity, false, false);
					}
				}
			}

			//reset barsDict
			myWrapper.resetBarsDict();
			closingPrices.clear();

			//calculatePn;
			System.out.println("MY REAL PNL : "+realReturns.stream().mapToDouble(Double::doubleValue).sum());
			System.out.println(" MY compound PNL :"+Actions.calculateCompoundPnL(realReturns));

		}
	}
}


