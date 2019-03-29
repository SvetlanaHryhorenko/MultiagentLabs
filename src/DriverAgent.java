import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.util.Random;

import static jade.lang.acl.MessageTemplate.MatchPerformative;

public class DriverAgent extends Agent {
    boolean isDriverFree;
    private MessageTemplate mt;

    private int xPosition;
    private int yPosition;

    private Integer driverStepNumber = 0;

    @Override
    protected void setup(){
        DFServiceHelper.registerAgentInYellowPages(this, "Driver", "LithuanianCar");
        isDriverFree = true;

        Random rand = new Random();
        xPosition = rand.nextInt(Melkor.xCity);
        yPosition = rand.nextInt(Melkor.yCity);

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                switch (driverStepNumber){
                    case 0:
                        if(isDriverFree){

                            mt = MatchPerformative(ACLMessage.PROPOSE);
                            ACLMessage invitationMessage = myAgent.receive(mt);
                            if(invitationMessage != null){
                                isDriverFree = false;
                                ACLMessage reply = invitationMessage.createReply();
                                reply.setPerformative(ACLMessage.AGREE);
                                driverStepNumber = 1;
                                myAgent.send(reply);

                            } else {
                                block();
                            }
                        } else {

                            mt = MatchPerformative(ACLMessage.PROPOSE);
                            ACLMessage invitationMessage = myAgent.receive(mt);
                            if(invitationMessage != null){
                                ACLMessage reply = invitationMessage.createReply();
                                reply.setPerformative(ACLMessage.CANCEL);
                                myAgent.send(reply);
                                driverStepNumber = 0;
                            } else {
                                block();
                            }
                        }
                        break;
                    case 1:
                        processConfirmationFromPassenger();
                        break;
                }
            }

            private void processConfirmationFromPassenger(){
                mt = MessageTemplate.or(MatchPerformative(ACLMessage.CONFIRM), MatchPerformative(ACLMessage.CANCEL));
                ACLMessage confirmationMessage = myAgent.receive(mt);
                if(confirmationMessage != null){
                    int performative = confirmationMessage.getPerformative();
                    if(performative == ACLMessage.CANCEL){
                        isDriverFree = true;
                    } else {
                        try {
                            Long travelTime = (Long) confirmationMessage.getContentObject();

                            System.out.println("Travel time = " + travelTime + " " + myAgent.getLocalName());
                            Melkor.sleep(travelTime);
                            isDriverFree = true;
                        } catch (UnreadableException e) {
                            e.printStackTrace();
                        }
                    }
                    driverStepNumber = 0;
                } else {
                    block();
                }
            }
        });
    }


    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e){
            e.printStackTrace();
        }
    }
}
