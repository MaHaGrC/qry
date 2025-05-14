package gridServer;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class Notifier {

    String prefix = "";

    public Notifier(String name) {
        this.prefix = name + " ";
    }

    void notify(String msg){
        main.notify( prefix + msg);
    }

    void info(String msg){
        main.notifyInfo( prefix + msg);
    }

    void warning(String msg){
        main.notifyWarn( prefix + msg);
    }

    void error(String msg){
        main.notifyError( prefix + msg);
    }

    public void error(Exception e) {
        main.notifyError( prefix + e.getMessage());
        //e.printStackTrace(); // filtered
        print(e);
    }

    public void error(Exception e, String hint) {
        main.notifyError( prefix + e.getMessage());
        main.notifyInfo(hint);
        //e.printStackTrace(); // filtered
        print(e);
    }

    static void print(Exception e) {
        List<StackTraceElement> stack = new ArrayList<>();
        for (StackTraceElement stackTraceElement : e.getStackTrace()) {
            if (stack.isEmpty() || stackTraceElement.getClassName().startsWith("gridServer.")) {
                stack.add(stackTraceElement);
            }
        }
        e.setStackTrace(stack.toArray(new StackTraceElement[0]));
        //
        e.printStackTrace();
        //
    }
}
