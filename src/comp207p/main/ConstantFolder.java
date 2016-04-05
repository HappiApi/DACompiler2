package comp207p.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ArithmeticInstruction;
import org.apache.bcel.generic.BranchHandle;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ConstantPushInstruction;
import org.apache.bcel.generic.GOTO;
import org.apache.bcel.generic.GotoInstruction;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.ISTORE;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.IndexedInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InstructionTargeter;
import org.apache.bcel.generic.LDC2_W;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.LocalVariableGen;
import org.apache.bcel.generic.LocalVariableInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.PUSH;
import org.apache.bcel.generic.StoreInstruction;
import org.apache.bcel.generic.TargetLostException;
import org.apache.bcel.generic.Type;
import org.apache.bcel.generic.TypedInstruction;
import org.apache.bcel.util.InstructionFinder;
import org.apache.bcel.verifier.structurals.ControlFlowGraph;
import org.apache.bcel.verifier.structurals.InstructionContext;

public class ConstantFolder {

    ClassParser parser;
    JavaClass original;
    JavaClass optimized;

    ClassGen cgen;
    ConstantPoolGen cpgen;
    MethodGen mgen;
    CodeExceptionGen[] cegen;
    LocalVariableGen[] lvgen;

    static final String reConstPushInstruction = "(BIPUSH|DCONST|FCONST|FCONST_2|ICONST|LCONST|SIPUSH|LDC|LDC2_W)"; // LDC_W is a subclass of LDC, so we don't need to include it
    static final String reUnaryInstruction = "(DNEG|FNEG|INEG|LNEG|" +
                                              "I2L|I2F|I2D|L2I|L2F|L2D|F2I|F2L|F2D|D2I|D2L|D2F)";
    static final String reBinaryInstruction = "(DADD|DDIV|DMUL|DREM|DSUB|" +
                                               "FADD|FDIV|FMUL|FREM|FSUB|" +
                                               "IADD|IAND|IDIV|IMUL|IOR|IREM|ISHL|ISHR|ISUB|IUSHR|IXOR|" +
                                               "LADD|LAND|LDIV|LMUL|LOR|LREM|LSHL|LSHR|LSUB|LUSHR|LXOR|" +
                                               "DCMPG|DCMPL|FCMPG|FCMPL|LCMP)";
    static final String reUnaryComparison = "(IFEQ|IFGE|IFGT|IFLE|IFLT|IFNE)";
    static final String reBinaryComparison = "(IF_ICMPEQ|IF_ICMPGE|IF_ICMPGT|IF_ICMPLE|IF_ICMPLT|IF_ICMPNE)";

