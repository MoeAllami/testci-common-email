package org.apache.commons.mail;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EmailTest {

    // Concrete implementation of the abstract Email class for testing
    private class ConcreteEmail extends Email {
        @Override
        public Email setMsg(String msg) throws EmailException {
            this.content = msg;
            this.updateContentType("text/plain");
            return this;
        }
    }

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_NAME = "Test User";
    private static final String TEST_HOST = "localhost";
    private static final String TEST_SUBJECT = "Test Subject";
    private static final String TEST_MESSAGE = "Test Message";
    
    private ConcreteEmail email;

    @Before
    public void setUp() throws Exception {
        email = new ConcreteEmail();
        // Set up basic email properties for testing
        email.setHostName(TEST_HOST);
        email.setSubject(TEST_SUBJECT);
        email.setFrom(TEST_EMAIL);
        email.setSSLOnConnect(false);   // Disable SSL
    }

    @After
    public void tearDown() throws Exception {
        email = null;
    }

    /**
     * Test for addBcc(String... emails) method
     */
    @Test
    public void testAddBccStringArray() throws Exception {
        // Test with a single email address
        email.addBcc(TEST_EMAIL);
        List<InternetAddress> bccList = email.getBccAddresses();
        assertEquals(1, bccList.size());
        assertEquals(TEST_EMAIL, bccList.get(0).getAddress());

        // Test with multiple email addresses
        email = new ConcreteEmail(); // Reset
        String[] emails = {
            "bcc1@example.com",
            "bcc2@example.com",
            "bcc3@example.com"
        };
        email.addBcc(emails);
        bccList = email.getBccAddresses();
        assertEquals(3, bccList.size());
        assertEquals("bcc1@example.com", bccList.get(0).getAddress());
        assertEquals("bcc2@example.com", bccList.get(1).getAddress());
        assertEquals("bcc3@example.com", bccList.get(2).getAddress());
    }

    /**
     * Test for addCc(String email) method
     */
    @Test
    public void testAddCcString() throws Exception {
        // Test with a single email address
        email.addCc(TEST_EMAIL);
        List<InternetAddress> ccList = email.getCcAddresses();
        assertEquals(1, ccList.size());
        assertEquals(TEST_EMAIL, ccList.get(0).getAddress());
        
        // Test with another email
        String secondEmail = "cc2@example.com";
        email.addCc(secondEmail);
        ccList = email.getCcAddresses();
        assertEquals(2, ccList.size());
        assertEquals(secondEmail, ccList.get(1).getAddress());
    }

    /**
     * Test for addHeader(String name, String value) method
     */
    @Test
    public void testAddHeader() throws Exception {
        // Setup header
        String headerName = "X-Priority";
        String headerValue = "1";
        
        // Get access to the headers Map using reflection
        // (since it's protected and we need to verify our test directly)
        java.lang.reflect.Field headersField = Email.class.getDeclaredField("headers");
        headersField.setAccessible(true);
        Map<String, String> headers = (Map<String, String>) headersField.get(email);
        
        // Before adding, map should be empty
        assertTrue(headers.isEmpty());
        
        // Add the header
        email.addHeader(headerName, headerValue);
        
        // Verify header was added to the internal map
        assertEquals(1, headers.size());
        assertEquals(headerValue, headers.get(headerName));
        
        // Now test that it appears in the built message
        email.setMsg(TEST_MESSAGE);
        email.addTo(TEST_EMAIL); // Add recipient to avoid exception
        email.buildMimeMessage();
        MimeMessage msg = email.getMimeMessage();
        
        // Verify header exists in the actual MIME message
        String[] headerValues = msg.getHeader(headerName);
        assertNotNull(headerValues);
        assertEquals(1, headerValues.length);
        assertTrue(headerValues[0].contains(headerValue)); // Contains rather than equals because of possible encoding
    }

    /**
     * Test for addReplyTo(String email, String name) method
     */
    @Test
    public void testAddReplyToStringString() throws Exception {
        email.addReplyTo(TEST_EMAIL, TEST_NAME);
        List<InternetAddress> replyToList = email.getReplyToAddresses();
        assertEquals(1, replyToList.size());
        assertEquals(TEST_EMAIL, replyToList.get(0).getAddress());
        assertEquals(TEST_NAME, replyToList.get(0).getPersonal());
        
        // Test with another reply-to address
        String secondEmail = "reply@example.com";
        String secondName = "Reply User";
        email.addReplyTo(secondEmail, secondName);
        replyToList = email.getReplyToAddresses();
        assertEquals(2, replyToList.size());
        assertEquals(secondEmail, replyToList.get(1).getAddress());
        assertEquals(secondName, replyToList.get(1).getPersonal());
    }

    /**
     * Test for buildMimeMessage() method
     */
    @Test
    public void testBuildMimeMessage() throws Exception {
        email.setMsg(TEST_MESSAGE);
        email.addTo(TEST_EMAIL);
        email.buildMimeMessage();
        
        MimeMessage msg = email.getMimeMessage();
        assertNotNull(msg);
        assertEquals(TEST_SUBJECT, msg.getSubject());
        
        // Test that calling buildMimeMessage() twice throws an exception
        try {
            email.buildMimeMessage();
            fail("Expected IllegalStateException was not thrown");
        } catch (IllegalStateException e) {
            // Expected behavior
        }
    }

    /**
     * Test for buildMimeMessage() with missing required fields
     */
    @Test(expected = EmailException.class)
    public void testBuildMimeMessageNoRecipient() throws Exception {
        // No recipient added
        email.setMsg(TEST_MESSAGE);
        email.buildMimeMessage();
    }

    /**
     * Test for getHostName() method
     */
    @Test
    public void testGetHostName() {
        assertEquals(TEST_HOST, email.getHostName());
        
        // Test when hostname is null but session is set
        ConcreteEmail emailWithSession = new ConcreteEmail();
        Properties props = new Properties();
        props.setProperty(EmailConstants.MAIL_HOST, "session.example.com");
        Session session = Session.getInstance(props);
        emailWithSession.setMailSession(session);
        assertEquals("session.example.com", emailWithSession.getHostName());
        
        // Test when both hostname and session are null
        ConcreteEmail emptyEmail = new ConcreteEmail();
        assertNull(emptyEmail.getHostName());
    }

    /**
     * Test for getMailSession() method
     */
    @Test
    public void testGetMailSession() throws Exception {
        Session session = email.getMailSession();
        assertNotNull(session);
        Properties props = session.getProperties();
        assertEquals(TEST_HOST, props.getProperty(EmailConstants.MAIL_HOST));
    }

    /**
     * Test for getSentDate() method
     */
    @Test
    public void testGetSentDate() throws Exception {
        // Test default (current date)
        Date beforeTest = new Date();
        Date sentDate = email.getSentDate();
        Date afterTest = new Date();
        
        // The sent date should be between beforeTest and afterTest
        assertTrue(sentDate.compareTo(beforeTest) >= 0);
        assertTrue(sentDate.compareTo(afterTest) <= 0);
        
        // Test with explicitly set date
        Date specificDate = new Date(123456789000L); // Specific timestamp
        email.setSentDate(specificDate);
        assertEquals(specificDate.getTime(), email.getSentDate().getTime());
        
        // Ensure we get a new Date instance, not the same object reference
        Date returnedDate = email.getSentDate();
        assertNotSame(specificDate, returnedDate);
    }

    /**
     * Test for getSocketConnectionTimeout() method
     */
    @Test
    public void testGetSocketConnectionTimeout() {
        // Test default value
        assertEquals(EmailConstants.SOCKET_TIMEOUT_MS, email.getSocketConnectionTimeout());
        
        // Test with custom value
        int timeout = 30000;
        email.setSocketConnectionTimeout(timeout);
        assertEquals(timeout, email.getSocketConnectionTimeout());
    }

    /**
     * Test for setFrom(String email) method
     */
    @Test
    public void testSetFromString() throws Exception {
        // Test with a different email than what was set in setUp
        String newEmail = "new@example.com";
        email.setFrom(newEmail);
        InternetAddress fromAddress = email.getFromAddress();
        assertEquals(newEmail, fromAddress.getAddress());
        assertNull(fromAddress.getPersonal());
    }
    
    /**
     * Test for combined email functionality
     */
    @Test
    public void testEmailIntegration() throws Exception {
        // Create and setup a full email
        email = new ConcreteEmail();
        email.setHostName(TEST_HOST);
        email.setSubject(TEST_SUBJECT);
        email.setFrom(TEST_EMAIL, TEST_NAME);
        email.addTo("to@example.com", "To User");
        email.addCc("cc@example.com");
        email.addBcc("bcc@example.com");
        email.addReplyTo("reply@example.com", "Reply User");
        email.addHeader("X-Priority", "1");
        email.setMsg("This is a test email.");
        
        // Build the message
        email.buildMimeMessage();
        MimeMessage msg = email.getMimeMessage();
        
        // Verify all parts are correctly set
        assertEquals(TEST_SUBJECT, msg.getSubject());
        assertEquals(TEST_NAME, ((InternetAddress) msg.getFrom()[0]).getPersonal());
        assertEquals(TEST_EMAIL, ((InternetAddress)msg.getFrom()[0]).getAddress());
        assertEquals("to@example.com", ((InternetAddress)msg.getRecipients(javax.mail.Message.RecipientType.TO)[0]).getAddress());
        assertEquals("cc@example.com", ((InternetAddress)msg.getRecipients(javax.mail.Message.RecipientType.CC)[0]).getAddress());
        assertEquals("bcc@example.com", ((InternetAddress)msg.getRecipients(javax.mail.Message.RecipientType.BCC)[0]).getAddress());
        assertEquals("reply@example.com", ((InternetAddress)msg.getReplyTo()[0]).getAddress());
        assertNotNull(msg.getHeader("X-Priority"));
    }
}
