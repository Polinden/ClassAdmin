package com.wolper.services;


import com.wolper.domain.*;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;




public class RegisterService extends NamedParameterJdbcDaoSupport implements RegisterServices {



    //properties to be setup for being consistent with other modules
    //names of topic and works, which are used by users of Register module
    private String TOPIC_WORK;
    private List<String> WORKS;

    Logger logger = LoggerFactory.getLogger(RegisterService.class);



    /**
     * Get subjects list (all subjects in a school)
     * Returns the list of Strings
     */
    @Override
    @Transactional(readOnly = true)
    public List<String> getAllSubjects() {
        String ifQuery = "select subject from subject_list order by subject";

        List<String> results = new ArrayList();
        try {
            results = getJdbcTemplate().query(ifQuery, (rs, row) -> {
                return new String(rs.getString(1));
            });
        } catch (DataAccessException e) {
            logger.error("Ошибка в базе данных - " + e);
            throw new LogicEception("Ошибка чтения БД", e.getMessage());
        }

        if (results == null) return new ArrayList<>();
        return results;
    }


    /**
     * Get teachers list (all teachers in a school)
     * Returns the list of Person objects
     */
    @Override
    @Transactional(readOnly = true)
    public List<Person> getAllPrepods() {
        String ifQuery = "select users.login, password, first_name, second_name, third_name from users " +
                "inner join authorities on authorities.login=users.login where authorities.authority='prepod' order by first_name";

        List<Person> results = new ArrayList();
        try {
            results = getJdbcTemplate().query(ifQuery, (rs, row) -> {
                Person pr = new Person();
                pr.setLogin(rs.getString(1));
                pr.setPasswd(rs.getString(2));
                pr.setFst_name(rs.getString(3));
                pr.setScd_name(rs.getString(4));
                pr.setThd_name(rs.getString(5));
                return pr;
            });
        } catch (DataAccessException e) {
            logger.error("Ошибка в базе данных - " + e);
            throw new LogicEception("Ошибка чтения БД", e.getMessage());
        }

        if (results == null) return new ArrayList<>();
        return results;
    }


    /**
     * Get students list (all students in a school)
     * Returns the list of Person objects
     */
    @Override
    @Transactional(readOnly = true)
    public List<Person> getAllStudents() {
        String ifQuery = "select class_and_students.class_name, users.login, password, first_name, second_name, third_name " +
                "from users inner join authorities on authorities.login=users.login inner join class_and_students " +
                "on class_and_students.students_login=users.login where authorities.authority='stud' order by class_and_students.class_name, first_name;";

        List<Person> results = new ArrayList();
        try {
            results = getJdbcTemplate().query(ifQuery, (rs, row) -> {
                Student pr = new Student();
                pr.setLogin(rs.getString(2));
                pr.setPasswd(rs.getString(3));
                pr.setFst_name(rs.getString(4));
                pr.setScd_name(rs.getString(5));
                pr.setThd_name(rs.getString(6));
                pr.setForm(rs.getString(1));
                return pr;
            });
        } catch (DataAccessException e) {
            logger.error("Ошибка в базе данных - " + e);
            throw new LogicEception("Ошибка чтения БД", e.getMessage());
        }

        if (results == null) return new ArrayList<>();
        return results;
    }


    /**
     * Get forms list (all froms in a school)
     * Form names are hardcoded in the database for the reason of keeping standards
     */
    @Override
    @Transactional(readOnly = true)
    public List<String> getClasses() {
        String ifQuery = "select class_name from class order by class_name;";

        List<String> results = new ArrayList();
        try {
            results = getJdbcTemplate().query(ifQuery, (rs, row) -> {
                return new String(rs.getString(1));
            });
        } catch (DataAccessException e) {
            logger.error("Ошибка в базе данных - " + e);
            throw new LogicEception("Ошибка чтения БД", e.getMessage());
        }

        if (results == null) return new ArrayList<>();
        return results;
    }


    /**
     * Get schedules for all teachers
     * Schedules are combinations of forms names (3 String) where a subject (2 -String) is taught by a teacher (1-Person)
     * We get Map<Person, <Map<String, Set<String>> (Person - for teacher, String -for subjects, Set<String> - for forms)
     */
    @Override
    @Transactional(readOnly = true)
    public Set<Schedule> getSchedules() {
        String ifQuery = "select users.first_name, users.second_name, users.third_name, subject, class_name, users.login from prepod " +
                "left join schedule_and_class on schedule_and_class.schedules_id=prepod.schedule_id inner join users on users.login=prepod.login " +
                "order by users.first_name, subject";


        try {
            Locale locale = new Locale.Builder().setLanguage("uk").setRegion("UA").build();

            List<SchWrapper> srs = getJdbcTemplate().query(ifQuery, (rs, row) -> SchWrapper.wrapperResultSet(rs));

            //prepare stream from result set
            Stream<SchWrapper> srs_iter = srs.stream();


            return srs_iter.limit(srs.size()).
                    collect(
                            Collectors.collectingAndThen(
                                    //first grouping by person
                                    Collectors.groupingBy((s)-> new Person(s.getF_name(), s.getSn_name(),
                                                                            s.getTd_name(), s.getLogin()),
                                            //then grouping by subject to treeset
                                            Collectors.groupingBy(SchWrapper::getSubject,
                                                    //map supplier
                                                    () -> new TreeMap<String, TreeSet<String>>(Collator.getInstance(locale)),
                                                    //mapping to forms
                                                    Collectors.mapping(SchWrapper::getForm,
                                                            Collectors.toCollection(() ->
                                                                    new TreeSet<String>(Collator.getInstance(locale))
                                                            )
                                                    )
                                            )
                                    ),
                                    //"grouping" finished and
                                    //we get Map<Person, <Map<String, Set<String>> (Person - for teacher, String -for subjects, Set<String> - for forms)
                                    //"and then" flatten it. first get stream again
                                    inter_map -> inter_map.entrySet().stream().
                                            map((es) -> new Schedule(es.getKey(), es.getValue())).
                                            collect(Collectors.toCollection(TreeSet::new))
                            )
                    );

        } catch (DataAccessException e) {
            logger.error("Ошибка в базе данных!" + e);
            throw new LogicEception("Ошибка чтения списка расписаний в БД", e.getMessage());
        }
    }


