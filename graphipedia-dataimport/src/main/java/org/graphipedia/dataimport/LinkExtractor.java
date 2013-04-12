//
// Copyright (c) 2012 Mirko Nasato
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
// THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
// OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
// ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
// OTHER DEALINGS IN THE SOFTWARE.
//
package org.graphipedia.dataimport;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class LinkExtractor extends SimpleStaxParser {

    private static String REDIRECT_REGEX = "(#REDIRECT)";
    private static String LINK_REGEX = "\\[\\[(.+?)\\]\\]";
    private static String HEADER_REGEX = "={2,5}(.+?)={2,5}";
    private static String RELATED_REGEX = "\\{\\{(.+?)\\}\\}";//"|\\{\\{Related articles(.+?)\\}\\}";
    private static String CLASS_REGEX = "\\|\\|(.+?)////";

    private static String COMBO_REGEX = REDIRECT_REGEX + "|" +
                                        LINK_REGEX + "|" +
                                        HEADER_REGEX + "|" +
                                        RELATED_REGEX;

    // private static final Pattern LINK_PATTERN = Pattern.compile("\\[\\[(.+?)\\]\\]");
    private static final Pattern COMBO_PATTERN = Pattern.compile(COMBO_REGEX);
    private static final Pattern CLASS_PATTERN = Pattern.compile(CLASS_REGEX);

    private final XMLStreamWriter writer;
    private final ProgressCounter pageCounter = new ProgressCounter();

    private String title;
    private String text;

    public LinkExtractor(XMLStreamWriter writer) {
        super(Arrays.asList("page", "title", "text"));
        this.writer = writer;
    }

    public int getPageCount() {
        return pageCounter.getCount();
    }

    @Override
    protected void handleElement(String element, String value) {
        if ("page".equals(element)) {
            if (!title.contains(":")) {
                try {
                    writePage(title, text);
                } catch (XMLStreamException streamException) {
                    throw new RuntimeException(streamException);
                }
            }
            title = null;
            text = null;
        } else if ("title".equals(element)) {
            title = value;
        } else if ("text".equals(element)) {
            text = value;
        }
    }

    private void writePage(String title, String text) throws XMLStreamException {
        writer.writeStartElement("p");
        
        writer.writeStartElement("t");
        writer.writeCharacters(title);
        writer.writeEndElement();
        
        Set<String> links = parseLinks(text);
        links.remove(title);
        
        String linkclass = "";

        for (String link : links) {

            Matcher metadata = CLASS_PATTERN.matcher(link);

            while(metadata.find()) {
                linkclass = metadata.group(1);
                link = link.replace(metadata.group(0),"||");
            }

            // writer.writeStartElement("l");
            writer.writeStartElement(linkclass);
            // if (linkclass.length()>1) {
            //     System.out.println(linkclass);
            // }
            writer.writeCharacters(link);
            writer.writeEndElement();
        }
        
        writer.writeEndElement();

        pageCounter.increment();
    }

    private Set<String> parseLinks(String text) {
        Set<String> links = new HashSet<String>();
        if (text != null) {
            Matcher matcher = COMBO_PATTERN.matcher(text);
            Integer redirect_flag = 0;
            Integer header_counter = 0;
            // Integer link_counter = 0;
            while (matcher.find()) {
                // String link = matcher.group(1);
                // System.out.println(matcher.group(0));
                if (matcher.group(1)!=null) { // redirect
                    redirect_flag = 1;
                } else if (matcher.group(2)!=null) { // link
                    String link = matcher.group(2);
                    String identifier = "l";

                    if (redirect_flag==1) {
                        identifier = "r";
                    }

                    if (!link.contains(":")) {
                        if (link.contains("|")) {
                            link = link.substring(link.lastIndexOf('|') + 1);
                            // links.add(buildLink("r",matcher.group(2),header_counter));
                        }
                        // links.add(link);
                        links.add(buildLink(identifier,link,header_counter));
                    }

                } else if (matcher.group(3)!=null) { // header
                    header_counter++;
                } else { // related link

                    String[] arr = matcher.group(4).split("\\|");
                    Integer i = 0;
                    for(String str: arr) {
                        if (i==0) {

                            // if (str!="Main") { // &&str!="Related articles") {
                            if (str.contains("Main")||str.contains("Related articles")) {
                            } else {
                                // System.out.println("["+str+"]");
                                break;
                            }
                            i = 1;
                        } else {
                            links.add(buildLink("h",str,header_counter));
                        }
                    }
                }
            }
        }
        // links.add("___"+Integer.toString(link_counter)+"___");
        return links;
    }

    private String buildLink(String identifier, String title, Integer counter) {
        return "||" + identifier + "////" + Integer.toString(counter) + "||" + title;
    }
}
