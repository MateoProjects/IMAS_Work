package urv.imas.utils;

import jade.proto.AchieveREInitiator;
import jade.lang.acl.ACLMessage;

import java.util.Objects;
import java.util.function.*;


public class OurRequestInitiator extends AchieveREInitiator{
    private OurAgent agent;
    private String taskName;
    private Consumer<ACLMessage> callback;

    public OurRequestInitiator(OurAgent myAgent, ACLMessage request, String taskName, Consumer<ACLMessage> callback)
    {
        super(myAgent, request);
        this.agent = myAgent;
        this.taskName = taskName;
        this.callback = callback;
    }
    public OurRequestInitiator(OurAgent myAgent, ACLMessage request, String taskName)
    {
        super(myAgent, request);
        this.agent = myAgent;
        this.taskName = taskName;
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

    protected void handleAgree (ACLMessage msg) {
        agent.showMessage(getSenderName(msg)+" agrees to perform the request");
    }
    protected void handleNotUnderstood (ACLMessage msg) {
        agent.showMessage(getSenderName(msg)+" did not understand the request: "+msg);
    }
    protected void handleRefuse (ACLMessage msg) {
        agent.showMessage(getSenderName(msg)+" refused the request: "+msg);
    }
    protected void handleOutOfSequence (ACLMessage msg) {
        agent.showMessage(getSenderName(msg)+" sent a message out of sequence: "+msg);
    }

    protected void handleInform (ACLMessage msg) {
        if(callback != null)
            callback.accept(msg);
        else
            agent.showMessage(getSenderName(msg)+" informs: " + msg.getContent());

    }
    protected void handleFailure (ACLMessage msg) {
        agent.showMessage(getSenderName(msg)+" failed: " + msg.getContent());
    }
}