package com.wolper.domain;


import com.wolper.services.LogicEception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

//getting a register record from rawset
//for farther processing into the school top marks xls report
public class STopWrapper {

String form;
String work;
String name;
Double mark;



    static Logger logger = LoggerFactory.getLogger(STopWrapper.class);


    public static STopWrapper wrapperResultSet(ResultSet rs) {
        try {
            STopWrapper st = new STopWrapper();
            st.form =rs.getString(1);
            st.work =rs.getString(2);
            st.name=rs.getString(3);
            st.mark=rs.getDouble(4);
            return st;

        } catch (SQLException e) {
            logger.error("Ошибка чтения списка всех отметок в БД!", e);
            throw new LogicEception("Ошибка чтения списка всех отметок в БД!", e.getMessage());
        }
    }


    public String getForm() {return form;}

    public String getWork() {return work;}

    public String getName() {return name;}

    public Double getMark() {return mark;}
}
