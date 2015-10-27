package uk.ac.ebi.reactome.solr.indexer.util;

import org.apache.log4j.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.util.Properties;

/**
 * Mail Utility class.
 *
 * @author Guilherme S. Viteri <gviteri@ebi.ac.uk>
 */
public class MailUtil {

    private static final Logger logger = Logger.getLogger(MailUtil.class);

    private static Properties properties;

    private static final String FROM = "reactome@reactome.org";

    private static final String TO = "reactome-default@reactome.org";

    static {
        loadProperties();
    }

    public static void sendMail(String subject, String text) {
        sendMail(FROM, properties.getProperty("mail.dest", TO), subject, text);
    }

    public static void sendMail(String from, String subject, String text) {
        sendMail(from, properties.getProperty("mail.dest", TO), subject, text);
    }

    public static void sendMail(String from, String to, String subject, String text) {

        // Get the default Session object.
        Session session = Session.getDefaultInstance(properties);

        try {
            // Create a default MimeMessage object.
            MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            message.setFrom(new InternetAddress(from));

            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

            // Set Subject: header field
            message.setSubject(subject);

            // Now set the actual message
            message.setText(text);

            // Send message
            Transport.send(message);

        } catch (MessagingException e) {
            logger.error("Error sending notification message");
        }
    }

    private static void loadProperties() {
        if (properties == null) {
            properties = new Properties();
        }

        try {
            final InputStream stream = MailUtil.class.getResourceAsStream("/mail.properties");
            properties.load(stream);

            stream.close();
        } catch (Exception e) {
            logger.error("Could not read mail.properties. Email will not work properly.");
        }

    }

}

