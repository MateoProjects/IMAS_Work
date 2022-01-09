package urv.imas.agents;

import urv.imas.utils.*;

import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.wrapper.AgentController;
import weka.core.Attribute;
import weka.core.Instances;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * This agent implements the Coordinator Agent that receives the training and testing requests, creates the classifiers,
 * splits the datasets and gathers the results of the prediction and testing request to the classifiers.
 * @author Team 1: Sergi Albiach, Anna Garriga, Benet Manzanares and Ramon Mateo.
 * @version  $Date: 2022-01-09 14:00:00 +0100 (Barcelona, 09 January 2022) $ $Revision: 1 $
 */
public class CoordinatorAgent extends OurAgent
{
    // Settings
    protected int NumClassifiers;
    protected int NumInstancesPerClassifier;
    protected int NumValidationInstancesPerClassifier;
    protected int NumAttributesPerClassifier;
    protected int NumAttributesDataset;

    protected AID[] ClassifiersAIDs;
    protected List<Attribute> [] ClassifiersAttributes;
    protected List<Integer> [] ClassifiersAttributesInteger;
    protected Instances [] ClassifiersDatasets;
    protected ACLMessage TrainingReply;
    protected double[] ClassifiersPrecisions;
    protected double[] ClassifiersWeights;
    protected int NumTrainedClassifiers;
    protected ACLMessage TestingReply;
    protected List<Integer>[] InstancesIdxPerClassifier;
    protected List<Double>[] WeightedPredictions;
    protected double[] TestingPredictions;
    protected int NumTestedClassifiers;


    ///////////////////////////////////////////////////////////////// Initialization /////////////////////////////////////////////////////////////////
    protected void setup() {
        RegisterInDF("coordinator");

        // Set default parameters
        NumClassifiers = 0;
        NumTrainedClassifiers = 0;

        // Create the behaviour for the agent life
        addBehaviour(new OurRequestResponder(this, MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                "Training and test phase", this::workingCallback));
    }


    ///////////////////////////////////////////////////////////////// Working behaviour /////////////////////////////////////////////////////////////////
    protected ACLMessage workingCallback(ACLMessage msg){
        ACLMessage reply;

        reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        try {
            OurMessage content = (OurMessage) msg.getContentObject();
            String type = content.name;
            Object[] args = (Object[])content.obj;

            // Start training or test
            if (type.equals("train")){
                int[] settings = (int[])args[1];
                initialization(settings);
                Instances dataset = (Instances)args[0];
                train(dataset);
                TrainingReply = reply;
                reply = null;   // Don't answer now, wait for the results (at trainingCallback)
            }
            else if (type.equals("test")){
                Instances[] testInstances = (Instances[])args[0];
                test(testInstances);
                TestingReply = reply;
                reply = null;   // Don't answer now, wait for the results (at testingCallback)
            }
            else
                throw new Exception("Request type ["+type+"] unknown");

        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = "Exception: "+e.getMessage();
            reply.setPerformative(ACLMessage.FAILURE);
            reply.setContent(errorMsg);
            showErrorMessage(errorMsg);
        }

        return reply;
    }


    ///////////////////////////////////////////////////////////////// Classifiers initialization /////////////////////////////////////////////////////////////////
    protected void initialization(int[] settings){
        NumClassifiers = settings[0];
        NumInstancesPerClassifier = settings[1];
        NumValidationInstancesPerClassifier = settings[2];
        NumAttributesPerClassifier = settings[3];

        createClassifiers();

        getClassifiersAIDs();
    }

    protected void createClassifiers(){ // TODO: Adapt this for multiple calls
        showMessage("Creating "+NumClassifiers+" classifiers");

        ClassifiersAIDs = new AID[NumClassifiers];
        ClassifiersPrecisions = new double[NumClassifiers];
        ClassifiersWeights = new double[NumClassifiers];

        // Reference from the virtual campus: https://campusvirtual.urv.cat/mod/page/view.php?id=2931726 (modified for creating agents in the current container)
        jade.wrapper.AgentContainer containerController = getContainerController();  // Get current container
        AgentController agentController;
        String classifierName = "";
        String className = "urv.imas.agents.ClassifierAgent";
        Object[] arguments = new Object[2];
        arguments[0] = getLocalName();
        try{

            for (int i = 0; i < NumClassifiers; i++){
                classifierName = "classifier"+i;
                agentController = containerController.createNewAgent(classifierName, className, arguments);
                agentController.start();
            }

        }catch (Exception e){
            showErrorMessage("while creating classifier "+classifierName+"\n"+e.getMessage());
        }
    }

