package urv.imas.agents;

import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.AchieveREInitiator;
import jade.proto.AchieveREResponder;
import jade.proto.ContractNetInitiator;
import jade.domain.FIPANames;
import jade.proto.ContractNetResponder;
import jade.wrapper.AgentController;
import urv.imas.utils.OurDataset;
import weka.core.Instances;

import java.util.Date;
import java.util.Vector;
import java.util.Enumeration;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;


/**
 * This agent implements a simple Ping Agent that registers itself with the DF and
 * then waits for ACLMessages.
 * If  a REQUEST message is received containing the string "ping" within the content
 * then it replies with an INFORM message whose content will be the string "pong".
 *
 * @author Tiziana Trucco - CSELT S.p.A.
 * @version  $Date: 2010-04-08 13:08:55 +0200 (gio, 08 apr 2010) $ $Revision: 6297 $
 */
public class CoordinatorAgent extends Agent
{
    private AID UserAID = null;
    private List <AID> classifiersAIDs;
    private int NumClassifiers;
    private Instances TrainDataset;
    private Instances TestDataset;

    ///////////////////////////////////////////////////////////////// Auxiliar methods /////////////////////////////////////////////////////////////////
    public void showMessage(String mss) {
        System.out.println(getLocalName()+" -> "+mss);
    }

    ///////////////////////////////////////////////////////////////// Initialization /////////////////////////////////////////////////////////////////
    protected void setup() {
        // Read name of classifiers as arguments (NOT ACTIVE NOW)
        Object[] args = getArguments();
        if (args != null && args.length > 0) {}else{}

        classifiersAIDs = new LinkedList<AID>();

        QueryResponder bh1 = new QueryResponder(this, MessageTemplate.MatchPerformative(ACLMessage.INFORM));
        addBehaviour(bh1);

        CNResponder bh2 = new CNResponder(this, MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
        addBehaviour(bh2);


    }

    protected void createClassifiers(){
        String[][] agentsSettings = new String[NumClassifiers][3];
        showMessage("Creating "+NumClassifiers+" classifiers");

        for (int i = 0; i < agentsSettings.length; i++){
            agentsSettings[i][0] = "classifier"+i;
            agentsSettings[i][1] = "urv.imas.agents.ClassifierAgent";
            agentsSettings[i][2] = getLocalName();
            // TODO: Compute attributes to use by this classifier
        }

        createClassifiersFromList(agentsSettings);
    }

    // Reference from the virtual campus: https://campusvirtual.urv.cat/mod/page/view.php?id=2931726 (modified for creating agents in the current container)
    protected void createClassifiersFromList(String[][] agentsSettings){
        jade.wrapper.AgentContainer ac = getContainerController();  // Get current container

        int k = 0;
        try{
            AgentController another = null;
            for (k = 0; k < agentsSettings.length; k++) {
                if(agentsSettings[k][2]=="") {
                    another = ac.createNewAgent(agentsSettings[k][0], agentsSettings[k][1], new Object[0]);
                } else {
                    Object[] arguments = new Object[1];
                    arguments[0] = agentsSettings[k][2];
                    another = ac.createNewAgent(agentsSettings[k][0], agentsSettings[k][1], arguments);
                }
                another.start();
            }
        }catch (Exception e){
            showMessage("ERROR while creating classifier "+agentsSettings[k][0]+"\n"+e.getMessage());
        }
    }

    ///////////////////////////////////////////////////////////////// Initialization behaviour /////////////////////////////////////////////////////////////////
    class QueryResponder extends AchieveREResponder
    {
        private ACLMessage userReply;
        public QueryResponder (Agent myAgent, MessageTemplate mt)
        {
            super(myAgent, mt);
        }

        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response)
        {
            return null;
        }

        protected ACLMessage handleRequest(ACLMessage msg)
        {
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent("Correct initialization.");

            if (msg != null) {
                String content = msg.getContent();
                if (content.contains("USER")) {
                    // If first message, create the required amount of classifiers
                    if (UserAID == null){
                        UserAID = msg.getSender();
                        NumClassifiers = Integer.parseInt(content.replaceAll("[\\D]", ""));
                        createClassifiers();
                        userReply = reply;
                        return null;
                    }
                }else if (content.contains("CLASSIFIER")){
                    classifiersAIDs.add(msg.getSender());
                    if(classifiersAIDs.size() == NumClassifiers){
                        send(userReply);
                    }
                    // TODO: Finish this behaviour (or change to the working behaviour) when all created agents are registered?
                } else {
                    showMessage(msg.getSender().getLocalName()+": Cannot register agent (invalid type).");
                }
            }else{
                showMessage("Message was empty!");
            }

            return reply;



        }
    }

    class CNResponder extends ContractNetResponder {
        public CNResponder (Agent myAgent, MessageTemplate mt)
        {
            super(myAgent, mt);
        }
        protected ACLMessage prepareResponse (ACLMessage msg) {

            if (msg != null) {
                try {
                    OurDataset request = (OurDataset) msg.getContentObject();
                    String type = request.name;
                    Instances dataset = request.instances;

                    showMessage(type + " dataset correctly received.");
                    if (type.equals("train")) TrainDataset = dataset;
                    else if (type.equals("test")) TestDataset = dataset;
                    // TODO INIT TRAIN STEP

                } catch (UnreadableException e) {
                    e.printStackTrace();
                    showMessage("Could not read message");
                }
            }else{
                showMessage("Message was empty!");
            }
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent("Dataset received.");
            return reply;
        }

    }


    ///////////////////////////////////////////////////////////////// Working behaviour /////////////////////////////////////////////////////////////////
    // TODO: Use a request responder behaviour? One behaviour for receiving requests from user and another for generating them to the classifiers?
    // TODO: Train
    // TODO: Test

}


