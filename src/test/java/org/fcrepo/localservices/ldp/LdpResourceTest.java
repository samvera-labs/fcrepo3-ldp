package org.fcrepo.localservices.ldp;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.*;

/**
 * Created by botimer on 11/2/16.
 */
public class LdpResourceTest {

    LdpResource resource;

    @Before
    public void setUp() throws Exception {
        resource = new LdpResource();
    }

    @Test
    public void writeObjProps() throws Exception {
        StringWriter out = new StringWriter();
        resource.writeObjProps(out);
        String output = out.toString();
        assertTrue("Turtle contains PID", output.contains("<demo:5>"));
    }

    @Test
    public void transformObjProps() throws Exception {
    }

    @Test
    public void extractDC() throws Exception {
        Model model = resource.extractDC(resource.demo5());
        String title       = getOnePredicate(model, DC.TITLE);
        String creator     = getOnePredicate(model, DC.CREATOR);
        String subject     = getOnePredicate(model, DC.SUBJECT);
        String description = getOnePredicate(model, DC.DESCRIPTION);
        String publisher   = getOnePredicate(model, DC.PUBLISHER);
        String format      = getOnePredicate(model, DC.FORMAT);
        String identifier  = getOnePredicate(model, DC.IDENTIFIER);

        assertEquals("Coliseum in Rome", title);
        assertEquals("Thornton Staples", creator);
        assertEquals("Architecture, Roman", subject);
        assertEquals("Image of Coliseum in Rome", description);
        assertEquals("University of Virginia Library", publisher);
        assertEquals("image/jpeg", format);
        assertEquals("demo:5", identifier);
    }

    @Test
    public void writeDC() throws Exception {
        StringWriter out = new StringWriter();
        resource.writeDC(out);
        String output = out.toString();

        RDFParser parser = Rio.createParser(RDFFormat.JSONLD);
        Model model = new LinkedHashModel();
        parser.setRDFHandler(new StatementCollector(model));
        parser.parse(new StringReader(output), "info:fedora/demo:5");

        String title = Models.objectString(model.filter(null, DC.TITLE, null)).orElse(null);
        String creator = Models.objectString(model.filter(null, DC.CREATOR, null)).orElse(null);

        assertEquals("Coliseum in Rome", title);
        assertEquals("Thornton Staples", creator);
    }

    @Test
    public void writeRELS() throws Exception {
        StringWriter out = new StringWriter();
        resource.writeRELS(out);
        String output = out.toString();
        assertTrue("Italian prefLabel is present", output.contains("Immagine del Colosseo a Roma"));
    }

    private String getOnePredicate(Model model, IRI pred) {
        return Models.objectString(model.filter(null, pred, null)).orElse(null);
    }

}