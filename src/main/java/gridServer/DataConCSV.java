package gridServer;

import com.opencsv.*;
import com.opencsv.exceptions.CsvException;
import org.hsqldb.lib.LineReader;

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
            data = load(fileName);
        } else {
            main.notifyError("file does not exists: " + completeFileName(fileName)  );
        }
        return new QryResponse(params, data);
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


    String load(String fileName) {
        StringBuffer data = new StringBuffer();
        System.out.println( fileName + " load");
        if (fileName.matches("[a-zA-Z0-9_.-]+")) {

            File file = new File("data/" + fileName + (fileName.contains(".") ? "" : ".csv"));
            try (FileInputStream reader = new FileInputStream(file)) {
                LineReader lineReader = new LineReader(reader, "UTF-8");
                while (true) {
                    String line = lineReader.readLine();
                    if (null == line) break;
                    //
                    data.append(line + System.lineSeparator());
                    // log
                    line = line.substring(1, line.length() - 1); // cut away "
                    String[] vals = line.split("\";\""); //CSV.split(line);
                    for (String val : vals) {
                        System.out.print(val + " <<>> ");
                    }
                    System.out.println();
                }
                lineReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return data.toString();
    }


    public static void updateCSV(String fileNameBase, String replace,
                                 int row, int col)  {
        String fileName = fileNameBase;
        if (fileName.matches("[a-zA-Z0-9_.-]+")) {
            fileName = completeFileName(fileName);

            // Read existing file
            CSVReader reader = null;

            try {
                reader = new CSVReaderBuilder(new FileReader(fileName)).build();
                List<String[]> csvBody = reader.readAll();
                // TODO improve...
                // retry with different delimiter ...
                if (csvBody.get(row).length - 1  < col || 1 == csvBody.get(0).length ){
                    String headline = String.join(",", csvBody.get(0));
                    char delimiter=  headline.replaceAll("^.*([,;|]).*","$1").charAt(0);
                    reader = new CSVReaderBuilder(new FileReader(fileName)).withCSVParser(new CSVParserBuilder().withSeparator(delimiter).build()).build();
                    csvBody = reader.readAll();
                }
                // get CSV row column  and replace with by using row and column
                csvBody.get(row)[col] = replace;
                reader.close();

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
            CSVReader reader = null;
            try {
                reader = new CSVReaderBuilder(new FileReader(fileName)).build();
                List<String[]> csvBody = reader.readAll();
                // get CSV row column  and replace with by using row and column
                reader.close();

                String[] cells = new String[csvBody.get(0).length];
                Arrays.fill(cells, "");
                csvBody.add(row,cells);

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
