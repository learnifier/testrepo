/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public final class OrgMaterialTypes {

    private static final List<String> types = createTypesList();
    private static final Set<String> typeSet = new HashSet<String>(types);

    private static List<String> createTypesList() {
        ArrayList<String> list = new ArrayList<String>();
        list.add("file");
        //Removed some type definitions
//        list.add("video");
//        list.add("audio");
//        list.add("apps");
//        list.add("images");
//        list.add("link");

        list.trimToSize();

        return list;
    }

    public static List<String> getTypes() {
        return types;
    }

    public static boolean isType(String str) {
        return typeSet.contains(str);
    }
}
