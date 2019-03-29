import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.util.ArrayList;

public class Main {

    public static void main(String[] args) throws StaleProxyException {
        ArrayList<Agent> passengerAgents = new ArrayList<>();
        jade.core.Runtime runtime = Runtime.instance();
        Profile profile = new ProfileImpl();
        AgentContainer mainContainer = runtime.createMainContainer(profile);
        Melkor.setMainContainer(mainContainer);

        for(int i = 0; i < Melkor.passengersCount; i++){
            Agent passengerAgent = new PassengerAgent();
            passengerAgents.add(passengerAgent);
            AgentController passengerAgentController = mainContainer.acceptNewAgent("passengerAgent" + i, passengerAgent);
            passengerAgentController.start();
        }


        for(int i = 0; i < Melkor.getDriversCount(); i++){
            Agent driverAgent = new DriverAgent();
            AgentController aeroDriverAgentController = mainContainer.acceptNewAgent("DriverAgent" + i, driverAgent);
            aeroDriverAgentController.start();
        }
    }
}
