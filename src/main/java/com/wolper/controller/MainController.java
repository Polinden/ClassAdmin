package com.wolper.controller;


import com.wolper.domain.Person;
import com.wolper.domain.ScheduleWebForm;
import com.wolper.domain.Student;
import com.wolper.services.CapchaService;
import com.wolper.services.LogicEception;
import com.wolper.services.MailService;
import com.wolper.services.RegisterServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.*;


@Controller
public class MainController {


    @Autowired
    RegisterServices jrDAO;

    @Autowired
    MailService mailsv;

    @Autowired
    CapchaService capchasv;


    Logger logger = LoggerFactory.getLogger(MainController.class);


    //start and exit page
    @RequestMapping(value="/")
    public String exitPage(HttpSession session, HttpServletResponse res) {
	res.setHeader("Cache-Control", "no-cache, no-store");
        session.invalidate();
        return "redirect: /goout";
    }


    //start menu
    @RequestMapping(value="/admin/startpage")
    public String startPage(HttpServletResponse res) {
        res.setHeader("Cache-Control", "no-cache, no-store");
        return "startpage.html";
    }


    //subjects_page
    @RequestMapping(value="/admin/subjects_page")
    public ModelAndView subjectsPage(HttpServletResponse res) {
        res.setHeader("Cache-Control", "no-cache, no-store");
        ModelAndView mv = new ModelAndView("subjects_page.html");
        Map<String, Object> map = new HashMap();
        map.put("prepods", jrDAO.getAllPrepods());
        map.put("subjs", jrDAO.getAllSubjects());
        mv.addAllObjects(map);
        return mv;
    }


    //prepod_page
    @RequestMapping(value="/admin/prepod_page")
    public ModelAndView prepodPage(HttpServletResponse res) {
        res.setHeader("Cache-Control", "no-cache, no-store");
        ModelAndView mv = new ModelAndView("prepod_page.html");
        Map<String, Object> map = new HashMap();
        map.put("prepods", jrDAO.getAllPrepods());
        map.put("subjs", jrDAO.getAllSubjects());
        mv.addAllObjects(map);
        return mv;
    }


    //schedule_page
    @RequestMapping(value="/admin/schedule_page")
    public ModelAndView schedulePage(HttpServletRequest request, HttpServletResponse res, @ModelAttribute("my_form") ScheduleWebForm fs) {
        res.setHeader("Cache-Control", "no-cache, no-store");
        ModelAndView mv = new ModelAndView("schedule_page.html");
        String login= fs.getLogin();
        //if starting the first time (not redirect) then login=''
        if (login.length()==0 && (fs.getTeacher().length()!=0 || fs.getSubject().length()!=0)) {
            logger.info("Ошибочно выбран логин для преподавателя"+fs.getTeacher()+fs.getSubject());
            throw new LogicEception("Ошибочно выбран логин для преподавателя ", fs.getTeacher()+fs.getSubject());
        }
        fs.setFormes(jrDAO.getFreeSubjects(fs.getSubject(), login));
        jrDAO.markSelectedSubjectsInSubgect4PrepodList(login, fs.getSubject(), fs.getFormes());
        mv.addObject("schedules", jrDAO.getSchedules());
        return mv;
    }


    //student_page
    @RequestMapping(value="/admin/student_page")
    public ModelAndView studentPage(HttpServletResponse res) {
        res.setHeader("Cache-Control", "no-cache, no-store");
        ModelAndView mv = new ModelAndView("student_page.html");
        mv.addObject("students", jrDAO.getAllStudents());
        mv.addObject("classes", jrDAO.getClasses());
        mv.addObject("person", new Student());
        return mv;
    }


    //get teacher schedule info for prepod page
    @RequestMapping(value="/get_class_list", method = RequestMethod.POST)
    public RedirectView getClassList(HttpServletResponse res, @ModelAttribute("my_form") ScheduleWebForm fs, RedirectAttributes redirectAttributes) {
        res.setHeader("Cache-Control", "no-cache, no-store");
        redirectAttributes.addFlashAttribute( "my_form", fs);
        RedirectView redirectView = new RedirectView("/admin/schedule_page", true);
        return redirectView;
    }


