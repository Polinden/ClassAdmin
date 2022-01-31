package com.wolper.domain;


import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class Person {

    @NotNull
    @Size (min=1, max=50)
    private String fst_name="";

    @NotNull
    @Size (min=1, max=50)
    private String scd_name="";

    @NotNull
    @Size (min=1, max=50)
    private String thd_name="";

    @NotNull
    @Size (min=1, max=20)
    private String login="";

    @NotNull
    @Size (min=1, max=20)
    private String passwd="";

    @NotNull
    private String exlogin ="";



    public String getFst_name() {return fst_name;}

    public void setFst_name(String fst_name) {this.fst_name = fst_name;}

    public String getScd_name() {return scd_name;}

    public void setScd_name(String scd_name) {this.scd_name = scd_name;}

    public String getThd_name() {return thd_name;}

    public void setThd_name(String thd_name) {this.thd_name = thd_name;}

    public String getLogin() {return login;}

    public void setLogin(String login) {this.login = login;}

    public String getPasswd() {return passwd;}

    public void setPasswd(String passwd) {this.passwd = passwd;}

    public String getExlogin() {return exlogin;}

    public void setExlogin(String exlogin) {this.exlogin = exlogin;}


    public Person(String fst_name, String scd_name, String thd_name, String login) {
        this.fst_name = fst_name;
        this.scd_name = scd_name;
        this.thd_name = thd_name;
        this.login = login;
    }


    public Person() {}

    @Override
    public String toString() {
        return "Фамилия='" + fst_name + '\'' +
                ", Имя='" + scd_name + '\'' +
                ", Отество='" + thd_name + '\'' +
                ", login='" + login;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Person person = (Person) o;

        return login != null ? login.equals(person.login) : person.login == null;
    }

    @Override
    public int hashCode() {
        return login != null ? login.hashCode() : 0;
    }
}