    /**
     * We get all available subject for combinations teacher+subject
     */
    @Override
    @Transactional(readOnly = true)
    public List<Subject> getFreeSubjects(String subject, String prepod) {
        //this and bellow methods work by the help of the following 2 views in the database
        //
        ////(===1===) free_cl_and_subh =====>
        //
        //SELECT x.c1,
        //        x.s1
        //FROM ( SELECT class.class_name AS c1,
        //        subject_list.subject AS s1
        //FROM class,
        //subject_list) x
        //WHERE NOT (x.c1::text || x.s1::text IN ( SELECT schedule_and_class.class_name::text || prepod.subject::text
        //        FROM prepod
        //        JOIN schedule_and_class ON prepod.schedule_id = schedule_and_class.schedules_id)) AND "substring"(x.c1::text, '[\d]+'::text) = "substring"(x.s1::text, '[\d]+'::text)
        //ORDER BY x.c1;
        //
        //
        //(===2===) prepod4cl_and_subh =======>
        //
        //SELECT sl.class_name,
        //        p.subject,
        //        p.login
        //FROM prepod p
        //JOIN schedule_and_class sl ON sl.schedules_id = p.schedule_id;


        String ifQuery = "select subject, array_agg(class_name) from (select class_name, subject  from prepod4cl_and_subh where login=? " +
                "union select * from free_cl_and_subh order by class_name) as z where subject=? group by subject";

        List<String[]> results;
        try {
            results = getJdbcTemplate()

                    .query(ifQuery,
                            (ps) -> {
                                ps.setString(1, prepod);
                                ps.setString(2, subject);
                            },
                            (rs, row) -> {
                                return (String[]) rs.getArray(2).getArray();
                            });
        } catch (DataAccessException e) {
            logger.error("Ошибка в базе данных при чтении: " + prepod + " : " + subject, e);
            throw new LogicEception("Ошибка чтения БД", e.getMessage());
        }

        if (results == null || results.isEmpty()) return new ArrayList<>();
        return Arrays.stream(results.get(0)).map((s) -> new Subject(s)).collect(Collectors.toList());

    }


    /**
     * We mark items in the list of all available subject as checked if a subject is selected for a teacher
     * @param  teacher_login is login of the teacher
     * @param  subject is subject
     * @param  forms is list of forms (form names) chosen for this teacher and subject as a schedule of teacher
     */
    @Override
    @Transactional(readOnly = true)
    public void markSelectedSubjectsInSubgect4PrepodList(String teacher_login, String subject, List<Subject> forms) {
        String ifQuery = "select class_name from prepod inner join schedule_and_class on prepod.schedule_id=schedule_and_class.schedules_id " +
                "where prepod.login=? and prepod.subject=?";

        final List<String> results;
        try {
            results = getJdbcTemplate()

                    .query(ifQuery,
                            (ps) -> {
                                ps.setString(1, teacher_login);
                                ps.setString(2, subject);
                            },
                            (rs, row) -> {
                                return rs.getString(1);
                            });
        } catch (DataAccessException e) {
            logger.error("Ошибка в базе данных при чтении: " + teacher_login, e);
            throw new LogicEception("Ошибка чтения БД", e.getMessage());
        }

        forms.stream().forEach(s -> s.setChecked(results.stream().anyMatch(s.getName()::equals)));
    }



    /**
     * Insert or update student or prepod in the register
     * First insert (update) into users table, then authorities table
     * Then if it was a student then insert him in a form list
     * We use basic Person object for both student and teacher (Student inherits Person and add new fields)
     * And the real type of 'person' parameter shows if it is a student or a teacher
     * @param  update means that we are updating (true) or inserting (when false)
     * @param  person is student or teacher object
     */
    @Override
    @PreAuthorize("hasAuthority('admin')")
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void setPrepodOrStudent(boolean update, Person person) {
        String ifQuery1 = "insert into users  (login, password, enabled, first_name, second_name, third_name) " +
                "values (?, ?, ?, ?, ?, ?)";
        String ifQuery2 = "update users set login=?, password=?, first_name=?, second_name=?, third_name=? where login=?";
        String ifQuery3 = "insert into authorities  (login, authority) values (?, ?)";
        String ifQuery4 = "update authorities set login=? where login=?";
        String ifQuery5 = "insert into class_and_students (class_name, students_login) values (?, ?)";
        String ifQuery6 = "update class_and_students set class_name=?, students_login=? " +
                "where class_name=? and students_login=?";

        //check if we have sent a student
        boolean student = person instanceof Student;

        try {
            if (update) {
                //update
                getJdbcTemplate()
                        .update(ifQuery2,
                                (ps) -> {
                                    ps.setString(1, person.getLogin());
                                    ps.setString(2, person.getPasswd());
                                    ps.setString(3, person.getFst_name());
                                    ps.setString(4, person.getScd_name());
                                    ps.setString(5, person.getThd_name());
                                    ps.setString(6, person.getExlogin());
                                });

                getJdbcTemplate()
                        .update(ifQuery4,
                                (ps) -> {
                                    ps.setString(1, person.getLogin());
                                    ps.setString(2, person.getExlogin());
                                });
                //if we are updating a student then update him to class_and_student list
                if (student) {
                    getJdbcTemplate().update(
                            ifQuery6,
                            (ps) -> {
                                ps.setString(1, ((Student) person).getForm());
                                ps.setString(2, ((Student) person).getLogin());
                                ps.setString(3, ((Student) person).getExform());
                                ps.setString(4, ((Student) person).getExlogin());
                            });
                }

                //insert
            } else {
                //if we are going to put an existing student (login=exlogin) to a new form
                //we are not executing the next block
                if (!person.getExlogin().equals(person.getLogin())) {
                    getJdbcTemplate()
                            .update(ifQuery1,
                                    (ps) -> {
                                        ps.setString(1, person.getLogin());
                                        ps.setString(2, person.getPasswd());
                                        ps.setBoolean(3, true);
                                        ps.setString(4, person.getFst_name());
                                        ps.setString(5, person.getScd_name());
                                        ps.setString(6, person.getThd_name());
                                    });

                    getJdbcTemplate()
                            .update(ifQuery3,
                                    (ps) -> {
                                        ps.setString(1, person.getLogin());
                                        ps.setString(2, student ? "stud" : "prepod");
                                    });
                }
                //if we are adding a student then add him to class_and_student list in any case
                if (student) {
                    getJdbcTemplate().update(
                            ifQuery5,
                            (ps) -> {
                                ps.setString(1, ((Student) person).getForm());
                                ps.setString(2, person.getLogin());
                            });
                }
            }

        } catch (DataAccessException e) {
            if (e.getMessage().contains("users_pkey"))
                throw new LogicEception("Ошибка при вводе логина", "Логин " + person.getLogin() + " уже существует. Выберите новый.");
            logger.error("Ошибка записи в базе данных при редактировании личных данных: " + person.toString(), e);
            if (e.getCause() instanceof PSQLException)
                throw new LogicEception("Ошибка в базе данных при редактировании личных данных: " + person.toString(),
                        ((PSQLException) e.getCause()).getServerErrorMessage().getMessage());
            throw new LogicEception("Ошибка записи в базе данных при редактировании личных данных: " + person.toString(), e.getMessage());
        }

    }