    //set teacher schedule
    @RequestMapping(value="/upd_class_list", method = RequestMethod.POST)
    public RedirectView setClassList(HttpServletResponse res, @ModelAttribute("my_form") ScheduleWebForm fs, RedirectAttributes redirectAttributes) {
        res.setHeader("Cache-Control", "no-cache, no-store");
        //check
        if (fs.getLogin().length()==0 || fs.getSubject().length()==0) {
            logger.info("Ошибочно заполнено расписание для преподавателя "+fs.getTeacher()+fs.getSubject());
            throw new LogicEception("Ошибочно заполнено расписание для преподавателя ", fs.getTeacher()+fs.getSubject());
        }
        //if the list of checked forms was cleaned we send empty list to the DAO
        if (fs.getNewly_checked()==null) fs.setNewly_checked(new ArrayList<String>());
        //update
        jrDAO.updateSchedule(fs.getLogin(), fs.getSubject(), fs.getNewly_checked());
        //redirect
        fs.setNewly_checked(null);
        redirectAttributes.addFlashAttribute( "my_form", fs);
        RedirectView redirectView = new RedirectView("/admin/schedule_page", true);
        return redirectView;
    }


    //delete teacher schedule
    @RequestMapping(value="/del_class_list", method = RequestMethod.POST)
    public RedirectView delClassList(HttpServletResponse res, @ModelAttribute("my_form") ScheduleWebForm fs, RedirectAttributes redirectAttributes) {
        res.setHeader("Cache-Control", "no-cache, no-store");
        //delete
        jrDAO.deleteSchedule(fs.getLogin(), fs.getSubject());
        //redirect
        RedirectView redirectView = new RedirectView("/admin/schedule_page", true);
        return redirectView;
    }


    //add teacher schedule the 1st case (from subjects page)
    @RequestMapping(value="/add_subj_list", method = RequestMethod.POST)
    public RedirectView addSubjectList(HttpServletResponse res, @RequestParam(value="subject") String subject,
                                     @RequestParam(value="selected") String selected, RedirectAttributes redirectAttributes) {

        res.setHeader("Cache-Control", "no-cache, no-store");
        ScheduleWebForm fs = new ScheduleWebForm();
        StringTokenizer st = new StringTokenizer(selected,"!&++&!");
        List<String> strl = new ArrayList();
        //pick login and teacher's name out of the coded string containing the both separated by !&++&!
        while (st.hasMoreElements()) strl.add(st.nextToken());
        //check
        if (strl.size()<2 || subject.length()==0) {
            logger.info("Ошибочно заполнены логин, имя и предмет выбранного преподавателя "+selected+" "+subject);
            throw new LogicEception("Ошибочно заполнены логин, имя и предмет выбранного преподавателя ", selected+" "+subject);
        }
        //insert subject and login in schedule list
        jrDAO.insertSchedule(strl.get(0), subject);
        //redirect
        fs.setLogin(strl.get(0)); fs.setTeacher(strl.get(1)); fs.setSubject(subject);
        redirectAttributes.addFlashAttribute( "my_form", fs);
        RedirectView redirectView = new RedirectView("/admin/schedule_page", true);
        return redirectView;
    }



    //add teacher schedule the 2nd case (from teachers page)
    @RequestMapping(value="/add_teach_list", method = RequestMethod.POST)
    public RedirectView addTeachList(HttpServletResponse res, @RequestParam(value="login") String login, @RequestParam(value="fst_name") String surname,
                                     @RequestParam(value="scd_name") String name, @RequestParam(value="thd_name") String fname,
                                     @RequestParam(value="selected") String selected, RedirectAttributes redirectAttributes) {

        res.setHeader("Cache-Control", "no-cache, no-store");
        ScheduleWebForm fs = new ScheduleWebForm();
        //check
        if (login.length()==0 || selected.length()==0 || surname.length()==0) {
            logger.info("Ошибочно заполнены логин, имя и предмет выбранного преподавателя "+selected+" "+login+" "+surname);
            throw new LogicEception("Ошибочно заполнено логин, имя и предмет выбранного преподавателя ", selected+" "+login+" "+surname);
        }
        //insert subject and login in schedule list
        jrDAO.insertSchedule(login, selected);
        fs.setLogin(login); fs.setTeacher(surname+" "+name+" "+fname); fs.setSubject(selected);
        redirectAttributes.addFlashAttribute( "my_form", fs);
        RedirectView redirectView = new RedirectView("/admin/schedule_page", true);
        return redirectView;
    }



