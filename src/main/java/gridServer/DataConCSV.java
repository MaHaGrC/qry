package gridServer;

import com.opencsv.*;
import com.opencsv.exceptions.CsvException;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DataConCSV implements DataConnector{
    @Override
    public boolean matches(String query, Map<String, String> params) {
        return query.matches(".*\\.csv\\b");
    }

    static final String dataDir = "data/";

    static String completeFileName(String fileName) {
        return  (fileName.contains("/") ? "" : dataDir ) + fileName + (fileName.contains(".") ? "" : ".csv");
    }

    static class CSVConfig {

        public String delimiter;
        public String quote;
        public String escape;
        public String encoding = "UTF-8";
        public String lineSeparator = System.lineSeparator();
        public String fileName = "";
        public char commentMarker = '#';
        public boolean skipLines = false;
        public boolean ignoreLeadingWhiteSpace = true;
        public boolean ignoreEmptyLines = true;
        public boolean ignoreQuotations = false;
        public boolean trim = true;

        private FileInputStream fis;
        private InputStreamReader reader;
        private FileOutputStream fos;
        private OutputStreamWriter writer;
        private ICSVWriter csvWriter;
        private CSVReader csvReader;

        public CSVConfig(String delimiter, String quote, String escape) {
            this.fileName = "";
            this.delimiter = delimiter;
            this.quote = quote;
            this.escape = escape;
        }

        public CSVConfig(String fileName) {
            this.fileName = fileName;
        }


        public String getFirstLine() {
            try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
                return reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                return "";
            }
        }

        private void initQuoteDelimiterEscape() {
            // assume the first line is a header and contains all information
            String firstLine = getFirstLine();
            // assume start with quote if quote is used
            quote = firstLine.substring(0, 1).replaceAll("[^\"']","");
            // assume if quote is used, before or after a quote we find the delimiter
            String quoteAndDelim = firstLine.replaceAll("[^,;|" + quote + "]","");
            delimiter = quoteAndDelim.replaceAll(quote+quote,"").substring(0,1);
            // assume escape is "\\"
            escape = "\\";
        }

        public CSVParser createParser(){
            if (null == delimiter || delimiter.isEmpty()) {
                initQuoteDelimiterEscape();
            }
            return new CSVParserBuilder()
                    .withSeparator(delimiter.charAt(0))
                    .withQuoteChar(quote.isEmpty() ? CSVWriter.NO_QUOTE_CHARACTER : quote.charAt(0))
                    .withEscapeChar(escape.charAt(0))
                    .build();
        }

        public CSVReader getReader() throws FileNotFoundException, UnsupportedEncodingException {
            fis = new FileInputStream(fileName);
            reader = new InputStreamReader(fis, encoding);
            csvReader = new CSVReaderBuilder(reader)
                    .withCSVParser( createParser() )
                    .withSkipLines( skipLines ? 1 : 0)
                    .build();
            return csvReader;
        }

        public ICSVWriter getWriter() throws FileNotFoundException, UnsupportedEncodingException {
            fos = new FileOutputStream(fileName);
            writer = new OutputStreamWriter( fos, encoding);
            csvWriter = new CSVWriterBuilder(writer)
                    .withSeparator(delimiter.charAt(0))
                    .withQuoteChar(quote.isEmpty() ? CSVWriter.NO_QUOTE_CHARACTER : quote.charAt(0))
                    .withEscapeChar(escape.charAt(0))
                    .build();
            return csvWriter;
        }

        public void close() throws IOException {
            if (null != fis) fis.close();
            if (null != reader) reader.close();
            if (null != csvReader) csvReader.close();
            if (null != fos) fos.close();
            if (null != writer) writer.close();
            if (null != csvWriter) csvWriter.close();
            fis = null;
            reader = null;
            csvReader = null;
            fos = null;
            writer = null;
            csvWriter = null;
        }


    }

    @Override
    public QryResponse run(String qry, Map<String, String> params, QryResponse qryResponse) {
        String fileName = qry;
        String id = params.get("id");
        String data = null;
        if (new File(completeFileName(fileName)).exists()){
            if (null != id && id.matches("[0-9]+,[0-9]+") && null != params.get("val")) {
                // UPDATE
                String[] i = id.split(",");
                updateCSV( fileName, params.get("val"), Integer.valueOf(i[0]), Integer.valueOf(i[1]));

            } else if (null != id && id.matches("[0-9]+") && (null == params.get("val") || params.get("val").isEmpty() )) {
                insertRowCSV(fileName, Integer.valueOf(id));
            }
            List<String[]> allRows = load(fileName, qryResponse);
            qryResponse.appendRows( allRows);
        } else {
            main.notifyError("file does not exists: " + completeFileName(fileName)  );
        }
        return qryResponse;
    }


    @Override
    public boolean checkConnection(Map<String, String> params){
        boolean ping = (new File(dataDir )).exists();
        System.out.println("DataConCSV.ping: " + (ping? "FAIL" : "valid"));
        return ping;
    }

    @Override
    public boolean isMatchingUrlType(UrlHelper url) {
        return false;
    }




    static List<String[]> load(String fileName, QryResponse qryResponse) {
        
        List<String[]> allRows = null;
        System.out.println( fileName + " load");
        if (fileName.matches("[a-zA-Z0-9_.-]+")) {


            try {
                CSVConfig csvConfig = new CSVConfig(
                        "data/" + fileName + (fileName.contains(".") ? "" : ".csv"));
                CSVReader csvReader = csvConfig.getReader();
                // Read all lines
                allRows = csvReader.readAll();
                //
                csvConfig.close();

            } catch (IOException | CsvException e) {
                e.printStackTrace();
                main.notifyError(e.getMessage());
            }
            
        }
        return allRows;
    }


    public static void updateCSV(String fileNameBase, String replace,
                                 int row, int col)  {
        String fileName = fileNameBase;
        if (fileName.matches("[a-zA-Z0-9_.-]+")) {
            fileName = completeFileName(fileName);

            try {
                CSVConfig csvConfig = new CSVConfig(fileName);
                CSVReader reader = csvConfig.getReader();
                List<String[]> data = reader.readAll();
                //
                data.get(row)[col] = replace;
                //
                ICSVWriter writer = csvConfig.getWriter();
                writer.writeAll(data);
                writer.flush();
                csvConfig.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                main.notifyError(e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                main.notifyError(e.getMessage());
            } catch (CsvException e) {
                e.printStackTrace();
                main.notifyError(e.getMessage());
            }
            main.notifyInfo("updated " + fileNameBase + "[" + row + ", " + col + "]");
        }
    }

    public static void insertRowCSV(String fileNameBase, int row )  {
        String fileName = fileNameBase;
        if (fileName.matches("[a-zA-Z0-9_.-]+")) {
            fileName = completeFileName(fileName);

            // Read existing file
            try {
                CSVConfig csvConfig = new CSVConfig(fileName);
                CSVReader reader = csvConfig.getReader();
                List<String[]> data = reader.readAll();
                //
                String[] cells = new String[data.get(0).length];
                Arrays.fill(cells, "");
                data.add(row,cells);
                //
                ICSVWriter writer = csvConfig.getWriter();
                writer.writeAll(data);
                writer.flush();
                csvConfig.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                main.notifyError(e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                main.notifyError(e.getMessage());
            } catch (CsvException e) {
                e.printStackTrace();
                main.notifyError(e.getMessage());
            }
            main.notifyInfo("inserted " + fileNameBase + "[" + row + "] row");
        }
    }


    public static void insertCSV(String fileNameBase, List<String[]> csvBody )  {
        String fileName = fileNameBase;
        if (fileName.matches("[a-zA-Z0-9_.-]+")) {
            fileName = completeFileName(fileName);

            try {
                // Write to CSV file which is open
                ICSVWriter writer = new CSVWriterBuilder(new FileWriter(fileName)).build();
                writer.writeAll(csvBody);
                writer.flush();
                writer.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                main.notifyError(e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                main.notifyError(e.getMessage());
            }
            main.notifyInfo("saved " + fileNameBase + "");
        }
    }


}
