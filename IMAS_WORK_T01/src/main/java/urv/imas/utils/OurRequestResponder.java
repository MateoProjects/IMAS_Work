package urv.imas.utils;

import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import jade.lang.acl.ACLMessage;

import java.util.Objects;
import java.util.function.*;


public class OurRequestResponder extends AchieveREResponder{
    private OurAgent agent;
    private String taskName;
    private Function<ACLMessage, ACLMessage> computeResult;
    private boolean sendAgree;

    public OurRequestResponder(OurAgent myAgent, MessageTemplate mt, String taskName, Function<ACLMessage, ACLMessage> computeResult)
    {
        super(myAgent, mt);
        this.agent = myAgent;
        this.taskName = taskName;
        this.computeResult = computeResult;
        this.sendAgree = true;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!Objects.equals(taskName, ""))
            agent.showMessage("Starting ["+taskName+"].");
    }

    @Override
    public int onEnd(){
        int result = super.onEnd();
        if (!Objects.equals(taskName, ""))
            agent.showMessage("["+taskName+"] ended.");
        return result;
    }

    public String getSenderName(ACLMessage msg){
        return "["+msg.getSender().getLocalName()+"]";
    }


    @Override
    protected ACLMessage handleRequest (ACLMessage request) { //old prepareResponse()
        ACLMessage reply;

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
        return reply;
    }


    @Override
    protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response){
        //agent.showMessage("Preparing the result for "+getSenderName(request));    // Maybe remove this
        return computeResult.apply(request);
    }
}