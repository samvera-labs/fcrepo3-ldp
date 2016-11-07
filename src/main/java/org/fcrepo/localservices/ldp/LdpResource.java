package org.fcrepo.localservices.ldp;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

/**
 * Created by botimer on 10/28/16.
 */
public class LdpResource {
    protected final Logger logger = LoggerFactory.getLogger(LdpResource.class);

    public static final String MIME_TURTLE = "text/turtle";
    public static final String MIME_JSONLD = "application/ld+json";

    @GET
    @Path("/info")
    @Produces("text/plain")
    public Response getInfo() {
        return Response.ok("This is only a test.").build();
    }

    @GET
    @Path("/properties")
    @Produces({MIME_TURTLE, MIME_JSONLD})
    public Response getObjectProperties() {
        StringWriter out = new StringWriter();
        return writeObjProps(out);
    }

    @GET
    @Path("/dc")
    @Produces({MIME_TURTLE, MIME_JSONLD})
    public Response getDC() {
        StringWriter out = new StringWriter();
        return writeDC(out);
    }

    @GET
    @Path("/relationships")
    @Produces({MIME_TURTLE, MIME_JSONLD})
    public Response getRelationships() {
        StringWriter out = new StringWriter();
        return writeRELS(out);
    }

    protected Response writeObjProps(Writer out) {
        RDFWriter writer = Rio.createWriter(RDFFormat.TURTLE, out);
        writer.set(BasicWriterSettings.PRETTY_PRINT, true);
        writer.startRDF();

        Element root = demo5();
        Model model = transformObjProps(root);
        for (Namespace namespace : model.getNamespaces()) {
            writer.handleNamespace(namespace.getPrefix(), namespace.getName());
        }
        model.forEach(writer::handleStatement);

        writer.endRDF();
        return Response.ok(out.toString()).type(MIME_TURTLE).build();
    }

    protected Response writeDC(Writer out) {
        RDFWriter writer = Rio.createWriter(RDFFormat.JSONLD, out);
        writer.set(BasicWriterSettings.PRETTY_PRINT, true);
        writer.startRDF();

        Element root = demo5();
        Model model = extractDC(root);
        for (Namespace namespace : model.getNamespaces()) {
            writer.handleNamespace(namespace.getPrefix(), namespace.getName());
        }
        model.forEach(writer::handleStatement);

        writer.endRDF();
        return Response.ok(out.toString()).type(MIME_JSONLD).build();
    }

    protected Response writeRELS(Writer out) {
        RDFWriter writer = Rio.createWriter(RDFFormat.TURTLE, out);
        writer.set(BasicWriterSettings.PRETTY_PRINT, true);
        writer.startRDF();

        Element root = demo5();
        Model model = extractRELS(root);
        for (Namespace namespace : model.getNamespaces()) {
            writer.handleNamespace(namespace.getPrefix(), namespace.getName());
        }
        model.forEach(writer::handleStatement);
        writer.endRDF();
        return Response.ok(out.toString()).type(MIME_TURTLE).build();
    }

    protected Model transformObjProps(Element root) {
        ModelBuilder mb = new ModelBuilder();
        mb.setNamespace("foxml", "info:fedora/fedora-system:def/foxml#");
        mb.setNamespace("fedora-model", "info:fedora/fedora-system:def/model#");

        String pid = root.getAttribute("PID");
        String version = root.getAttribute("VERSION");
        String schema = root.getAttribute("xsi:schemaLocation");

        mb.subject(pid);

        Element objProps = (Element) root.getElementsByTagName("foxml:objectProperties").item(0);
        NodeList props = objProps.getElementsByTagName("foxml:property");
        for (int i = 0; i < props.getLength(); i++) {
            Element prop = (Element) props.item(i);
            String predicate = prop.getAttribute("NAME");
            String object = prop.getAttribute("VALUE");
            mb.add(predicate, object);
        }

        return mb.build();
    }

    protected Model extractDC(Element root) {
        ModelBuilder mb = new ModelBuilder();

        mb.setNamespace("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/");
        mb.setNamespace(DC.NS);

        String pid = root.getAttribute("PID");
        mb.subject(pid);

        NodeList streams = root.getElementsByTagName("foxml:datastream");
        for (int i = 0; i < streams.getLength(); i++) {
            Element el = (Element) streams.item(i);
            if ("DC".equals(el.getAttribute("ID"))) {
                Element dcRoot = (Element) el.getElementsByTagName("oai_dc:dc").item(0);
                addDC(mb, dcRoot);
            }
        }
        return mb.build();
    }

    protected void addDC(ModelBuilder mb, Element dcRoot) {
        NodeList nodes = dcRoot.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) node;
                mb.add(el.getTagName(), el.getTextContent());
            }
        }
    }

    protected Model extractRELS(Element root) {
        Model model = new LinkedHashModel();

        NodeList streams = root.getElementsByTagName("foxml:datastream");
        for (int i = 0; i < streams.getLength(); i++) {
            Element el = (Element) streams.item(i);
            if ("RELS-EXT".equals(el.getAttribute("ID"))) {
                Element relsRoot = (Element) el.getElementsByTagName("rdf:RDF").item(0);
                Model rels = parseRELS(relsRoot);
                rels.getNamespaces().forEach(model::setNamespace);
                model.addAll(rels);
            }
        }
        return model;
    }

    protected Model parseRELS(Element relsRoot) {
        Model model = new LinkedHashModel();
        relsRoot = (Element) relsRoot.cloneNode(true);
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            StreamResult result = new StreamResult(new StringWriter());
//            if ("".equals(relsRoot.getAttribute("xmlns:rdf"))) {
//                relsRoot.setAttribute("xmlns:rdf", RDF.NAMESPACE);
//            }
//            if ("".equals(relsRoot.getAttribute("xmlns"))) {
//                relsRoot.setAttribute("xmlns", "info:fedora/fedora-system:FedoraRELSExt-1.0");
//            }
            relsRoot.setAttribute("xmlns", RDF.NAMESPACE);
            DOMSource source = new DOMSource(relsRoot);
            transformer.transform(source, result);
            String xmlString = result.getWriter().toString();
            xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<rdf:RDF xmlns=\"info:fedora/fedora-system:FedoraRELSExt-1.0#\"\n" +
                    "         xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
                    "         xmlns:fedora-model=\"info:fedora/fedora-system:def/model#\">" +
                    "<rdf:Description rdf:about=\"info:fedora/demo:5\"><fedora-model:hasModel rdf:resource=\"info:fedora/demo:UVA_STD_IMAGE_1\"/><prefLabel xml:lang=\"it\">Immagine del Colosseo a Roma</prefLabel></rdf:Description></rdf:RDF>";

            model = Rio.parse(new StringReader(xmlString), "info:fedora/demo:5", RDFFormat.RDFXML);
        } catch (TransformerException e) {
            logger.info("", e);
        } catch (IOException e) {
            logger.info("", e);
        }
        return model;
    }

    protected Element demo5() {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("demo-5.xml");
        try {
            Document doc = builder().parse(stream);
            return doc.getDocumentElement();
        } catch (Exception e) {
            logger.info("Exception parsing demo-5.xml:", e);
        }
        return null;
    }

    protected DocumentBuilder builder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder;
    }

}
