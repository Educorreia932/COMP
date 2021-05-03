package stage;

import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.specs.comp.ollir.*;

import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

/**
 * Copyright 2021 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

public class BackendStage implements JasminBackend {
    int stackCount = 0;
    String superClassName;

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        ClassUnit ollirClass = ollirResult.getOllirClass();

        try {
            // Example of what you can do with the OLLIR class

            ollirClass.checkMethodLabels();  // Check the use of labels in the OLLIR loaded
            ollirClass.buildCFGs();          // Build the CFG of each method
            // ollirClass.outputCFGs();         // Output to .dot files the CFGs, one per method
            ollirClass.buildVarTables();     // Build the table of variables for each method
//            ollirClass.show();               // Print to console main information about the input OLLIR

            // Convert the OLLIR to a String containing the equivalent Jasmin code
            String jasminCode = "";

            String classAccessModifier = acessModifierToString(ollirClass.getClassAccessModifier());

            if (classAccessModifier.isEmpty())
                classAccessModifier = "public";

            String className = ollirClass.getClassName();

            jasminCode += String.format(".class %s %s\n", classAccessModifier, className);

            superClassName = ollirClass.getSuperClass();

            if (superClassName == null)
                superClassName = "java/lang/Object";

            jasminCode += String.format(".super %s\n\n", superClassName);

            // Iterate over class methods
            for (Method method : ollirClass.getMethods()) {
                jasminCode += generateMethod(method);

                stackCount = 0;
            }

            // More reports from this stage
            List<Report> reports = new ArrayList<>();

            System.out.println(jasminCode);

            return new JasminResult(ollirResult, jasminCode, reports);
        }

        catch (OllirErrorException e) {
            return new JasminResult(
                ollirClass.getClassName(),
                null,
                Collections.singletonList(
                    Report.newError(
                        Stage.GENERATION,
                        -1,
                        -1,
                        "Exception during Jasmin generation",
                        e
                    )
                )
            );
        }
    }

    private String acessModifierToString(AccessModifiers accessModifier) {
        switch (accessModifier) {
            case PUBLIC:
                return "public";

            case PRIVATE:
                return "private";

            case PROTECTED:
                return "protected";

            case DEFAULT:
                return "";
        }

        return "";
    }

    private String elementTypeToString(ElementType elementType) {
        switch (elementType) {
            case VOID:
            case OBJECTREF:
                return "V";

            case INT32:
                return "I";

            case ARRAYREF:
                return "[";
        }

        return "";
    }

    private String generateMethod(Method method) {
        String code = ".method ";

        // Method access modifier
        String methodAccessModifier = acessModifierToString(method.getMethodAccessModifier());

        if (!methodAccessModifier.isEmpty())
            code += methodAccessModifier + " ";

        if (method.isStaticMethod())
            code += "static ";

        // Method name
        String methodName = method.getMethodName();

        if (method.isConstructMethod())
            methodName = "<init>";

        code += methodName;

        // Method descriptor
        code += generateMethodDescriptor(method.getParams(), method.getReturnType(), methodName) + "\n";

        code += "\t.limit stack 99\n";    // NOTE: Temporary for Assignment 2
        code += "\t.limit locals 99\n\n"; // NOTE: Temporary for Assignment 2

        if (method.isConstructMethod())
            code += "\taload_0\n";

        // Iterate over method's instructions
        for (Instruction instruction : method.getInstructions())
            code += generate(instruction);

        code += ".end method\n\n";

        return code;
    }

    private String generateMethodDescriptor(List<Element> parameters, Type returnType, String methodName) {
        String descriptor = "(";

        // Iterate over method's parameters
        for (Element parameter : parameters) {
            // Element type
            descriptor += elementTypeToString(parameter.getType().getTypeOfElement());

            if (methodName.equals("main"))
                descriptor += "Ljava/lang/String";

            else if (parameter.isLiteral())
                descriptor += ((LiteralElement) parameter).getLiteral();

            else
                descriptor += ((Operand) parameter).getName();

            descriptor += ";";
        }

        descriptor += ")" + elementTypeToString(returnType.getTypeOfElement());

        return descriptor;
    }

    private String generate(Instruction instruction) {
        switch (instruction.getInstType()) {
            case CALL:
                return generate((CallInstruction) instruction);
            case GOTO:
                return generate((GotoInstruction) instruction);
            case NOPER:
                return generate((SingleOpInstruction) instruction);
            case ASSIGN:
                return generate((AssignInstruction) instruction);
            case BRANCH:
                return generate((CondBranchInstruction) instruction);
            case RETURN:
                return generate((ReturnInstruction) instruction);
            case GETFIELD:
                return generate((GetFieldInstruction) instruction);
            case PUTFIELD:
                return generate((PutFieldInstruction) instruction);
            case UNARYOPER:
                return generate((UnaryOpInstruction) instruction);
            case BINARYOPER:
                return generate((BinaryOpInstruction) instruction);
        }

        return "";
    }

    private String generate(CallInstruction instruction) {
        // Invocation type
        String invocationType = instruction.getInvocationType().toString();

        if (invocationType.equals("NEW"))
            invocationType = "invokespecial";

        // Class name
        String className = "";

        if (instruction.getFirstArg().isLiteral())
            className += ((LiteralElement) instruction.getFirstArg()).getLiteral();

        else
            className += ((Operand) instruction.getFirstArg()).getName();

        // Method name
        String methodName = "";

        if (instruction.getSecondArg() != null)
            if (instruction.getSecondArg().isLiteral())
                methodName += ((LiteralElement) instruction.getSecondArg()).getLiteral();

            else
                methodName += ((Operand) instruction.getSecondArg()).getName();

        methodName = methodName.replaceAll("\"", "");

        if (methodName.equals("<init>"))
            className = superClassName;

        if (methodName.isEmpty())
            methodName = "<init>";

        // Descriptor
        Type returnType = instruction.getReturnType();

        String descriptor = generateMethodDescriptor(instruction.getListOfOperands(), returnType, methodName);

        return String.format("\t%s %s/%s%s\n", invocationType, className, methodName, descriptor);
    }

    private String generate(GotoInstruction instruction) {
        return String.format("\tgoto %s\n", instruction.getLabel());
    }

    private String generate(SingleOpInstruction instruction) {
        String code = "\t";
        Element operand = instruction.getSingleOperand();

        if (operand.isLiteral()) {
            code += "ldc " + ((LiteralElement) operand).getLiteral();
        }

        code += "\n";

        return code;
    }

    private String generate(AssignInstruction instruction) {
        String code = generate(instruction.getRhs()); // Generate RHS

        String variableType = elementTypeToString(instruction.getDest().getType().getTypeOfElement()).toLowerCase();
        code += "\t" + variableType + "load " + stackCount + "\n";

        return code;
    }

    private String generate(CondBranchInstruction instruction) {
        return "";
    }

    private String generate(ReturnInstruction instruction) {
        return "\treturn\n";
    }

    private String generate(GetFieldInstruction instruction) {
        return null;
    }

    private String generate(PutFieldInstruction instruction) {
        return null;
    }

    private String generate(UnaryOpInstruction instruction) {
        OperationType opType = instruction.getUnaryOperation().getOpType();
        String elementType = elementTypeToString(instruction.getRightOperand().getType().getTypeOfElement());

        String code = "\t" + elementType;

        code += "\t" + elementType + opType.toString().toLowerCase() + "\n"; // Operation code

        return code;
    }

    private String generate(BinaryOpInstruction instruction) {
        OperationType opType = instruction.getUnaryOperation().getOpType();
        String elementType = elementTypeToString(instruction.getLeftOperand().getType().getTypeOfElement()).toLowerCase();

        String code = "\t" + elementType + "store " + stackCount + "\n";

        stackCount++;

        code += "\t" + elementType + "store " + stackCount + "\n";

        code += "\t" + elementType + opType.toString().toLowerCase() + "\n"; // Operation code

        code += "\n";

        return code;
    }
}