    /**
     * Delete a teacher form users and authorities tables
     * A student to be deleted only from the form list and stays in users and authorities
     * In this method we call supporting 'cleaningAllStudents' which is explained bellow
     * @param  person is student or teacher object to be deleted
     */
    @Override
    @PreAuthorize("hasAuthority('admin')")
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void delPrepodOrStudent(Person person) {
        String ifQuery1 = "delete from users where login=?";
        String ifQuery2 = "delete from authorities where login=?";
        String ifQuery3 = "delete from class_and_students where class_name=? and students_login=?";

        //check if we have sent a student (not teacher)
        boolean student = person instanceof Student;

        try {
            if (!student) {
                getJdbcTemplate().update(ifQuery2, (ps) -> {
                    ps.setString(1, person.getLogin());
                });
                getJdbcTemplate().update(ifQuery1, (ps) -> {
                    ps.setString(1, person.getLogin());
                });
            } else {
                //delete from form
                getJdbcTemplate().update(ifQuery3, (ps) -> {
                    ps.setString(1, ((Student) person).getForm());
                    ps.setString(2, ((Student) person).getLogin());
                });
                //delete orphans
                cleaningAllStudents();
            }

        } catch (DataAccessException e) {
            logger.error("Ошибка в базе данных при удаленнии: " + person.toString(), e);
            if (e.getCause() instanceof PSQLException)
                throw new LogicEception("Ошибка в базе данных при удаленнии: " + person.toString(),
                        ((PSQLException) e.getCause()).getServerErrorMessage().getMessage());
            throw new LogicEception("Ошибка в базе данных при удаленнии: " + person.toString(), e.getMessage());
        }
    }


    /**
     * Delete, update or insert subject in the table 'subjects'
     * @param  del is for delete
     * @param  update is for update
     * @param  subject new subject name
     * @param  exsubject old (to be replaced) subject name
     */
    @Override
    @PreAuthorize("hasAuthority('admin')")
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void editSubjectsList(boolean del, boolean update, String subject, String exsubject) {
        String ifQuery1 = "delete from subject_list where subject=?";
        String ifQuery2 = "insert into subject_list (subject) values (?)";
        String ifQuery3 = "update subject_list set subject=? where subject=?";

        try {
            if (del) {
                getJdbcTemplate().update(ifQuery1, (ps) -> {
                    ps.setString(1, subject);
                });

            } else if (!update) {
                getJdbcTemplate().update(ifQuery2, (ps) -> {
                    ps.setString(1, subject);
                });
            } else {
                getJdbcTemplate().update(ifQuery3, (ps) -> {
                    ps.setString(1, subject);
                    ps.setString(2, exsubject);
                });
            }

        } catch (DataAccessException e) {
            if (e.getMessage().contains("journal_subject_fkey")) throw new LogicEception("Невозможно удалить предмет " + subject,
                    "Он присутсвует в журнале оценок, и его удаление приведет к разрушению журнала");
            logger.error("Ошибка в базе данных при редактировании списка предметов: " + subject, e);
            if (e.getCause() instanceof PSQLException)
                throw new LogicEception("Ошибка в базе данных при редактировании списка предметов", ((PSQLException) e.getCause()).getServerErrorMessage().getMessage());
            throw new LogicEception("Ошибка в базе данных при редактировании списка предметов", e.getMessage() + e.getCause().toString());
        }
    }


