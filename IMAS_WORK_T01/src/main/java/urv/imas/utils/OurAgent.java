package urv.imas.utils;

import jade.domain.FIPANames;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.core.Agent;
import jade.core.*;
import jade.lang.acl.ACLMessage;


public class OurAgent extends Agent {

    ///////////////////////////////////////////////////////////////// Auxiliar methods /////////////////////////////////////////////////////////////////
    protected void showMessage(String text) {
        System.out.println(getLocalName()+" -> "+text);
    }

    protected void showErrorMessage(String text){
        showMessage("ERROR: "+text);
    }

    protected ACLMessage createOurMessageRequest(AID agent, String type, Object[] cont){
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(agent);
        OurMessage content = new OurMessage(type,  cont);
        try
        {
            msg.setContentObject(content);
            msg.setLanguage("JavaSerialization");
        } catch(Exception e){
            showErrorMessage("while creating "+type+" message:\n" + e.getMessage());
        }

        return msg;
    }

    ///////////////////////////////////////////////////////////////// Directory Facilitator /////////////////////////////////////////////////////////////////
    protected void RegisterInDF(String type){
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(type);
        sd.setName(getLocalName());
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
            showMessage("Registered at DF");
        } catch (FIPAException e) {
            showErrorMessage("registering at DF: "+e.getMessage());
        }
    }

    protected jade.util.leap.List getFromDF(String type){
        jade.util.leap.List agents = new jade.util.leap.ArrayList();

        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(type);
        dfd.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, dfd);
            for (int i = 0; i < result.length; i++)
                agents.add(result[i].getName());    // getName() is equal to get agent's AID
        }catch(Exception e){
            showMessage("ERROR searching "+type+" at DF: "+e.getMessage());
        }
        return agents;
    }

    protected AID blockingGetFromDF(String type){
        AID id = null;
        jade.util.leap.List agents;

        while(id == null){
            agents = getFromDF(type);
            if(agents.size() > 0)
                id = (AID) agents.get(0);
        }

        return id;
    }
}