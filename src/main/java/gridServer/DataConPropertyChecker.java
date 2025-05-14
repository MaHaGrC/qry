package gridServer;

import org.apache.commons.text.StringSubstitutor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataConPropertyChecker implements DataConnector{

    String fileName = "prop.check";

    @Override
    public boolean matches(String query, Map<String, String> params) {
        return query.matches("prop.check");
    }

    @Override
    public QryResponse run(String query, Map<String, String> params, QryResponse qryResponse) {

        String result = "";
        String filter = "";
        //filter = "iAGENT INTEG UI";
        List<List<String>> data = new ArrayList<>();
        Map<String,String> props = new HashMap<>();
        //
        try {
            String fileNameProperties = "..\\lmg\\lmg_osp\\Lmg_Server\\conf\\ear\\jar\\ipim\\ipim-custom.properties";

            readProps(props, fileNameProperties);
            readProps(props, "..\\lmg\\lmg_osp\\Lmg_Server\\conf\\ear\\jar\\ipim\\override\\ipim-development-custom.properties");

            Scanner scanner;
            System.out.println("DataConPropertyChecker " + fileName + " reading ...");
            scanner = new Scanner(new File("data/" + fileName));
            List<String> list = new ArrayList<>();
            data.addAll(Collections.singleton(List.of("property_name = property_value # comment  # rule".split(" *[=#] *"))));
            Pattern pattern = Pattern.compile("^([^= ]+) *(==|=) *([^#][^ ])? *(#[^#]*)? *(#.*)?");
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    String key = matcher.group(1);
                    String operation = matcher.group(2);
                    String value = matcher.group(3);
                    String comment = matcher.group(4);
                    String rule = matcher.group(5);
                    list.clear();
                    list.add( key );
                    list.add( operation );
                    list.add( value );
                    list.add( comment );
                    if ("=".equals(operation)) {
                        props.put(key, evaluate(props, value ));
                    }
                    String rule_result = checkRule(props, key, value, rule);
                    list.add( rule_result);
                    list.add( rule );
                    System.out.println("OK " + line);
                } else {
                    System.out.println("WRN ignore unknown pattern ("+ line +")");
                }

                if (!list.isEmpty()) {
                    // ensure equal size
                    while (list.size()< 5){
                        list.add(new String(""));
                    }

                    data.add(list);

                }
            }

            scanner.close();
        } catch (IOException  e) {
            throw new RuntimeException(e);
        }
        //
        //data.add("property_name = property_value # comment ".split(" *[=#] *"));
        //data.add("upload.ftp.user.server.1=iviewadmin # user for Server1".split(" *[=#] *"));
        //
        checkRule( props, data);

        for (List<String> row : data) {
            /*
            if (!result.isEmpty() && row.length > 3){
                // skipp coloring on header ...
                String color = row[3].contains("Skipp") ? "grey" :!row[3].contains("FAIL") ? "green" : row[3].contains("OK") || row[3].contains("FAIL '4") ? "orange" : "red" ;
                if (!row[2].isEmpty()) { // 1st connector color always
                    row[0] = "<div style='color: " + color + "'>" + row[0] + "</div>";
                    row[1] = "<a href='" + row[1] + "' style='color: " + color + "'>" + row[1] + "</a>";
                    row[2] = "<div style='color: " + color + "'>" + row[2] + "</div>";
                    row[3] = "<div style='color: " + color + "'>" + row[3] + "</div>";
                } else {  // fallback only url
                    row[0] = "<div style='color: " + "grey" + "'>" + row[0] + "</div>";
                    color = color.replace("red", "grey"); // assume fallbacks fail ,-(
                    row[1] = "<a href='" + row[1] + "' style='color: " + color + "'>" + row[1] + "</a>";
                    row[2] = "<div style='color: " + color + "'>" + row[2] + "</div>";
                    row[3] = "<div style='color: " + color + "'>" + row[3] + "</div>";
                }
            }
            */
            result += "\"" + String.join("\",\"",row) + "\"" + System.lineSeparator();
        }

        return new QryResponse(params, result);
    }

    private void checkRule(Map<String, String> props, List<List<String>> data) {

        // asset.prefix.external.1.jpg
        List<String> assetSystems = List.of("1".split(" "));
        List<String> assetExtension = List.of("jpg png pdf".split(" "));
        List<String> assetPart = List.of("prefix suffix".split(" "));
        List<String> assetSize = List.of(". icon small original zoom".split(" "));

        // artificial names start with "templates"




    }

    public static List<String> process(Map<String, String> props, List<String> data) {
        Pattern patternComment = Pattern.compile("^#.*|^(.*) # .*");
        Pattern patternSubst = Pattern.compile(".*(%\\{([^}]+)\\}).*");
        Pattern patternIf = Pattern.compile("( *)(ifdef|ifndef|else|endif) ([^ ]*) *");
        Pattern patternOption = Pattern.compile("( *)(?:([^ =:]*) (.?<)|(>>) *([^ ]*)) *");
        Pattern pattern = Pattern.compile("( *)([^ =:]*) *= *(.*[^ ])? *");
        List<String> dataOut = new ArrayList<>(); // allow inserting ...
        boolean cond = true;
        int indent = 0;
        int indent_end = 0;
        String operation = null;
        String key = null;
        List<String> keysSet = null;
        String val = null;
        String delimiter = null;
        String comment = null;
        Matcher matcher = null;
        for (String row : data) {
            dataOut.add( row);
            keysSet = null;
            System.out.println("      \""+row+"\"");
            //
            if ((matcher = patternComment.matcher(row)).matches()){
                row = matcher.group(1);
            }
            if ( null != row) {
                while ((matcher = patternSubst.matcher(row)).matches()) {
                    String tmp_pattern = matcher.group(1);
                    String tmp_key = matcher.group(2);
                    String tmp_val = Util.optional(props.get( tmp_key),"");
                    row = row.replace(tmp_pattern, tmp_val);
                }
            }
            //
            if ( null == row) {
            } else if ((matcher = patternIf.matcher(row)).matches()) {
                indent = matcher.group(1).length();
                operation = matcher.group(2);
                key = matcher.group(3);
                if ("ifdef".equals(operation) || "ifndef".equals(operation)) {
                    cond = null != props.get(key) && !props.get(key).isEmpty();
                    if (operation.contains("n")) {
                        cond = !cond;
                    }
                    // disable all until end .... or prop on same/lower indent
                    indent_end = indent;
                } else {
                    cond = "else".equals(operation) ? !cond : true; // else // endif
                }
            }  else if (!cond && indent_end < indent) {
                // skipp negative-if-branch
            } else if ((matcher = patternOption.matcher(row)).matches()) { // "( *)(?:([^ ]*) (<)*|(>>) *([^ ]*)) *"
                indent = matcher.group(1).length();
                key = Util.optional( matcher.group(2), key) ; // set key and keep it ....
                operation = Util.optional( matcher.group(3), matcher.group(4));
                val = matcher.group(5);
                if (operation.endsWith("<")) {
                    delimiter = operation.length()>1 ? operation.substring(0,1) : null;
                } else if (operation.equals(">>")) {
                    if (null != delimiter && props.containsKey(key)){
                        val = props.get(key) + delimiter + val;
                    }
                    keysSet = setProp(props, key, val);
                }
            } else if ((matcher = pattern.matcher(row)).matches()) {
                indent = matcher.group(1).length();
                key = matcher.group(2);
                val = matcher.group(3);
                if (!cond && indent_end < indent) {
                } else  {
                    cond = true;
                    keysSet = setProp(props, key, val);
                }
            }
            //
            if (null != keysSet && (keysSet.size() > 1 || !keysSet.get(0).equals(key))){
                for (String temp_key : keysSet) {
                    // insert multiplied keys with value, use index+2 to indicate insertation
                    dataOut.add( " ".repeat( indent + 2) +  temp_key + " = " + val );
                }
            }
            //
            if (null == row) {
                System.out.println("INFO \"" + comment + "\"");
            } else if (null == matcher || !matcher.matches()) {
                System.out.println("SKIPP \"" + row+ "\"");
            } else {
                System.out.println("      \""+row+"\" :: " + props.toString() );
            }
        }
        return dataOut;
    }

    private static List<String> setProp(Map<String, String> props, String key, String value) {
        List<String> keysSet = new ArrayList<>();
        Pattern patternMultiply = Pattern.compile("(.*?)(?:\\{([^}]+)\\})(.*)");
        Matcher matcher = null;
        if ((matcher = patternMultiply.matcher(key)).matches()){
            String tmp_key_prefix = matcher.group(1);
            String[] keys = matcher.group(2).split("\\|");
            String tmp_key_suffix = matcher.group(3);
            for (String keyX : keys) {
                keysSet.addAll( setProp( props, tmp_key_prefix + keyX + tmp_key_suffix, value));
            }
        } else {
            keysSet.add(key);
            props.put( key, value);
        }
        return keysSet;
    }


    private static void readProps(Map<String, String> props, String fileNameProperties) throws FileNotFoundException {
        System.out.println("DataConPropertyChecker " + fileNameProperties + " reading ...");
        Scanner scanner = new Scanner(new File( fileNameProperties ));
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] splits = line.split(" *[=#] *");
            if (splits.length>1) {
                props.put( splits[0], splits[1]);
            }
        }
        scanner.close();
        System.out.println("DataConPropertyChecker " + fileNameProperties + "  " + props.size() + " properties found." );
    }

    private String evaluate(Map<String, String> props, String valWithTemplates) {
        // replace place-holder  %{template.iview.server.uri}
        StringSubstitutor stringSubstitutor = new StringSubstitutor(props);
        String valueEvaluated = stringSubstitutor.replace(valWithTemplates);
        return valueEvaluated ;
    }

    private String checkRule(Map<String, String> props, String key, String valWithTemplates, String rule) {
        String msg = rule;
        // [ OK | FAIL | NONE ]  [MSG ]
        //
        String valueExpected = evaluate(props, valWithTemplates);
        //
        // check for
        //      - equal "="
        if (valueExpected.equals(props.get(key))) {
            msg = "OK";
        } else {
            msg= "FAIL " + "(expected: " + valueExpected + ")";
        }

        return msg ;
    }

    @Override
    public boolean checkConnection(Map<String, String> params){
        return true;
    }


    @Override
    public boolean isMatchingUrlType(UrlHelper url) {
        return false;
    }


}
