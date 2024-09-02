package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class RandomServiceTest {

    @Test
    public void testNextInt() {
        RandomService service = new RandomService();
        service.random = Mockito.mock(Random.class);

        when(service.random.nextInt()).thenReturn(23);
        assertEquals(23, service.nextInt());
    }

}