    private void getClassifiersAIDs() {
        jade.util.leap.List classifiers;
        do{
            classifiers = getFromDF("classifier");
        }while(classifiers.size() < NumClassifiers);
        showMessage("There are "+classifiers.size()+" classifiers for training");

        AID classifier;
        int idx;
        for(int c=0; c < classifiers.size(); c++){
            classifier = (AID) classifiers.get(c);
            idx = classifierNameToIdx(classifier.getLocalName());
            ClassifiersAIDs[idx] = classifier;
        }
    }

    protected int classifierNameToIdx(String name){
        int v = 0;
        try{
            v = Integer.parseInt(name.replaceAll("[\\D]", ""));
        }catch(Exception e){
            showErrorMessage("This should not happen: " + e.getMessage()); // Not possible
        }

        return v;
    }


    ///////////////////////////////////////////////////////////////// Training /////////////////////////////////////////////////////////////////
    protected void train(Instances dataset){
        showMessage("Starting training");
        NumTrainedClassifiers = 0;

        // Create the parallel behaviour for classifiers training
        ParallelBehaviour trainingBhv = new ParallelBehaviour(ParallelBehaviour.WHEN_ALL);

        //BE CAREFUL WITH -1, IF WE REMOVE THE CLASS LATER WE SHOULD REMOVE IT HERE TOO
        NumAttributesDataset = dataset.numAttributes()-1;

        Random rng = new Random();
        ClassifiersAttributes = new List[NumClassifiers];
        ClassifiersAttributesInteger = new List[NumClassifiers];
        ClassifiersDatasets = new Instances[NumClassifiers];
        WeightedPredictions = new List[NumClassifiers];

        List<Attribute> attributes = Collections.list(dataset.enumerateAttributes());
        List<Integer> attributesIndexes = IntStream.range(0, NumAttributesDataset).boxed().collect(Collectors.toList());

        Collections.shuffle(attributesIndexes);
        int start = 0;
        int end = NumAttributesPerClassifier;

        // SELECT INDICES IN A WAY THAT EVERY ONE IS SELECTED AT LEAST ONCE. THEN IS RANDOM
        ACLMessage msg;
        Object[] content;
        Instances classifierDataset;
        for(int c=0; c < NumClassifiers; c++) {
            WeightedPredictions[c] = new LinkedList<>();
            // Get selected attributes indexes
            if (start < attributes.size() - 1) {
                ClassifiersAttributesInteger[c] = attributesIndexes.subList(start, end);
                start += NumAttributesPerClassifier;
                end += NumAttributesPerClassifier;
                if (start != attributes.size() - 1 && end >= attributes.size()) {
                    end = attributes.size() - 1;
                    start = end - NumAttributesPerClassifier;
                }
            }
            else {
                Collections.shuffle(attributesIndexes);
                ClassifiersAttributesInteger[c] = attributesIndexes.subList(0, NumAttributesPerClassifier);
            }

            ClassifiersAttributes[c] = ClassifiersAttributesInteger[c].stream().map(attributes::get).collect(Collectors.toList());
            dataset.randomize(rng);
            classifierDataset = new Instances(dataset, 0, NumInstancesPerClassifier);
            classifierDataset.setClassIndex(classifierDataset.numAttributes() - 1);
            filterAttributes(classifierDataset, ClassifiersAttributesInteger[c]);

            // Create message and send training request to classifier
            content = new Object[2];
            content[0] = classifierDataset;
            content[1] = NumValidationInstancesPerClassifier;
            msg = createOurMessageRequest(ClassifiersAIDs[c], "train", content);


            trainingBhv.addSubBehaviour(new OurRequestInitiator(this, msg, (this::trainingCallback)));
        }
        addBehaviour(trainingBhv);
    }

    protected void filterAttributes(Instances dataset, List<Integer> desiredAttrsIdxs){
        int deleted = 0;
        int total = dataset.numAttributes();

        for (int i  = 0; i < total; i++)
            if (!desiredAttrsIdxs.contains(i) && dataset.classIndex() != (i-deleted))
            {
                dataset.deleteAttributeAt(i-deleted);
                deleted += 1;
            }
    }

    protected void trainingCallback(ACLMessage msg){
        try {
            double accuracy = (double) msg.getContentObject();
            int idx = classifierNameToIdx(msg.getSender().getLocalName());
            ClassifiersPrecisions[idx] = accuracy;
            showMessage(msg.getSender().getLocalName()+" finished training with an accuracy of "+ accuracy);

            // Check if
            NumTrainedClassifiers++;
            if(NumTrainedClassifiers >= NumClassifiers){
                // Compute classifier weights
                double totalAccuracy = 0;
                for (double classifiersPrecision : ClassifiersPrecisions)
                    totalAccuracy += classifiersPrecision;

                for(int i=0; i<ClassifiersWeights.length; i++)
                    ClassifiersWeights[i] = ClassifiersPrecisions[i] / totalAccuracy;

                // Compute average accuracy
                double avgAccuracy = totalAccuracy / ClassifiersPrecisions.length;

                // Finally, send the reply
                TrainingReply.setContent("Training completed by "+NumTrainedClassifiers+" classifiers with an average accuracy of "+avgAccuracy);
                send(TrainingReply);
            }
        } catch (UnreadableException e) {
            showErrorMessage("Training callback failed for message: "+msg);
        }
    }


