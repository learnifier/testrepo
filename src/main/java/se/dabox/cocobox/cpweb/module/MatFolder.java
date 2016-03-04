/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jerker Klang (jerker.klang@learnifier.com)
 */
public class MatFolder {

    private Long id;
    private String name;
    private List<MatFolder> folders = new ArrayList<>();

    public MatFolder(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<MatFolder> getFolders() {
        return folders;
    }

    @Override
    public String toString() {
        return "MatFolder{" + "id=" + id + ", name=" + name + ", children=" + folders + '}';
    }

}
