package comp207p.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ArithmeticInstruction;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ConstantPushInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.LDC2_W;
import org.apache.bcel.generic.PUSH;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.TargetLostException;
import org.apache.bcel.generic.Type;
import org.apache.bcel.generic.TypedInstruction;
import org.apache.bcel.util.InstructionFinder;

public class ConstantFolder {

    ClassParser parser = null;

    JavaClass original = null;
    JavaClass optimized = null;

    public ConstantFolder(String classFilePath) {
        try {
            this.parser = new ClassParser(classFilePath);
            this.original = this.parser.parse();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public void optimize() {
        ClassGen cgen = new ClassGen(original);
        ConstantPoolGen cpgen = cgen.getConstantPool();

        // Implement your optimization here

        Method[] methods = cgen.getMethods();
        for (Method m : methods) {
            this.optimizeMethod(cgen, cpgen, m);
        }

        this.optimized = cgen.getJavaClass();
    }

    public void optimizeMethod(ClassGen cgen, ConstantPoolGen cpgen, Method method) {

        // Get the Code of the method, which is a collection of bytecode instructions
        Code methodCode = method.getCode();

        // Now get the actualy bytecode data in byte array,
        // and use it to initialise an InstructionList
        InstructionList instList = new InstructionList(methodCode.getCode());

        // Initialise a method generator with the original method as the baseline
        MethodGen methodGen = new MethodGen(method.getAccessFlags(), method.getReturnType(), method.getArgumentTypes(), null, method.getName(), cgen.getClassName(), instList, cpgen);

        // Use InstructionFinder to search for a pattern of instructions
        // (in our case, a constant binary expression)
        InstructionFinder f = new InstructionFinder(instList);
        String constPush = "(ConstantPushInstruction|LDC|LDC2_W)"; // LDC_W is a subclass of LDC, so we don't need to include it
        String binaryExprPattern = constPush + " " + constPush + " " + "ArithmeticInstruction";

        for (Iterator<?> e = f.search(binaryExprPattern); e.hasNext(); ) {
            InstructionHandle[] handles = (InstructionHandle[])e.next();
            this.optimizeConstantBinaryExpr(handles, instList, cpgen);
        }

        // setPositions(true) checks whether jump handles
        // are all within the current method
        instList.setPositions(true);

        // set max stack/local
        methodGen.setMaxStack();
        methodGen.setMaxLocals();

        // generate the new method with replaced iconst
        Method newMethod = methodGen.getMethod();
        // replace the method in the original class
        cgen.replaceMethod(method, newMethod);
    }

    // Converts binary arithmetic operation to a single constant.
    // `handles` expects the 3 instructions (2 operands + 1 operation) that make up the binary expression.
    // It mutates the instruction list and (if necessary) constant pool.
    public void optimizeConstantBinaryExpr(InstructionHandle[] handles, InstructionList instList, ConstantPoolGen cpgen) {

        InstructionHandle operand1 = handles[0];
        InstructionHandle operand2 = handles[1];
        InstructionHandle operator = handles[2];

        String operatorName = operator.getInstruction().getName();
        String type = operatorName.substring(0,1);
        String operation = operatorName.substring(1);

        Object a = getValue(operand1.getInstruction(), cpgen);
        Object b = getValue(operand2.getInstruction(), cpgen);

        // Integer operations

        if (type.equals("i") && operation.equals("add")) {
            instList.insert(operand1, new PUSH(cpgen, (int)a + (int)b));
        } else if (type.equals("i") && operation.equals("sub")) {
            instList.insert(operand1, new PUSH(cpgen, (int)a - (int)b));
        } else if (type.equals("i") && operation.equals("mul")) {
            instList.insert(operand1, new PUSH(cpgen, (int)a * (int)b));
        } else if (type.equals("i") && operation.equals("div")) {
            instList.insert(operand1, new PUSH(cpgen, (int)a / (int)b));

        // Long operations

        } else if (type.equals("l") && operation.equals("add")) {
            instList.insert(operand1, new PUSH(cpgen, (long)a + (long)b));
        } else if (type.equals("l") && operation.equals("sub")) {
            instList.insert(operand1, new PUSH(cpgen, (long)a - (long)b));
        } else if (type.equals("l") && operation.equals("mul")) {
            instList.insert(operand1, new PUSH(cpgen, (long)a * (long)b));
        } else if (type.equals("l") && operation.equals("div")) {
            instList.insert(operand1, new PUSH(cpgen, (long)a / (long)b));

        // Float operations

        } else if (type.equals("f") && operation.equals("add")) {
            instList.insert(operand1, new PUSH(cpgen, (float)a + (float)b));
        } else if (type.equals("f") && operation.equals("sub")) {
            instList.insert(operand1, new PUSH(cpgen, (float)a - (float)b));
        } else if (type.equals("f") && operation.equals("mul")) {
            instList.insert(operand1, new PUSH(cpgen, (float)a * (float)b));
        } else if (type.equals("f") && operation.equals("div")) {
            instList.insert(operand1, new PUSH(cpgen, (float)a / (float)b));

        // Double operations

        } else if (type.equals("d") && operation.equals("add")) {
            instList.insert(operand1, new PUSH(cpgen, (double)a + (double)b));
        } else if (type.equals("d") && operation.equals("sub")) {
            instList.insert(operand1, new PUSH(cpgen, (double)a - (double)b));
        } else if (type.equals("d") && operation.equals("mul")) {
            instList.insert(operand1, new PUSH(cpgen, (double)a * (double)b));
        } else if (type.equals("d") && operation.equals("div")) {
            instList.insert(operand1, new PUSH(cpgen, (double)a / (double)b));

        } else {
            // reached when instruction is not handled, e.g. bitwise operators or shifts.
            System.out.println("Couldn't optimise: type=" + type + " operation=" + operation);
            // return is to prevent deleting instructions, since nothing has been added.
            return;
        }

        // Delete the 3 instructions making up the expression
        try {
            instList.delete(operand1);
            instList.delete(operand2);
            instList.delete(operator);
        } catch (TargetLostException e) {
            e.printStackTrace();
        }

        System.out.println("Optimised: type=" + type + " operation=" + operation);
    }

    // Get the value of a ConstantPushInstruction, LDC, or LDC2_W instruction
    public Object getValue(Instruction instruction, ConstantPoolGen cpgen) {
        if (instruction instanceof ConstantPushInstruction) {
            ConstantPushInstruction a = (ConstantPushInstruction)instruction;
            return a.getValue();
        } else if (instruction instanceof LDC) {
            LDC a = (LDC)instruction;
            return a.getValue(cpgen);
        } else if (instruction instanceof LDC2_W) {
            LDC2_W a = (LDC2_W)instruction;
            return a.getValue(cpgen);
        } else {
            return null;
        }
    }

    // Get the type of an instruction - might be useful later on.
    // public Type getType(Instruction instruction, ConstantPoolGen cpgen) {
    //     if (instruction instanceof TypedInstruction) {
    //         TypedInstruction a = (TypedInstruction)instruction;
    //         return a.getType(cpgen);
    //     } else {
    //         return Type.UNKNOWN;
    //     }
    // }

    public void write(String optimisedFilePath) {
        this.optimize();

        try {
            FileOutputStream out = new FileOutputStream(new File(optimisedFilePath));
            this.optimized.dump(out);
        } catch (FileNotFoundException e) {
            // Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // Auto-generated catch block
            e.printStackTrace();
        }
    }
}
