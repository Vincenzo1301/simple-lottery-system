package se.hh.simplelotterysystem.email;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Properties;

public class GmailSender {

  public void sendEmail(String recipient, String subject, String body) throws Exception {
    Gmail service = GmailService.getGmailService();
    MimeMessage mimeMessage = createEmail(recipient, "vauricch@gmail.com", subject, body);
    Message message = createMessageWithEmail(mimeMessage);
    service.users().messages().send("me", message).execute();
  }

  private MimeMessage createEmail(String to, String from, String subject, String bodyText)
      throws MessagingException {
    Properties props = new Properties();
    Session session = Session.getDefaultInstance(props, null);

    MimeMessage email = new MimeMessage(session);
    email.setFrom(new InternetAddress(from));
    email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
    email.setSubject(subject);
    email.setText(bodyText);
    return email;
  }

  private Message createMessageWithEmail(MimeMessage email) throws Exception {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    email.writeTo(buffer);
    byte[] bytes = buffer.toByteArray();
    String encodedEmail = Base64.getUrlEncoder().encodeToString(bytes);

    Message message = new Message();
    message.setRaw(encodedEmail);
    return message;
  }
}
