package comp207p.main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LDC2_W;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.LocalVariableGen;
import org.apache.bcel.generic.LocalVariableInstruction;

public class DependencyMap {

    LinkedHashMap<InstructionHandle, Collection<InstructionHandle>> instructionMap;

    public DependencyMap() {
        this.instructionMap = new LinkedHashMap<InstructionHandle, Collection<InstructionHandle>>();
    }

    public void addKey(InstructionHandle key) {
        if (!instructionMap.containsKey(key)) {
            instructionMap.put(key, new ArrayList<InstructionHandle>());
        }
    }

    public void addDependency(InstructionHandle key, InstructionHandle value) {
        addKey(key);
        instructionMap.get(key).add(value);
    }

    public boolean containsKey(InstructionHandle key) {
        return instructionMap.containsKey(key);
    }

    public Collection<InstructionHandle> get(InstructionHandle key) {
        return instructionMap.get(key);
    }

    public Set<InstructionHandle> keySet() {
        return instructionMap.keySet();
    }

    public Collection<Collection<InstructionHandle>> values() {
        return instructionMap.values();
    }

}
