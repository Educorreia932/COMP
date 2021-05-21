package stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
    private int registCount = 1, labelCount = 1, stackCount = 0, maxStackCount = 0;
    String className;
    String superClassName;
    HashMap<String, Integer> variablesRegists = new HashMap<>();

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        ClassUnit ollirClass = ollirResult.getOllirClass();

        try {
            // Example of what you can do with the OLLIR class

            ollirClass.checkMethodLabels();  // Check the use of labels in the OLLIR loaded
            ollirClass.buildCFGs();          // Build the CFG of each method
            // ollirClass.outputCFGs();         // Output to .dot files the CFGs, one per method
            ollirClass.buildVarTables();     // Build the table of variables for each method
            // ollirClass.show();               // Print to console main information about the input OLLIR

            // Convert the OLLIR to a String containing the equivalent Jasmin code
            String jasminCode = "";

            // Class name
            String classAccessModifier = acessModifierToString(ollirClass.getClassAccessModifier());

            if (classAccessModifier.isEmpty())
                classAccessModifier = "public";

            className = ollirClass.getClassName();

            jasminCode += String.format(".class %s %s\n", classAccessModifier, className);

            // Superclass name
            superClassName = ollirClass.getSuperClass();

            if (superClassName == null)
                superClassName = "java/lang/Object";

            jasminCode += String.format(".super %s\n\n", superClassName);

            // Iterate over class fields
            for (Field field : ollirClass.getFields())
                jasminCode += generateField(field);

            if (ollirClass.getFields().size() > 0)
                jasminCode += "\n";

            // Iterate over class methods
            for (Method method : ollirClass.getMethods()) {
                jasminCode += generateMethod(method);

                // Reset counts
                registCount = 1;
                stackCount = 0;
                maxStackCount = 0;
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

    private void incrementStack() {
        stackCount++;

        if (stackCount > maxStackCount)
            maxStackCount = stackCount;
    }

    private void decrementStack() {
        stackCount--;
    }

    private void decrementStack(int value) {
        stackCount -= value;
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

            case BOOLEAN:
            case INT32:
                return "I";

            case ARRAYREF:
                return "[";

            case STRING:
                return "Ljava/lang/String;";

            case OBJECTREF:
                return "A";
        }

        return "";
    }

    private String elementToString(Element element) {
        if (element.isLiteral())
            return ((LiteralElement) element).getLiteral();

        else
            return ((Operand) element).getName();
    }

    private String opTypeToString(OperationType opType) {
        switch (opType) {
            case EQ:
                return "if_icmpeq";
            case EQI32:
                return "ifeq";
            case NEQ:
                return "if_icmpne";
            case NEQI32:
                return "ifneq";
            case LTH:
                return "if_icmplt";
            case LTE:
                return "if_icmple";
            case GTH:
                return "if_icmpgt";
            case GTE:
                return "if_icmpge";
            case ANDB:
                return "iand";
            case ORB:
                return "ior";
            default:
                return "i" + opType.toString().toLowerCase();
        }
    }

    private String generateField(Field field) {
        String code = ".field ";

        code += acessModifierToString(field.getFieldAccessModifier()) + " ";

        if (field.isStaticField())
            code += "static ";

        if (field.isFinalField())
            code += "final ";

        code += field.getFieldName() + " ";
        code += elementTypeToString(field.getFieldType().getTypeOfElement());

        return code + "\n";
    }

    private String generateMethod(Method method) {
        variablesRegists = new HashMap<>();
        String header = ".method ";

        // Method access modifier
        String methodAccessModifier = acessModifierToString(method.getMethodAccessModifier());

        if (!methodAccessModifier.isEmpty())
            header += methodAccessModifier + " ";

        if (method.isStaticMethod())
            header += "static ";

        // Method name
        String methodName = method.getMethodName();

        if (method.isConstructMethod())
            methodName = "<init>";

        header += methodName;

        // Method descriptor
        header += generateMethodDescriptor(method.getParams(), method.getReturnType(), methodName) + "\n";

        String code = "";

        // Map method's parameters to regists
        for (Element operand : method.getParams())
            variablesRegists.put(((Operand) operand).getName(), registCount++);

        if (method.isConstructMethod()) {
            code += "\taload_0\n";
            incrementStack();
        }

        // Iterate over method's instructions
        for (Instruction instruction : method.getInstructions()) {
            // Iterate over instruction's labels
            for (String label : method.getLabels(instruction))
                code += String.format("%s:\n", label);

            code += generate(instruction);
        }

        if (method.getReturnType().getTypeOfElement() == ElementType.VOID)
            code += "\treturn\n";

        code += ".end method\n\n";

        String stackLimit = String.format("\t.limit stack %d\n", maxStackCount);
        String localsLimit = String.format("\t.limit locals %d\n\n", registCount);

        code = header + stackLimit + localsLimit + code;

        return code;
    }

    private String generateMethodDescriptor(List<Element> parameters, Type returnType, String methodName) {
        String descriptor = "(";

        // Iterate over method's parameters
        for (Element parameter : parameters) {
            // Element type
            descriptor += elementTypeToString(parameter.getType().getTypeOfElement());

            if (methodName.equals("main"))
                descriptor += "Ljava/lang/String;";

            else if (parameter.getType().getTypeOfElement() == ElementType.ARRAYREF)
                descriptor += "I";
        }

        descriptor += ")" + elementTypeToString(returnType.getTypeOfElement());

        if (returnType.getTypeOfElement() == ElementType.ARRAYREF)
            descriptor += "I"; // Only integer arrays are allowed

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
        if (instruction.getInvocationType() == CallType.ldc) {
            incrementStack();

            return String.format("\tldc %s\n", ((LiteralElement) instruction.getFirstArg()).getLiteral());
        }

        String code = "";

        Operand firstArg = ((Operand) instruction.getFirstArg());

        // Class name
        String className = "";

        if (firstArg.getType().getTypeOfElement() == ElementType.OBJECTREF)
            className += ((ClassType) firstArg.getType()).getName();

        else
            className += firstArg.getName();

        if (instruction.getInvocationType() == CallType.invokevirtual) {
            int regist = variablesRegists.getOrDefault(firstArg.getName(), 0);

            code += String.format("\taload %d\n", regist);
            incrementStack();
        }

        // Method name
        String methodName = "";

        if (instruction.getSecondArg() != null)
            if (instruction.getSecondArg().isLiteral())
                methodName += ((LiteralElement) instruction.getSecondArg()).getLiteral();

            else
                methodName += ((Operand) instruction.getSecondArg()).getName();

        methodName = methodName.replaceAll("\"", "");

        if (methodName.equals("<init>") && className.equals("this"))
            className = superClassName;

        else if (className.equals("this"))
            className = this.className;

        // Invocation type
        String invocationType = instruction.getInvocationType().toString().toLowerCase();

        // Call to arraylength
        if (instruction.getInvocationType() == CallType.arraylength) {
            code += generate(new SingleOpInstruction(instruction.getFirstArg()));
            code += String.format("\t%s\n", invocationType);

            return code;
        }

        // Load operands
        for (Element operand : instruction.getListOfOperands()) {
            SingleOpInstruction opInstruction = new SingleOpInstruction(operand);

            code += generate(opInstruction);
        }

        // If is constructor
        if (invocationType.equals("new")) {
            if (className.equals("array"))
                code += String.format("\t%s%s int\n", invocationType, className);

            else {
                code += String.format("\t%s %s\n", invocationType, className);
                code += "\tdup\n";
                incrementStack();
            }

            incrementStack();
        }

        else {
            // Descriptor
            Type returnType = instruction.getReturnType();

            String descriptor = generateMethodDescriptor(instruction.getListOfOperands(), returnType, methodName);

            code += String.format("\t%s %s/%s%s\n", invocationType, className, methodName, descriptor);

            if (returnType.getTypeOfElement() == ElementType.VOID)
                decrementStack();
        }

        return code;
    }

    private String generate(GotoInstruction instruction) {
        return String.format("\tgoto %s\n", instruction.getLabel());
    }

    private String generate(SingleOpInstruction instruction) {
        String code = "\t";
        Element operand = instruction.getSingleOperand();
        ElementType operandType = operand.getType().getTypeOfElement();

        // Literal
        if (operand.isLiteral()) {
            code += "ldc " + ((LiteralElement) operand).getLiteral();
            incrementStack();
        }

        // Array
        else if (operand instanceof ArrayOperand) {
            ArrayOperand arrayOperand = (ArrayOperand) operand;
            int regist = variablesRegists.get(arrayOperand.getName());

            // Load array reference
            code += String.format("aload %s\n", regist);
            incrementStack();

            // Iterate over index operands
            for (Element indexOperand : arrayOperand.getIndexOperands())
                code += generate(new SingleOpInstruction(indexOperand));

            code += "\tiaload";
            incrementStack();
        }

        // Operand
        else {
            int regist = variablesRegists.get(((Operand) operand).getName());
            String loadType = elementTypeToString(operandType).toLowerCase();

            if (operandType == ElementType.STRING || operandType == ElementType.ARRAYREF)
                loadType = "a";

            code += loadType + "load " + regist;
            incrementStack();
        }

        code += "\n";

        return code;
    }

    private String generate(AssignInstruction instruction) {
        String code = generate(instruction.getRhs()); // Generate RHS

        // Assignment type
        ElementType assignType = instruction.getTypeOfAssign().getTypeOfElement();
        String assignTypeString = elementTypeToString(assignType).toLowerCase();

        if (assignType == ElementType.STRING || assignType == ElementType.ARRAYREF)
            assignTypeString = "a";

            // Boolean assignment using binary logic operation
        else if (assignType == ElementType.BOOLEAN && instruction.getRhs().getInstType() == InstructionType.BINARYOPER) {
            BinaryOpInstruction rhs = (BinaryOpInstruction) instruction.getRhs();

            code = generate(new CondBranchInstruction(
                rhs.getLeftOperand(),
                rhs.getRightOperand(),
                rhs.getUnaryOperation(),
                String.format(" Comparison_%d", labelCount)
            ));

            code += "\ticonst_1\n"; // True
            code += String.format("\tgoto Assign_%d\n", labelCount);
            code += String.format("Comparison_%d:\n", labelCount);
            code += "\ticonst_0\n"; // False
            code += String.format("Assign_%d:\n", labelCount);

            labelCount++;
        }

        // Get variable's correspondent regist number
        int regist = variablesRegists.getOrDefault(((Operand) instruction.getDest()).getName(), registCount++);

        // Store instruction
        if (instruction.getDest() instanceof ArrayOperand) {
            int stackSize = 2;

            ArrayOperand arrayOperand = (ArrayOperand) instruction.getDest();

            // Load array reference
            code = String.format("\taload %s\n", regist);
            incrementStack();

            // Iterate over index operands
            for (Element indexOperand : arrayOperand.getIndexOperands()) {
                code += generate(new SingleOpInstruction(indexOperand));
                stackSize++;
            }

            // Load value
            code += generate(instruction.getRhs());

            // Store value in array
            code += "\tiastore\n";

            decrementStack(stackSize);
        }

        else {
            code += "\t" + assignTypeString + "store " + regist + "\n";
            decrementStack();
        }

        // Update variable table with correspondent regist
        variablesRegists.put(((Operand) instruction.getDest()).getName(), regist);

        return code;
    }

    private String generate(CondBranchInstruction instruction) {
        String code = "";

        code += generate(new SingleOpInstruction(instruction.getLeftOperand()));
        code += generate(new SingleOpInstruction(instruction.getRightOperand()));

        OperationType opType = instruction.getCondOperation().getOpType();

        if (opType == OperationType.ANDB || opType == OperationType.ORB) {
            code += String.format("\t%s\n", opTypeToString(opType));
            opType = OperationType.EQI32;
        }

        code += String.format("\t%s %s\n", opTypeToString(opType), instruction.getLabel());

        return code;
    }

    private String generate(ReturnInstruction instruction) {
        String code = "";

        if (instruction.hasReturnValue()) {
            Element operand = instruction.getOperand();
            ElementType returnType = operand.getType().getTypeOfElement();
            SingleOpInstruction opInstruction = new SingleOpInstruction(operand);
            String returnTypeString = elementTypeToString(returnType).toLowerCase();

            if (returnType == ElementType.STRING || returnType == ElementType.ARRAYREF)
                returnTypeString = "a";

            code += generate(opInstruction);
            code += String.format("\t%sreturn\n", returnTypeString);
        }

        return code;
    }

    private String generate(GetFieldInstruction instruction) {
        String code = "\taload_0\n";
        incrementStack();

        String className = elementToString(instruction.getFirstOperand());

        if (className.equals("this"))
            className = this.className;

        String fieldName = elementToString(instruction.getSecondOperand());
        String fieldType = elementTypeToString(instruction.getSecondOperand().getType().getTypeOfElement());

        code += String.format("\tgetfield %s/%s %s\n", className, fieldName, fieldType);

        return code;
    }

    private String generate(PutFieldInstruction instruction) {
        String code = "\taload_0\n";
        incrementStack();

        code += generate(new SingleOpInstruction(instruction.getThirdOperand()));

        String className = elementToString(instruction.getFirstOperand());

        if (className.equals("this"))
            className = this.className;

        String fieldName = elementToString(instruction.getSecondOperand());

        String fieldType = elementTypeToString(instruction.getSecondOperand().getType().getTypeOfElement());

        code += String.format("\tputfield %s/%s %s\n", className, fieldName, fieldType);

        return code;
    }

    private String generate(UnaryOpInstruction instruction) {
        String opType = opTypeToString(instruction.getUnaryOperation().getOpType());
        String elementType = elementTypeToString(instruction.getRightOperand().getType().getTypeOfElement());

        String code = "\t" + elementType;

        code += "\t" + elementType + opType + "\n"; // Operation code

        return code;
    }

    private String generate(BinaryOpInstruction instruction) {
        String code = "";

        String opType = opTypeToString(instruction.getUnaryOperation().getOpType());
        Element leftOperand = instruction.getLeftOperand();
        Element rightOperand = instruction.getRightOperand();

        code += generate(new SingleOpInstruction(leftOperand));
        code += generate(new SingleOpInstruction(rightOperand));

        code += "\t" + opType + "\n"; // Operation code
        code += "\n";

        decrementStack();

        return code;
    }
}
