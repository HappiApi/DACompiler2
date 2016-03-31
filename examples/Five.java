import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ICONST;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.TargetLostException;

public class Five
{
    ClassParser parser = null;
    ClassGen gen = null;

    JavaClass original = null;
    JavaClass optimized = null;

    public Five(String classFilePath)
    {
        try
        {
            this.parser = new ClassParser(classFilePath);
            this.original = this.parser.parse();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    // we rewrite integer constants with 5 :)
    private void optimizeMethod(ClassGen cgen, ConstantPoolGen cpgen, Method method)
    {
        // Get the Code of the method, which is a collection of bytecode instructions
        Code methodCode = method.getCode();

        // Now get the actualy bytecode data in byte array,
        // and use it to initialise an InstructionList
        InstructionList instList = new InstructionList(methodCode.getCode());

        // Initialise a method generator with the original method as the baseline
        MethodGen methodGen = new MethodGen(method.getAccessFlags(), method.getReturnType(), method.getArgumentTypes(), null, method.getName(), cgen.getClassName(), instList, cpgen);

        // InstructionHandle is a wrapper for actual Instructions
        for (InstructionHandle handle : instList.getInstructionHandles())
        {
            // if the instruction inside is iconst
            if (handle.getInstruction() instanceof ICONST)
            {
                // insert new one with integer 5, and...
                instList.insert(handle, new ICONST(5));
                try
                {
                    // delete the old one
                    instList.delete(handle);

                }
                catch (TargetLostException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
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
    private void optimize()
    {
        // load the original class into a class generator
        ClassGen cgen = new ClassGen(original);
        ConstantPoolGen cpgen = cgen.getConstantPool();

        // Do your optimization here
        Method[] methods = cgen.getMethods();
        for (Method m : methods)
        {
            optimizeMethod(cgen, cpgen, m);

        }

        // we generate a new class with modifications
        // and store it in a member variable
        this.optimized = cgen.getJavaClass();
    }
    public void write(String optimisedFilePath)
    {
        this.optimize();

        try
        {
            FileOutputStream out = new FileOutputStream(new File(optimisedFilePath));
            this.optimized.dump(out);
        }
        catch (FileNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void main(String args[])
    {
        Five optimizer = new Five(args[0]);
        optimizer.write(args[1]);

    }
}
