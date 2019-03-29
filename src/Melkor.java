import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class Melkor extends Agent {
    public static AtomicLong iterationNumber = new AtomicLong(1);
    private static ArrayList<Long> driverWaitingTimeArray = new ArrayList<>();

    public static final int passengersCount = 100;
    private static int driversCount = 4;

    private static AgentContainer mainContainer;

    public static int xCity = 7900;
    public static int yCity = 8100;

    public static void setMainContainer(AgentContainer mainContainer) {
        Melkor.mainContainer = mainContainer;
    }

    @Override
    protected void setup(){
        DFServiceHelper.registerAgentInYellowPages(this, "DarkMelkor", "LordOfEverything");
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                System.out.println("Azaza");
                if(getAverageTaxiWaitingTime()>60)
                {
                    incrementDriversCount();
                }
                driverWaitingTimeArray.clear();
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static synchronized void incrementDriversCount(){
        Agent aeroDriverAgent = new DriverAgent();
        AgentController aeroDriverAgentController = null;
        try {
            aeroDriverAgentController = mainContainer.acceptNewAgent("aeroDriverAgent" + driversCount, aeroDriverAgent);
            driversCount++;
            System.out.println("Add driver " + driversCount);
            aeroDriverAgentController.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getDriversCount() {
        return driversCount;
    }

    public static void sleep(long timeInMillis){
        try {
            System.out.println("start");
            Thread.sleep(timeInMillis);
            System.out.println("finish");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void addDriverWaitingTime(Long driverWaitingTime){
        driverWaitingTimeArray.add(driverWaitingTime);
    }

    public static double getAverageTaxiWaitingTime(){
        iterationNumber.incrementAndGet();
        long waitingTime = 0;
        if(!driverWaitingTimeArray.isEmpty()){
            for (Long time : driverWaitingTimeArray) {
                waitingTime += time;
            }
        }
        return driverWaitingTimeArray.size() != 0 ? waitingTime / driverWaitingTimeArray.size() : 0;
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