    //insert student
    @RequestMapping(value="/ins_student_list", method = RequestMethod.POST)
    public RedirectView insStudentList(HttpServletResponse res, @Valid @ModelAttribute("my_form") Student person, BindingResult br, RedirectAttributes ra) {
        res.setHeader("Cache-Control", "no-cache, no-store");
        //check
        if (br.hasErrors()) {
            logger.info("ФИО студента передано с ошибкой "+person.toString());
            //List<FieldError> errors = br.getFieldErrors();
            //for (FieldError error : errors ) logger.info(error.getObjectName() + " - " + error.getDefaultMessage());
            throw new LogicEception("ФИО студента передано с ошибкой ", person.toString());
        }
        //save
        jrDAO.setPrepodOrStudent(false, person);
        //redirect
        ra.addFlashAttribute("revertto1", person.getLogin());
        ra.addFlashAttribute("revertto2", person.getForm());
        RedirectView redirectView = new RedirectView("/admin/student_page", true);
        return redirectView;
    }



    //replace student
    @RequestMapping(value="/upd_student_list", method = RequestMethod.POST)
    public RedirectView updStudentList(HttpServletResponse res, @Valid @ModelAttribute("my_form") Student person, BindingResult br, RedirectAttributes ra) {
        res.setHeader("Cache-Control", "no-cache, no-store");
        //check
        if (br.hasErrors()) {
            logger.info("ФИО студента передано с ошибкой "+person.toString());
            throw new LogicEception("ФИО студента передано с ошибкой ", person.toString());
        }
        //save
        jrDAO.setPrepodOrStudent(true, person);
        //redirect
        ra.addFlashAttribute("revertto1", person.getLogin());
        ra.addFlashAttribute("revertto2", person.getForm());
        RedirectView redirectView = new RedirectView("/admin/student_page", true);
        return redirectView;
    }



    //insert teacher
    @RequestMapping(value="/ins_teach_list", method = RequestMethod.POST)
    public RedirectView insSTeacherList(HttpServletResponse res, @Valid @ModelAttribute("my_form") Person person, BindingResult br, RedirectAttributes ra) {
        res.setHeader("Cache-Control", "no-cache, no-store");
        //check
        if (br.hasErrors()) {
            logger.info("ФИО студента передано с ошибкой "+person.toString());
            throw new LogicEception("ФИО преподавателя передано с ошибкой ", person.toString());
        }
        //save
        jrDAO.setPrepodOrStudent(false, person);
        //redirect
        ra.addFlashAttribute("revertto", person.getLogin());
        RedirectView redirectView = new RedirectView("/admin/prepod_page", true);
        return redirectView;
    }



    //replace teacher
    @RequestMapping(value="/upd_teach_list", method = RequestMethod.POST)
    public RedirectView updTeacherList(HttpServletResponse res, @Valid @ModelAttribute("my_form") Person person, BindingResult br, RedirectAttributes ra) {
        res.setHeader("Cache-Control", "no-cache, no-store");
        //check
        if (br.hasErrors()) {
            logger.info("ФИО студента передано с ошибкой "+person.toString());
            throw new LogicEception("ФИО преподавателя передано с ошибкой ", person.toString());
        }
        //save
        jrDAO.setPrepodOrStudent(true, person);
        //redirect
        ra.addFlashAttribute("revertto", person.getLogin());
        RedirectView redirectView = new RedirectView("/admin/prepod_page", true);
        return redirectView;
    }



    //delete student
    @RequestMapping(value="/del_student_list", method = RequestMethod.POST)
    public RedirectView deldStudentList(HttpServletResponse res, @ModelAttribute("my_form") Student person) {
        res.setHeader("Cache-Control", "no-cache, no-store");
        //delete
        jrDAO.delPrepodOrStudent(person);
        RedirectView redirectView = new RedirectView("/admin/student_page", true);
        return redirectView;
    }



    //delete teacher
    @RequestMapping(value="/del_teach_list", method = RequestMethod.POST)
    public RedirectView delTeacherList(HttpServletResponse res, @ModelAttribute("my_form") Person person) {
        res.setHeader("Cache-Control", "no-cache, no-store");
        //delete
        jrDAO.delPrepodOrStudent(person);
        RedirectView redirectView = new RedirectView("/admin/prepod_page", true);
        return redirectView;
    }


    //add subject
    @RequestMapping(value="/ins_subj_list", method = RequestMethod.POST)
    public RedirectView addFormsList(HttpServletResponse res, @RequestParam(value="subject") String subject, @RequestParam(value="exsubject") String exsubject, RedirectAttributes ra) {
        res.setHeader("Cache-Control", "no-cache, no-store");
        //add
        jrDAO.editSubjectsList(false, false, subject, exsubject);
        //redirect
        ra.addFlashAttribute("revertto", subject);
        RedirectView redirectView = new RedirectView("/admin/subjects_page", true);
        return redirectView;
    }



