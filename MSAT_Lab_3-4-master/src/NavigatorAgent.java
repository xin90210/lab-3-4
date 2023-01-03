
import aima.core.agent.Action;
import aima.core.agent.impl.DynamicAction;
import aima.core.environment.wumpusworld.*;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Random;

public class NavigatorAgent extends Agent {

    private AID speleolAgent;
    private HybridWumpusAgent DecisionAgent;
    private String[] answers = {"I think you should %s.", "You should %s.", "In this situation it will be better to %s.", "I would advise you to %s."};

    @Override
    protected void setup() {
        DecisionAgent = new HybridWumpusAgent();
        System.out.println("Hello! Navigator agent " + getAID().getName() + " is ready!");
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("navigator-of-wumpus-world");
        sd.setName("wumpus-gold-finder");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        addBehaviour(new SpeleolRequestBehavior());
    }

    private class SpeleolRequestBehavior extends CyclicBehaviour {
        public void action() {
            ACLMessage msg = myAgent.receive(MessageTemplate.and( MessageTemplate.MatchPerformative(ACLMessage.REQUEST),MessageTemplate.MatchConversationId("Ask-for-action")));
            if (msg != null) {
                if (speleolAgent == null)
                    speleolAgent = msg.getSender();
                if (speleolAgent.equals(msg.getSender()))
                    myAgent.addBehaviour(new SendAction(msg));
                else {
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                }
            }
            else
            {
                block();
            }
        }
    }

    private class SendAction extends OneShotBehaviour {
        ACLMessage msg;

        public SendAction(ACLMessage m)
        {
            super();
            msg = m;
        }
        public void action() {
            String content = msg.getContent();
            ACLMessage reply = msg.createReply();
            if (content != null) {
                reply.setPerformative(ACLMessage.PROPOSE);
                content = content.toLowerCase();
                AgentPercept res = new AgentPercept(
                    content.contains("stench"), 
                    content.contains("breeze"),
                    content.contains("glitter"), 
                    content.contains("bump"), 
                    content.contains("scream"));
                Action act = DecisionAgent.execute(res);
                reply.setContent(generateAction(act));
            } else {
                reply.setPerformative(ACLMessage.REFUSE);
                reply.setContent("not-available");
            }
            myAgent.send(reply);
            System.out.println(getAID().getName() + " -- " + reply.getContent());
        }

        private String generateAction(Action act) {
            String a = ((DynamicAction)act).getName();
            if (a.contains("Turn"))
                a = "turn " + a.toLowerCase().substring(4);
            else if (a.equals("Forward"))
                a = "go forward";
            else
                a = a.toLowerCase();
            return String.format(answers[new Random().nextInt(3)], a);
        }
    }
    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Navigator agent "  + getAID().getName()+ "is terminating");
    }
}