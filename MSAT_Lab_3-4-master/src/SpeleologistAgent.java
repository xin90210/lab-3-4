import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Random;

public class SpeleologistAgent extends Agent {

    private AID navAgent;
    private AID envAgent;
    @Override
    protected void setup() {
        System.out.println("Hello! Speleologist agent " + getAID().getName() + " is ready!");
        addBehaviour(new WakerBehaviour(this,5000) {
            @Override
            protected void onWake() {
                DFAgentDescription forNavAgent = new DFAgentDescription();
                DFAgentDescription forCave = new DFAgentDescription();
                ServiceDescription sdNavAgent = new ServiceDescription();
                ServiceDescription sdCave = new ServiceDescription();
                sdNavAgent.setType("navigator-of-wumpus-world");
                sdCave.setType("wumpus-cave");
                forNavAgent.addServices(sdNavAgent);
                forCave.addServices(sdCave);
                try {
                    navAgent = DFService.search(myAgent, forNavAgent)[0].getName();
                    envAgent = DFService.search(myAgent, forCave)[0].getName();
                }
                catch (FIPAException fe) {
                    fe.printStackTrace();
                }
                myAgent.addBehaviour(new GoldSearchingBehaviour());
            }
        });

    }

    private class GoldSearchingBehaviour extends Behaviour {

        private int step = 0;
        private MessageTemplate mt;
        private String message;
        private String[] questions = {"Hey, there is a %s here. ", "Here is something %s .", "Oh my god, I feel that %s here. "};
        @Override
        public void action() {
            switch (step) {
                case 0:
                    ACLMessage requestPercept = new ACLMessage(ACLMessage.REQUEST);
                    requestPercept.addReceiver(envAgent);
                    requestPercept.setConversationId("percept");
                    myAgent.send(requestPercept);
                    System.out.println(getAID().getName()+" -- gathering information about cave");
                    mt = MessageTemplate.MatchConversationId("percept");
                    step++;
                    break;
                case 1:
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            message = prepareSentence(reply.getContent());
                            step++;
                        }
                    } else {
                        block();
                    }
                    break;
                case 2:
                    ACLMessage askForAction = new ACLMessage(ACLMessage.REQUEST);
                    askForAction.addReceiver(navAgent);
                    askForAction.setContent(message);
                    askForAction.setConversationId("Ask-for-action");
                    myAgent.send(askForAction);
                    System.out.println(getAID().getName() + " -- " + message);
                    mt = MessageTemplate.MatchConversationId("Ask-for-action");
                    step++;
                    break;
                case 3:
                    ACLMessage reply2 = myAgent.receive(mt);
                    if (reply2 != null) {
                        if (reply2.getPerformative() == ACLMessage.PROPOSE) {
                            message = proccessSentence(reply2.getContent());
                            step++;
                        }
                    } else {
                        block();
                    }
                    break;
                case 4:
                    ACLMessage action = new ACLMessage(ACLMessage.CFP);
                    action.addReceiver(envAgent);
                    action.setContent(message);
                    action.setConversationId("action");
                    myAgent.send(action);
                    System.out.println(getAID().getName() + " -- "+ message);
                    mt = MessageTemplate.and(
                            MessageTemplate.MatchConversationId("action"),
                            MessageTemplate.MatchInReplyTo(action.getReplyWith()));
                    step++;
                    break;
                case 5:
                    if (message == "Climb") {
                        step++;
                        doDelete();
                        return;
                    }
                    else
                        step=0;
                    break;
            }
        }

        private String proccessSentence(String content) {
            if (content.contains("forward"))
                return "Forward";
            else if (content.contains("shoot"))
                return "Shoot";
            else if (content.contains("climb"))
                return "Climb";
            else if (content.contains("grab"))
                return "Grab";
            else if (content.contains("right"))
                return "TurnRight";
            else if (content.contains("left"))
                return "TurnLeft";
            throw new IllegalStateException("Unexpected action!");
        }

        private String prepareSentence(String content) {
            StringBuilder temp = new StringBuilder();
            if (content.contains("Stench"))
                temp.append(String.format(questions[new Random().nextInt(3)], "stench"));
            if (content.contains("Breeze"))
                temp.append(String.format(questions[new Random().nextInt(3)], "breeze"));
            if (content.contains("Glitter"))
                temp.append(String.format(questions[new Random().nextInt(3)], "glitter"));
            if (content.contains("Bump"))
                temp.append(String.format(questions[new Random().nextInt(3)], "bump"));
            if (content.contains("Scream"))
                temp.append(String.format(questions[new Random().nextInt(3)], "scream"));
            temp.append("What should I do?");
            return temp.toString();
        }

        @Override
        public boolean done() {
            return step == 6;
        }
    }

    @Override
    protected void takeDown() {
        System.out.println("Speleologist agent" + getAID().getName()+"is terminating");
    }

}
