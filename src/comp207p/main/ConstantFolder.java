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

    String reConstPushInstruction = "(BIPUSH|DCONST|FCONST|ICONST|LCONST|SIPUSH|LDC|LDC2_W)"; // LDC_W is a subclass of LDC, so we don't need to include it
    String reUnaryInstruction  = "(DNEG|FNEG|INEG|LNEG|" +
                                  "I2L|I2F|I2D|L2I|L2F|L2D|F2I|F2L|F2D|D2I|D2L|D2F)";
    String reBinaryInstruction = "(DADD|DDIV|DMUL|DREM|DSUB|" +
                                  "FADD|FDIV|FMUL|FREM|FSUB|" +
                                  "IADD|IAND|IDIV|IMUL|IOR|IREM|ISHL|ISHR|ISUB|IUSHR|IXOR|" +
                                  "LADD|LAND|LDIV|LMUL|LOR|LREM|LSHL|LSHR|LSUB|LUSHR|LXOR)";

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

        boolean optimizationOccurred = true;

        while (optimizationOccurred) {
            optimizationOccurred = false;
            optimizationOccurred = this.optimizeAllUnaryExprs(instList, cpgen) || optimizationOccurred;
            optimizationOccurred = this.optimizeAllBinaryExprs(instList, cpgen) || optimizationOccurred;
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

    public boolean optimizeAllUnaryExprs(InstructionList instList, ConstantPoolGen cpgen) {

        // Use InstructionFinder to search for a pattern of instructions
        // (in our case, a constant binary expression)
        String pattern = reConstPushInstruction + " " + reUnaryInstruction;

        boolean optimizedLastPass = true;
        boolean somethingWasOptimized = false;

        while (optimizedLastPass) {
            optimizedLastPass = false;
            InstructionFinder f = new InstructionFinder(instList);
            for (Iterator<?> e = f.search(pattern); e.hasNext(); ) {
                InstructionHandle[] handles = (InstructionHandle[])e.next();
                boolean optimizedThisPass = this.optimizeConstantUnaryExpr(handles, instList, cpgen);
                optimizedLastPass = optimizedLastPass || optimizedThisPass;
                somethingWasOptimized = somethingWasOptimized || optimizedThisPass;
            }
        }

        return somethingWasOptimized;
    }

    public boolean optimizeConstantUnaryExpr(InstructionHandle[] handles, InstructionList instList, ConstantPoolGen cpgen) {

        InstructionHandle operand = handles[0];
        InstructionHandle operator = handles[1];

        String opName = operator.getInstruction().getName();
        Object value = getValue(operand.getInstruction(), cpgen);
        Number number = (Number)value;

        // Negation

        if (opName.equals("ineg")) {
            instList.insert(operand, new PUSH(cpgen, -(int)value));
        } else if (opName.equals("lneg")) {
            instList.insert(operand, new PUSH(cpgen, -(long)value));
        } else if (opName.equals("fneg")) {
            instList.insert(operand, new PUSH(cpgen, -(float)value));
        } else if (opName.equals("dneg")) {
            instList.insert(operand, new PUSH(cpgen, -(double)value));

        // Type conversion

        } else if (opName.equals("l2i") ||
                   opName.equals("f2i") ||
                   opName.equals("d2i")) {
            instList.insert(operand, new PUSH(cpgen, number.intValue()));
        } else if (opName.equals("i2l") ||
                   opName.equals("f2l") ||
                   opName.equals("d2l")) {
            instList.insert(operand, new PUSH(cpgen, number.longValue()));
        } else if (opName.equals("i2f") ||
                   opName.equals("l2f") ||
                   opName.equals("d2f")) {
            instList.insert(operand, new PUSH(cpgen, number.floatValue()));
        } else if (opName.equals("i2d") ||
                   opName.equals("l2d") ||
                   opName.equals("f2d")) {
            instList.insert(operand, new PUSH(cpgen, number.doubleValue()));

        } else {
            // reached when instruction is not handled, e.g. bitwise operators or shifts.
            System.out.println("Couldn't optimise: " + opName);
            // return is to prevent deleting instructions, since nothing has been added.
            return false;
        }

        // Delete the 3 instructions making up the expression
        try {
            instList.delete(operand);
            instList.delete(operator);
        } catch (TargetLostException e) {
            e.printStackTrace();
        }

        System.out.println("Optimised: " + opName);

        return true;
    }

    public boolean optimizeAllBinaryExprs(InstructionList instList, ConstantPoolGen cpgen) {

        // Use InstructionFinder to search for a pattern of instructions
        // (in our case, a constant binary expression)
        String pattern = reConstPushInstruction + " " + reConstPushInstruction + " " + reBinaryInstruction;

        boolean optimizedLastPass = true;
        boolean somethingWasOptimized = false;

        while (optimizedLastPass) {
            optimizedLastPass = false;
            InstructionFinder f = new InstructionFinder(instList);
            for (Iterator<?> e = f.search(pattern); e.hasNext(); ) {
                InstructionHandle[] handles = (InstructionHandle[])e.next();
                boolean optimizedThisPass = this.optimizeConstantBinaryExpr(handles, instList, cpgen);
                optimizedLastPass = optimizedLastPass || optimizedThisPass;
                somethingWasOptimized = somethingWasOptimized || optimizedThisPass;
            }
        }

        return somethingWasOptimized;
    }

    // Converts binary arithmetic operation to a single constant.
    // `handles` expects the 3 instructions (2 operands + 1 operation) that make up the binary expression.
    // It mutates the instruction list and (if necessary) constant pool.
    public boolean optimizeConstantBinaryExpr(InstructionHandle[] handles, InstructionList instList, ConstantPoolGen cpgen) {

        InstructionHandle operand1 = handles[0];
        InstructionHandle operand2 = handles[1];
        InstructionHandle operator = handles[2];

        String opName = operator.getInstruction().getName();

        Object a = getValue(operand1.getInstruction(), cpgen);
        Object b = getValue(operand2.getInstruction(), cpgen);

        // Integer operations

        if (opName.equals("iadd")) {
            instList.insert(operand1, new PUSH(cpgen, (int)a + (int)b));
        } else if (opName.equals("isub")) {
            instList.insert(operand1, new PUSH(cpgen, (int)a - (int)b));
        } else if (opName.equals("imul")) {
            instList.insert(operand1, new PUSH(cpgen, (int)a * (int)b));
        } else if (opName.equals("idiv")) {
            instList.insert(operand1, new PUSH(cpgen, (int)a / (int)b));
        } else if (opName.equals("irem")) {
            instList.insert(operand1, new PUSH(cpgen, (int)a % (int)b));
        } else if (opName.equals("iand")) {
            instList.insert(operand1, new PUSH(cpgen, (int)a & (int)b));
        } else if (opName.equals("ior")) {
            instList.insert(operand1, new PUSH(cpgen, (int)a | (int)b));
        } else if (opName.equals("ixor")) {
            instList.insert(operand1, new PUSH(cpgen, (int)a ^ (int)b));
        } else if (opName.equals("ishl")) {
            instList.insert(operand1, new PUSH(cpgen, (int)a << (int)b));
        } else if (opName.equals("ishr")) {
            instList.insert(operand1, new PUSH(cpgen, (int)a >> (int)b));
        } else if (opName.equals("iushr")) {
            instList.insert(operand1, new PUSH(cpgen, (int)a >>> (int)b));

        // Long operations

        } else if (opName.equals("ladd")) {
            instList.insert(operand1, new PUSH(cpgen, (long)a + (long)b));
        } else if (opName.equals("lsub")) {
            instList.insert(operand1, new PUSH(cpgen, (long)a - (long)b));
        } else if (opName.equals("lmul")) {
            instList.insert(operand1, new PUSH(cpgen, (long)a * (long)b));
        } else if (opName.equals("ldiv")) {
            instList.insert(operand1, new PUSH(cpgen, (long)a / (long)b));
        } else if (opName.equals("lrem")) {
            instList.insert(operand1, new PUSH(cpgen, (long)a % (long)b));
        } else if (opName.equals("land")) {
            instList.insert(operand1, new PUSH(cpgen, (long)a & (long)b));
        } else if (opName.equals("lor")) {
            instList.insert(operand1, new PUSH(cpgen, (long)a | (long)b));
        } else if (opName.equals("lxor")) {
            instList.insert(operand1, new PUSH(cpgen, (long)a ^ (long)b));
        } else if (opName.equals("lshl")) {
            instList.insert(operand1, new PUSH(cpgen, (long)a << (long)b));
        } else if (opName.equals("lshr")) {
            instList.insert(operand1, new PUSH(cpgen, (long)a >> (long)b));
        } else if (opName.equals("lushr")) {
            instList.insert(operand1, new PUSH(cpgen, (long)a >>> (long)b));

        // Float operations

        } else if (opName.equals("fadd")) {
            instList.insert(operand1, new PUSH(cpgen, (float)a + (float)b));
        } else if (opName.equals("fsub")) {
            instList.insert(operand1, new PUSH(cpgen, (float)a - (float)b));
        } else if (opName.equals("fmul")) {
            instList.insert(operand1, new PUSH(cpgen, (float)a * (float)b));
        } else if (opName.equals("fdiv")) {
            instList.insert(operand1, new PUSH(cpgen, (float)a / (float)b));
        } else if (opName.equals("frem")) {
            instList.insert(operand1, new PUSH(cpgen, (float)a % (float)b));

        // Double operations

        } else if (opName.equals("dadd")) {
            instList.insert(operand1, new PUSH(cpgen, (double)a + (double)b));
        } else if (opName.equals("dsub")) {
            instList.insert(operand1, new PUSH(cpgen, (double)a - (double)b));
        } else if (opName.equals("dmul")) {
            instList.insert(operand1, new PUSH(cpgen, (double)a * (double)b));
        } else if (opName.equals("ddiv")) {
            instList.insert(operand1, new PUSH(cpgen, (double)a / (double)b));
        } else if (opName.equals("drem")) {
            instList.insert(operand1, new PUSH(cpgen, (double)a % (double)b));

        } else {
            // reached when instruction is not handled
            System.out.println("Couldn't optimise: " + opName);
            // return is to prevent deleting instructions, since nothing has been added.
            return false;
        }

        // Delete the 3 instructions making up the expression
        try {
            instList.delete(operand1);
            instList.delete(operand2);
            instList.delete(operator);
        } catch (TargetLostException e) {
            e.printStackTrace();
        }

        System.out.println("Optimised: " + opName);

        return true;
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