    ///////////////////////////////////////////////////////////////// Test /////////////////////////////////////////////////////////////////
    protected void test(Instances[] testInstances){
        showMessage("Starting testing");
        TestingPredictions = new double[testInstances.length];
        int c,i;

        // Get classifier's used attributes
        List<Instances>[] instsPerClassifier = new List[NumClassifiers];
        InstancesIdxPerClassifier = new List[NumClassifiers];
        for(c=0; c < instsPerClassifier.length; c++)
        {
            instsPerClassifier[c] = new LinkedList<>();
            InstancesIdxPerClassifier[c] = new LinkedList<>();
        }

        // Get instances attributes
        List<Attribute>[] attrsPerInstance = new List[testInstances.length];
        for(i=0; i < attrsPerInstance.length; i++)
        {
            attrsPerInstance[i] = new LinkedList<Attribute>();
            Enumeration attrs = testInstances[i].enumerateAttributes();
            while(attrs.hasMoreElements())
                attrsPerInstance[i].add((Attribute)attrs.nextElement());
        }

        // Compute matches between instances and classifiers and start testing if possible
        ParallelBehaviour pb = new ParallelBehaviour(); // Create the parallel behaviour for classifiers testing
        List<Attribute> classifierAttrs;
        Instances inst;
        ACLMessage msg;
        Object[] cont;
        for(c=0; c < NumClassifiers; c++)
        {
            // Get instances which match with classifier
            classifierAttrs = ClassifiersAttributes[c];
            for(i=0; i < testInstances.length; i++) {
                inst = new Instances(testInstances[i]);    // Create a copy
                filterAttributes(inst, attrsPerInstance[i], classifierAttrs);
                if (inst.numAttributes() == classifierAttrs.size()+1) // +1 because of the class attribute
                {
                    // If contains all the attributes
                    instsPerClassifier[c].add(inst);
                    InstancesIdxPerClassifier[c].add(i);
                }
            }

            // If there is an instance -> Create message and send test request
            if(instsPerClassifier[c].size() > 0){
                Object[] content = new Object[1];
                content[0] = instsPerClassifier[c];
                msg = createOurMessageRequest(ClassifiersAIDs[c], "test", content);
                pb.addSubBehaviour(new OurRequestInitiator(this, msg, (this::testingCallback)));
            }
        }
        addBehaviour(pb);
    }

    protected void filterAttributes(Instances inst, List<Attribute> instAttrs, List<Attribute> desiredAttrs){
        Attribute attr;
        int numDeleted = 0;
        for (int i=0; i < instAttrs.size(); i++){
            attr = instAttrs.get(i);
            if(!desiredAttrs.contains(attr) && inst.classIndex() != (i-numDeleted))
            {
                inst.deleteAttributeAt(i-numDeleted);
                numDeleted++;    // To compensate removing
            }
        }
    }

    protected void testingCallback(ACLMessage msg){
        try {
            String classifier_name = msg.getSender().getLocalName();
            int classifierIdx = classifierNameToIdx(classifier_name);
            double[] predictions  = (double[]) msg.getContentObject();
            showMessage(classifier_name+" finished testing with a total of "+predictions.length+" predictions");

            // Accumulate WEIGHTED_PREDS = [((PREDICTIONS*2)-1)*VALIDATION_ACCURACY]
            int instanceIdx;
            for (int i = 0; i < predictions.length; i++)
            {
                double res = (predictions[i]*2-1)*ClassifiersWeights[classifierIdx];
                instanceIdx = InstancesIdxPerClassifier[classifierIdx].get(i);
                TestingPredictions[instanceIdx] += res;
            }

            // Check if all classifiers have finished
            NumTestedClassifiers++;
            if (NumTestedClassifiers == NumClassifiers){
                // Discretice predicitons
                for (int i = 0; i < TestingPredictions.length; i++){
                    TestingPredictions[i] = (TestingPredictions[i]>0) ? 1.0 : 0.0;
                }

                // Finally, send the reply
                try{
                    TestingReply.setContentObject(TestingPredictions);
                    send(TestingReply);
                }catch(Exception e){
                    String errorMsg = "Exception during results gathering:"+e.getMessage();
                    showErrorMessage(errorMsg);
                    TestingReply.setPerformative(ACLMessage.FAILURE);
                    TestingReply.setContent(errorMsg);
                    send(TestingReply);
                }
                showMessage("Testing completed");
            }

        } catch (UnreadableException e) {
            showErrorMessage("Testing callback failed for message: "+msg);
        }
    }
}


