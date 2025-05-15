/*
 * Copyright 2018, University Library Rostock
 * 
 * This file is part of the program "Signaturetikettendruck (Signed)".
 * https://github.com/ubrostock/signaturetikettendruck
 * 
 * "Signaturetikettendruck" is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * "Signaturetikettendruck" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.uni.rostock.ub.tools.signed.util;

import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import de.uni.rostock.ub.tools.signed.model.ShelfmarkObject;

/**
 * Catalog service, that retrieves shelfmark and location from catalog via SRU
 * 
 * @author Robert Stephan, Rostock University Library
 *
 */
public class SignedCatalogService {
    private final SignedConfigService config;

    public SignedCatalogService(SignedConfigService config) {
        this.config = config;
    }

    public ShelfmarkObject retrieveShelfmarkObject(String barcode) throws Exception {
        ShelfmarkObject resultShelfmark = null;
        if (config.getConfig().getProperty("signed.folio.url") != null) {
            resultShelfmark = retrieveShelfmarkObjectFromFOLIOWithBarcode(barcode);
        } else if (config.getConfig().getProperty("signed.sru.url") != null) {
            resultShelfmark = retrieveShelfmarkObjectFromOPACWithBarcode(barcode);
        }
        if (resultShelfmark == null) {
            throw new NullPointerException("Das Quellsystem ist falsch konfiguriert.");
        }
        return resultShelfmark;
    }

    private ShelfmarkObject retrieveShelfmarkObjectFromFOLIOWithBarcode(String barcode) throws Exception {
        ShelfmarkObject result = new ShelfmarkObject();
        return result;
    }

    private ShelfmarkObject retrieveShelfmarkObjectFromOPACWithBarcode(String barcode) throws Exception {
        ShelfmarkObject result = new ShelfmarkObject();
        URL url = new URL(config.getConfig().getProperty("signed.sru.url").replace("${barcode}", barcode));
        System.out.println("KatalogSuche: " + url.toString());
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        DocumentBuilder builder = builderFactory.newDocumentBuilder();

        Document doc = null;
        InputStream is = null;
        try {
            is = url.openStream();
            doc = builder.parse(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
        if (doc == null) {
            throw new NullPointerException("Katalogisat konnte nicht gelesen werden");
        }

        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {

            @SuppressWarnings("rawtypes")
            @Override
            public Iterator getPrefixes(String namespaceURI) {
                return null;
            }

            @Override
            public String getPrefix(String namespaceURI) {
                return null;
            }

            @Override
            public String getNamespaceURI(String prefix) {
                if (prefix.equals("pica"))
                    return "info:srw/schema/5/picaXML-v1.0";
                if (prefix.equals("srw"))
                    return "http://www.loc.gov/zing/srw/";
                return null;
            }
        });
        XPathExpression expr = xpath
                .compile(config.getConfig().getProperty("signed.xpath.object").replace("${barcode}", barcode));
        // NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        Node node = (Node) expr.evaluate(doc, XPathConstants.NODE);

        if (node != null) {
            Node n2 = null;
            XPathExpression expr2 = null;
            ;
            expr2 = xpath.compile(config.getConfig().getProperty("signed.xpath.object.signature"));
            n2 = (Node) expr2.evaluate(node, XPathConstants.NODE);
            if (n2 != null) {
                result.setSignature(n2.getTextContent());
            }

            expr2 = xpath.compile(config.getConfig().getProperty("signed.xpath.object.location"));
            n2 = (Node) expr2.evaluate(node, XPathConstants.NODE);
            if (n2 != null) {
                result.setLocation(n2.getTextContent());
            }
            expr2 = xpath.compile(config.getConfig().getProperty("signed.xpath.object.loanindicator"));
            n2 = (Node) expr2.evaluate(node, XPathConstants.NODE);
            if (n2 != null) {
                result.setLoanindicator(n2.getTextContent());
            }
        } else {
            System.err.println("Keine Signatur gefunden !!!");
        }
        return result;
    }

}
