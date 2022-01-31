package com.wolper.domain;

import com.wolper.services.LogicEception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.ResultSet;
import java.sql.SQLException;



//getting a register record from rawset
//for farther processing into the schedule page
public class SchWrapper {



    static Logger logger = LoggerFactory.getLogger(SchWrapper.class);


    String f_name;
    String sn_name;
    String td_name;
    String subject;
    String form ="";
    String login;



    public static SchWrapper wrapperResultSet(ResultSet rs) {
        try {
            SchWrapper sw = new SchWrapper();
            sw.f_name =rs.getString(1);
            sw.sn_name =rs.getString(2);
            sw.td_name =rs.getString(3);
            sw.subject=rs.getString(4);
            sw.login=rs.getString(6);
            if (rs.getString(5) != null)  sw.form =rs.getString(5);
            return sw;

        } catch (SQLException e) {
            logger.error("Ошибка чтения сприска расписаний в БД!", e);
            throw new LogicEception("Ошибка чтения сприска расписаний в БД", e.getMessage());
        }

    }



    public String getF_name() {return f_name;}

    public String getSn_name() {return sn_name;}

    public String getTd_name() {return td_name;}

    public String getSubject() {return subject;}

    public String getForm() {return form;}

    public String getLogin() {return login;}

}