    //del subject
    @RequestMapping(value="/del_subj_list", method = RequestMethod.POST)
    public RedirectView delFormsList(HttpServletResponse res, @RequestParam(value="subject") String subject, @RequestParam(value="exsubject") String exsubject) {
        res.setHeader("Cache-Control", "no-cache, no-store");
        //delete
        jrDAO.editSubjectsList(true, false, subject, exsubject);
        RedirectView redirectView = new RedirectView("/admin/subjects_page", true);
        return redirectView;
    }


    //update subject
    @RequestMapping(value="/upd_subj_list", method = RequestMethod.POST)
    public RedirectView updFormsList(HttpServletResponse res, @RequestParam(value="subject") String subject, @RequestParam(value="exsubject") String exsubject, RedirectAttributes ra) {
        res.setHeader("Cache-Control", "no-cache, no-store");
        //update
        jrDAO.editSubjectsList(false, true, subject, exsubject);
        RedirectView redirectView = new RedirectView("/admin/subjects_page", true);
        //redirect
        ra.addFlashAttribute("revertto", subject);
        return redirectView;
    }


    //delete a whole form
    @RequestMapping(value="/del_all_form", method = RequestMethod.POST)
    public RedirectView delAllForm(HttpServletResponse res, @ModelAttribute("my_form") Student person) {
        res.setHeader("Cache-Control", "no-cache, no-store");
        //delete
        jrDAO.deleteAllForm(person.getForm());
        RedirectView redirectView = new RedirectView("/admin/student_page", true);
        //redirect
        return redirectView;
    }


    //shift a whole form
    @RequestMapping(value="/ch_all_form", method = RequestMethod.POST)
    public RedirectView chngAllForm(HttpServletResponse res, @ModelAttribute("my_form") Student person, @RequestParam("selected") String new_form) {
        res.setHeader("Cache-Control", "no-cache, no-store");
        //change
        jrDAO.changeAllForm(person.getForm(), new_form);
        RedirectView redirectView = new RedirectView("/admin/student_page", true);
        //redirect
        return redirectView;
    }


    //benchmarks - average marks and percentage marks
    @RequestMapping(value="/admin/benchmarks/{type}", method = RequestMethod.GET)
    public ModelAndView benchmarks(HttpServletResponse res, @PathVariable String type, @RequestParam(value = "sel_form", required = false) String sf,
                                   @RequestParam(value = "sel_subj", required = false) String ss, @RequestParam(value = "ch_topic", required = false) String ch_top) {
        res.setHeader("Cache-Control", "no-cache, no-store");
        ModelAndView mv = new ModelAndView();
        //query for list of forms and their subjects and send it to web page
        Map<String, List<String>> classList = jrDAO.getFormsAndSubjects(false);
        mv.addObject("from_and_subjects", classList);
        //if initially call and no subject selected set 'All subjects' and willingly set a selected form
        if (sf==null) {for (String s : classList.keySet()) {sf=s; break;}}
        if (ss==null || ss.equals("Все предметы")) ss="";
        //empty class list error
        if (sf==null) throw new LogicEception("Нет информации для отображения!", ":В журнале нет текущих оценок!");
        //check if topic or current marks are required
        boolean if_top=ch_top!=null&&ch_top.equals("topic");
        //setup checkbox data if topic marks were selected
        mv.addObject("ch_topic", if_top);
        //set form and subject fields for select-option elements on web page
        mv.addObject("sform", sf);
        mv.addObject("ssubj", ss.isEmpty()?"Все предметы":ss);
        //finally read the data for a charts
        switch (type) {
        case "average":     mv.addObject("mymap", jrDAO.getAverageMarks(sf, ss, if_top));
                            mv.setViewName("benchmark1.html");
                            break;
        case "percentage":  mv.addObject("mymap", jrDAO.getPersantageMarks(sf, ss, if_top));
                            mv.setViewName("benchmark2.html");
                            break;
        }
        return mv;
    }


