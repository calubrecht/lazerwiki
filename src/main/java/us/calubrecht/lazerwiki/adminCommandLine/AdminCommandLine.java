package us.calubrecht.lazerwiki.adminCommandLine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.LazerWikiAuthenticationManager;
import us.calubrecht.lazerwiki.service.UserService;

import java.io.Console;
import java.util.List;
import java.util.Scanner;

@Component
public class AdminCommandLine implements CommandLineRunner {
    private static Logger logger = LoggerFactory
            .getLogger(AdminCommandLine.class);

    @Autowired
    UserService userService;

    @Override
    public void run(String... args) throws Exception {
        if (args.length > 0) {
            logger.info("EXECUTING : command line runner");
            logger.info(args[0]);

            if ("-createAdminUser".equals(args[0])) {
                logger.info("creating admin user");
                System.out.println("Admin username: ");
                try (Scanner scanner = new Scanner(System.in)) {
                    String user = scanner.nextLine();
                    String password = "pass";
                    String confirm = "confirm";
                    while (!password.equals(confirm)) {
                        System.out.println("Password: ");
                        password = readPassword(scanner);
                        System.out.println("Confirm Password: ");
                        confirm = readPassword(scanner);
                        if (!password.equals(confirm)) {
                            System.out.println("Passwords are not equal. Try again");
                        }
                    }
                    if (userService.getUser(user) != null) {
                        System.out.println("User already exists");
                    }
                    else {
                        userService.addUser(user, password, List.of(LazerWikiAuthenticationManager.USER, LazerWikiAuthenticationManager.ADMIN));
                        System.out.println("Admin user " + user + " created");
                    }
                }
            }
        }
    }

    public String readPassword(Scanner scanner) {
        Console c = System.console();
        if (c == null) {
            // If running from IDE, console may not be available. Password input will not be masked.
            return scanner.nextLine();
        }
        return new String(System.console().readPassword());
    }
}