    /**
     * Insert schedule which is a combination of a teachers login and subject name
     * @param  login is a teacher
     * @param  subject is a subject name
     */
    @Override
    @PreAuthorize("hasAuthority('admin')")
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void insertSchedule(String login, String subject) {
        String ifQuery = "insert into prepod (subject, login) select ?,? where not exists " +
                "(select login from prepod where subject=? and login=?)";
        try {

            GeneratedKeyHolder holder = new GeneratedKeyHolder();
            getJdbcTemplate().update((con) -> {
                PreparedStatement ps = con.prepareStatement(ifQuery, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, subject);
                ps.setString(2, login);
                ps.setString(3, subject);
                ps.setString(4, login);
                return ps;
            }, holder);

            //get id from updated query
            long id=-1L;
            Map<String, Object> keymap=holder.getKeys();
            if (keymap!=null) for (String s: keymap.keySet()) {id = (long) keymap.get(s);  break;}
            if (id>-1) setupStandartWorkList(id);

        } catch (DataAccessException e) {
            logger.error("Ошибка в базе данных при добавлении расписания: " + subject + " : " + login, e);
            if (e.getCause() instanceof PSQLException)
                throw new LogicEception("Ошибка в базе данных при добавлении расписания", ((PSQLException) e.getCause()).getServerErrorMessage().getMessage());
            throw new LogicEception("Ошибка в базе данных при добавлении расписания", e.getMessage() + e.getCause().toString());
        }
    }

    /**
     * Supporting method which takes a list of tasks from proprety file and
     * Inserts them as standard work types for a newly created schedule (see above)
     * Is called from 'insertSchedule'
     * @param  id is id of a schedule created by 'insertSchedule' method
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void setupStandartWorkList(long id) {
        String ifQuery = "insert into works (sched4w_id, work) select %d, '%s' where not exists " +
                "(select sched4w_id from works where sched4w_id=%d and work='%s')";

        List<String> ifQs = new ArrayList<>();
        try {
            for (String s : WORKS) ifQs.add(String.format(ifQuery, id, s,id, s));
            getJdbcTemplate().batchUpdate(ifQs.toArray(new String[0]));
        } catch (DataAccessException e) {
        logger.error("Ошибка в базе данных при добавлении стандартных работ!", e);
            if (e.getCause() instanceof PSQLException)
                throw new LogicEception("Ошибка в базе данных при добавлении стандартных работ!", ((PSQLException) e.getCause()).getServerErrorMessage().getMessage());
        throw new LogicEception("Ошибка в базе данных при добавлении стандартных работ!", e.getMessage() + e.getCause().toString());
    }
    }


    /**
     * Links a schedule which is a combination of a teachers login and subject name
     * With form names where this teacher lectures this subject
     * Method first clean the existing combinations of schedule and form names in the table 'schedule_and_class'
     * Then insets newly selected by user
     * @param  login is a teacher
     * @param  subject is a subject name
     * @param  forms is a list of the forms
     */
    @Override
    @PreAuthorize("hasAuthority('admin')")
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void updateSchedule(String login, String subject, List<String> forms) {
        String ifQuery1 = "select schedule_id  from  prepod where subject=? and  login=?";
        String ifQuery2 = "delete from  schedule_and_class where schedules_id=?";
        String ifQuery3 = "insert into  schedule_and_class (schedules_id, class_name) values (?,?)";
        try {

            //first get id for combination teacher and subject
            List<Long> result = getJdbcTemplate()
                    .query(ifQuery1, (ps) -> {
                                ps.setString(1, subject);
                                ps.setString(2, login);
                            }, (rs, row) -> {
                                return rs.getLong(1);
                            }
                    );

            if (result.isEmpty()) return;
            Long id = result.get(0);

            //then clean old values and insert new
            getJdbcTemplate().update(ifQuery2, (ps) -> {
                ps.setLong(1, id);
            });

            for (String s : forms)
                getJdbcTemplate().update(ifQuery3, (ps) -> {
                    ps.setLong(1, id);
                    ps.setString(2, s);
                });

        } catch (DataAccessException e) {
            logger.error("Ошибка в базе данных при добавлении расписания: " + subject + " : " + login, e);
            if (e.getCause() instanceof PSQLException)
                throw new LogicEception("Ошибка в базе данных при добавлении расписания", ((PSQLException) e.getCause()).getServerErrorMessage().getMessage());
            throw new LogicEception("Ошибка в базе данных при добавлении расписания", e.getMessage() + e.getCause().toString());
        }
    }


    /**
     * Deletes schedule (teacher and subject combination)
     * @param  login is a teacher
     * @param  subject is a subject name
     */
    @Override
    @PreAuthorize("hasAuthority('admin')")
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void deleteSchedule(String login, String subject) {

        String ifQuery1 = "delete from  prepod where login=? and subject=?";

        try {
            getJdbcTemplate().update(ifQuery1, (ps) -> {
                ps.setString(1, login);
                ps.setString(2, subject);
            });

        } catch (DataAccessException e) {
            logger.error("Ошибка в базе данных при добавлении расписания: " + subject + " : " + login, e);
            if (e.getCause() instanceof PSQLException)
                throw new LogicEception("Ошибка в базе данных при добавлении расписания", ((PSQLException) e.getCause()).getServerErrorMessage().getMessage());
            throw new LogicEception("Ошибка в базе данных при добавлении расписания", e.getMessage() + e.getCause().toString());
        }
    }


    /**
     * Delete all students in the form
     * @param  form is a form's name
     */
    @Override
    @PreAuthorize("hasAuthority('admin')")
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void deleteAllForm(String form) {

        String ifQuery1 = "delete from class_and_students where class_name=?";

        try {
            getJdbcTemplate().update(ifQuery1, (ps) -> {
                ps.setString(1, form);
            });

            //delete orphans
            cleaningAllStudents();

        } catch (DataAccessException e) {
            logger.error("Ошибка в базе данных при удалении класса: " + form, e);
            if (e.getCause() instanceof PSQLException)
                throw new LogicEception("Ошибка в базе данных при удалении класса ", ((PSQLException) e.getCause()).getServerErrorMessage().getMessage());
            throw new LogicEception("Ошибка в базе данных при удалении класса ", e.getMessage() + e.getCause().toString());
        }
    }

