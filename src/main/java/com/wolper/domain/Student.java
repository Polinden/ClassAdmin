package com.wolper.domain;


import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;




public class Student extends Person {


    @NotNull
    @Size(min=1)
    private String form="";

    @NotNull
    private String exform="";


    public String getForm() {return form;}

    public void setForm(String form) {this.form = form;}

    public String getExform() {return exform;}

    public void setExform(String exform) {this.exform = exform;}
}
