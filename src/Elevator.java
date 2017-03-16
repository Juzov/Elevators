import java.util.LinkedList;

/**
 * Created by Emil on 07/03-17.
 */
public class Elevator extends Thread{
    private int id;
    private float location = 0;
    private int door = -1;
    private int direction = 0;
    LinkedList<Operation> queue = new LinkedList<>();

    public Elevator(int id){
        this.id = id;
    }

    @Override
    public void run(){
        //add all my instructions

        while (true) {
            try {
                if(!queue.isEmpty()){
                    //queue is ordered by closest operation from current location that has the same direction
                    Operation temp = queue.getFirst();
                    //set direction
                    this.direction = temp.direction;
                    //move in right direction
                    sendMove(this.direction);
                    //are we on a floor?
                    while (true){
                        boolean open = false;
                        //Are we on a floor?
                        if(this.location % 1 == 0){
                            if (queue.size() == 1)
                                this.direction = 0;
                            //does anyone want to be picked up from here? (right direction)
                            Operation bCom = Handler.bMonitor.checkFloor(this.direction,this.location);
                            //does anyone want out?
                            if(this.location == queue.getFirst().location) {
                                queue.removeFirst();
                                open = true;
                            }
                            //open anyone wants to be picked up
                            if(bCom != null)    open = true;
                        }

                        if(open){
                            sendMove(0);
                            setDoor(1);
                            System.out.println(id + ": Doors Open");
                            sleep(200);
                            System.out.println(id + ": Doors Closed");
                            setDoor(-1);
                            break;
                        }
                    }
                }
                else{
                    Operation temp = Handler.bMonitor.checkNearest(this.location);
                    if(temp != null){
                        setDoor(1);
                        System.out.println(id + ": Doors Open");
                        sleep(200);
                        System.out.println(id + ": Doors Closed");
                        setDoor(-1);
                    }
                }
            } catch (Exception E) {
                run();
            }
        }
    }

    public int compLocation(float location){

        if      (location > this.location)   return  1;
        else if (location < this.location)   return  -1;
        return 0;

    }


    //set if we're on floor check if anyone wants in the same direction.
    public void setLocation(float location){

        this.location = location;
        //We're on a floor
        if(location % 1 == 0){

        }

    }

    public float getLocation(){
        return location;
    }

    public int getDirection(){
        return direction;
    }


    public boolean isOpen(){
        return door != -1;
    }



    public void addOperation(Operation temp, int i){
        queue.add(i,temp);
    }

    public int getSpot(Operation temp, int i){
        if (queue.get(i) != null){
            int myDirection = queue.get(i).direction;
            float location = queue.get(i).location;
            if(getDirection() == 0)
                return i + 1;
            else if(myDirection == temp.direction){
                int nextDirection = queue.get(i + 1).direction;

                if(temp.direction == -1) {
                    if (temp.location > location) return i;
                    else if(nextDirection != temp.direction) return i + 1;
                }
                else {
                    if (temp.location < location) return i;
                    else if(nextDirection != temp.direction) return i + 1;
                }
            }
        }
        return i;
    }

    public void addOpFirst(Operation temp){
        this.direction = 0;
        queue.clear();
        queue.add(temp);
    }

    synchronized private void sendMove(int direction){
        String m = ("m" + " " + id + " " + direction);
        Handler.sendCommand(m);
    }

    synchronized void toggleDoors(int bool){
        String d = ("d" + " " + id + " " + bool);
        Handler.sendCommand(d);
    }

    //1 open - -1 close
    synchronized void setDoor(int bool){
        String d = ("d" + " " + id + " " + bool);
        Handler.sendCommand(d);
    }
}
