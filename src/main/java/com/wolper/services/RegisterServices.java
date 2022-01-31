package com.wolper.services;

import com.wolper.domain.Person;
import com.wolper.domain.Schedule;
import com.wolper.domain.Subject;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;


public interface RegisterServices extends InitializingBean {
    //self explaining method: the name and sql string tell everything
    List<String> getAllSubjects();

    //self explaining method: the name and sql string tell everything
    List<Person> getAllPrepods();

    //self explaining method: the name and sql string tell everything
    List<Person> getAllStudents();

    //self explaining method: the name and sql string tell everything
    List<String> getClasses();

    //get schedules for all teachers
    //we get Map<Person, <Map<String, Set<String>> (Person - for teacher, String -for subjects, Set<String> - for forms)
    Set<Schedule> getSchedules();

    //we get all available subject for combinations teacher+subject
    List<Subject> getFreeSubjects(String subject, String prepod);

    //we mark items in the list of all available subject as checked if a subject is selected for a teacher
    void markSelectedSubjectsInSubgect4PrepodList(String prepod_login, String subject, List<Subject> formes);

    //self explaining method: the name and sql string tell everything
    void setPrepodOrStudent(boolean update, Person person);

    //self explaining method: the name and sql string tell everything
    void delPrepodOrStudent(Person person);

    //self explaining method: the name and sql string tell everything
    void editSubjectsList(boolean del, boolean update, String subject, String exsubject);

    //self explaining method: the name and sql string tell everything
    void insertSchedule(String login, String subject);

    //self explaining method: the name and sql string tell everything
    void updateSchedule(String login, String subject, List<String> present);

    //self explaining method: the name and sql string tell everything
    void deleteSchedule(String login, String subject);

    //self explaining method: the name and sql string tell everything
    void deleteAllForm(String form);

    //self explaining method: the name and sql string tell everything
    void changeAllForm(String form1, String form2);

    //self explaining method: the name and sql string tell everything
    List<Double> getPersantageMarks(String from, String subj, boolean current);

    //self explaining method: the name and sql string tell everything
    Map<String, Double> getAverageMarks(String from, String subj, boolean current);

    //self explaining method: the name and sql string tell everything
    Map<String, List<String>> getFormsAndSubjects(boolean for_sumarry);

    //self explaining method: the name and sql string tell everything
    Map<String, Map<String, Integer>> getGetSummaryMarks(String form, String subject);

    //self explaining method: the name and sql string tell everything
    Map<String, Double> getGetTopMarks(boolean current);

    //self explaining method: the name and sql string tell everything
    public Map<String, Map<String, Map<String, Integer>>> getGetSummaryMarks(String form);

    //self explaining method: the name and sql string tell everything
    public Map<String, Map <String, Map <String, Double>>> getGetAllSummaryMarks();
}
