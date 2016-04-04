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

    InstructionList instList;
    LinkedHashMap<Integer, Collection<Integer>> instructionMap;

    public DependencyMap(InstructionList instList) {
        this.instList = instList;
        this.instructionMap = new LinkedHashMap<Integer, Collection<Integer>>();
    }

    public void addDependency(InstructionHandle keyHandle, InstructionHandle valueHandle) {
        Integer key = keyHandle.getPosition();
        Integer value = valueHandle.getPosition();
        if (!instructionMap.containsKey(key)) {
            instructionMap.put(key, new ArrayList<Integer>());
        }
        instructionMap.get(key).add(value);
    }

    public boolean containsKey(InstructionHandle keyHandle) {
        Integer key = keyHandle.getPosition();
        return instructionMap.containsKey(key);
    }

    public Set<InstructionHandle> get(InstructionHandle keyHandle) {
        Integer key = keyHandle.getPosition();
        return mapToInstHandles(instructionMap.get(key));
    }

    public Set<InstructionHandle> keySet() {
        return mapToInstHandles(instructionMap.keySet());
    }

    public Collection<Set<InstructionHandle>> values() {
        Collection<Collection<Integer>> collection = instructionMap.values();
        Collection<Set<InstructionHandle>> output = new ArrayList<Set<InstructionHandle>>();
        for (Collection<Integer> positions : collection) {
            output.add(mapToInstHandles(positions));
        }
        return output;
    }

    public Set<InstructionHandle> mapToInstHandles(Collection<Integer> positions) {
        Set<InstructionHandle> output = new HashSet<InstructionHandle>();
        for (Integer position : positions) {
            output.add(instList.findHandle(position));
        }
        return output;
    }

}
