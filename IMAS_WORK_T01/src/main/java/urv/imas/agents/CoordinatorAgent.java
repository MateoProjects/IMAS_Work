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
import urv.imas.utils.OurMessage;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


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
    // Settings
    private AID UserAID = null;
    private int NumClassifiers;
    private int NumInstancesPerClassifier;
    private int NumTrainingInstancesPerClassifier;
    private int NumValidationInstancesPerClassifier;
    private int NumAttributesPerClassifier;
    private int NumAttributesDataset;

    private List <AID> classifiersAIDs;
    private Instances TrainDataset;
    private Instances TestDataset;
    private List<Attribute> [] classifiersAttributes;
    private List<Integer> [] classifiersAttributesInteger;
    private Instances [] classifiersInstances;




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

        // Create the sequential behaviour for the agent life
        ParallelBehaviour sb = new ParallelBehaviour();// TODO: Change by sequential
        sb.addSubBehaviour(new QueryResponder(this, MessageTemplate.MatchPerformative(ACLMessage.INFORM))); // Initialization
        sb.addSubBehaviour(new CNResponder(this, MessageTemplate.MatchPerformative(ACLMessage.REQUEST)));   // Training and test
        addBehaviour(sb);
    }


    ///////////////////////////////////////////////////////////////// Initialization behaviour /////////////////////////////////////////////////////////////////
    class QueryResponder extends AchieveREResponder{
        private ACLMessage userReply;
        public QueryResponder (Agent myAgent, MessageTemplate mt)
        {
            super(myAgent, mt);
        }

        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response)
        {
            return null;
        }

        protected ACLMessage handleRequest(ACLMessage msg){
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);

            if (msg.getContent() != null) {
                reply.setContent("Correct initialization.");
                AID sender = msg.getSender();

                if (sender.getLocalName().equals("user")) {
                    // If first message, create the required amount of classifiers
                    if (UserAID == null){
                        UserAID = sender;

                        // Get settings
                        try {
                            OurMessage content = (OurMessage) msg.getContentObject();
                            int[] settings = (int[]) content.obj;
                            NumClassifiers = settings[0];
                            NumInstancesPerClassifier = settings[1];
                            NumTrainingInstancesPerClassifier = settings[2];
                            NumValidationInstancesPerClassifier = settings[3];
                            NumAttributesPerClassifier = settings[4];

                            // Create classifiers
                            createClassifiers();

                            // Store replay for when all classifiers callback is received
                            userReply = reply;
                        }catch(jade.lang.acl.UnreadableException e){
                            showMessage("ERROR while receiving User initialization: "+e.getMessage());
                        }

                        // Don't answer now
                        return null;
                    }
                }else if (sender.getLocalName().contains("classifier")){
                    classifiersAIDs.add(msg.getSender());
                    if(classifiersAIDs.size() == NumClassifiers){
                        send(userReply);
                        // TODO: Finish this behaviour
                    }
                } else {
                    showMessage(msg.getSender().getLocalName()+": Cannot register agent (invalid type).");
                }
            }else{
                reply.setContent("Content was empty");  // TODO:
                showMessage("Message was empty!");
            }

            return reply;
        }
    }


    // Reference from the virtual campus: https://campusvirtual.urv.cat/mod/page/view.php?id=2931726 (modified for creating agents in the current container)
    protected void createClassifiers(){
        showMessage("Creating "+NumClassifiers+" classifiers");
        String classifierName = "";
        String className = "urv.imas.agents.ClassifierAgent";
        Object[] arguments = new Object[2];
        arguments[0] = getLocalName();

        jade.wrapper.AgentContainer containerController = getContainerController();  // Get current container
        AgentController agentController = null;
        try{
            for (int i = 0; i < NumClassifiers; i++){
                classifierName = "classifier"+i;
                // TODO: arguments[1] = attributes Compute attributes to use by this classifier and add to arguments
                agentController = containerController.createNewAgent(classifierName, className, arguments);
                agentController.start();

            }
        }catch (Exception e){
            showMessage("ERROR while creating classifier "+classifierName+"\n"+e.getMessage());
        }
    }


    ///////////////////////////////////////////////////////////////// Working behaviour /////////////////////////////////////////////////////////////////
    class CNResponder extends ContractNetResponder {
        public CNResponder (Agent myAgent, MessageTemplate mt)
        {
            super(myAgent, mt);
        }
        protected ACLMessage prepareResponse (ACLMessage msg) {
            if (msg != null) {
                try {
                    OurMessage content = (OurMessage) msg.getContentObject();
                    String type = content.name;
                    Instances dataset = (Instances)content.obj;

                    // Start training or test
                    if (type.equals("train")){
                        train(dataset);
                    }
                    else if (type.equals("test")){
                        ArrayList<Integer> predictions = test(dataset);
                    }

                    // TODO: Prepare answer message

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

    class TrainingInitiator extends ContractNetInitiator{
        private String startMsg;
        public TrainingInitiator(Agent myAgent, ACLMessage CfpMsg, String startMsg)
        {
            super(myAgent, CfpMsg);
            this.startMsg = startMsg;
        }

        @Override
        public void onStart() {
            super.onStart();
            showMessage(startMsg);
        }

        protected void handleNotUnderstood (ACLMessage msg) {
            showMessage(msg.getSender().getLocalName()+" did not understand the message");
        }
        protected void handleRefuse (ACLMessage msg) {
            showMessage(msg.getSender().getLocalName()+" refused the message");
        }
        protected void handleInform (ACLMessage msg) {
            showMessage(msg.getSender().getLocalName()+" informs: " + msg.getContent());
        }
        protected void handleFailure (ACLMessage msg) {
            showMessage(msg.getSender().getLocalName()+" failed");
        }
    }

    protected void train(Instances dataset){
        // Create the sequential behaviour for the agent life
        ParallelBehaviour pb = new ParallelBehaviour();

        showMessage("Starting train");
        TrainDataset = dataset;
        //BE CAREFUL WITH -1, IF WE REMOVE THE CLASS LATER WE SHOULD REMOVE IT HERE TOO
        NumAttributesDataset = dataset.numAttributes()-1;

        Random random = new Random(42);
        classifiersAttributes = new List[NumClassifiers];
        classifiersAttributesInteger = new List[NumClassifiers];
        classifiersInstances = new Instances[NumClassifiers];

        List<Attribute> attributes = Collections.list(dataset.enumerateAttributes());
        List<Integer> attributesIndexes = IntStream.range(0, NumAttributesDataset).boxed().collect(Collectors.toList());
        List<Integer> attributesIndexesOriginal = IntStream.range(0, dataset.numAttributes()).boxed().collect(Collectors.toList());

        Collections.shuffle(attributesIndexes);
        int start = 0;
        int end = NumAttributesPerClassifier;

        // SELECT INDICES IN A WAY THAT EVERY ONE IS SELECTED AT LEAST ONCE. THEN IS RANDOM
        for(int c=0; c < NumClassifiers; c++) {
            if (start == attributes.size() - 1) {
                Collections.shuffle(attributesIndexes);
                classifiersAttributesInteger[c] = attributesIndexes.subList(0, NumAttributesPerClassifier);
            } else {
                classifiersAttributesInteger[c] = attributesIndexes.subList(start, end);
                start += NumAttributesPerClassifier;
                end += NumAttributesPerClassifier;
                if (start != attributes.size() - 1 && end >= attributes.size()) {
                    end = attributes.size() - 1;
                    start = end - NumAttributesPerClassifier;
                }
            }
            classifiersAttributes[c] = classifiersAttributesInteger[c].stream().map(attributes::get).collect(Collectors.toList());

            dataset.randomize(random);
            classifiersInstances[c] = new Instances(dataset, 0, NumInstancesPerClassifier);
            classifiersInstances[c] = deleteAttributes(classifiersInstances[c],classifiersAttributesInteger[c]);

            // Send training to classifier
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver (classifiersAIDs.get(c));
            OurMessage content = new OurMessage("train",  classifiersInstances[c]);
            try
            {
                msg.setContentObject(content);
                msg.setLanguage("JavaSerialization");
            } catch(Exception e){
                showMessage("ERROR while creating dataset message:\n" + e.getMessage());
            }
            ACLMessage trainDatasetMsg = msg;
            pb.addSubBehaviour(new TrainingInitiator(this, trainDatasetMsg, "Initializing training phase in classifier " + c));
        }
        addBehaviour(pb);
    }

    private Instances deleteAttributes(Instances classifiersInstances,List<Integer> classifierAttributes)
    {
        int deleted = 0;
        int total = NumAttributesDataset;
        for (int i  = 0; i < total; i++)
        {
            if (!classifierAttributes.contains(i))
            {
                classifiersInstances.deleteAttributeAt(i-deleted);
                deleted += 1;
            }
        }
        return classifiersInstances;
    }

    protected ArrayList<Integer> test(Instances dataset){
        showMessage("Starting test");
        ArrayList<Integer> results = new ArrayList<Integer>(TestDataset.numInstances());
        TestDataset = dataset;

        return results;
    }
}


