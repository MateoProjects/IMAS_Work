package urv.imas.utils;

import java.io.Serializable;
import weka.core.Instances;

public class OurMessage implements Serializable {
    public String name;
    public Serializable obj;

    public OurMessage (String n, Serializable obj) {
        name = n;
        this.obj = obj;
    }
}