package urv.imas.utils;

import weka.core.Instances;

public class OurDataset implements java.io.Serializable {
    public String name;
    public Instances instances;
    public OurDataset (String n, Instances is) {
        name = n;
        instances = is;
    }
    public String toString() {
        return ("Dataset type "+name+"\nInstances:"+instances.toString());
    }

}