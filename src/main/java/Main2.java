import com.ib.client.*;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Main2 {
	public static void main (String[]args) throws InterruptedException, IOException {
		// Creates a new Wrappers -> EClientSocket + EWrapper
		Ewrapperimpl myWrapper = new Ewrapperimpl();

		//setting my signal +client variables
		final EClientSocket m_client = myWrapper.getClient();
		final EReaderSignal m_signal = myWrapper.getSignal();

		// establish connection to TWS
		m_client.eConnect("127.0.0.1",8001,0);

		//creating new reader object + adding additional thread for the Ereader object
		final EReader reader = new EReader(m_client, m_signal);
		reader.start();
		//An additional thread is created in to manage reader messages
		new Thread(() -> {
			while (m_client.isConnected()) {
				m_signal.waitForSignal();
				try {
					reader.processMsgs();
				} catch (Exception e) {
					System.out.println("Exception: "+e.getMessage());
				}
			}
		}).start();
		Thread.sleep(5000);

		//=========================================== DEFINE VARIABLES ===========================================================================================================================================================================================================
		//MAIN VARIABELS DEFINE   :
		final boolean demoMode = false;
		final boolean backtestingMode = false;
		final String backTestingTime ="5 Y";
		final String backTestStartDate="";
		myWrapper.setBotMode("STK");; //->STK\FX\CFD&FUT ---- > the bot desired contract
		final int strategyNum=1; // 1 - my strategy | 2 - michel bot
		final String candlesTimeFrame = "15 mins";  //==Valid Bar Sizes ---> 1 secs 5 secs	10 secs	15 secs	30 secs ||  1 min	2 mins	3 mins	5 mins	10 mins	15 mins	20 mins	30 mins ||  1 hour	2 hours	3 hours	4 hours	8 hours || 1 day || 1 week || 1 month

		//CAPITAL SETTINGS
		final Double precentageFromCapital =100.0;//the precent from capital you want to use in every trade
		final Double initialRiskPrecent =5.0; // how much money are you willing to risk in total precentage of your investmant
		final int capital = Actions.calculateCapital(myWrapper, m_client, precentageFromCapital);

		//CREATE CONTRACTS
		Contract contract1= Actions.createContract(myWrapper.getBotMode(),"TQQQ","STK","USD","SMART",null);//PRIMARY *(if botMode="STK" / "FX" then LastTradeDateOrContractMonth=null)*
		Contract contract2= Actions.createContract(myWrapper.getBotMode(),"AXLA", "STK","USD","SMART",null);//SECONDERY *(if botMode="STK" / "FX" then LastTradeDateOrContractMonth=null)*
		ArrayList<Contract> contractsList= new ArrayList<>(Arrays.asList(contract1,contract2));
		//Strategy 1 variables
		final boolean inverseStrategy = false ;
		final boolean longShort=true;
		final boolean exponentialStrategy = false ;//currenlty not available !
		final int macdSlow = 21; // mast be bigger then macdFast
		final int macdFast = 12; // mast be smaller then macdSlow
		final int signalLine = 6;
		final int rsi = 14;
			//secondery timeframe
		final String seconderyCandlesTimeFrame="15 mins";
		final int amountOfSeconderyCandles=5;
		String andOrSwicth ="AND";// "AND" / "OR" -> means that the exit strategy will be !secondery || !primerystrategy / !secondery&&!primary

		//Strategy 2 variables:
		final int ZscroeWindow=75;
		final int smaZwindow=75;

		//excel file path
		final String excelFilePath="/Users/pelegfishman/Programming/exclesALGOp";
		final String loadEsitindExcelFilePath="/Users/pelegfishman/Programming/exclesALGOp/output_20231004_211535.xlsx"; // an empty string will start a new file automaticly , and a path will load and exsiting excel to work on .


		//================================================ END ====================================================================================================================================================================================================================
		//BACKTEST / REAL
		if(strategyNum==1){
			if(!backtestingMode) {
                m_client.reqPositions();
                while (!myWrapper.getIsPositionsArived()) {
                    Thread.sleep(500);
                }
                //checking for open positions in case of PC shutdown mid strategy
                boolean isCurrenlyInDeal;
                String currentPositionType;
                Double currentStartPrice = null;
                int currentQuantity = 0;
                if (Integer.parseInt(myWrapper.getCurrentOpenPositionSize().toString()) != 0) {
                    currentQuantity = Integer.parseInt(myWrapper.getCurrentOpenPositionSize().toString());
                    currentStartPrice = myWrapper.getCurrentOpenPositionCost();
                    isCurrenlyInDeal = true;
                    if (Double.parseDouble(myWrapper.getCurrentOpenPositionSize().toString()) > 0) {
                        currentPositionType = "BUY";
                    } else {
                        currentPositionType = "SELL";
                    }
                } else {
                    currentPositionType = "";
                    isCurrenlyInDeal = false;
                }

                //placing the strategy
                Actions.placeOrderStrategy1(myWrapper, m_client, excelFilePath, ZscroeWindow, smaZwindow, candlesTimeFrame, contract1, initialRiskPrecent, precentageFromCapital,isCurrenlyInDeal,currentPositionType,currentQuantity,currentStartPrice,loadEsitindExcelFilePath);
            } else if (backtestingMode) {
				BackTest.backtestStrategy1(myWrapper,m_client,excelFilePath,contract1,backTestStartDate,backTestingTime,candlesTimeFrame,ZscroeWindow,smaZwindow,loadEsitindExcelFilePath);
			}
		}
		else if (strategyNum==2) {
			if(!backtestingMode){
				m_client.reqPositions();
				while(!myWrapper.getIsPositionsArived()) {
					Thread.sleep(500);
				}
				//checking for open positions in case of PC shutdown mid strategy
				boolean isCurrenlyInDeal;
				String currentPositionType;
				if(Integer.parseInt(myWrapper.getCurrentOpenPositionSize().toString()) != 0) {
					isCurrenlyInDeal=true;
					if(Double.parseDouble(myWrapper.getCurrentOpenPositionSize().toString())>0) {
						currentPositionType = "BUY";
					} else {
						currentPositionType = "SELL";
					}
				} else {
					currentPositionType="";
					isCurrenlyInDeal=false;
				}
				//placing the strategy
				Actions.placeOrderStrategy2(myWrapper,m_client,capital,initialRiskPrecent,contract1,contract2,candlesTimeFrame,macdSlow,macdFast,signalLine,rsi,inverseStrategy,longShort,demoMode,isCurrenlyInDeal,currentPositionType,seconderyCandlesTimeFrame,amountOfSeconderyCandles,andOrSwicth,excelFilePath,loadEsitindExcelFilePath);
			}
			else {
				myWrapper.resetBarsDict();
				//requestong historical data + waiting for response
				m_client.reqHistoricalData(999, contract1,backTestStartDate,backTestingTime,candlesTimeFrame,"MIDPOINT",1,1,false,null);
				Actions.waitingForHistData(myWrapper);
//				backtesintg strategy
				int minimumCandles = Actions.calculateMinimumCandlesStrategy2(macdFast, macdSlow, signalLine, rsi);
				HashMap <Integer,ArrayList<Bar>> barsDict = myWrapper.getBarsDict();
				if(barsDict.get(999).size()<=minimumCandles) {
					System.out.println("need more data !! request longer backTestingTime ");
				} else {
					BackTest.backTestStrategy2(myWrapper,m_client,longShort,inverseStrategy,barsDict,initialRiskPrecent,macdFast,macdSlow,signalLine,rsi,minimumCandles,excelFilePath,backTestingTime,backTestStartDate,loadEsitindExcelFilePath);
				}
			}
		}
	}
}