    /**
     * Transfer all students from one form to another (form 10a to 11a for example)
     * @param  from_form is from
     * @param  to_form is to
     */
    //self explaining method: the name and sql string tell everything
    @Override
    @PreAuthorize("hasAuthority('admin')")
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void changeAllForm(String from_form, String to_form) {

        String ifQuery1 = "update class_and_students set class_name=? where class_name=?";

        try {
            getJdbcTemplate().update(ifQuery1, (ps) -> {
                ps.setString(1, to_form);
                ps.setString(2, from_form);
            });


        } catch (DataAccessException e) {
            if (e.getMessage().contains("class_and_students_class_name_students_login_key"))
                throw new LogicEception("Ошибка перевода класса", "Класс "
                        + to_form + " уже содерждит учеников класса " + from_form + ". Переводите учеников по одному.");
            logger.error("Ошибка в базе данных при перемещении класса " + from_form, e);
            if (e.getCause() instanceof PSQLException)
                throw new LogicEception("Ошибка в базе данных при перемещении класса ", ((PSQLException) e.getCause()).getServerErrorMessage().getMessage());
            throw new LogicEception("Ошибка в базе данных при перемещении класса ", e.getMessage() + e.getCause().toString());
        }
    }

    /**
     * Supplementary method to clean all orphan and odd records about students.
     * When a student was deleted from a form list he sould be deleted from database totally.
     * But if a student is mentioned in register then in order
     * To prevent the destruction of register he is only disabled for login (but stays in database)
     */
    //self explaining method: the name and sql string tell everything
    @Transactional(propagation = Propagation.MANDATORY)
    public void cleaningAllStudents() {

        String ifQuery2 = "delete from authorities where authority='stud' and login not in" +
                "(select students_login from class_and_students " +
                "union select stud_login from journal)";
        String ifQuery3 = "delete from users where login not in (select login from authorities)";
        //if student is not deleted but disabled
        String ifQuery4 = "update users set enabled=false from authorities as ath where ath.login=users.login " +
                "and ath.authority='stud' and ath.login not in (select students_login from class_and_students)";


        try {
            //after a student is deleted from class_and_students list
            //delete orphans records about students (students not in any form and not in journal)
            getJdbcTemplate().update(ifQuery2);
            getJdbcTemplate().update(ifQuery3);
            //if we have not deleted a student (as his login is referred to from the journal)
            //then block his access to service (enable=false)
            getJdbcTemplate().update(ifQuery4);

        } catch (DataAccessException e) {
            logger.error("Ошибка в базе данных при очистке записей о студентах", e);
            if (e.getCause() instanceof PSQLException)
                throw new LogicEception("Ошибка в базе данных при очистке записей о студентах ", ((PSQLException) e.getCause()).getServerErrorMessage().getMessage());
            throw new LogicEception("Ошибка в базе данных при очистке записей о студентах ", e.getMessage() + e.getCause().toString());
        }
    }


