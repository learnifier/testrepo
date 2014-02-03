/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.excel;

import java.io.InputStream;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class RosterReaderTest {

    public RosterReaderTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testReadInvalidFile() {

        RosterReader rr = new RosterReader();
        try  {
            rr.readContacts(getTestStream("invalid_format.xlsx"));
            Assert.fail("Should throw exception");
        } catch(RosterFormatException ex) {
            //This is ok
        }
        
    }

    @Test
    public void testReadTwoContacts() {

        RosterReader rr = new RosterReader();
        List<Contact> contacts = rr.readContacts(getTestStream("twocontacts.xlsx"));

        Assert.assertNotNull("contacts is null", contacts);
        
        Assert.assertEquals("Expected 2 contacts", 2, contacts.size());
        Assert.assertEquals("Expected 0 errors", 0, rr.getErrors().size());
    }

    @Test
    public void testReadTwoMissingFirstname() {

        RosterReader rr = new RosterReader();
        List<Contact> contacts = rr.readContacts(getTestStream("twocontacts_missingfname.xlsx"));

        Assert.assertNotNull("contacts is null", contacts);

        Assert.assertEquals("Expected 1 contacts", 1, contacts.size());
        Assert.assertEquals("Expected 1 errors", 1, rr.getErrors().size());
    }

    @Test
    public void testReadTwoMissingLastname() {

        RosterReader rr = new RosterReader();
        List<Contact> contacts = rr.readContacts(getTestStream("twocontacts_missinglname.xlsx"));

        Assert.assertNotNull("contacts is null", contacts);

        Assert.assertEquals("Expected 1 contacts", 1, contacts.size());
        Assert.assertEquals("Expected 1 errors", 1, rr.getErrors().size());
    }

    @Test
    public void testReadTwoMissingEmail() {

        RosterReader rr = new RosterReader();
        List<Contact> contacts = rr.readContacts(getTestStream("twocontacts_missingemail.xlsx"));

        Assert.assertNotNull("contacts is null", contacts);

        Assert.assertEquals("Expected 1 contacts", 1, contacts.size());
        Assert.assertEquals("Expected 1 errors", 1, rr.getErrors().size());
    }

    @Test
    public void testReadTwoInvalidEmail() {

        RosterReader rr = new RosterReader();
        List<Contact> contacts = rr.readContacts(getTestStream("twocontacts_invalidemail.xlsx"));

        Assert.assertNotNull("contacts is null", contacts);

        Assert.assertEquals("Expected 1 contacts", 1, contacts.size());
        Assert.assertEquals("Expected 1 errors", 1, rr.getErrors().size());
    }

    @Test
    public void testReadTwoContactsEmptyLines() {

        RosterReader rr = new RosterReader();

        List<Contact> contacts = rr.readContacts(getTestStream("twocontacts_emptylines.xlsx"));

        Assert.assertNotNull("contacts is null", contacts);

        Assert.assertEquals("Expected 2 contacts", 2, contacts.size());
        Assert.assertEquals("Expected 0 errors", 0, rr.getErrors().size());
    }

    private static InputStream getTestStream(String fname) {
        InputStream is =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(fname);

        if (is == null) {
            throw new IllegalStateException("Unable to locate test resource: "+fname);
        }

        return is;
    }
}
