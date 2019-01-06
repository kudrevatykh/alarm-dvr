package dvr.impl;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dvr.DVRService;

public class DVRServiceImpl implements DVRService {
    
    private static Logger LOGGER = LoggerFactory.getLogger(DVRServiceImpl.class);

    private ConcurrentHashMap<String, AtomicLong> recordings = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, Process> processes = new ConcurrentHashMap<>();

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    
    private String url = System.getenv("DVR_URL");
    private String to = System.getenv("MAIL_TO");
    private String host = System.getenv("MAIL_HOST");


    public boolean shouldStartRecording(String channel) {
        AtomicLong atomicLong = recordings.computeIfAbsent(channel, (key) -> new AtomicLong());
        long time = System.currentTimeMillis();
        while (true) {
            long old = atomicLong.get();
            if (old > time) {
                return false;
            }
            if (atomicLong.compareAndSet(old, time)) {
                return old == 0;
            }
        }
    }

    @Override
    public void startRecording(String channel) throws IOException {
        if (shouldStartRecording(channel)) {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-v", "16", "-y", "-i",
                     url + channel, "/tmp/" + channel + ".mp4")
                            .redirectError(Redirect.INHERIT).redirectOutput(Redirect.INHERIT);
            processes.put(channel, pb.start());
            executorService.scheduleWithFixedDelay(() -> {
                try {
                    check(channel);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }, 50, 1, TimeUnit.SECONDS);
        }
    }

    private void check(String channel) throws InterruptedException, IOException {
        AtomicLong atomicLong = recordings.get(channel);
        while (true) {
            long expect = atomicLong.get();
            if (expect == 0) {
                throw new IllegalStateException();
            }
            if (expect > System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(60)) {
                return;
            }
            if (expect < System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(60) && atomicLong.compareAndSet(expect, 0)) {
                break;
            }
        }
        LOGGER.info("about to stop ffmpeg");
        Process f = processes.remove(channel);
        f.destroy();
        f.waitFor();
        LOGGER.info("ffmpeg stopped");
        Path p = Paths.get("/tmp", channel+".mp4");
       
        sendEmail(channel);
        Files.delete(p);
        
    }

    void sendEmail(String channel) {
     // Recipient's email ID needs to be mentioned.

        // Sender's email ID needs to be mentioned
        String from = "video@alarm";

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", "25");

        try {
            // Get the Session object.
            Session session = Session.getInstance(props);

           // Create a default MimeMessage object.
           Message message = new MimeMessage(session);

           // Set From: header field of the header.
           message.setFrom(new InternetAddress(from));

           // Set To: header field of the header.
           message.setRecipients(Message.RecipientType.TO,
              InternetAddress.parse(to));

           // Set Subject: header field
           message.setSubject("video form channel "+channel);

           // Create the message part
           BodyPart messageBodyPart = new MimeBodyPart();

           // Now set the actual message
           messageBodyPart.setText("see attachement");

           Multipart multipart = new MimeMultipart();

           multipart.addBodyPart(messageBodyPart);

           messageBodyPart = new MimeBodyPart();
           DataSource source = new FileDataSource("/tmp/"+channel+".mp4");
           messageBodyPart.setDataHandler(new DataHandler(source));
           messageBodyPart.setFileName(channel+".mp4");
           multipart.addBodyPart(messageBodyPart);

           message.setContent(multipart);

           Transport.send(message);

           LOGGER.info("Sent message successfully....");
    
        } catch (Exception e) {
           e.printStackTrace();
        }

    }
}