    /**
     * Returns percents of marks in the intervals 12..10,9..7,6..4,3..1
     * @param from specify the form, never empty or null
     * @param  subj specify the form, newer null, if empty then all subject are analyzed
     * @param  topic specify if only topic marks are analyzed or all current marks otherwise
     */
    @Override
    @Transactional(readOnly = true)
    public List<Double> getPersantageMarks(String from, String subj, boolean topic) {

        //this and the next methods work by the help of 3 functions in the database:
        //
        ////(===1===)
        //CREATE OR REPLACE FUNCTION class_aver(class text, subj text, topic_work text)
        //RETURNS TABLE(s_name text, s_mark numeric)
        //LANGUAGE plpgsql
        //AS $function$
        //BEGIN
        //IF topic_work<>'' THEN
        //return query select u.first_name||' '||substring(u.second_name, 1,1)||'.', round(avg(CASE WHEN mark <> 0 THEN mark ELSE NULL END),1) as av from journal
        //inner join
        //users as u on u.login=stud_login where class_name=class and subject=subj and date>get_stud_year_startj()
        // and work=topic_work
        // group by u.login order by av desc;
        //
        //ELSE
        //return query select u.first_name||' '||substring(u.second_name, 1,1)||'.', round(avg(CASE WHEN mark <> 0 THEN mark ELSE NULL END),1) as av from journal
        //inner join
        //users as u on u.login=stud_login where class_name=class and subject=subj and date>get_stud_year_startj() group by u.login order by av desc;
        //END IF;
        //END
        //$function$
        //;
        //



        ////(===2===)
        //CREATE OR REPLACE FUNCTION class_aver_allsubj(class text, topic_work text)
        //RETURNS TABLE(s_name text, s_mark numeric)
        //LANGUAGE plpgsql
        //AS $function$
        //BEGIN
        //IF topic_work<>'' THEN
        //return query select u.first_name||' '||substring(u.second_name, 1,1)||'.', round(avg(CASE WHEN mark <> 0 THEN mark ELSE NULL END),1) as av from journal
        //inner join
        //users as u on u.login=stud_login where class_name=class and date>get_stud_year_startj()
        //and work=topic_work
        // group by u.login order by av desc;
        //
        //ELSE
        //return query select u.first_name||' '||substring(u.second_name, 1,1)||'.', round(avg(CASE WHEN mark <> 0 THEN mark ELSE NULL END),1) as av from journal
        //inner join
        //users as u on u.login=stud_login where class_name=class and date>get_stud_year_startj() group by u.login order by av desc;
        //END IF;
        //END
        //$function$
        //;



        ////(===3===)
        //CREATE OR REPLACE FUNCTION get_stud_year_startj()
        //RETURNS timestamp
        //LANGUAGE plpgsql
        //AS $function$
        //BEGIN
        //return case when (age(now(),  to_timestamp(date_part('year', now())||'-09-01', 'YYYY-MM-DD'))<interval '0') then (to_timestamp(date_part('year', now())||'-09-01', 'YYYY-MM-DD') - interval '1 years') else (to_timestamp(date_part('year', now())||'-09-01', 'YYYY-MM-DD'))  end;
        //END
        //$function$
        //;


        String ifQuery1 = subj.isEmpty() ?

                "with x as (select (CASE WHEN 10<=s_mark and s_mark<=12 THEN 1 ELSE 0 END) as m10_12, " +
                        "(CASE WHEN 7<=s_mark and s_mark<10 THEN 1 ELSE 0 END) as m7_9, " +
                        "(CASE WHEN 4<=s_mark and s_mark<7 THEN 1 ELSE 0 END) as m4_6, " +
                        "(CASE WHEN 0<=s_mark and s_mark<4 THEN 1 ELSE 0 END) as m0_3 from class_aver_allsubj(?,?)), " +
                        "c as (select CASE WHEN count(*)<>0 THEN count(*) ELSE 1 END as n from class_aver_allsubj(?,?) where s_mark NOTNULL) " +
                        "select round(((sum(x.m10_12) over ())/n::numeric), 2) as i10_12, " +
                        "round(((sum(x.m7_9) over ())/n::numeric), 2) as i7_9, " +
                        "round(((sum(x.m4_6) over ())/n::numeric), 2) as i4_6, " +
                        "round(((sum(x.m0_3) over ())/n::numeric), 2) as i0_3  from x,c limit 1"
                :
                "with x as (select (CASE WHEN 10<=s_mark and s_mark<=12 THEN 1 ELSE 0 END) as m10_12, " +
                        "(CASE WHEN 7<=s_mark and s_mark<10 THEN 1 ELSE 0 END) as m7_9, " +
                        "(CASE WHEN 4<=s_mark and s_mark<7 THEN 1 ELSE 0 END) as m4_6, " +
                        "(CASE WHEN 0<=s_mark and s_mark<4 THEN 1 ELSE 0 END) as m0_3 from class_aver(?, ?, ?)), " +
                        "c as (select CASE WHEN count(*)<>0 THEN count(*) ELSE 1 END as n from class_aver(?, ?, ?)  where s_mark NOTNULL) " +
                        "select round(((sum(x.m10_12) over ())/n::numeric), 2) as i10_12, " +
                        "round(((sum(x.m7_9) over ())/n::numeric), 2) as i7_9, " +
                        "round(((sum(x.m4_6) over ())/n::numeric), 2) as i4_6, " +
                        "round(((sum(x.m0_3) over ())/n::numeric), 2) as i0_3  from x,c limit 1";


        List<Double> result;
        try {
            result = getJdbcTemplate().query(ifQuery1, (ps) -> {
                        ps.setString(1, from);
                        if (!subj.isEmpty()) {
                            ps.setString(2, subj);
                            ps.setString(3, topic?TOPIC_WORK:"");
                            ps.setString(4, from);
                            ps.setString(5, subj);
                            ps.setString(6, topic?TOPIC_WORK:"");
                        } else {
                            ps.setString(2, topic?TOPIC_WORK:"");
                            ps.setString(3, from);
                            ps.setString(4, topic?TOPIC_WORK:"");
                        }
                    },
                    (rs) -> {
                        List<Double> results = new ArrayList<>();
                        while (rs.next()) {
                            results.add(rs.getDouble(1));
                            results.add(rs.getDouble(2));
                            results.add(rs.getDouble(3));
                            results.add(rs.getDouble(4));
                        }
                        return results;
                    }
            );
        } catch (DataAccessException e) {
            logger.error("Ошибка в базе данных при чтении процентного бала!", e);
            throw new LogicEception("Ошибка чтения БД  при чтении процентного бала!", e.getMessage());
        }
        return result;
    }



