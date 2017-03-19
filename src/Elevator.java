import java.util.Iterator;
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
    public void run() {
        //fix internal struggles


        while (true) {
            try {

                if (!queue.isEmpty()) {
                    System.out.println("YES!");
                    //queue is ordered by closest operation from current location that has the same direction
                    Operation temp = queue.getFirst();
                    if(temp == null)
                         continue;
                    if ((int) temp.location == 32000) {
                        sendMove(0);
                        continue;
                    }

                    //set direction
                    this.direction = temp.direction;
                    //move in right direction
                    sendMove(this.direction);

                    //are we on a floor?

                        while (true){
                            //System.out.println(oldFloor);
                            float at = this.location;
                            float onFloor = Math.abs(at - Math.round(at));
                            if(onFloor < 0.040) {
                                System.out.println(onFloor);
                                Operation current = queue.getFirst();
                                if(Math.round(current.location) == Math.round(at)){
                                    sendMove(0);
                                    setDoor(1);
                                    System.out.println(id + ": Doors Open");
                                    sleep(8000);
                                    System.out.println(id + ": Doors Closed");
                                    setDoor(-1);
                                    queue.remove(current);
                                    break;
                                }



                            }
                        }

                    }


                    /*while (true){
                        boolean open = false;
                        //Are we on a floor?
                        if(this.location % 1 == 0){
                            synchronized (this){
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
                    }*/

                else{
                    System.out.println("Hello");
/*                    Operation temp = Handler.bMonitor.checkNearest(this.location);
                    System.out.println("BYE");
                    if(temp != null){
                        setDoor(1);
                        System.out.println(id + ": Doors Open");
                        sleep(200);
                        System.out.println(id + ": Doors Closed");
                        setDoor(-1);
                    }*/
                    }
                }catch(Exception E){
                System.out.println("nope");
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

        if(Math.abs(this.location - location) > 0.03) this.location = location;
        //We're on a floor

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

  public void getSpot(Operation temp){
        if(queue.contains(temp))
            return;

        float tempDist = Math.abs(temp.location - this.location);
        if(queue.isEmpty()) {queue.add(temp); return;}

        int i;
        if(temp.direction == this.direction) {
            for (i = 0; i < queue.size(); i++) {
                Operation iteration = queue.get(i);
                if (iteration.direction != this.direction) {queue.add(i,temp); return;}
                else {
                    float itDist = Math.abs(iteration.location - this.location);
                    if(tempDist < itDist) {queue.add(i,temp); return;}
                }
            }
            queue.addLast(temp);
        }
        else{
            for (i = 0; i < queue.size(); i++) {
                Operation iteration = queue.get(i);
                if (iteration.direction != this.direction) {
                    float itDist = Math.abs(iteration.location - this.location);
                    if(tempDist < itDist) {queue.add(i,temp); return;}
                }
            }
            queue.addLast(temp);
        }
    }

    synchronized public void addOpFirst(Operation temp){
        this.direction = 0;
        queue.clear();
        queue.add(temp);
    }

    synchronized private void sendWhere(){
        String w = ("w " + id);
        Handler.sendCommand(w);
    }


    synchronized private void sendMove(int direction){
        String m = ("m " + id + " " + direction);
        Handler.sendCommand(m);
    }

    synchronized void toggleDoors(int bool){
        String d = ("d " + id + " " + bool);
        Handler.sendCommand(d);
    }

    //1 open - -1 close
    synchronized void setDoor(int c){
        String d = ("d " + this.id + " " + c);
        Handler.sendCommand(d);
    }
}
