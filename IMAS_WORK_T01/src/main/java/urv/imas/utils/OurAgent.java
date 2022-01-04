package urv.imas.utils;

import jade.core.Agent;


public class OurAgent extends Agent {

    ///////////////////////////////////////////////////////////////// Auxiliar methods /////////////////////////////////////////////////////////////////
    public void showMessage(String mss) {
        System.out.println(getLocalName()+" -> "+mss);
    }


}