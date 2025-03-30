package us.calubrecht.lazerwiki.service;

import org.springframework.stereotype.Service;

@Service
public class TemplateService {


    public String getVerifyEmailTemplate(String site, String email, String userName, String randomKey) {
        return """
                <html>
                <head>
                <style>
                  div.body {background:lightgray; padding:5em;}
                  div.main {background:white; padding:1em; max-width:800px; border-color:black; border-style:solid; border-width:3px}
                  div.centered {text-align: center; font-weight: 900; font-size: larger}
                </style>
                </head>
                <body>
                <div class="body">
                <div class="main">
                <p>Hi %s,</p>
                <p>Someone at %s has requested to connect the email address: %s with the user account %s.</p>
                <p>If this was not you, you do not need to take any action.</p>
                <p>If you wish to complete this connection, enter the token below where indicated in the app. This token
                will expire in 15 minutes: </p>
                <div class="centered">%s</div>
                </div></div>
                </body>
                </html>
                """.formatted(userName, site, email, userName, randomKey);
    }
}
