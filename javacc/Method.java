import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.HashMap;
import java.util.List;

public class Method {
    private String name;
    private Type return_type;
    private List<Symbol> parameters;
    private HashMap<Symbol, ?> local_variables;

    public String getName() {
        return name;
    }

    public Type getReturnType() {
        return return_type;
    }

    public List<Symbol> getParameters() {
        return parameters;
    }

    public HashMap<Symbol, ?> getLocal_variables() {
        return local_variables;
    }
}
