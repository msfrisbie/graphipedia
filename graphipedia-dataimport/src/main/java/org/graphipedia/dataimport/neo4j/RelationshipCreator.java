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
package org.graphipedia.dataimport.neo4j;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.graphipedia.dataimport.ProgressCounter;
import org.graphipedia.dataimport.SimpleStaxParser;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.unsafe.batchinsert.BatchInserter;

import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.DynamicRelationshipType;

public class RelationshipCreator extends SimpleStaxParser {

    private final BatchInserter inserter;
    private final Map<String, Long> inMemoryIndex;

    private final ProgressCounter linkCounter = new ProgressCounter();

    private static String METADATA_REGEX = "\\|\\|(.+?)\\|\\|";

    private static final Pattern METADATA_PATTERN = Pattern.compile(METADATA_REGEX);

    private long nodeId;
    private int badLinkCount = 0;

    public RelationshipCreator(BatchInserter inserter,  Map<String, Long> inMemoryIndex) {
        super(Arrays.asList("t", "l", "h", "r"));
        this.inserter = inserter;
        this.inMemoryIndex = inMemoryIndex;
    }

    public int getLinkCount() {
        return linkCounter.getCount();
    }

    public int getBadLinkCount() {
        return badLinkCount;
    }

    // element is the XML tag

    @Override
    protected void handleElement(String element, String value) {
        // this assumes in-order t -> l detection
        if ("t".equals(element)) {
            nodeId = findNodeId(value);
        } else if ("l".equals(element)||"r".equals(element)||"h".equals(element)) {
            createRelationship(nodeId, value, element);
        }
    }

    private void createRelationship(long nodeId, String link, String linkclass) {
        String title = extractLinkTitle(link);
        Long linkNodeId = findNodeId(title);
        String linkDistance = extractLinkCount(link);
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("dist",linkDistance);
        if (linkNodeId != null) {
            inserter.createRelationship(nodeId,
                                        linkNodeId,
                                        getType(linkclass),
                                        properties);
            linkCounter.increment();
        } else {
            // System.out.println(link);
            // System.out.println(extractLinkTitle(link));
            if (title.length()<2) {
                linkNodeId = findNodeId(title.toUpperCase());
            } else {
                linkNodeId = findNodeId(title.substring(0,1).toUpperCase() + title.substring(1)); 
            }
            if (linkNodeId != null) {
                inserter.createRelationship(nodeId,
                                            linkNodeId,
                                            getType(linkclass),
                                            properties);
                linkCounter.increment();
            } else {
                // if (link.contains("philosophy")) {
                //     System.out.println(link);
                //     String title = extractLinkTitle(link);
                //     System.out.println(title.substring(0,1).toUpperCase() + title.substring(1));
                // }
                badLinkCount++;
            }
        }
    }

    private String extractLinkTitle(String raw) {
        return raw.replaceAll(METADATA_REGEX,"");
    }

    private String extractLinkCount(String raw) {
        Matcher m = METADATA_PATTERN.matcher(raw);
        String s = "";
        while (m.find()) {
            s = m.group(1);    
        }
        return s;
    }

    private Long findNodeId(String title) {
        return inMemoryIndex.get(title);
    }

    private WikiRelationshipType getType(String linkclass) {
        linkclass = linkclass.replaceAll("\\.","");
        if (linkclass=="l") {
            return WikiRelationshipType.Link;
        } else if (linkclass=="r") {
            return WikiRelationshipType.Redirect;
        } else if (linkclass=="h") {
            return WikiRelationshipType.Related;
        }
        System.out.println("This should never execute: [" + linkclass + "]");
        return WikiRelationshipType.Link;
    }
}
