import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExcelWriter {
    private final String path;
    private final File myFile ;
    private Workbook workbook ;
    private Sheet currentSheet ;

    //CONSTRUCTOR1
    public ExcelWriter(String path,Boolean backtestMode,int strategyNumber,String existingFilePath) throws IOException {
        //workbook init
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = sdf.format(new Date());
        String filename = "output_" + timestamp + ".xlsx";
        if (existingFilePath.equals("")) {
            this.path = path + File.separator + filename;
            this.myFile=new File(this.path);
            this.workbook = new XSSFWorkbook();
            this.currentSheet = workbook.createSheet("sheet1");

            //creating a new headers to an empty file
            if (strategyNumber==1){
                    // Create header row
                    Row headerRow = currentSheet.createRow(0);
                    Cell headerCell0 = headerRow.createCell(0);
                    headerCell0.setCellValue("Date");
                    Cell headerCell1 = headerRow.createCell(1);
                    headerCell1.setCellValue("Action");
                    Cell headerCell2 = headerRow.createCell(2);
                    headerCell2.setCellValue("Strategy Signal");
                    Cell headerCell7= headerRow.createCell(3);
                    headerCell7.setCellValue("Current Price");
                    Cell headerCell3 = headerRow.createCell(4);
                    headerCell3.setCellValue("Zscrore");
                    Cell headerCell4 = headerRow.createCell(5);
                    headerCell4.setCellValue("smaZ");
                    Cell headerCell5 = headerRow.createCell(6);
                    headerCell5.setCellValue("COMPOUND PNL");
                    Cell headerCell6 = headerRow.createCell(7);
                    headerCell6.setCellValue("REAL PNL");
                // Auto size columns
                ExcelWriter.autoSizeColumn(this.currentSheet,0,backtestMode ? 8 : 3);
            } else if (strategyNumber==2) {
                if(backtestMode){
                    // Create header row
                    Row headerRow = currentSheet.createRow(0);
                    Cell headerCell0 = headerRow.createCell(0);
                    headerCell0.setCellValue("TIME");
                    Cell headerCell1 = headerRow.createCell(1);
                    headerCell1.setCellValue("MACD slow");
                    Cell headerCell2 = headerRow.createCell(2);
                    headerCell2.setCellValue("MACD fast");
                    Cell headerCell3 = headerRow.createCell(3);
                    headerCell3.setCellValue("signle line");
                    Cell headerCell4 = headerRow.createCell(4);
                    headerCell4.setCellValue("RSI");
                    Cell headerCell5 = headerRow.createCell(5);
                    headerCell5.setCellValue("COMPOUND PNL");
                    Cell headerCell6 = headerRow.createCell(6);
                    headerCell6.setCellValue("REAL PNL");
                    Cell headerCell7 = headerRow.createCell(7);
                    headerCell7.setCellValue("back testing time");
                    Cell headerCell8 = headerRow.createCell(8);
                    headerCell8.setCellValue("back testing start date");
                }
                else{
                    // Create header row
                    Row headerRow = currentSheet.createRow(0);
                    Cell headerCell0 = headerRow.createCell(0);
                    headerCell0.setCellValue("TIME");
                    Cell headerCell1 = headerRow.createCell(1);
                    headerCell1.setCellValue("TICKER");
                    Cell headerCell2 = headerRow.createCell(2);
                    headerCell2.setCellValue("COMPOUND PNL");
                    Cell headerCell3 = headerRow.createCell(3);
                    headerCell3.setCellValue("REAL PNL");
                }
                // Auto size columns
                ExcelWriter.autoSizeColumn(this.currentSheet,0,backtestMode ? 8 : 3);
            }

            // Write the output to the specified file path
            try (FileOutputStream fileOut = new FileOutputStream(this.path)) {
                workbook.write(fileOut);
            }
        } else {
            this.path = existingFilePath;
            this.myFile = new File(this.path);
            if (myFile.exists()) {
                this.workbook = new XSSFWorkbook(new FileInputStream(this.path));
                this.currentSheet = this.workbook.getSheetAt(0);
            } else {
                throw new IOException("Specified file does not exist: " + existingFilePath);
            }
        }
    }

    // GETTERS:
    public String getPath() {
        return this.path;
    }
    public File getMyFile() {
        return this.myFile;
    }
    public Workbook getWorkbook() {
        return this.workbook;
    }
    public Sheet getCurrentSheet() {
        return this.currentSheet;
    }
    public String getCellValue (int sheetIndex , int rowNum ,int columnNum){
        Sheet sheet = workbook.getSheetAt(sheetIndex);    // Get the first sheet
        Row row = sheet.getRow(rowNum);               // Get the second row (0-based index)
        Cell cell = row.getCell(columnNum);              // Get the second cell (0-based index)

        // Determine cell type and retrieve value accordingly
        switch (cell.getCellType()) {
            case STRING:
                String stringValue = cell.getStringCellValue();
                return stringValue;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date dateValue = cell.getDateCellValue();
                    return dateValue.toString();
                } else {
                    double numericValue = cell.getNumericCellValue();
                    return String.valueOf(numericValue);
                }
            case BOOLEAN:
                boolean booleanValue = cell.getBooleanCellValue();
                return String.valueOf(booleanValue);
            case FORMULA:
                String formulaValue = cell.getCellFormula();
                return formulaValue;
            default:
                return "Unknown cell type";
        }
    }


    //METHODS
    public void save() throws IOException {
        try (FileOutputStream fileOut = new FileOutputStream(this.path)) {
            workbook.write(fileOut);
        }
    }
    public void writeInCell(int columNumber , int rowNumber , String cellValue){
        //select existing row  / create new row
       Row currentRow=this.currentSheet.getRow(rowNumber);
       if(currentRow==null){
           currentRow=this.currentSheet.createRow(rowNumber);
       }
       // create blank cell / get existing cell
       Cell cell =currentRow.getCell(columNumber, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
       //setting sell value
        cell.setCellValue(cellValue);
    }
    public static void writeBacktestPnLStrategy2(ExcelWriter excelWriter ,int rowCounter,int macdSlow,int macdFast,int signalLine,int RSI,Double compoundPnL , Double realPnL,String backTestingTime,String backTestingStartDate) throws IOException {
        //getting current loop time stamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd_HH:mm:ss");
        String timestamp = sdf.format(new Date());
        //writing data to excel sheet
        excelWriter.writeInCell(0,rowCounter,timestamp);
        excelWriter.writeInCell(1,rowCounter,Integer.toString(macdSlow));
        excelWriter.writeInCell(2,rowCounter,Integer.toString(macdFast));
        excelWriter.writeInCell(3,rowCounter,Integer.toString(signalLine));
        excelWriter.writeInCell(4,rowCounter,Integer.toString(RSI));
        excelWriter.writeInCell(5,rowCounter,compoundPnL.toString());
        excelWriter.writeInCell(6,rowCounter,realPnL.toString());
        excelWriter.writeInCell(7,rowCounter,backTestingTime);
        excelWriter.writeInCell(8,rowCounter,backTestingStartDate);
        // Auto size columns
        ExcelWriter.autoSizeColumn(excelWriter.currentSheet, 0,8);
        //save changes
        excelWriter.save();
    }
    public static void writeBacktestPnLStrategy1(ExcelWriter excelWriter ,int rowCounter,Double currentPrice,String currentAction,Double currentZscore,Double currentSmaZ,String currentDate,String currentBuyStrategy , Double currentRealPnL,Double currentCompoundPnL) throws IOException {
        //getting current loop time stamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd_HH:mm:ss");
        String timestamp = sdf.format(new Date());
        //writing data to excel sheet
        excelWriter.writeInCell(1,rowCounter,currentBuyStrategy);
        excelWriter.writeInCell(2,rowCounter,currentZscore.toString());
        excelWriter.writeInCell(3,rowCounter,currentSmaZ.toString());
        excelWriter.writeInCell(4,rowCounter,currentAction);
        excelWriter.writeInCell(5,rowCounter,currentDate);
        excelWriter.writeInCell(6,rowCounter, String.valueOf(currentPrice));


            excelWriter.writeInCell(7,rowCounter, String.valueOf(currentRealPnL));
            excelWriter.writeInCell(8,rowCounter, String.valueOf(currentCompoundPnL));

        // Auto size columns
        ExcelWriter.autoSizeColumn(excelWriter.currentSheet, 0,8);
        //save changes
        excelWriter.save();
    }
    public static void writeStrategy2PnL(ExcelWriter excelWriter ,int rowCounter,String ticker,Double compoundPnL , Double realPnL) throws IOException {
        //getting current loop time stamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd_HH:mm:ss");
        String timestamp = sdf.format(new Date());
        //writing data to excel sheet
        excelWriter.writeInCell(0,rowCounter,timestamp);
        excelWriter.writeInCell(1,rowCounter,ticker);
        excelWriter.writeInCell(2,rowCounter,compoundPnL.toString());
        excelWriter.writeInCell(3,rowCounter,realPnL.toString());
        // Auto size columns
        ExcelWriter.autoSizeColumn(excelWriter.currentSheet, 0,3);
        //save changes
        excelWriter.save();
    }
    public static void writeStrategy1PnL(ExcelWriter excelWriter ,int rowCounter,boolean backTestMode,String currentAction,String currentPrice,String currentBuyStrategy,String currentZscore,String currentSmaZ,String currentRealPnL ,String currentCompoundPnL,String currentDate) throws IOException {
        //getting current loop time stamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd_HH:mm:ss");
        String timestamp = sdf.format(new Date());
        //writing data to excel sheet
        if (backTestMode) {
            excelWriter.writeInCell(0, rowCounter, currentDate);
        } else {
            excelWriter.writeInCell(0, rowCounter, timestamp);
        }
        excelWriter.writeInCell(1,rowCounter,currentAction);
        excelWriter.writeInCell(2,rowCounter,currentBuyStrategy);
        excelWriter.writeInCell(3,rowCounter,currentPrice);
        excelWriter.writeInCell(4,rowCounter,currentZscore);
        excelWriter.writeInCell(5,rowCounter, currentSmaZ);


        excelWriter.writeInCell(6,rowCounter, currentCompoundPnL);
        excelWriter.writeInCell(7,rowCounter, currentRealPnL);

        // Auto size columns
        ExcelWriter.autoSizeColumn(excelWriter.currentSheet, 0,8);
        //save changes
        excelWriter.save();
    }
    public static void autoSizeColumn(Sheet currentSheet , int columnStart,int columnEnd){
        for(int i= columnStart ; i<=columnEnd;i++){
            currentSheet.autoSizeColumn(i);
        }
    }
}