    public ConstantFolder(String classFilePath) {
        try {
            this.parser = new ClassParser(classFilePath);
            this.original = this.parser.parse();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public void optimize() {
        cgen = new ClassGen(original);
        cpgen = cgen.getConstantPool();

        // Implement your optimization here

        Method[] methods = cgen.getMethods();
        for (Method m : methods) {
            // Get optimised method
            Method method = this.optimizeMethod(m);
            // Replace the method in the original class
            cgen.replaceMethod(m, method);
        }

        this.optimized = cgen.getJavaClass();
    }

    public Method optimizeMethod(Method method) {

        // Get the Code of the method, which is a collection of bytecode instructions
        Code methodCode = method.getCode();

        // Now get the actualy bytecode data in byte array,
        // and use it to initialise an InstructionList
        InstructionList instList = new InstructionList(methodCode.getCode());

        // Initialise a method generator with the original method as the baseline
        mgen = new MethodGen(method.getAccessFlags(), method.getReturnType(), method.getArgumentTypes(), null, method.getName(), cgen.getClassName(), instList, cpgen);

        // Store CodeExceptionGen and LocalVariableGen to redirect any targets
        // they have to instructions (deleteInstruction method uses these)
        cegen = mgen.getExceptionHandlers();
        lvgen = mgen.getLocalVariables();

        // Print the class and method name for debugging purposes
        System.out.println("\n---\n\033[0;1m" + cgen.getClassName() + "." + mgen.getName() + "\033[0;0m\n");

        boolean optimizationOccurred = true;

        // Repeatedly apply optimisations until no optimisations are applied in the last pass.
        while (optimizationOccurred) {
            optimizationOccurred = false;
            optimizationOccurred = this.optimizeAllUnaryExprs(instList) || optimizationOccurred;
            optimizationOccurred = this.optimizeAllBinaryExprs(instList) || optimizationOccurred;
            optimizationOccurred = this.optimizeAllUnaryComparisons(instList) || optimizationOccurred;
            optimizationOccurred = this.optimizeAllBinaryComparisons(instList) || optimizationOccurred;
            optimizationOccurred = this.optimizeDynamicVariables(instList) || optimizationOccurred;
            optimizationOccurred = this.removeDeadCode(instList) || optimizationOccurred;
        }

        // setPositions(true) checks whether jump handles
        // are all within the current method
        instList.setPositions(true);

        // set max stack/local
        mgen.setMaxStack();
        mgen.setMaxLocals();

        // return the new, edited method
        return mgen.getMethod();
    }

    public boolean optimizeAllUnaryExprs(InstructionList instList) {

        // Constant unary expression pattern
        String pattern = reConstPushInstruction + " " + reUnaryInstruction;

        boolean somethingWasOptimized = false;

        InstructionFinder f = new InstructionFinder(instList);
        for (Iterator<?> e = f.search(pattern); e.hasNext(); ) {
            InstructionHandle[] handles = (InstructionHandle[])e.next();
            boolean optimizedThisPass = this.optimizeUnaryExpr(handles, instList);
            somethingWasOptimized = somethingWasOptimized || optimizedThisPass;
        }

        return somethingWasOptimized;
    }

    public boolean optimizeUnaryExpr(InstructionHandle[] handles, InstructionList instList) {

        InstructionHandle operand = handles[0];
        InstructionHandle operator = handles[1];

        // If the operator has targeters, removing it could change the semantics of our program.
        if (operator.hasTargeters()) {
            return false;
        }

        String opName = operator.getInstruction().getName();
        Object value = getConstValue(operand.getInstruction(), cpgen);
        Number number = (Number)value;

        PUSH newInstruction;

        // Negation

        if (opName.equals("ineg")) {
            newInstruction = new PUSH(cpgen, -(int)value);
        } else if (opName.equals("lneg")) {
            newInstruction = new PUSH(cpgen, -(long)value);
        } else if (opName.equals("fneg")) {
            newInstruction = new PUSH(cpgen, -(float)value);
        } else if (opName.equals("dneg")) {
            newInstruction = new PUSH(cpgen, -(double)value);

        // Type conversion

        } else if (opName.equals("l2i") ||
                   opName.equals("f2i") ||
                   opName.equals("d2i")) {
            newInstruction = new PUSH(cpgen, number.intValue());
        } else if (opName.equals("i2l") ||
                   opName.equals("f2l") ||
                   opName.equals("d2l")) {
            newInstruction = new PUSH(cpgen, number.longValue());
        } else if (opName.equals("i2f") ||
                   opName.equals("l2f") ||
                   opName.equals("d2f")) {
            newInstruction = new PUSH(cpgen, number.floatValue());
        } else if (opName.equals("i2d") ||
                   opName.equals("l2d") ||
                   opName.equals("f2d")) {
            newInstruction = new PUSH(cpgen, number.doubleValue());

        } else {
            // reached when instruction is not handled
            System.out.println("Couldn't optimise: " + opName);
            // return is to prevent deleting instructions, since nothing has been added.
            return false;
        }

        InstructionHandle newInstHandle = instList.insert(operand, newInstruction);

        System.out.print("Replacing: \033[0;31m");
        System.out.print(operand);
        System.out.print("\n           ");
        System.out.print(operator);
        System.out.print("\033[0m\n     with: \033[0;32m");
        System.out.print(newInstHandle);
        System.out.println("\033[0m\n");

        // Delete the 2 instructions making up the expression
        deleteInstruction(operand, newInstHandle, instList);
        deleteInstruction(operator, newInstHandle, instList);

        return true;
    }

    public boolean optimizeAllBinaryExprs(InstructionList instList) {

        // Constant binary expression pattern
        String pattern = reConstPushInstruction + " " + reConstPushInstruction + " " + reBinaryInstruction;

        boolean somethingWasOptimized = false;

        InstructionFinder f = new InstructionFinder(instList);
        for (Iterator<?> e = f.search(pattern); e.hasNext(); ) {
            InstructionHandle[] handles = (InstructionHandle[])e.next();
            boolean optimizedThisPass = this.optimizeBinaryExpr(handles, instList);
            somethingWasOptimized = somethingWasOptimized || optimizedThisPass;
        }

        return somethingWasOptimized;
    }

    // Converts binary arithmetic operation to a single constant.
    // `handles` expects the 3 instructions (2 operands + 1 operation) that make up the binary expression.
    // It mutates the instruction list and (if necessary) constant pool.
    public boolean optimizeBinaryExpr(InstructionHandle[] handles, InstructionList instList) {

        InstructionHandle operand1 = handles[0];
        InstructionHandle operand2 = handles[1];
        InstructionHandle operator = handles[2];

        if (operand2.hasTargeters() || operator.hasTargeters()) {
            return false;
        }

        String opName = operator.getInstruction().getName();

        Object a = getConstValue(operand1.getInstruction(), cpgen);
        Object b = getConstValue(operand2.getInstruction(), cpgen);

        PUSH newInstruction;

        // Integer operations

        if (opName.equals("iadd")) {
            newInstruction = new PUSH(cpgen, (int)a + (int)b);
        } else if (opName.equals("isub")) {
            newInstruction = new PUSH(cpgen, (int)a - (int)b);
        } else if (opName.equals("imul")) {
            newInstruction = new PUSH(cpgen, (int)a * (int)b);
        } else if (opName.equals("idiv")) {
            newInstruction = new PUSH(cpgen, (int)a / (int)b);
        } else if (opName.equals("irem")) {
            newInstruction = new PUSH(cpgen, (int)a % (int)b);
        } else if (opName.equals("iand")) {
            newInstruction = new PUSH(cpgen, (int)a & (int)b);
        } else if (opName.equals("ior")) {
            newInstruction = new PUSH(cpgen, (int)a | (int)b);
        } else if (opName.equals("ixor")) {
            newInstruction = new PUSH(cpgen, (int)a ^ (int)b);
        } else if (opName.equals("ishl")) {
            newInstruction = new PUSH(cpgen, (int)a << (int)b);
        } else if (opName.equals("ishr")) {
            newInstruction = new PUSH(cpgen, (int)a >> (int)b);
        } else if (opName.equals("iushr")) {
            newInstruction = new PUSH(cpgen, (int)a >>> (int)b);

        // Long operations

        } else if (opName.equals("ladd")) {
            newInstruction = new PUSH(cpgen, (long)a + (long)b);
        } else if (opName.equals("lsub")) {
            newInstruction = new PUSH(cpgen, (long)a - (long)b);
        } else if (opName.equals("lmul")) {
            newInstruction = new PUSH(cpgen, (long)a * (long)b);
        } else if (opName.equals("ldiv")) {
            newInstruction = new PUSH(cpgen, (long)a / (long)b);
        } else if (opName.equals("lrem")) {
            newInstruction = new PUSH(cpgen, (long)a % (long)b);
        } else if (opName.equals("land")) {
            newInstruction = new PUSH(cpgen, (long)a & (long)b);
        } else if (opName.equals("lor")) {
            newInstruction = new PUSH(cpgen, (long)a | (long)b);
        } else if (opName.equals("lxor")) {
            newInstruction = new PUSH(cpgen, (long)a ^ (long)b);
        } else if (opName.equals("lshl")) {
            newInstruction = new PUSH(cpgen, (long)a << (long)b);
        } else if (opName.equals("lshr")) {
            newInstruction = new PUSH(cpgen, (long)a >> (long)b);
        } else if (opName.equals("lushr")) {
            newInstruction = new PUSH(cpgen, (long)a >>> (long)b);

        // Float operations

        } else if (opName.equals("fadd")) {
            newInstruction = new PUSH(cpgen, (float)a + (float)b);
        } else if (opName.equals("fsub")) {
            newInstruction = new PUSH(cpgen, (float)a - (float)b);
        } else if (opName.equals("fmul")) {
            newInstruction = new PUSH(cpgen, (float)a * (float)b);
        } else if (opName.equals("fdiv")) {
            newInstruction = new PUSH(cpgen, (float)a / (float)b);
        } else if (opName.equals("frem")) {
            newInstruction = new PUSH(cpgen, (float)a % (float)b);

        // Double operations

        } else if (opName.equals("dadd")) {
            newInstruction = new PUSH(cpgen, (double)a + (double)b);
        } else if (opName.equals("dsub")) {
            newInstruction = new PUSH(cpgen, (double)a - (double)b);
        } else if (opName.equals("dmul")) {
            newInstruction = new PUSH(cpgen, (double)a * (double)b);
        } else if (opName.equals("ddiv")) {
            newInstruction = new PUSH(cpgen, (double)a / (double)b);
        } else if (opName.equals("drem")) {
            newInstruction = new PUSH(cpgen, (double)a % (double)b);

        // Comparisons

        } else if (opName.equals("lcmp")) {
            int value;
            if ((long)a > (long)b) {
                value = 1;
            } else if ((long)a == (long)b) {
                value = 0;
            } else {
                value = -1;
            }
            newInstruction = new PUSH(cpgen, value);
        } else if (opName.equals("fcmpg") || opName.equals("fcmpl")) {
            int value;
            if (Float.isNaN((float)a) || Float.isNaN((float)b)) {
                value = opName.equals("fcmpg") ? 1 : -1;
            } else if ((float)a > (float)b) {
                value = 1;
            } else if ((float)a == (float)b) {
                value = 0;
            } else {
                value = -1;
            }
            newInstruction = new PUSH(cpgen, value);
        } else if (opName.equals("dcmpg") || opName.equals("dcmpl")) {
            int value;
            if (Double.isNaN((double)a) || Double.isNaN((double)b)) {
                value = opName.equals("dcmpg") ? 1 : -1;
            } else if ((double)a > (double)b) {
                value = 1;
            } else if ((double)a == (double)b) {
                value = 0;
            } else {
                value = -1;
            }
            newInstruction = new PUSH(cpgen, value);

        } else {
            // reached when instruction is not handled
            System.out.println("Couldn't optimise: " + opName);
            // return is to prevent deleting instructions, since nothing has been added.
            return false;
        }

        InstructionHandle newInstHandle = instList.insert(operand1, newInstruction);

        System.out.print("Replacing: \033[0;31m");
        System.out.print(operand1);
        System.out.print("\n           ");
        System.out.print(operand2);
        System.out.print("\n           ");
        System.out.print(operator);
        System.out.print("\033[0m\n     with: \033[0;32m");
        System.out.print(newInstHandle);
        System.out.println("\033[0m\n");

        // Delete the 3 instructions making up the expression
        deleteInstruction(operand1, newInstHandle, instList);
        deleteInstruction(operand2, newInstHandle, instList);
        deleteInstruction(operator, newInstHandle, instList);

        return true;
    }

    public boolean optimizeAllUnaryComparisons(InstructionList instList) {

        // Unary comparison pattern
        String pattern = reConstPushInstruction + " " + reUnaryComparison;

        boolean somethingWasOptimized = false;

        InstructionFinder f = new InstructionFinder(instList);
        for (Iterator<?> e = f.search(pattern); e.hasNext(); ) {
            InstructionHandle[] handles = (InstructionHandle[])e.next();
            boolean optimizedThisPass = this.optimizeUnaryComparisonExpr(handles, instList);
            somethingWasOptimized = somethingWasOptimized || optimizedThisPass;
        }

        return somethingWasOptimized;
    }

    public boolean optimizeUnaryComparisonExpr(InstructionHandle[] handles, InstructionList instList) {

        InstructionHandle ifInstruction = handles[1];
        String opName = ifInstruction.getInstruction().getName();

        int operand = (int)getConstValue(handles[0].getInstruction(), cpgen);

        InstructionHandle target = ((IfInstruction)ifInstruction.getInstruction()).getTarget();
        boolean follow = false;

        if (opName.equals("ifeq")) {
            follow = operand == 0;
        } else if (opName.equals("ifne")) {
            follow = operand != 0;
        } else if (opName.equals("iflt")) {
            follow = operand < 0;
        } else if (opName.equals("ifle")) {
            follow = operand <= 0;
        } else if (opName.equals("ifgt")) {
            follow = operand > 0;
        } else if (opName.equals("ifge")) {
            follow = operand >= 0;
        } else {
            // reached when instruction is not handled
            System.out.println("Couldn't optimise: " + opName);
            // return because we don't want to delete instructions
            return false;
        }

        InstructionHandle newTarget;

        if (follow) {
            BranchInstruction gotoInstruction = new GOTO(target);
            BranchHandle gotoInstHandle = instList.insert(handles[0], gotoInstruction);
            newTarget = gotoInstHandle;

            System.out.print("Adding: \033[0;32m");
            System.out.print(gotoInstHandle);
            System.out.println("\033[0m");
        } else {
            newTarget = ifInstruction.getNext();
        }

        System.out.print("Removing:  \033[0;31m");
        System.out.print(handles[0]);
        System.out.print("\n           ");
        System.out.print(ifInstruction);
        System.out.println("\033[0m\n");

        deleteInstruction(handles[0], newTarget, instList);
        deleteInstruction(ifInstruction, newTarget, instList);

        return true;
    }

    public boolean optimizeAllBinaryComparisons(InstructionList instList) {

        // Binary comparison pattern
        String pattern = reConstPushInstruction + " " + reConstPushInstruction + " " + reBinaryComparison;

        boolean somethingWasOptimized = false;

        InstructionFinder f = new InstructionFinder(instList);
        for (Iterator<?> e = f.search(pattern); e.hasNext(); ) {
            InstructionHandle[] handles = (InstructionHandle[])e.next();
            boolean optimizedThisPass = this.optimizeBinaryComparisonExpr(handles, instList);
            somethingWasOptimized = somethingWasOptimized || optimizedThisPass;
        }

        return somethingWasOptimized;
    }

    public boolean optimizeBinaryComparisonExpr(InstructionHandle[] handles, InstructionList instList) {

        InstructionHandle ifInstruction = handles[2];
        String opName = ifInstruction.getInstruction().getName();

        int operand1 = (int)getConstValue(handles[0].getInstruction(), cpgen);
        int operand2 = (int)getConstValue(handles[1].getInstruction(), cpgen);

        InstructionHandle target = ((IfInstruction)ifInstruction.getInstruction()).getTarget();
        boolean follow = false;

        if (opName.equals("if_icmpeq")) {
            follow = operand1 == operand2;
        } else if (opName.equals("if_icmpne")) {
            follow = operand1 != operand2;
        } else if (opName.equals("if_icmplt")) {
            follow = operand1 < operand2;
        } else if (opName.equals("if_icmple")) {
            follow = operand1 <= operand2;
        } else if (opName.equals("if_icmpgt")) {
            follow = operand1 > operand2;
        } else if (opName.equals("if_icmpge")) {
            follow = operand1 >= operand2;
        } else {
            // reached when instruction is not handled
            System.out.println("Couldn't optimise: " + opName);
            // return because we don't want to delete instructions
            return false;
        }

        InstructionHandle newTarget;

        if (follow) {
            BranchInstruction gotoInstruction = new GOTO(target);
            BranchHandle gotoInstHandle = instList.insert(handles[0], gotoInstruction);
            newTarget = gotoInstHandle;

            System.out.print("Adding: \033[0;32m");
            System.out.print(gotoInstHandle);
            System.out.println("\033[0m");
        } else {
            newTarget = ifInstruction.getNext();
        }

        System.out.print("Removing:  \033[0;31m");
        System.out.print(handles[0]);
        System.out.print("\n           ");
        System.out.print(handles[1]);
        System.out.print("\n           ");
        System.out.print(ifInstruction);
        System.out.println("\033[0m\n");

        deleteInstruction(handles[0], newTarget, instList);
        deleteInstruction(handles[1], newTarget, instList);
        deleteInstruction(ifInstruction, newTarget, instList);

        return true;
    }

    // Get the value of a ConstantPushInstruction, LDC, or LDC2_W instruction
    public Object getConstValue(Instruction instruction, ConstantPoolGen cpgen) {
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

    public boolean optimizeDynamicVariables(InstructionList instList) {

        InstructionHandle[] instHandles = instList.getInstructionHandles();

        instList.setPositions(true);
        ControlFlowGraph flowGraph = new ControlFlowGraph(mgen);
        ReachingMap loadReachingMap = new ReachingMap();
        ReachingMap storeReachingMap = new ReachingMap();

        String pattern = "(StoreInstruction|IINC)";
        InstructionFinder finder = new InstructionFinder(instList);

        for (Iterator<?> iter = finder.search(pattern); iter.hasNext(); ) {
            InstructionHandle[] handles = (InstructionHandle[])iter.next();
            InstructionHandle handle = handles[0];
            buildReachingMaps(handle, flowGraph, loadReachingMap, storeReachingMap);
        }

        boolean somethingWasOptimized = false;

        for (InstructionHandle storeInstHandle : storeReachingMap.keySet()) {
            somethingWasOptimized = optimizeStoreInstruction(storeInstHandle, instList, loadReachingMap, storeReachingMap) || somethingWasOptimized;
        }

        return somethingWasOptimized;
    }

    public void buildReachingMaps(InstructionHandle storeInstHandle, ControlFlowGraph flowGraph, ReachingMap loadReachingMap, ReachingMap storeReachingMap) {

        LocalVariableInstruction storeInstruction = (LocalVariableInstruction)storeInstHandle.getInstruction();
        int storeInstructionIndex = storeInstruction.getIndex();

        storeReachingMap.addKey(storeInstHandle);

        Set<InstructionHandle> visited = new HashSet<InstructionHandle>();
        Stack<InstructionContext> frontier = new Stack<InstructionContext>();

        InstructionHandle nextInstHandle = storeInstHandle.getNext();
        frontier.push(flowGraph.contextOf(nextInstHandle));

        while (!frontier.empty()) {

            InstructionContext context = frontier.pop();
            InstructionHandle instHandle = context.getInstruction();
            Instruction instruction = instHandle.getInstruction();

            if (instruction instanceof LoadInstruction ||
                instruction instanceof IINC) {
                int index = ((IndexedInstruction)instruction).getIndex();
                if (index == storeInstructionIndex) {
                    loadReachingMap.addReaching(instHandle, storeInstHandle);
                    storeReachingMap.addReaching(storeInstHandle, instHandle);
                }
            }

            if (instruction instanceof StoreInstruction ||
                instruction instanceof IINC) {
                int index = ((IndexedInstruction)instruction).getIndex();
                if (index == storeInstructionIndex) {
                    break;
                }
            }

            for (InstructionContext nextContext : context.getSuccessors()) {
                InstructionHandle nextHandle = nextContext.getInstruction();
                if (!visited.contains(nextHandle)) {
                    frontier.push(nextContext);
                    visited.add(nextHandle);
                }
            }
        }
    }

    public boolean optimizeStoreInstruction(InstructionHandle storeInstHandle, InstructionList instList, ReachingMap loadReachingMap, ReachingMap storeReachingMap) {

        LocalVariableInstruction storeInstruction = (LocalVariableInstruction)storeInstHandle.getInstruction();
        Collection<InstructionHandle> loadsReached = storeReachingMap.get(storeInstHandle);

        if (storeInstruction instanceof IINC) {
            return false;
        } else if (!isConstantInstruction(storeInstHandle.getPrev()) ||
            storeInstHandle.hasTargeters()) {
            return false;
        }

        if (!allLoadsCanBeOptimized(loadsReached, loadReachingMap)) {
            return false;
        }

        InstructionHandle constantInstHandle = storeInstHandle.getPrev();
        Instruction constantInstruction = constantInstHandle.getInstruction().copy();

        for (InstructionHandle loadInstHandle : loadsReached) {

            Instruction loadInstruction = loadInstHandle.getInstruction();
            InstructionHandle newTarget;

            if (loadInstruction instanceof IINC) {

                IINC iinc = (IINC)loadInstruction;
                int constValue = (int)getConstValue(constantInstruction, cpgen);
                int value = constValue + iinc.getIncrement();
                newTarget = instList.insert(loadInstHandle, new PUSH(cpgen, value));
                InstructionHandle newStore = instList.append(newTarget, new ISTORE(iinc.getIndex()));

                System.out.print("Replacing: \033[0;31m");
                System.out.print(loadInstHandle);
                System.out.print("\033[0m\n     with: \033[0;32m");
                System.out.print(newTarget);
                System.out.print("\n           ");
                System.out.print(newStore);
                System.out.println("\033[0m\n");

            } else {

                newTarget = instList.insert(loadInstHandle, constantInstruction.copy());

                System.out.print("Replacing: \033[0;31m");
                System.out.print(loadInstHandle);
                System.out.print("\033[0m\n     with: \033[0;32m");
                System.out.print(newTarget);
                System.out.println("\033[0m\n");
            }

            deleteInstruction(loadInstHandle, newTarget, instList);
        }

        System.out.print("Removing:  \033[0;31m");
        System.out.print(constantInstHandle);
        System.out.print("\n           ");
        System.out.print(storeInstHandle);
        System.out.println("\033[0m\n");

        InstructionHandle newTarget = storeInstHandle.getNext();
        deleteInstruction(constantInstHandle, newTarget, instList);
        deleteInstruction(storeInstHandle, newTarget, instList);

        return true;
    }

    public boolean isConstantInstruction(InstructionHandle instHandle) {
        if (instHandle == null) return false;
        Instruction instruction = instHandle.getInstruction();
        return instruction instanceof ConstantPushInstruction ||
               instruction instanceof LDC ||
               instruction instanceof LDC2_W;
    }

    public boolean allLoadsCanBeOptimized(Collection<InstructionHandle> loadsReached, ReachingMap loadReachingMap) {
        for (InstructionHandle loadInstHandle : loadsReached) {
            if (!(loadReachingMap.containsKey(loadInstHandle) &&
                  loadReachingMap.get(loadInstHandle).size() == 1)) {
                return false;
            }
        }
        return true;
    }

    public boolean removeDeadCode(InstructionList instList) {
        ControlFlowGraph flowGraph = new ControlFlowGraph(mgen);
        boolean somethingWasOptimized = false;
        for (InstructionHandle instHandle : instList.getInstructionHandles()) {
            boolean isDead = false;
            if (instHandle.getInstruction() instanceof GotoInstruction) {
                InstructionHandle target = ((GotoInstruction)instHandle.getInstruction()).getTarget();
                if (target.equals(instHandle.getNext())) {
                    isDead = true;
                }
            }
            if (flowGraph.isDead(instHandle)) {
                isDead = true;
            }
            if (isDead) {
                somethingWasOptimized = true;
                System.out.print("Dead code: \033[0;31m");
                System.out.print(instHandle);
                System.out.println("\033[0m\n");
                deleteInstruction(instHandle, instHandle.getNext(), instList);
            }
        }
        return somethingWasOptimized;
    }

    public void deleteInstruction(InstructionHandle instHandle, InstructionHandle newTarget, InstructionList instList) {
        instList.redirectBranches(instHandle, newTarget);
        instList.redirectExceptionHandlers(cegen, instHandle, newTarget);
        instList.redirectLocalVariables(lvgen, instHandle, newTarget);
        try {
            instList.delete(instHandle);
        } catch (TargetLostException e) {
            InstructionHandle[] targets = e.getTargets();
            System.out.println("\nINITIAL ERROR: Failed to delete instruction");
            for (int i = 0; i < targets.length; i++) {
                InstructionTargeter[] targeters = targets[i].getTargeters();
                for (int j = 0; j < targeters.length; j++) {
                    targeters[j].updateTarget(targets[i], newTarget);
                }
            }
            try {
                System.out.println("ERROR RESOLVED: Instruction deleted");
                instList.delete(instHandle);
            } catch (TargetLostException err) {
                System.out.println("\nTERMINAL ERROR: Failed to delete instruction:");
                System.out.println(instHandle);
                System.out.println("Targeters:");
                InstructionHandle[] ts = err.getTargets();
                for (int i = 0; i < ts.length; i++) {
                    InstructionTargeter[] targeters = ts[i].getTargeters();
                    for (int j = 0; j < targeters.length; j++) {
                        System.out.println(targeters[j]);
                    }
                }
            }
        }
    }

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
