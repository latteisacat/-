import java.util.*;

public class State implements Cloneable{
    Environment environment;
    Memory memory;
    int stackPointer;
    int dynamicLink;
    int staticLink;

    State(){
        environment = new Environment();
        memory = new Memory(10000);
        stackPointer = 0;
        dynamicLink = 0;
        staticLink = 0;
    }

    State(State s, Variable variable , Value value){
        this.environment = s.environment.deepCopy();
        this.memory = s.memory;
        this.stackPointer = s.stackPointer;
        this.staticLink = s.staticLink;
        if (s.environment.contains(variable)) {
            int addr = s.getAddr(variable);
            this.memory.set(addr, value);
        }
        else {
            this.environment.add(new Pair(variable, this.stackPointer));
            this.memory.set(this.stackPointer, value);
            this.stackPointer++;
        }
    }
    State(State s){
        this.environment = s.environment.deepCopy();
        this.memory = s.memory;
        this.stackPointer = s.stackPointer;
        this.staticLink = s.staticLink;
    }

    State allocate(Declarations ds){
        if(stackPointer + ds.size() < memory.size){
            for(int i = 0; i < ds.size(); i++){
                Declaration d = ds.get(i);
                environment.add(new Pair(d.v, stackPointer));
                memory.set(stackPointer, Value.mkValue(Type.UNDEFINED));
                stackPointer++;
            }
        }
        else{
            System.out.println("Stack overflow");
            System.exit(0);
        }
        return this;
    }

    State deallocate(Declarations ds){
        for(int i = 0; i < ds.size(); i++){
            environment.remove(environment.size() - 1);
            stackPointer--;
            memory.set(stackPointer, Value.mkValue(Type.UNUSED));  // mark unused
        }
        return this;
    }

    State minus (int n){
        for(int i = 0; i < n; i++) {
            environment.remove(environment.size() - 1);
        }
        return this;
    }

    State plus(State former){
        for(int i = staticLink; i < former.environment.size(); i++){
            Pair p = former.environment.get(i);
            environment.add(p);
        }
        return this;
    }

    int getAddr(Variable v){
        for(int i = environment.size() - 1; i >= 0; i--){
            if(environment.get(i).v.equals(v)){
                return environment.get(i).addr;
            }
        }
        return -1;
    }

    public State onion(State t){
        for(int i = 0; i < t.environment.size(); i++){
            Variable key = (t.environment.get(i)).v;
            Value tvalue = t.memory.get(t.getAddr(key));
            if (environment.contains(key)) {
                int addr = getAddr(key);
                memory.set(addr, tvalue);
            }
            else {
                int taddress = t.getAddr(key);
                environment.add(new Pair(key, taddress));
                memory.set(taddress, tvalue);
            }
        }
        return this;
    }

    public void display(){
        System.out.println("State: ");
        for(int i = 0; i < this.environment.size(); i++){
            System.out.print("\t");
            System.out.print("\t");
            System.out.println(
                    "Variable: "
                    + this.environment.get(i).v +
                    ", Address: " + this.environment.get(i).addr +
                    ", Value: " + this.memory.get(this.environment.get(i).addr)
            );
        }
        // memory.display();
        System.out.println("");
    }
}

class Pair{
    Variable v;
    int addr;
    Pair(){}
    Pair(Variable v, int addr){
        this.v = v;
        this.addr = addr;
    }
}
class Environment extends ArrayList<Pair>{
    public void display(){
        System.out.println("Environment: ");
        for(int i = 0; i < this.size(); i++){
            System.out.print("\t");
            System.out.print("\t");
            System.out.println("Variable: " + this.get(i).v + ", Address: " + this.get(i).addr);
        }
    }

    @Override
    public boolean contains(Object o) {
        Variable v = (Variable)o;
        for(int i = 0; i < this.size(); i++){
            if(this.get(i).v.equals(o)){
                return true;
            }
        }
        return false;
    }

    public Environment deepCopy() {
        Environment clonedEnvironment = new Environment();
        for(Pair pair : this) {
            clonedEnvironment.add(new Pair(new Variable(pair.v.toString()), pair.addr));
        }
        return clonedEnvironment;
    }

}

class Memory extends ArrayList<Value>{
    int size;
    Memory(int size){
        this.size = size;
        for(int i = 0; i < size; i++){
            this.add(Value.mkValue(Type.UNUSED));
        }
    }


    public void display(){
        System.out.println("Memory: ");
        for(int i = 0; i < this.size; i++){
            if(get(i).type.equals(Type.UNUSED)) break;
            System.out.print("\t");
            System.out.print("\t");
            System.out.println("Address: " + i  + ", Value: " + this.get(i));
        }
    }
}