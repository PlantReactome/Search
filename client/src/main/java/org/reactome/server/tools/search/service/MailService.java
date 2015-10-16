package org.reactome.server.tools.search.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

/**
 * Mail Service
 * Created by gsviteri on 15/10/2015.
 */
@Service
public class MailService {


    @Autowired
    private MailSender mailSender; // MailSender interface defines a strategy
    // for sending simple mails

    public void send(String toAddress, String fromAddress, String subject, String msgBody) {

        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setFrom(fromAddress);
        simpleMailMessage.setTo(toAddress);
        simpleMailMessage.setSubject(subject);
        simpleMailMessage.setText(msgBody);

        mailSender.send(simpleMailMessage);
    }

}
