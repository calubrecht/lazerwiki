package us.calubrecht.lazerwiki.service;

import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class RandomService {

    Random random = new Random();

    public int nextInt() {
        return random.nextInt();
    }

    public String randomKey() {
        return "ABC FFF";
    }
}
