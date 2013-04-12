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

import org.graphipedia.dataimport.ProgressCounter;
import org.graphipedia.dataimport.SimpleStaxParser;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.unsafe.batchinsert.BatchInserter;

public class RelationshipCreator extends SimpleStaxParser {

    private final BatchInserter inserter;
    private final Map<String, Long> inMemoryIndex;

    private final ProgressCounter linkCounter = new ProgressCounter();

    private long nodeId;
    private int badLinkCount = 0;

    public RelationshipCreator(BatchInserter inserter,  Map<String, Long> inMemoryIndex) {
        super(Arrays.asList("t", "l"));
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
        System.out.println(element);
        // this assumes in-order t -> l detection
        if ("t".equals(element)) {
            nodeId = findNodeId(value);
        } else if ("l".equals(element)||"r".equals(element)||"h".equals(element)) {
            createRelationship(nodeId, value, element);
        }
    }

    private void createRelationship(long nodeId, String link, String linkclass) {
        Long linkNodeId = findNodeId(link);
        if (linkNodeId != null) {
            if (linkclass=="l") {
                inserter.createRelationship(nodeId, linkNodeId, WikiRelationshipType.Link, null); //, MapUtil.map());
            } else if (linkclass=="r") {
                inserter.createRelationship(nodeId, linkNodeId, WikiRelationshipType.Redirect, null);
            } else if (linkclass=="h") {
                inserter.createRelationship(nodeId, linkNodeId, WikiRelationshipType.Related, null);
            }
            linkCounter.increment();
        } else {
            badLinkCount++;
        }
    }

    // private enum

    private Long findNodeId(String title) {
        return inMemoryIndex.get(title);
    }

}
