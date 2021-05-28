package visitor;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import table.MySymbolTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConstantFoldingVisitor  extends PostorderJmmVisitor<Boolean, List<Report>> {
    public ConstantFoldingVisitor(MySymbolTable symbolTable, List<Report> report_list) {

        addVisit("Add", this::verifySum);
        addVisit("Sub", this::verifySub);
        addVisit("Mult", this::verifyMult);
        addVisit("Div", this::verifyDiv);
        addVisit("LessThan", this::verifyLessThan);
        addVisit("And", this::verifyAnd);
        addVisit("Parentheses", this::verifyParentheses);
    }

    private List<Report> verifyParentheses(JmmNode node, Boolean aBoolean) {
        if(node.getChildren().size() != 1){
            return new ArrayList<>();
        }
        if(node.getChildren().get(0).getKind().equals("Integer")){
            ((JmmNodeImpl) node).setKind("Integer");
            node.put("value", node.getChildren().get(0).get("value"));
        }

        else if(node.getChildren().get(0).getKind().equals("True") || node.getChildren().get(0).getKind().equals("False")){
            ((JmmNodeImpl) node).setKind(node.getChildren().get(0).getKind());
            node.put("value", node.getChildren().get(0).get("value"));
        }
        else{
            return new ArrayList<>();
        }
        node.removeChild(0);
        return new ArrayList<>();
    }

    private List<Report> verifyLessThan(JmmNode node, Boolean aBoolean) {
        if(node.getChildren().get(0).getKind().equals("Integer") && node.getChildren().get(1).getKind().equals("Integer")){
            String result = String.valueOf(Integer.parseInt(node.getChildren().get(0).get("value")) < Integer.parseInt(node.getChildren().get(1).get("value")));

            result = result.toLowerCase();
            node.put("value", result);
            result = result.substring(0, 1).toUpperCase() + result.substring(1);

            ((JmmNodeImpl) node).setKind(result);
            node.removeChild(1);
            node.removeChild(0);
        }
        return new ArrayList<>();

    }

    private boolean nodeIsBooleanConstant(JmmNode node){
        return (node.getKind().equals("True") || node.getKind().equals("False"));
    }

    private List<Report> verifyAnd(JmmNode node, Boolean aBoolean) {
        JmmNode firstChild = node.getChildren().get(0);
        JmmNode secondChild = node.getChildren().get(1);
        if(nodeIsBooleanConstant(firstChild) && nodeIsBooleanConstant(secondChild)){

            String result;
            if(firstChild.getKind().equals("True") && secondChild.getKind().equals("True")){
                result = "True";
            }
            else{
                result = "False";
            }

            ((JmmNodeImpl) node).setKind(result);
            node.put("value", result);
            node.removeChild(1);
            node.removeChild(0);
        }
        return new ArrayList<>();
    }

    public List<Report> verifySum(JmmNode node, Boolean aBoolean) {
        if(node.getChildren().get(0).getKind().equals("Integer") && node.getChildren().get(1).getKind().equals("Integer")){
            String result = String.valueOf(Integer.parseInt(node.getChildren().get(0).get("value")) + Integer.parseInt(node.getChildren().get(1).get("value")));
            ((JmmNodeImpl) node).setKind("Integer");
            node.put("value", result);
            node.removeChild(1);
            node.removeChild(0);
        }
        return new ArrayList<>();
    }

    private List<Report> verifySub(JmmNode node, Boolean aBoolean) {
        if(node.getChildren().get(0).getKind().equals("Integer") && node.getChildren().get(1).getKind().equals("Integer")){
            String result = String.valueOf(Integer.parseInt(node.getChildren().get(0).get("value")) - Integer.parseInt(node.getChildren().get(1).get("value")));
            ((JmmNodeImpl) node).setKind("Integer");
            node.put("value", result);
            node.removeChild(1);
            node.removeChild(0);
        }
        return new ArrayList<>();
    }

    private List<Report> verifyMult(JmmNode node, Boolean aBoolean) {
        if(node.getChildren().get(0).getKind().equals("Integer") && node.getChildren().get(1).getKind().equals("Integer")){
            String result = String.valueOf(Integer.parseInt(node.getChildren().get(0).get("value")) * Integer.parseInt(node.getChildren().get(1).get("value")));
            ((JmmNodeImpl) node).setKind("Integer");
            node.put("value", result);
            node.removeChild(1);
            node.removeChild(0);
        }
        return new ArrayList<>();
    }

    private List<Report> verifyDiv(JmmNode node, Boolean aBoolean) {
        if (node.getChildren().get(0).getKind().equals("Integer") && node.getChildren().get(1).getKind().equals("Integer")) {
            String result = String.valueOf(Integer.parseInt(node.getChildren().get(0).get("value")) / Integer.parseInt(node.getChildren().get(1).get("value")));
            ((JmmNodeImpl) node).setKind("Integer");
            node.put("value", result);
            node.removeChild(1);
            node.removeChild(0);
        }
        return new ArrayList<>();
    }





}
