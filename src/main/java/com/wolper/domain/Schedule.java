package com.wolper.domain;


import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Schedule  implements Comparable {


    private Person teacher;

    //structure
    //subject-+ form1
    //        |
    //        + form2
    //        |
    //        + form3...
    private Map<String, ? extends Set<String>> subj_forms_map;

    private String teachersname;



    //get and set
    public Person getTeacher() {return teacher;}

    public void setTeacher(Person teacher) {this.teacher = teacher;}

    public Map<String, ? extends Set<String>> getSubj_forms_map() {
        return this.subj_forms_map;
    }

    public void setSubj_forms_map(Map<String, ? extends Set<String>> subj_forms_map) {this.subj_forms_map = subj_forms_map;}

    public String getTeachersname() {return teacher.getFst_name()+
                                        " "+ teacher.getScd_name()+
                                            " "+ teacher.getThd_name();}

    public void setTeachersname(String teachersname) {this.teachersname = teachersname;}


    //constructors
    public Schedule(){}

    public Schedule(Person teacher, Map<String, ? extends Set<String>> subj_forms_map) {
        this.teacher = teacher;
        this.subj_forms_map=subj_forms_map;
    }

    //for comparing
    Locale locale = new Locale.Builder().setLanguage("uk").setRegion("UA").build();
    Collator cmp = Collator.getInstance(locale);

    @Override
    public int compareTo(Object o) {
        if (!(o instanceof Schedule)) throw new IllegalArgumentException();
        if (equals(o)) return 0;
        Schedule toCompare = (Schedule) o;
        int i = cmp.compare(this.teacher.getFst_name(), toCompare.getTeacher().getFst_name());
        if (i!=0) return i;
        i = cmp.compare(this.teacher.getScd_name(), toCompare.getTeacher().getScd_name());
        if (i!=0) return i;
        return cmp.compare(this.teacher.getThd_name(), toCompare.getTeacher().getThd_name());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Schedule schedule = (Schedule) o;
        return teacher != null ? teacher.equals(schedule.teacher) : schedule.teacher == null;
    }

    @Override
    public int hashCode() {
        return teacher != null ? teacher.hashCode() : 0;
    }


    @Override
    public String toString() {
        return "Schedule{" +
                "teacher=" + teacher.toString() +
                ", subj_forms_map=" + subj_forms_map +
                ", teachersname='" + teachersname + '\'' +
                '}';
    }
}
