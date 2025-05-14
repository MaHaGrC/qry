package gridServer;

import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Util {
    public static <T> T optional(T val, T fallback){
        return null != val ? val : fallback;
    }


    public static String toString(FileTime fileTime) {
        final  SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return df.format((new Date(fileTime.toMillis())));
    }

    public static String toString(Date date) {
        final  SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return df.format(date);
    }


}
