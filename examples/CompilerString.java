import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;

public class CompilerString
{
    ClassParser parser = null;
    ClassGen gen = null;

    JavaClass original = null;
    JavaClass optimized = null;

    public CompilerString(String classFilePath)
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

    private void optimize()
    {
        // load the original class into a class generator
        ClassGen cgen = new ClassGen(original);
        ConstantPoolGen cpgen = cgen.getConstantPool();

        // get the current constant pool
        ConstantPool cp = cpgen.getConstantPool();
        // get the constants in the pool
        Constant[] constants = cp.getConstantPool();
        for (int i = 0; i < constants.length; i++)
        {
            // string constants take two entries in the pool
            // the first one is of ConstantString, which contains
            // an index to the second entry, which is ConstantUtf8
            // (displayed Asciz when disassembled by javap)
            //
            // ConstantUtf8 (Asciz) entries are used to store method names, etc
            // whereas we are only interested in String constants
            // So we first look for ConstantString entry,
            // then retrieve the index of ConstantUtf8 entry, which we then replace
            if (constants[i] instanceof ConstantString)
            {
                ConstantString cs = (ConstantString) constants[i];
                cp.setConstant(cs.getStringIndex(), new ConstantUtf8("Compiler"));
            }
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
        System.out.println(args[0]);
        CompilerString optimizer = new CompilerString(args[0]);
        optimizer.write(args[1]);

    }
}
