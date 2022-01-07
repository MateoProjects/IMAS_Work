package urv.imas.utils;

import jade.domain.FIPANames;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.core.Agent;
import jade.core.*;


public class OurAgent extends Agent {

    ///////////////////////////////////////////////////////////////// Auxiliar methods /////////////////////////////////////////////////////////////////
    public void showMessage(String mss) {
        System.out.println(getLocalName()+" -> "+mss);
    }

    protected void RegisterInDF(String type){
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setName(getLocalName());
        sd.setType(type);
        dfd.addServices(sd);
        dfd.setName(getAID());
        try {
            DFService.register(this, dfd);
            showMessage("Registered at DF");
        } catch (FIPAException e) {
            showMessage("ERROR registering at DF: "+e.getMessage());
        }
    }

    protected jade.util.leap.List getFromDF(String type){
        jade.util.leap.List agents = new jade.util.leap.ArrayList();

        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(type);
        dfd.addServices(sd);

        try {
            SearchConstraints c = new SearchConstraints();
            DFAgentDescription[] result = DFService.search(this, dfd, c);
            for (int i = 0; i < result.length; i++)
                agents.add(result[i].getName());   // getName() is equal to get agent's AID
        }catch(Exception e){
            showMessage("ERROR searching "+type+" at DF: "+e.getMessage());
        }

        //for(int i=0; i<agents.length; i++)
        //    showMessage(""+agents.get(i));
        showMessage(""+agents.get(0));
        return agents;
    }
}