package com.nhom02.streamwire.model;

import java.io.Serializable;

public class Category implements Serializable {
    private String id;
    private String name;
    private boolean isSelected;

    public Category() {
    }

    public Category(String id, String name) {
        this.id = id;
        this.name = name;
        this.isSelected = false;
    }

    public Category(String id, String name, boolean isSelected) {
        this.id = id;
        this.name = name;
        this.isSelected = isSelected;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}

