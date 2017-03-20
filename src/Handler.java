

import java.net.Socket;
import java.io.*;
import java.util.Iterator;
import java.util.LinkedList;


class Handler {

    private static PrintWriter wr;
    private static Elevator[] elevators;
    //public static bMonitor bqueue = new bMonitor();
    private static LinkedList<Operation> bOperations = new LinkedList<>();

    final private static Object s = new Object();

    //bcommand method adds  boperation into list of not yet taken calls of elevator
    static public void addBOperation(Operation newOp){
        synchronized (s){
            if(bOperations.isEmpty()) { bOperations.addLast(newOp);  return;}

            else if(bOperations.getLast().isEqual(newOp)) return;

            bOperations.addLast(newOp);
        }
    }

    //elevator of same direction finds someone waiting outside going the same direction
    static public boolean checkFloor(int floor, int direction) {
        boolean remove = false;
        synchronized (s) {

            if (bOperations.isEmpty())
                return false;

            for (Operation temp : bOperations) {
                if (Math.round(temp.location) == floor) {
                    if (temp.direction == direction) {
                        bOperations.remove(temp);
                        remove = true;
                    }
                }
            }
            return remove;
        }
    }

    //the direction here will be the direction from current position
    //instead of being the floor button direction
    static public Operation checkNearest(float location){
        float minDiff = 100;
        Operation nearest = null;
        synchronized (s) {
            if (bOperations.isEmpty()) return null;

            for (Operation temp : bOperations) {
                float diff = Math.abs(location - temp.location);
                if (diff < minDiff) { minDiff = diff; nearest = temp;}
            }
            bOperations.remove(nearest);

            if(nearest == null) return null;

            if(nearest.location - location > 0)         nearest.direction = 1;
            else if(nearest.location - location == 0)   nearest.direction = 0;
            else                                        nearest.direction = -1;
            return nearest;
        }
    }




    synchronized public static void sendCommand(String command)
    {
        wr.println(command);
        wr.flush();
    }

    public static void commandHandler(String command) throws InterruptedException {
        String[] sCommand = command.split(" ");
        if (sCommand.length < 5 && sCommand.length > 1) {
            String operation = sCommand[0];
            switch (operation) {
                case "b":
                    bCommand(sCommand);
                    break;
                case "p":
                    pCommand(sCommand);
                    break;
                case "f":
                    fCommand(sCommand);
                    break;
                default:
                    System.out.println("illegal input");
                    break;
            }
        }
    }


    //put floor requests on a queue.
    //elevator checks when on the floor if someone is waiting to go on the same direction here
    //if so put him into the elevator operation queue.

    //b location direction
    static void bCommand(String[] sCommand){

        float floor = Float.parseFloat(sCommand[1]);
        int direction = Integer.parseInt(sCommand[2]);


        Operation temp = new Operation(direction,floor);


        synchronized (s){
            addBOperation(temp);
        }

    }

    //set location
    static void fCommand(String[] sCommand){
        int elev = Integer.parseInt(sCommand[1]);
        float location = Float.parseFloat(sCommand[2]);
        elevators[elev].setLocation(location);
    }

    static void pCommand(String[] sCommand){
        int elev = Integer.parseInt(sCommand[1]);
        int instruction = Integer.parseInt(sCommand[2]);

        if(instruction == 32000)
            elevators[elev].addStop(new Operation(0,(float) instruction));
        else{
            //Are we going upward or downward from current pos?
            int direction = elevators[elev].compLocation((float)instruction);
            Operation temp = new Operation(direction,instruction);

            //Where should this command be positioned in the queue?
            elevators[elev].getSpot(temp);
        }
    }


    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.out.println("usage: java Handler <host> <port> <elevator count> <floor count>");
            System.exit(1);
        }

        String host = args[0];
        int port = -1, nElev = -1, nFloors = -1;

        try {
            port = Integer.parseInt(args[1]);
            nElev = Integer.parseInt(args[2]);
            nFloors = Integer.parseInt(args[3]);

            if (port < 1 || nElev < 1 || nFloors < 1) {
                throw new Exception();
            }
        } catch (Exception exception) {
            System.out.println("error: invalid <port>, <elevator count> or <floor count>");
            System.exit(1);
        }

        Socket socket = new Socket(host, port);

        socket.setSoLinger(false, 0);

        elevators = new Elevator[nElev + 1];

        //not used
        elevators[0] = new Elevator(0);
        //elevators to be started as threads
        for(int i = 1; i < nElev + 1; i++) {
            elevators[i] = new Elevator(i);
            elevators[i].start();
        }

        wr = new PrintWriter(socket.getOutputStream());

        BufferedReader rd = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String commandWithArguments;

        while ((commandWithArguments = rd.readLine()) != null) {
            if(!commandWithArguments.equals(""))
                commandHandler(commandWithArguments);
        }

        socket.close();
    }
}