/*
 Copyright (c) 2014, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Stephen Gold's name may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL STEPHEN GOLD BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3maze.model;

import java.util.logging.Logger;

/**
 * A collectible item. Each item has a named type.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class Item
        implements Comparable {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(Item.class.getName());
    // *************************************************************************
    // fields
    /**
     * name of this item's type
     */
    private String typeName;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an item with a named type.
     *
     * @param typeName (not null, not empty)
     */
    Item(String typeName) {
        assert typeName != null;
        assert typeName.length() > 0 : typeName;

        this.typeName = typeName;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read the type name of this item.
     *
     * @return (not null, not empty)
     */
    public String getTypeName() {
        assert typeName != null;
        assert typeName.length() > 0 : typeName;

        return typeName;
    }
    // *************************************************************************
    // Comparable methods

    /**
     * Compare with another item.
     *
     * @param object (instance of Item)
     * @return 0 if the items have the same type name
     */
    @Override
    public int compareTo(Object object) {
        Item otherItem = (Item) object;
        String otherName = otherItem.typeName;
        return typeName.compareTo(otherName);
    }
}