    //summaries - summary marks
    @RequestMapping(value="/admin/summaries/{type}", method = RequestMethod.GET)
    public ModelAndView summuries(HttpServletResponse res, @PathVariable String type, @RequestParam(value = "sel_form", required = false) String sf,
                                   @RequestParam(value = "sel_subj", required = false) String ss) {
        res.setHeader("Cache-Control", "no-cache, no-store");
        ModelAndView mv = new ModelAndView();
        Map<String, List<String>> classList = jrDAO.getFormsAndSubjects(true);
        mv.addObject("from_and_subjects", classList);
        //if initially call and no subject selected willingly set a form and subject
        if (sf==null || ss==null) {
            for (String s : classList.keySet()) {sf=s; break;}
            //empty class list for summary marks error
            if (sf==null) throw new LogicEception("Нет информации для отображения!", "Журнал оценок еще не содержит итогов текущего года!");
            ss=classList.get(sf).get(0)!=null?classList.get(sf).get(0):"";
        }
        //setup checkbox data if topic marks were selected
        mv.addObject("sform", sf);
        mv.addObject("ssubj", ss);
        //get data for chart or table
        mv.addObject("mymap", jrDAO.getGetSummaryMarks(sf, ss));
        //select view depending on input (chart or table)
        switch (type) {
            case "chart": mv.setViewName("summaries1.html");
                          break;
            case "table": mv.setViewName("summaries2.html");
                          //we are insisting on using current marks in th table of a form summary marks
                          mv.addObject("aver", jrDAO.getAverageMarks(sf, ss, false));
                          break;
        }
        return mv;
    }



    //xls Report 1
    @RequestMapping(value="/get_XLS_top", method = RequestMethod.GET)
    public String topEXS(HttpServletResponse res, Model model) {
        res.setHeader("Cache-Control", "no-cache, no-store");
        model.addAttribute("data", jrDAO.getGetAllSummaryMarks());
        return "reportEXS2";
    }


    //xls Report 2
    @RequestMapping(value="/get_XLS_sum", method = RequestMethod.GET)
    public String sumEXS(HttpServletResponse res, Model model, @RequestParam(value = "sel_form", required = false) String sf) {
        model.addAttribute("form", sf);
        model.addAttribute("data", jrDAO.getGetSummaryMarks(sf));
        res.setHeader("Cache-Control", "no-cache, no-store");
        return "reportEXS1";
    }


    //top - top20
    @RequestMapping(value="/admin/top", method = RequestMethod.GET)
    public ModelAndView top20(HttpServletResponse res, @RequestParam(value="top") String top) {
        res.setHeader("Cache-Control", "no-cache, no-store");
        ModelAndView mv = new ModelAndView();
        //check if a user selected topic or current marks to be analysed
        mv.addObject("top", jrDAO.getGetTopMarks(top.equals("topic")));
        mv.setViewName("summaries3.html");
        return mv;
    }


    //pass recovery
    @RequestMapping(value="/public/recoverypass", method = RequestMethod.POST)
    public String passrecovery(@RequestParam(value="mail") String to, @RequestParam(value="name") String name,
                               @RequestParam(value="form") String form,
                               @RequestParam(value="prepod", required = false) Boolean prepod,
                               @RequestParam(name="g-recaptcha-response") String recaptchaResponse,
                               HttpServletRequest request) {
        if (capchasv.verifyRecaptcha(request.getRemoteAddr(), recaptchaResponse)) {
              mailsv.sendMessage(to, name, form, prepod!=null?prepod:false);
        }
        else throw new LogicEception("Повторите ввод!", "Ошибка ввода Captcha!");
        return "redirect: ../goout";
    }


   //dashboard
    @RequestMapping(value="/admin/dashboard", method = RequestMethod.GET)
    public ModelAndView top20(HttpServletResponse res) {
        res.setHeader("Cache-Control", "no-cache, no-store");
        ModelAndView mv = new ModelAndView();
        //check if a user selected topic or current marks to be analysed
        mv.setViewName("dashboard.html");
        return mv;
    }





}



//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//Exceptions handler. Logic exception is a wrapper
@ControllerAdvice
class GlobalControllerExceptionHandler {
    @ExceptionHandler(LogicEception.class)
    public ModelAndView forbiden(LogicEception ex) {
        ModelAndView modelAndView = new ModelAndView("exception.html");
        modelAndView.addObject("errCode", ex.getErrCode());
        modelAndView.addObject("errMsg", ex.getErrMsg());
        return modelAndView;
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView prohibeted(AccessDeniedException ex) {
        ModelAndView modelAndView = new ModelAndView("exception.html");
        modelAndView.addObject("errCode", "Доступ запрещен");
        modelAndView.addObject("errMsg", "Вашему логину разрешено только просматривать информацию");
        return modelAndView;
    }
}
