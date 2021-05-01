package stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        ClassUnit ollirClass = ollirResult.getOllirClass();

        try {
            // Example of what you can do with the OLLIR class

            ollirClass.checkMethodLabels();  // Check the use of labels in the OLLIR loaded
            ollirClass.buildCFGs();          // Build the CFG of each method
            // ollirClass.outputCFGs();         // Output to .dot files the CFGs, one per method
            ollirClass.buildVarTables();     // Build the table of variables for each method
            ollirClass.show();               // Print to console main information about the input OLLIR

            // Convert the OLLIR to a String containing the equivalent Jasmin code
            String jasminCode = "";

            String classAccessModifier = acessModifierToString(ollirClass.getClassAccessModifier());

            if (classAccessModifier.isEmpty())
                classAccessModifier = "public";

            String className = ollirClass.getClassName();

            jasminCode += String.format(".class %s %s\n", classAccessModifier, className);

            String superClassName = ollirClass.getSuperClass();

            if (superClassName == null)
                superClassName = "java/lang/Object";

            jasminCode += String.format(".super %s\n\n", superClassName);

            // Iterate over class methods
            for (Method method : ollirClass.getMethods()) {
                jasminCode += ".method ";

                // Method access modifier
                String methodAccessModifier = acessModifierToString(method.getMethodAccessModifier());

                if (!methodAccessModifier.isEmpty())
                    jasminCode += methodAccessModifier + " ";

                if (method.isStaticMethod())
                    jasminCode += "static ";

                // Method name
                String methodName = method.getMethodName();

                if (method.isConstructMethod())
                    methodName = "<init>";

                jasminCode += methodName;

                // Method descriptor
                jasminCode += generateMethodDescriptor(method.getParams(), method.getReturnType()) + "\n";

                String instructionCode = "";

                if (method.isConstructMethod())
                    instructionCode = "\taload_0\n";

                // Iterate over method's instructions
                for (Instruction instruction : method.getInstructions()) {
                    switch (instruction.getInstType()) {
                        case CALL:
                            instructionCode = generate((CallInstruction) instruction);
                            break;
                        case GOTO:
                            instructionCode = generate((GotoInstruction) instruction);
                            break;
                        case NOPER:
                            instructionCode = "";
                            break;
                        case ASSIGN:
                            instructionCode = generate((AssignInstruction) instruction);
                            break;
                        case BRANCH:
                            instructionCode = generate((CondBranchInstruction) instruction);
                            break;
                        case RETURN:
                            instructionCode = generate((ReturnInstruction) instruction);
                            break;
                        case GETFIELD:
                            instructionCode = generate((GetFieldInstruction) instruction);
                            break;
                        case PUTFIELD:
                            instructionCode = generate((PutFieldInstruction) instruction);
                            break;
                        case UNARYOPER:
                            instructionCode = generate((UnaryOpInstruction) instruction);
                            break;
                        case BINARYOPER:
                            instructionCode = generate((BinaryOpInstruction) instruction);
                            break;
                    }

                    jasminCode += instructionCode;
                }

                jasminCode += ".end method\n\n";
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
                return "V";

            case INT32:
                return "I";

            case ARRAYREF:
                return "[";
        }

        return "";
    }

    private String generateMethodDescriptor(List<Element> parameters, Type returnType) {
        String descriptor = "(";

        // Iterate over method's parameters
        for (Element parameter : parameters) {
            descriptor += elementTypeToString(parameter.getType().getTypeOfElement());

            if (parameter.isLiteral())
                descriptor += ((LiteralElement) parameter).getLiteral();

            else {
                descriptor += ((Operand) parameter).getName();
            }

            descriptor += ";";
        }

        descriptor += ")" + elementTypeToString(returnType.getTypeOfElement());

        return descriptor;
    }

    private String generate(CallInstruction instruction) {
        // Invocation type
        String invocationType = instruction.getInvocationType().toString();

        // Class name
        String className = "java/";

        if (instruction.getFirstArg().isLiteral())
            className += ((LiteralElement) instruction.getFirstArg()).getLiteral();

        else
            className += ((Operand) instruction.getFirstArg()).getName();

        // Method name
        String methodName = "";

        if (instruction.getSecondArg().isLiteral())
            methodName += ((LiteralElement) instruction.getSecondArg()).getLiteral();

        else
            methodName += ((Operand) instruction.getSecondArg()).getName();

        methodName = methodName.replaceAll("\"", "");

        // Descriptor
        String descriptor = generateMethodDescriptor(instruction.getListOfOperands(), instruction.getReturnType());

        return String.format("\t%s %s/%s%s\n", invocationType, className, methodName, descriptor);
    }

    private String generate(GotoInstruction instruction) {
        return String.format("\tgoto %s\n", instruction.getLabel());
    }

    private String generate(AssignInstruction instruction) {
        return "\tlstore";
    }

    private String generate(CondBranchInstruction instruction) {

        return null;
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
        String code = "\t";
        OperationType opType = instruction.getUnaryOperation().getOpType();

        switch (opType) {
            case ADD:
            case SUB:
            case MUL:
            case DIV:
                code += "i" + opType.toString().toLowerCase();
                break;
            case XOR:
                break;
            case AND:
                break;
            case OR:
                break;
        }

        code += "\n";

        return code;
    }

    private String generate(BinaryOpInstruction instruction) {
        String code = "\t";
        OperationType opType = instruction.getUnaryOperation().getOpType();

        switch (opType) {
            case NOT:
                break;
        }

        code += "\n";

        return code;
    }
}
