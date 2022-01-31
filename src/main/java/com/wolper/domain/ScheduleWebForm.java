package com.wolper.domain;


import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

public class ScheduleWebForm {


    @NotNull
    @Size(min=1)
    private String teacher ="";

    @NotNull
    @Size(min=1)
    private String subject="";

    private List<Subject> formes;

    private List<String> newly_checked;

    private String login="";



    public ScheduleWebForm() {
        setFormes(new ArrayList<>());}

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public List<Subject> getFormes() {return formes;}

    public void setFormes(List<Subject> formes) {this.formes = formes;}

    public List<String> getNewly_checked() {
        return newly_checked;
    }

    public void setNewly_checked(List<String> newly_checked) {
        this.newly_checked = newly_checked;
    }

    public String getLogin() {return login;}

    public void setLogin(String login) {this.login = login;}
}
