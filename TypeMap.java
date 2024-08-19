import java.util.*;

public class TypeMap extends HashMap<Variable, Type> { 

// TypeMap is implemented as a Java HashMap.  
// Plus a 'display' method to facilitate experimentation.
    public void display( ) {
        String sep ="\t";
        for (Variable key : keySet( )) {
            System.out.print(sep + key );
            Type t = this.get(key);
            if(t instanceof Prototype){
                System.out.println("(function), " + ((Prototype)t) +  ", ");
                ((Prototype)t).params.display(1);
            }
            else{
                System.out.println(", "+ get(key));
            }
        }
    }

    public TypeMap onion(TypeMap tm){
        TypeMap res = new TypeMap();
        res.putAll(this);
        res.putAll(tm);
        return res;
    }

}
