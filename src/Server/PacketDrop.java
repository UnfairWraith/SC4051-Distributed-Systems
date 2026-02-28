package Server;

import java.util.Random;

public class PacketDrop {
    private static double DROP_RATE = 0.3; // 30% packet drop rate
    private Random random;

    public PacketDrop(double DROP_RATE) {    
        this.DROP_RATE = DROP_RATE;   
        this.random = new Random();
    }

    public boolean shouldDropPacket() {
        return random.nextDouble() < DROP_RATE;
    }
}
