import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;


class CIServerTest {

    private static Add res;


    

    @Test
    public void posTest(){
        res = new Add(1, 1000);
        
        assertEquals(res.getRes(),3);
    }

    @Test
    public void negTest(){
        res = new Add(1, 1000);
        assertNotEquals(res.getRes(), 4);
    }



}