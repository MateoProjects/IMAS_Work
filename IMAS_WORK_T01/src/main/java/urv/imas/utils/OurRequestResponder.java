package urv.imas.utils;

import urv.imas.utils.*;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import jade.lang.acl.ACLMessage;
import java.util.function.*;


public class OurRequestResponder extends AchieveREResponder{
    private OurAgent agent;
    private String taskName;
    private String conversationID;
    private Function<ACLMessage, ACLMessage> computeResult;
    private boolean sendAgree;

    public OurRequestResponder(OurAgent myAgent, MessageTemplate mt, String taskName,String conversationID, Function<ACLMessage, ACLMessage> computeResult)
    {
        super(myAgent, mt);
        this.agent = myAgent;
        this.conversationID = conversationID;
        this.taskName = taskName;
        this.computeResult = computeResult;
        this.sendAgree = true;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (taskName != "")
            agent.showMessage("Starting ["+taskName+"] (responder).");
    }

    @Override
    public int onEnd(){
        int result = super.onEnd();
        if (taskName != "")
            agent.showMessage("["+taskName+"] ended (responder).");
        return result;
    }

    public String getSenderName(ACLMessage msg){
        return "["+msg.getSender().getLocalName()+"]";
    }


    @Override
    protected ACLMessage handleRequest (ACLMessage request) { //old prepareResponse() ?
        ACLMessage reply = null;

        String res = getSenderName(request)+" requests a task";

        if(sendAgree){
            res += ", sending agree...";
            reply = request.createReply();
            reply.setPerformative(ACLMessage.AGREE);
        }
        else{
            reply = computeResult.apply(request);
        }

        agent.showMessage(res);
        reply.setConversationId(this.conversationID);
        return reply;
    }


    @Override
    protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response){
        //agent.showMessage("Preparing the result for "+getSenderName(request));    // Maybe remove this
        ACLMessage reply = computeResult.apply(request);
        reply.setConversationId(this.conversationID);
        return reply;
    }
}