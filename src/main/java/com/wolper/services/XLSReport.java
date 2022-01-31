package com.wolper.services;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.view.document.AbstractXlsView;




@Configuration
public class XLSReport {

    @Autowired
    TranslitNencode tnc;

    @Bean(name = "reportEXS1")
    public AbstractXlsView  report1() {


        return new AbstractXlsView() {


            @Override
            protected void buildExcelDocument(Map<String, Object> model, Workbook workbook,
                                              HttpServletRequest request, HttpServletResponse response)  {

                try {

                    //locale
                    Locale locale = new Locale.Builder().setLanguage("uk").setRegion("UA").build();

                    // get from model
                    String formName = (String) model.get("form");
                    Map<String, Map<String, Map<String, Integer>>> data =
                                                    (Map<String, Map<String, Map<String, Integer>>>) model.get("data");

                    // change the file name
                    response.setCharacterEncoding("UTF-8");
                    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                    response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"",
                            tnc.urluencode("Report_"+tnc.transliterate(formName)+".xls")));

                    //check1
                    if (data==null||data.size()==0) return;

                    //all book styling
                    Styler fstyle=new Styler(workbook);

                    //first cycle on works - make sheet for each work
                    for (String work : data.keySet()) {

                        Sheet sheet = workbook.createSheet(work);
                        Map<String, Map<String, Integer>> students = data.get(work);

                        //check2
                        if (students.size() == 0) continue;

                        //pick subject to the list of all ever mentioned subjects
                        Set<String> subjects = students.entrySet().stream()
                                .flatMap(x -> x.getValue().entrySet().stream())
                                .map(x -> x.getKey()).collect(Collectors
                                        .toCollection(() -> new TreeSet<>(Collator
                                                .getInstance(locale))));
                        //check3
                        if (subjects.size() == 0) continue;


                        //sheet stiling+++++++++++++++++++++++++++++++++++++++++++++++++++
                        final float DEFAULT_HEIGHT = sheet.getDefaultRowHeightInPoints();
                        final float HEADER1_HEGHT = 2 * DEFAULT_HEIGHT;
                        final float HEADER2_HEGHT =8 * sheet.getDefaultRowHeightInPoints();
                        //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++


                        //make title header1
                        Row header1 = sheet.createRow(0);
                        header1.setHeightInPoints((HEADER1_HEGHT));
                        sheet.setColumnWidth(0, fstyle.COLUMN_WIDTH0);
                        Cell header1_cell = header1.createCell(0);
                        header1_cell.setCellValue("Класс " + formName + ", " + work);
                        header1_cell.setCellStyle(fstyle.h_style);

                        //merging cells in header styning++++++++++++++++++++++++++++++++++
                        CellRangeAddress cra = new CellRangeAddress(0, 0, 0, subjects.size());
                        sheet.addMergedRegion(cra);
                        RegionUtil.setBorderBottom(BorderStyle.MEDIUM, cra, sheet);
                        RegionUtil.setBorderTop(BorderStyle.MEDIUM, cra, sheet);
                        RegionUtil.setBorderLeft(BorderStyle.MEDIUM, cra, sheet);
                        RegionUtil.setBorderRight(BorderStyle.MEDIUM, cra, sheet);
                        //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

                        //make table head  header2
                        Row header2 = sheet.createRow(1);
                        header2.setHeightInPoints(HEADER2_HEGHT);
                        int i = 1;
                        Cell cell_fio = header2.createCell(0);
                        cell_fio.setCellValue("ФИО");
                        cell_fio.setCellStyle(fstyle.sh_style);
                        //next - internal cycle on subjects
                        for (String subject : subjects) {
                            sheet.setColumnWidth(i, fstyle.COLUMN_WIDTH1);
                            Cell header2_cell = header2.createCell(i++);
                            header2_cell.setCellValue(subject);
                            header2_cell.setCellStyle(fstyle.sh_90_style);
                        }

                        //make table body - rendering cells
                        //last internal cycle on students
                        i = 2;
                        for (String student : students.keySet()) {
                            Row body1 = sheet.createRow(i++);
                            int j = 0;
                            body1.createCell(j++).setCellValue(student);
                            for (String s : subjects) {
                                Cell cell = body1.createCell(j++);
                                Integer mark = students.get(student).get(s);
                                if (mark != null && mark != 0) cell.setCellValue(mark);
                            }
                        }

                    }
                } catch(Exception e) {
                    logger.error("Ошибка подготовки XLS", e);
                    throw new LogicEception("Ошибка подготовки XLS", e.getMessage());
                }
            }
        };
    }


    @Bean(name = "reportEXS2")
    public AbstractXlsView  report2() {
        return new AbstractXlsView() {


            @Override
            protected void buildExcelDocument(Map<String, Object> model, Workbook workbook,
                                              HttpServletRequest request, HttpServletResponse response) throws Exception {

                //locale
                Locale locale = new Locale.Builder().setLanguage("uk").setRegion("UA").build();

                // get from model
                Map<String, Map <String, Map <String, Double>>>  data =
                        (Map<String, Map <String, Map <String, Double>>> ) model.get("data");

                // change the file name
                response.setCharacterEncoding("UTF-8");
                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"",
                                                                                    "topmarks145.xls"));

                //check1
                if (data==null||data.size()==0) return;

                //all book styling
                Styler fstyle=new Styler(workbook);

                //each sheet for each work
                for (String work : data.keySet()) {
                    // create excel xls sheet
                    Sheet sheet = workbook.createSheet(work);

                    //sheet stiling+++++++++++++++++++++++++++++++++++++++++++++++++++
                    final float DEFAULT_HEIGHT = sheet.getDefaultRowHeightInPoints();
                    final float HEADER1_HEGHT = 2 * DEFAULT_HEIGHT;
                    final float HEADER2_HEGHT = 1.5f * sheet.getDefaultRowHeightInPoints();
                    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++


                    //current line
                    int current_sheat_line=0;

                    //make title header1
                    String [] header_string={"ФИО","Класс","Ср.бал"};
                    Row header1 = sheet.createRow(current_sheat_line++);
                    header1.setHeightInPoints((HEADER1_HEGHT));
                    for (int j=0; j<header_string.length; j++) {
                        sheet.setColumnWidth(j, j==0?fstyle.COLUMN_WIDTH0:fstyle.COLUMN_WIDTH1);
                        Cell header1_cell = header1.createCell(j);
                        header1_cell.setCellValue(header_string[j]);
                        header1_cell.setCellStyle(fstyle.h_style);
                    }

                    //extract forms for grouping
                    Map<String, Map<String, Double>> forms = data.get(work);

                    //check2
                    if (forms.size()==0) continue;

                    //total rows (for setting filter)
                    int total_row_count=0;

                    //trick for sorting digit+string form names
                    List<String> forms_list=new ArrayList<>(forms.keySet());
                    Collections.sort(forms_list, new TrickyComparator().thenComparing(d -> d.replaceAll("[\\d-]","")));

                    for (String form : forms_list) {

                        //current line in the sheet
                        int form_header_line=current_sheat_line;

                        //make table head  subheader - header2
                        Row header2 = sheet.createRow(current_sheat_line++);
                        header2.setHeightInPoints(HEADER2_HEGHT);
                        Cell cell_aver = header2.createCell(0);
                        Cell cell_form = header2.createCell(1);
                        Cell cell_form_aver = header2.createCell(2);
                        cell_aver.setCellValue("Ср.бал класса");
                        cell_form.setCellValue(form);
                        cell_aver.setCellStyle(fstyle.sh_noframe_style);
                        cell_form.setCellStyle(fstyle.sh_noframe_style);
                        cell_form_aver.setCellStyle(fstyle.sh_dig_stile);

                        //extract students for grouping
                        Map<String, Double> students = forms.get(form);
                        double aver_mark=0;

                        //check3
                        if (students.size()==0) continue;

                        int count_no_zero=0;
                        for (String student : students.keySet()) {
                            Double mark_me=students.get(student);
                            if (mark_me==null||mark_me==0) continue;
                            Row body = sheet.createRow(current_sheat_line++);
                            body.createCell(0).setCellValue(student);
                            body.createCell(1).setCellValue(form);
                            Cell call_mark = body.createCell(2);
                            call_mark.setCellValue(mark_me);
                            call_mark.setCellStyle(fstyle.s_style);
                            aver_mark+=mark_me;
                            count_no_zero++;
                            total_row_count=call_mark.getRow().getRowNum();
                        }
                        aver_mark=aver_mark/count_no_zero;
                        cell_form_aver.setCellValue(aver_mark);

                        //group the rows together for each form
                        sheet.groupRow(form_header_line+1,current_sheat_line);
                        sheet.setRowGroupCollapsed(form_header_line+1, true);
                        sheet.setAutoFilter(CellRangeAddress.valueOf("B1:B"+(++total_row_count)));
                    }
                }
            }
        };
    }



    class Styler {

        Workbook workbook;
        final int COLUMN_WIDTH0 = 7000;
        final int COLUMN_WIDTH1 = 2500;
        CellStyle h_style, sh_90_style, sh_style, sh_dig_stile, s_style, sh_noframe_style;
        Font font1;

        Styler(Workbook wb){

            workbook=wb;
            font1 = workbook.createFont();
            font1.setBold(true);

            //simple digital cell
            s_style = workbook.createCellStyle();
            s_style.setDataFormat(wb.createDataFormat().getFormat("0.0"));

            //header cell - framed, solid, green, centered
            h_style = workbook.createCellStyle();
            h_style.setFont(font1);
            h_style.setWrapText(true);
            h_style.setBorderBottom(BorderStyle.MEDIUM);
            h_style.setBorderLeft(BorderStyle.MEDIUM);
            h_style.setBorderRight(BorderStyle.MEDIUM);
            h_style.setBorderTop(BorderStyle.MEDIUM);
            h_style.setAlignment(HorizontalAlignment.CENTER);
            h_style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            h_style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            //subheader cell - left aligned, yellow
            sh_style = workbook.createCellStyle();
            sh_style.cloneStyleFrom(h_style);
            sh_style.setVerticalAlignment(VerticalAlignment.BOTTOM);
            sh_style.setAlignment(HorizontalAlignment.LEFT);
            sh_style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            sh_style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            //subheader 90 cell - 90 degrees rotated, right aligned
            sh_90_style = workbook.createCellStyle();
            sh_90_style.cloneStyleFrom(sh_style);
            sh_90_style.setRotation((short) 90);
            sh_90_style.setAlignment(HorizontalAlignment.RIGHT);


            //subheader no frame
            sh_noframe_style = workbook.createCellStyle();
            sh_noframe_style.cloneStyleFrom(sh_style);
            sh_noframe_style.setBorderBottom(BorderStyle.NONE);
            sh_noframe_style.setBorderLeft(BorderStyle.NONE);
            sh_noframe_style.setBorderRight(BorderStyle.NONE);
            sh_noframe_style.setBorderTop(BorderStyle.NONE);


            //subheader digital cell
            sh_dig_stile = workbook.createCellStyle();
            sh_dig_stile.cloneStyleFrom(sh_noframe_style);
            sh_dig_stile.setDataFormat(wb.createDataFormat().getFormat("0.0"));
            sh_dig_stile.setAlignment(HorizontalAlignment.RIGHT);

        }
    }



    class TrickyComparator implements Comparator<String>{
        @Override
        public int compare(String s1, String s2) {
            try {
                Integer res1 = Integer.parseInt(s1.replaceAll("\\D", ""));
                Integer res2 = Integer.parseInt(s2.replaceAll("\\D", ""));
                return res1.compareTo(res2);
            } catch (NumberFormatException n) {return 0;}
        }
    }
}