    /**
     * Returns Map of student name and his average mark
     * @param from specify the form, never empty or null
     * @param  subj specify the form, newer null, if empty then all subject are analyzed
     * @param  topic specify if only topic marks are analyzed or all current marks otherwise
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Double> getAverageMarks(String from, String subj, boolean topic) {

        String ifQuery1 = subj.isEmpty() ? "select * from class_aver_allsubj(?,?)" : "select * from class_aver(?,?,?)";


        Locale locale = new Locale.Builder().setLanguage("uk").setRegion("UA").build();
        Map<String, Double> result;
        try {
            result = getJdbcTemplate().query(ifQuery1, (ps) -> {
                        ps.setString(1, from);
                        if (!subj.isEmpty()) {ps.setString(2, subj); ps.setString(3, topic?TOPIC_WORK:"");}
                        else ps.setString(2, topic?TOPIC_WORK:"");
                    },
                    (rs) -> {
                        Map<String, Double> results = new TreeMap<>(Collator.getInstance(locale).reversed());
                        while (rs.next()) {
                            Double sn = rs.getDouble(2);
                            //in case of null (no marks) skip the student
                            if (sn == null || sn == 0) continue;
                            results.put(rs.getString(1), sn);
                        }
                        return results;
                    }
            );
        } catch (DataAccessException e) {
            logger.error("Ошибка в базе данных при чтении среднего бала!", e);
            throw new LogicEception("Ошибка чтения БД  при чтении среднего бала!", e.getMessage());
        }
        return result;
    }


    /**
     * Returns Map of form names and lists of linked subjects
     * For this purpose we are analyzing the register table in database and find
     * which teachers have filled it in
     * @param for_summary defines if we take into account only milestone marks (semesters' and years') or all marks
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, List<String>> getFormsAndSubjects(boolean for_summary) {

        String ifQuery = for_summary ? "select distinct class_name, array_agg(distinct subject) from journal " +
                "where date between get_stud_year_summary_start() and get_stud_year_summary_end()" +
                "group by class_name order by class_name"
                :
                "select distinct class_name, array_agg(distinct subject) from journal " +
                        "where date>get_stud_year_startj()"+
                        "group by class_name order by class_name;";


        Map<String, List<String>> result;
        try {
            result = getJdbcTemplate().query(ifQuery,
                    (rs) -> {
                        Map<String, List<String>> results = new TreeMap<>();
                        while (rs.next()) {
                            results.put(rs.getString(1), Arrays.asList((String[]) rs.getArray(2).getArray()));
                        }
                        return results;
                    }
            );
        } catch (DataAccessException e) {
            logger.error("Ошибка в базе данных при чтении учеников и классов!", e);
            throw new LogicEception("Ошибка в базе данных при чтении учеников и классов!", e.getMessage());
        }
        return result;
    }


    /**
     * Returns Map of students surnames and Map of milestone names (1,2-semesters and year) and marks themselves
     * The result is a Map <MileStone Map <Sername+Name, Mark>>
     * @param form specify the form, never empty or null
     * @param  subject specify the form, newer null or empty
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Map<String, Integer>> getGetSummaryMarks(String form, String subject) {

        //works by the help of functions
        //===(1)
        //CREATE OR REPLACE FUNCTION get_stud_year_summary_end()
        //RETURNS timestamp
        //LANGUAGE plpgsql
        //AS $function$
        //BEGIN
        //  return case when (age(now(),  to_timestamp(date_part('year', now())||'-08-31', 'YYYY-MM-DD'))<interval '0')
        //  then (to_timestamp(date_part('year', now())||'-08-31', 'YYYY-MM-DD') - interval '1 years')
        //  else (to_timestamp(date_part('year', now())||'-08-31', 'YYYY-MM-DD'))  end;
        //END
        //$function$
        //;

        //
        //===(2)
        //CREATE OR REPLACE FUNCTION get_stud_year_summary_start()
        //RETURNS timestamp
        //LANGUAGE plpgsql
        //AS $function$
        //BEGIN
        //  return case when (age(now(),  to_timestamp(date_part('year', now())||'-08-31', 'YYYY-MM-DD'))<interval '0')
        //  then (to_timestamp(date_part('year', now())||'-08-01', 'YYYY-MM-DD') - interval '1 years')
        //  else (to_timestamp(date_part('year', now())||'-08-01', 'YYYY-MM-DD'))  end;
        //END
        //$function$
        //;


        String ifQuery =
                "select first_name, substring(second_name,1,1), mark, work from journal " +
                "inner join users on users.login=journal.stud_login " +
                "where class_name=? and subject=? and date between get_stud_year_summary_start() " +
                "and get_stud_year_summary_end() order by date";


        Locale locale = new Locale.Builder().setLanguage("uk").setRegion("UA").build();
        Map<String, Map<String, Integer>> result;
        try {
            result = getJdbcTemplate().query(ifQuery,
                    (ps) -> {
                        ps.setString(1, form);
                        ps.setString(2, subject);
                    },
                    (rs) -> {
                        Map<String, Map<String, Integer>> resul = new HashMap<>();
                        Set<String> names = new HashSet<>();
                        while (rs.next()) {
                            String work = rs.getString(4);
                            //filter for back compatibility with the previous version
                            //where "(к)" marked works were possible (for corrected marks)
                            if (work == null || work.isEmpty()) continue;
                            if (work.contains("(к)")) continue;
                            resul.putIfAbsent(work, new TreeMap<>(Collator.getInstance(locale).reversed()));
                            Map<String, Integer> map = resul.get(work);
                            String name = rs.getString(1) + " " + rs.getString(2) + ".";
                            names.add(name);
                            map.put(name, rs.getInt(3));
                        }
                        for (String k : resul.keySet()) {
                            for (String n : names) resul.get(k).putIfAbsent(n, 0);
                        }
                        return resul;
                    }
            );
        } catch (DataAccessException e) {
            logger.error("Ошибка в базе данных при чтении итогового бала!", e);
            throw new LogicEception("Ошибка чтения БД  при чтении итогового бала!", e.getMessage());
        }
        return result;
    }



    /**
     * Returns Map of students surnames and Map of milestone names (1,2-semesters and year) and
     * Map of subject names and finaly marks themselves
     * @param form specify the form, never empty or null
     */
    //self explaining method: the name and sql string tell everything
    @Override
    @Transactional(readOnly = true)
    public Map<String, Map<String, Map<String, Integer>>> getGetSummaryMarks(String form) {

        String ifQuery = "select first_name, substring(second_name,1,1), mark, work, subject from journal " +
                        "inner join users on users.login=journal.stud_login " +
                        "where class_name=? and date between get_stud_year_summary_start() " +
                        "and get_stud_year_summary_end() order by date";


        Locale locale = new Locale.Builder().setLanguage("uk").setRegion("UA").build();
        List<MarksWrapper> srs;
        Map<String, Map<String, Map<String, Integer>>> result;

        try {
            srs = getJdbcTemplate().query(ifQuery,
                    (ps) -> {ps.setString(1, form);},
                    (rs) -> {
                            List<MarksWrapper> rslt = new ArrayList<>();
                            while (rs.next()) {
                                MarksWrapper j = MarksWrapper.wrapperResultSet(rs);
                                if (j!=null) rslt.add(j);
                            }
                            return rslt;
                        });

            //processing with streams
            //to Work - Name - Subject - Mark
            result = srs.stream().
                                //filter for back compatibility with the previous version
                                //where "(к)" marked works were possible (for corrected marks)
                                filter(s-> !s.getWork().contains("(к)")).
                                        collect(Collectors.groupingBy(MarksWrapper::getWork,
                                                ()->new TreeMap<>(Collator.getInstance(locale)),
                                                    Collectors.groupingBy(MarksWrapper::getName,
                                                        ()->new TreeMap<>(Collator.getInstance(locale)),
                                                            Collectors.toMap(MarksWrapper::getSubject, MarksWrapper::getMark))));

        } catch (DataAccessException e) {
            logger.error("Ошибка в базе данных при чтении итогового бала  для EXL!", e);
            throw new LogicEception("Ошибка чтения БД  при чтении итогового бала для EXL - ", e.getMessage());
        }
        return result;
    }



