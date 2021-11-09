package urv.imas.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;
import jade.proto.AchieveREResponder;
import jade.proto.ContractNetInitiator;
import jade.domain.FIPANames;

import java.util.Date;
import java.util.Vector;
import java.util.Enumeration;
import java.util.List;
import java.util.LinkedList;
import jade.core.behaviours.*;

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
    private AID UserAID;
    private List <AID> classifiersAIDs ;


    public void showMessage(String mss) {
        System.out.println(getLocalName()+" -> "+mss);
    }

    //Nou comportament que estén la part iniciadora d’un Query
    class QueryResponder extends AchieveREResponder
    {
        //El constructor rep una instància de l’agent que l’ha creat
        //i el missatge a enviar inicialment

        public QueryResponder (Agent myAgent, MessageTemplate mt)
        {
            super(myAgent, mt);
        }
        //Mètodes que caldrà implementar amb les accions pertinents
        protected ACLMessage prepareResponse (ACLMessage msg)
        {
            if (msg != null) {
                String content = msg.getContent();
                if (content.equals("I am USER")) {
                    UserAID = msg.getSender();

                }else if (content.equals("I am CLASSIFIER")){
                    classifiersAIDs.add(msg.getSender());

                } else {
                    showMessage(msg.getSender().getLocalName()+": Cannot register agent (invalid type).");
                }
            }else{
                showMessage("Message was empty!");
            }

            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent("Correct initialization.");
            return reply;
        }
    }


    protected void setup() {
        // Read name of classifiers as arguments (NOT ACTIVE NOW)
        Object[] args = getArguments();
        if (args != null && args.length > 0) {}else{}

        classifiersAIDs = new LinkedList<AID>();

        QueryResponder bh1 = new QueryResponder(this, MessageTemplate.MatchPerformative(ACLMessage.INFORM));
        addBehaviour(bh1);

    }
}


