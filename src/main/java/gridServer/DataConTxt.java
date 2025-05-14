package gridServer;

import org.hsqldb.lib.LineReader;

import java.io.*;
import java.util.List;
import java.util.Map;

public class DataConTxt implements DataConnector{

    static String defaultFileExtension = "txt";
    static String defaultFileExtension_ = "." + defaultFileExtension;

    static boolean fileAsOneLine = false; // needed for mermaid etc??

    static final String dataDir = "data/";

    static String completeFileName(String fileName) {
        return  (fileName.contains("/") ? "" : dataDir ) + fileName + (fileName.contains(".") ? "" : defaultFileExtension_);
    }


    @Override
    public boolean matches(String query, Map<String, String> params) {
        return query.matches(".*\\.(md|txt|"+defaultFileExtension+")\\b");
    }

    @Override
    public QryResponse run(String qry, Map<String, String> params, QryResponse qryResponse) {
        String fileName = qry;
        String id = params.get("id");
        QryResponse data = new QryResponse(params);
        if (new File(completeFileName(fileName)).exists()){
            if (null != id && id.matches("[0-9]+,[0-9]+") && null != params.get("val")) {
                // UPDATE
                String[] i = id.split(",");
                updateFile( fileName, new String[]{params.get("val")}, Integer.valueOf(i[0]), Integer.valueOf(i[1]), false);

            } else if (null != id && id.matches("[0-9]+") && (null == params.get("val") || params.get("val").isEmpty() )) {
                updateFile(fileName, new String[]{""}, Integer.valueOf(id), 0, true);
            }
            data = load(fileName, data);
        } else {
            main.notifyError("file does not exists: " + completeFileName(fileName) );
        }
        return data;
    }


    @Override
    public boolean checkConnection(Map<String, String> params){
        boolean ping = (new File(dataDir)).exists();
        System.out.println("DataConFile.ping: " + (ping? "FAIL" : "valid"));
        return ping;
    }

    @Override
    public boolean isMatchingUrlType(UrlHelper url) {
        return false;
    }


    QryResponse load(String fileName, QryResponse data) {
        System.out.println( fileName + " load");
        if (fileName.matches("[a-zA-Z0-9_.-]+")) {

            File file = new File(completeFileName(fileName));
            data.appendRow("doc"); // header
            try (FileInputStream reader = new FileInputStream(file)) {
                LineReader lineReader = new LineReader(reader, "UTF-8");
                while (true) {
                    String line = lineReader.readLine();
                    if (null == line) break;
                    if (fileAsOneLine) {
                        data.append(line + "\\n");
                    } else {
                        data.appendRow(line);
                    }
                    System.out.println(line);
                }
                lineReader.close();
            } catch (IOException e) {
                main.notifyError("Error reading file: " + fileName + " " + e.getMessage());
                e.printStackTrace();
            }
        }
        return data;
    }




    public static void updateFile(String fileNameBase, String[] linesUpdate,
                                  int row, int col, boolean insertRow)  {
        String fileName = fileNameBase;
        if (fileName.matches("[a-zA-Z0-9_.-]+")) {
            fileName = completeFileName(fileName);

            try (FileInputStream reader = new FileInputStream(fileName)) {
                try( FileOutputStream lineWriter = new FileOutputStream(fileName + ".tmp")) {
                    LineReader lineReader = new LineReader(reader, "UTF-8");
                    int rowCurrent = 0;
                    while (true) {
                        String line = lineReader.readLine();
                        if (null == line) break;
                        rowCurrent++;
                        if (rowCurrent == row) {
                            for (String lineUpdate : linesUpdate) {
                                lineWriter.write(lineUpdate.getBytes());
                                lineWriter.write(System.lineSeparator().getBytes());
                            }
                        }
                        if (rowCurrent != row || insertRow) {
                            lineWriter.write(line.getBytes());
                            lineWriter.write(System.lineSeparator().getBytes());
                        }
                    }
                }
            } catch (IOException e) {
                main.notifyError("Error reading file: " + fileName + " " + e.getMessage());
                e.printStackTrace();
            }
            File file = new File(fileName + ".tmp");
            File file2 = new File(fileName);
            file2.delete();
            file.renameTo(file2);
            main.notifyInfo(   (insertRow ? "inserted " : "updated ") + fileName + "[" + row + ", " + col + "]");
        }
    }




    public static void insertTxt(String fileNameBase, List<String[]> csvBody )  {

        String fileName = fileNameBase;
        if (fileName.matches("[a-zA-Z0-9_.-]+")) {
            fileName = completeFileName(fileName);

            try( FileOutputStream lineWriter = new FileOutputStream(fileName )) {
                for (String[] lineCols : csvBody) {
                    for (String lineUpdate : lineCols) {
                        lineWriter.write(lineUpdate.getBytes());
                        lineWriter.write(System.lineSeparator().getBytes());
                    }
                }
            } catch (IOException e) {
                main.notifyError("Error writing file: " + fileName + " " + e.getMessage());
                e.printStackTrace();
            }
            main.notifyInfo(   "saved " + fileNameBase);
        }

    }


}