    /**
     * Returns Map of 20 best students surnames and their average marks
     * Average marks for all subject are criteria for such a selection
     * The logic is a bit complex, as if a student has less then 30 marks he is rejected
     * And a student which has more marks is rated higher then
     * then one with the same average mark but less number of marks
     * @param topic if only topic marks are analyzed or all current marks otherwise
     */
    //self explaining method: the name and sql string tell everything
    @Override
    @Transactional(readOnly = true)
    public Map<String, Double> getGetTopMarks(boolean topic) {

        //create or replace function top21(topic_work text) returns TABLE(n text, m numeric)
        //LANGUAGE plpgsql
        //as $QQQ$
        //BEGIN
        //IF topic_work<>'' THEN
        //return query
        //with sss1 as
        //        (select stud_login as login, u.first_name||' '
        //         ||substring(u.second_name, 1,1)||'.' as name,
        //         round(avg(CASE WHEN mark <> 0 THEN mark ELSE null END),1) as av,
        //count(mark) as cm from journal
        //inner join users as u on u.login=stud_login
        //where date>get_stud_year_startj() and work=topic_work group by stud_login, u.login),

        //sss2 as
        //(select max(cm)+0.1 as cccm, max(av)+0.1 as xxxm from sss1)

        //select name||' '||cas.class_name as names, av from sss2, sss1
        //inner join class_and_students as cas on cas.students_login=login
        //where (cas.class_name ~ '\d{1,2}[а-я]{1,1}$')=true and av IS NOT NULL
        //order by (7*av/xxxm+cm/cccm) desc limit 20;

        //ELSE
        //return query
        //with sss1 as
        //        (select stud_login as login, u.first_name||' '
        //         ||substring(u.second_name, 1,1)||'.' as name,
        //         round(avg(CASE WHEN mark <> 0 THEN mark ELSE null END),1) as av,
        //count(mark) as cm from journal
        //inner join users as u on u.login=stud_login
        //where date>get_stud_year_startj() group by stud_login, u.login),

        //sss2 as
        //(select max(cm)+0.1 as cccm, max(av)+0.1 as xxxm from sss1 where cm>30)

        //select name||' '||cas.class_name as names, av from sss2, sss1
        //inner join class_and_students as cas on cas.students_login=login
        //where (cas.class_name ~ '\d{1,2}[а-я]{1,1}$')=true and cm>30 and av IS NOT NULL
        //order by (7*av/xxxm+cm/cccm) desc limit 20;

        //END IF;

        //END
        //$QQQ$
        //;


        String iQuery = "select n,m from top21(?)";
        Map<String, Double> result;
        try {
            result = getJdbcTemplate().query(iQuery,
                    (ps) -> {ps.setString(1, topic?TOPIC_WORK:"");},
                    (rs) -> {
                        Map<String, Double> resul = new HashMap<>();
                        while (rs.next()) {
                            resul.put(rs.getString(1), rs.getDouble(2));
                        }
                        return resul;
                    }
            );
        } catch (DataAccessException e) {
            logger.error("Ошибка в базе данных при чтении ТОП бала!", e);
            throw new LogicEception("Ошибка чтения БД  при чтении ТОП бала - ", e.getMessage());
        }
        return result;
    }



    /**
     * Returns Map of students surnames and Map of milestone names (1,2-semesters and year) and marks themselves
     * For the whole school (all forms and subjects)
     * * The result is a Map<Work, Map<Form, Map <Name, Mark>>>>
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Map <String, Map <String, Double>>> getGetAllSummaryMarks() {

        String ifQuery =
                "select class_name, work, first_name||' '|| substring(second_name,1,1)||'.', " +
                        "round(avg(mark), 1) from journal " +
                        "inner join users on users.login=journal.stud_login " +
                        "where date between get_stud_year_summary_start() " +
                        "and get_stud_year_summary_end() " +
                        "group by class_name, work, first_name, second_name " +
                        "order by class_name, first_name";


        Locale locale = new Locale.Builder().setLanguage("uk").setRegion("UA").build();

        try {
            List<STopWrapper> stl = new ArrayList<>();
            getJdbcTemplate().query(ifQuery,
                    (rs) -> {
                        stl.add(STopWrapper.wrapperResultSet(rs));
                    }
            );


            return stl.stream().
                    //filter for back compatibility with the previous version
                    //where "(к)" marked works were possible (for corrected marks)
                    filter(s-> !s.getWork().contains("(к)")).
                            collect(Collectors.groupingBy(STopWrapper::getWork,
                                ()->new TreeMap<>(Collator.getInstance(locale)),
                                    Collectors.groupingBy(STopWrapper::getForm,
                                        Collectors.toMap(STopWrapper::getName,
                                            STopWrapper::getMark, Double::max,
                                                ()->new TreeMap<>(Collator.getInstance(locale))))));

        } catch (DataAccessException e) {
            logger.error("Ошибка в базе данных при чтении всех итоговых балов!", e);
            throw new LogicEception("Ошибка чтения БД  при чтении всех итоговых балов!", e.getMessage());
        }
    }




    //setters
    public void setTOPIC_WORK(String TOPIC_WORK) {this.TOPIC_WORK = TOPIC_WORK;}
    public void setWORKS(List<String> WORKS) {this.WORKS = WORKS;}
}

        //function to clean database
        //create or replace function clean_jn() returns void
        //    as $fun$
        //    begin
        //    with dd as (select d,s,cn from
        //    (select date as d, subject as s, class_name as cn, sum(mark)
        //    as m from journal group by date, subject, class_name) as dd1 where m=0)
        //    delete from journal as j where (j.date, j.class_name, j.subject) in
        //    (select d,cn,s from dd);
        //    end;
        //    $fun$
        //    language plpgsql;

















