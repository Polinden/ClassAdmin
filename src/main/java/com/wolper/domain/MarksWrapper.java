package com.wolper.domain;

import com.wolper.services.LogicEception;
import java.sql.ResultSet;
import java.sql.SQLException;



//getting a register record from rawset
//for farther processing into the content of xls table
public class MarksWrapper {
    String work;
    String name;
    String subject;
    Integer mark;

    public MarksWrapper(String work, String name, String subject, Integer mark) {
        this.work = work;
        this.name = name;
        this.subject = subject;
        this.mark = mark;
    }


    //factory
    static public MarksWrapper wrapperResultSet(ResultSet rs) {
        try {
            String work = rs.getString(4);
            if (work == null || work.isEmpty()) return null;
            String name = rs.getString(1) + " " + rs.getString(2) + ".";
            return new MarksWrapper(work, name, rs.getString(5), rs.getInt(3));
        } catch (SQLException e) {
            throw new LogicEception("Ошибка чтения сприска расписаний в БД", e.getMessage());
        }
    }

    public String getWork() {
        return work;
    }

    public String getName() {
        return name;
    }

    public String getSubject() {
        return subject;
    }

    public Integer getMark() {
        return mark;
    }

}